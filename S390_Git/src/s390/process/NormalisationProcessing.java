/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import ps.db.SQL;
import ps.util.Report;
import ps.util.SupplierX;
import s390.Quasar;
import s390.Runner;
import s390.normalisation.NormalisationRange;
import s390.normalisation.QuasarAverageFlux;
import s390.sdss.spec.SpectrumDbReader;

/**
 * TODO:
 * 
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class NormalisationProcessing implements Runnable {

	/**
	 * Main entry point
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {
		Runner.run(NormalisationProcessing.class);
	}
	
	
	/**
	 * SQL for database manipulation
	 */
	@Inject SQL sql;
	
	
	/**
	 * Spectrum database
	 */
	@Inject SpectrumDbReader specDb;
	
	
	/**
	 * Diagnostic reporting
	 */
	@Inject Report report;
	
	
	/**
	 * Parameter: output directory.
	 */
	@Inject @Named("dirOut") File dirOut;
	

	/**
	 * Main program
	 */
	@Override
	public void run() {

		report.info("Starting...");

		File fSerial =  new File(dirOut, "QuasarAverageFlux.dat");
		
		// Create list of quasar average fluxes for all non-BAL quasars
		
		// Uncomment to read list from database
		/*
		List<QuasarAverageFlux> listQuasarFlux = QuasarAverageFlux
				.getQuasarAverageFluxes(sql, specDb, report);

		serializeListQuasarFlux(listQuasarFlux, fSerial);
		*/
		
		// Uncomment to read list from previously serialised version
		List<QuasarAverageFlux> listQuasarFlux = deserializeListQuasarFlux(fSerial);
		
		report.info("%s non-BAL quasars", listQuasarFlux.size());

		report.info("writeOutListQuasarFlux()...");
		writeOutListQuasarFlux(listQuasarFlux);
					
		report.info("sortByLuminosity()...");
		sortByLuminosity(listQuasarFlux);
		
		report.info("...Finished");

	}


	/**
	 * Serialize the list of quasar average fluxes to a file.
	 * 
	 * @param listQuasarFlux
	 *            A list of quasar average fluxes for all non-BAL quasars,
	 *            sorted by quasar object id.
	 * @param fSerial
	 *            file to serialize list to
	 */
	void serializeListQuasarFlux(List<QuasarAverageFlux> listQuasarFlux, File fSerial) {

		try {
			
			ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(fSerial));
			os.writeObject(listQuasarFlux);
			os.close();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	
	/**
	 * Deserialize the list of quasar average fluxes from a file.
	 * 
	 * @param fSerial
	 *            file to serialize list to
	 * @return A list of quasar average fluxes for all non-BAL quasars, sorted
	 *         by quasar object id.
	 */
	@SuppressWarnings("unchecked")
	List<QuasarAverageFlux> deserializeListQuasarFlux(File fSerial) {

		try {
			
			ObjectInputStream is = new ObjectInputStream(new FileInputStream(fSerial));
			Object obj = is.readObject();
			is.close();
			return (List<QuasarAverageFlux>)obj;
			
		} catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	
	/**
	 * Write the list of quasar average fluxes to a file.
	 * 
	 * @param listQuasarFlux
	 *            A list of quasar average fluxes for all non-BAL quasars,
	 *            sorted by quasar object id.
	 */
	void writeOutListQuasarFlux(List<QuasarAverageFlux> listQuasarFlux) {

		// Output the results
		PrintWriter pw = SupplierX.get(() -> new PrintWriter(new File(dirOut,
				"QuasarAverageFlux.csv")));
		pw.printf("z,mid-z,M,a1,a2,a3,a4,a5\n");

		for (QuasarAverageFlux ns : listQuasarFlux) {
			Quasar q = ns.getQuasar();
			double z = q.getRedshift();
			double m = q.getAbsoluteMagnitude();
			NormalisationRange nr = NormalisationRange.getNormalisation(z);
			double mz = nr == null? -1 : nr.getMidZ();
			pw.printf("%s,%s,%s",z,mz,m);
			for (double d : ns.getAvgRange()) {
				pw.printf(",%s",d);
			}
			pw.println();
		}
		pw.close();
		
	}
	
	
	/**
	 * Write the list of quasar average flux ratios by luminosity.
	 * 
	 * @param listQuasarFlux
	 *            A list of quasar average fluxes for all non-BAL quasars,
	 *            sorted by quasar object id.
	 */
	void sortByLuminosity(List<QuasarAverageFlux> listQuasarFlux) {

		// Declare a comparator to sort quasars by luminosity
		Comparator<Quasar> cmpQuasarLuminosity = 
				Comparator.comparing(Quasar::getAbsoluteMagnitude);
		
		// Use first comparator to construct comparator of QuasarAverageFlux
		Comparator<QuasarAverageFlux> cmpLuminosity = 
				Comparator.comparing(QuasarAverageFlux::getQuasar, cmpQuasarLuminosity);
				
		// Declare a grouping function to bin quasars in luminosity increments of 0.2, i.e. floor(lum * 5)/5
		Function<QuasarAverageFlux, Double> groupByLuminosity =
				q -> ((Math.floor(q.getQuasar().getAbsoluteMagnitude()*5))/5);

		// Produce a map of QuasarAverageFlux binned by luminosity.
		Map<Double, List<QuasarAverageFlux>> quasarsByLuminosity = listQuasarFlux
				.stream().sorted(cmpLuminosity)
				.collect(Collectors.groupingBy(groupByLuminosity));		
		
		// Get a sorted list of the magnitude bins.
		List<Double> listMag = quasarsByLuminosity.keySet().stream().sorted()
				.collect(Collectors.toList());
		
		// Output the results
		PrintWriter pw = SupplierX.get(() -> new PrintWriter(new File(dirOut,
				"QuasarFluxRatios.csv")));

		pw.println("Mag,n,in2,ir2,n2,r2/r1,in3,ir3,n3,r3/r2,in4,ir4,n4,r4/r3,in5,ir5,n5,r5/r4");
		for (Double d : listMag) {
			
			int numQ = quasarsByLuminosity.get(d).size();
			if (numQ < 1000) {
				// Ignore bins with less than 1000 quasars
				continue;
			}
			pw.printf("%s,%s", d, numQ);
			
			int nrange = NormalisationRange.getList().size();
			for (int i = 1; i < nrange; i++) {
				List<Double> rats = new ArrayList<Double>(quasarsByLuminosity.size());
				for (QuasarAverageFlux ns : quasarsByLuminosity.get(d)) {
					double avgRange[] = ns.getAvgRange();
					if ((avgRange[i] > 0) && (avgRange[i - 1] > 0)) {
						double rat = avgRange[i] / avgRange[i - 1];
						rats.add(rat);
					}
				}
				getAvg2SD(rats, pw);
			}
			pw.println();
		}
		
		pw.close();
		
	}
	
	
	/**
	 * Average of values excluding outliers. Take the geometric mean of all the
	 * values. Then eliminate those that are more than two standard deviations
	 * from the mean, and take the mean of the remainder.
	 * 
	 * @param list
	 *            list of values
	 * @return geometric mean of all values with two standard deviations of the
	 *         geometric mean
	 */
	private double getAvg2SD(List<Double> listx, PrintWriter pw) {

		// Map the list of values to their logs
		List<Double> list = listx.stream().map(x -> Math.log(x)).collect(Collectors.toList());
		
		
		int inputListSize = list.size();
		if (inputListSize == 0) {
			// No inputs - average is zero
			pw.printf(",0,-1,0,-1");
			return 0;
		}
		
		// Get the mean of the input values. Since we are working with
		// logs, this is actually the geometric mean.
		final double avg = list.stream().mapToDouble(x -> x).average()
				.getAsDouble();
		
		// The variance is the average of the squares of differences from the mean
		final double variance = list.stream()
				.mapToDouble(x -> Math.pow(x - avg, 2)).average().getAsDouble();
		
		// Standard deviation is the square root of the variance.
		final double stddev = Math.sqrt(variance);
		
		// Filter out everything more than two stddevs from mean.
		DoubleSummaryStatistics stats = list.stream()
				.filter(x -> Math.abs(x - avg) < (2 * stddev))
				.collect(Collectors.summarizingDouble(x -> x));
		
		// See how many are left, and take their average
		int outputListSize = (int)stats.getCount();
		double newAvg = stats.getAverage();
		
		pw.printf(",%s,%s,%s,%s", inputListSize, Math.exp(avg), outputListSize, Math.exp(newAvg));
		
		// Take the antilog to get back to original value domain
		return Math.exp(newAvg);
		
	}

	
}

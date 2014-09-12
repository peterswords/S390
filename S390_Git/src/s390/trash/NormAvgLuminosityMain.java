/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.trash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import ps.db.SQL;
import ps.util.MainModule;
import ps.util.Report;
import s390.Cosmology;
import s390.CommonDependencies;
import s390.Quasar;
import s390.composite.IntermediateSpectrum;
import s390.normalisation.NormalisationRange;
import s390.normalisation.QuasarAverageFlux;
import s390.sdss.spec.SpectrumDbReader;

/**
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class NormAvgLuminosityMain implements Runnable {

	public static void main(String[] args) {
		MainModule.run(NormAvgLuminosityMain.class, new CommonDependencies());
	}
	
	@Inject SQL sql;
	@Inject SpectrumDbReader specIndex;
	@Inject Cosmology cosmology;
	Report report = new Report();
	
	@Override
	public void run() {

		doNormalisation();
		
	}


	void doNormalisation() {
		String qp;
		qp = "where BAL_FLAG_VI = 0";
		// qp = "where BAL_FLAG_VI = 0 and MI >= -28.4 and MI <= -27.8";
		//qp = "where BAL_FLAG_VI = 0 and MI >= -26.7 and MI <= -26.6";
		
		report.info("Starting...");

		// Set up the query to retrieve a list of quasars, excluding BALs
		String strQuery = String.format(
				"select %s from QCAT_LITE %s", Quasar.QCAT_FIELDS, qp);
		// "select %s from QCAT where BAL_FLAG_VI = 0 limit 20", Quasar.QCAT_FIELDS);
		
		// Declare a comparator to sort quasars by luminosity
		// TODO: apparent mag is currently mi z=2
		Comparator<QuasarAverageFlux> sortByLuminosity =
				(q1, q2) -> (int) Math.signum(q1.getQuasar().getAbsoluteMagnitude() - q2.getQuasar().getAbsoluteMagnitude());
				//(q1, q2) -> (int) Math.signum(q1.getAbsoluteMagnitude(cosmology) - q2.getAbsoluteMagnitude(cosmology));
		
		// Declare a grouping function to bin quasars in luminosity increments of 0.2, i.e. floor(lum * 5)/5
		Function<QuasarAverageFlux, Double> groupByLuminosity =
				q -> ((Math.floor(q.getQuasar().getAbsoluteMagnitude()*5))/5);

		Map<Long, Quasar> quasarsByObjID = new HashMap<>();
		sql.query(strQuery).stream().map(Quasar::new).forEach(q -> quasarsByObjID.put(q.getObjID(), q));
		
		List<QuasarAverageFlux> listQuasarFlux = new ArrayList<QuasarAverageFlux>();
		IntermediateSpectrum intermediateSpectrum = new IntermediateSpectrum();
		int zz = 0;
		for (long objID : specIndex.getAllObjID()) {
			zz++;
			if (zz % 1000 == 0) {
				report.info("%s", zz);
			}
			Quasar q = quasarsByObjID.get(objID);
			if (q != null) {
				intermediateSpectrum.shiftAndRebin(specIndex.get(objID), q.getRedshift());
				listQuasarFlux.add(new QuasarAverageFlux(q, intermediateSpectrum));				
			}
		}
		
		report.info("Finishing...");
		// Produce a binned map of quasars by red shift. Each entry is a red shift bin value mapped
		// to a list of quasars in that bin.
		Map<Double, List<QuasarAverageFlux>> quasarsByLuminosity =
				listQuasarFlux.stream().sorted(sortByLuminosity) // sort by mag
				.collect(Collectors.groupingBy(groupByLuminosity)); // bin by mag increments of 0.2
		
		
		List<Double> listMag = new ArrayList<Double>();
		listMag.addAll(quasarsByLuminosity.keySet());
		Collections.sort(listMag);
		
		System.out.println("Mag,n,in2,ir2,n2,r2/r1,in3,ir3,n3,r3/r2,in4,ir4,n4,r4/r3,in5,ir5,n5,r5/r4");
		for (Double d : listMag) {
			
			int numQ = quasarsByLuminosity.get(d).size();
			if (numQ < 1000) {
				continue;
			}
			System.out.printf("%s,%s", d, numQ);
			
			int nrange = NormalisationRange.getList().size();
			for (int i = 1; i < nrange; i++) {
				List<Double> rats = new ArrayList<Double>();
				for (QuasarAverageFlux ns : quasarsByLuminosity.get(d)) {
					double avgRange[] = ns.getAvgRange();
					if ((avgRange[i] > 0) && (avgRange[i - 1] > 0)) {
						double rat = avgRange[i] / avgRange[i - 1];
						rats.add(rat);
					}
				}
				getAvg2SD(rats);
			}
			System.out.println();
		}
		
		
	}
	
	/**
	 * Average of the values within two standard deviations.
	 * @param list list of values
	 */
	private void getAvg2SD(List<Double> listx) {

		List<Double> list = listx.stream().map(x -> Math.log(x)).collect(Collectors.toList());
		int inSize = list.size();
		if (inSize == 0) {
			System.out.printf(",0,-1,0,-1");
			return;
		}
		final double avg = list.stream().mapToDouble(x -> x).average()
				.getAsDouble();
		final double variance = list.stream()
				.mapToDouble(x -> Math.pow(x - avg, 2)).average().getAsDouble();
		final double stddev = Math.sqrt(variance);
		DoubleSummaryStatistics stats = list.stream()
				.filter(x -> Math.abs(x - avg) < (2 * stddev))
				.collect(Collectors.summarizingDouble(x -> x));
		int outSize = (int)stats.getCount();
		double newAvg = stats.getAverage();
		System.out.printf(",%s,%s,%s,%s", inSize, Math.exp(avg), outSize, Math.exp(newAvg));
	}

	/**
	 * Average of the values within two standard deviations.
	 * @param list list of values
	 */
	private void getAvg2SDxxx(List<Double> list) {

		int inSize = list.size();
		if (inSize == 0) {
			System.out.printf(",0,-1,0,-1");
			return;
		}
		final double avg = list.stream().mapToDouble(x -> x).average()
				.getAsDouble();
		final double variance = list.stream()
				.mapToDouble(x -> Math.pow(x - avg, 2)).average().getAsDouble();
		final double stddev = Math.sqrt(variance);
		DoubleSummaryStatistics stats = list.stream()
				.filter(x -> Math.abs(x - avg) < (2 * stddev))
				.collect(Collectors.summarizingDouble(x -> x));
		int outSize = (int)stats.getCount();
		double newAvg = stats.getAverage();
		System.out.printf(",%s,%s,%s,%s", inSize, avg, outSize, newAvg);
	}

}

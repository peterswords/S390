/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import ps.db.SQL;
import ps.util.Report;
import ps.util.KeyValuePair;
import ps.util.SupplierX;
import s390.Cosmology;
import s390.Quasar;
import s390.Runner;
import s390.composite.AccumulatorSpectrum;
import s390.composite.CPixel;
import s390.composite.CompositeSpectrum;
import s390.composite.IntermediateSpectrum;
import s390.sdss.spec.Spectrum;
import s390.sdss.spec.SpectrumDbReader;

/**
 * Make composite spectra. The input spectra are binned according to a provided
 * binning function and then composited, producing a set of composite output
 * spectra.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class CompositeSpectra implements Runnable {

	/**
	 * Main entry point.
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {
		Runner.run(CompositeSpectra.class);
	}
	
	
	// Dependencies
	
	/**
	 * SQL for database processing
	 */
	@Inject SQL sql;
	
	
	/**
	 * Cosmology parameters
	 */
	@Inject Cosmology cosmology;
	
	
	/**
	 * Spectrum database reader
	 */
	@Inject SpectrumDbReader specDb;
	
	
	/**
	 * Console output
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

		// Produce composites by quasar luminosity in 0.5 magnitude bins
		doSpecCombine(Functions.binning(0.5, Quasar::getAbsoluteMagnitude), "M");
		
		// Produce composites by red shift in 0.2 interval bins		
		doSpecCombine(Functions.binning(0.2, Quasar::getRedshift), "z");
		
	}


	/**
	 * Combine spectra to produce composite
	 * 
	 * @param binning
	 *            a binning function to group spectra by
	 * @param filePrefix
	 *            String prefix for output file names
	 */
	void doSpecCombine(Function<Quasar, Double> binning, String filePrefix) {
		
		report.info("Begin...");
		
		// Produce a binned map of quasars. Each entry is a bin value mapped
		// to a list of quasars in that bin.
		Map<Double, List<Quasar>> zbinnedQuasars = Quasar.queryGood(sql)
				.collect(Collectors.groupingBy(binning));

		
		// Informational output:
		// Print bins in key order showing lower bound for each bin,
		// and number of items in bin
		zbinnedQuasars
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(
						e -> report.info("z = %s: %s", e.getKey(), e.getValue()
								.size()));
		

		// For each binned quasar list, combine the list into a composite spectrum
		List<Map.Entry<Double, CompositeSpectrum>> zbinnedComposites = zbinnedQuasars
				// get bins as a stream of (key = z, value = list of quasars)
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				// Map the quasar lists to composite spectra
				.map(e -> new KeyValuePair<Double, CompositeSpectrum>(e
						.getKey(), processBinnedSpectra(e.getValue(), binning)))
				.collect(Collectors.toList());
		
		outputBinnedComposites(zbinnedComposites, filePrefix); 
		
	}
	
	/**
	 * Combine the spectra in a bin to produce a composite.
	 * 
	 * @param quasars
	 *            list of quasars in bin
	 * @param binning
	 *            binning function used to group quasars
	 * @return a composite of the input spectra
	 */
	CompositeSpectrum processBinnedSpectra(List<Quasar> quasars, Function<Quasar, Double> binning) {
		
		// Sort for efficiency of loading spectrum
		quasars.sort(Comparator.comparing(q -> q.getID()));
		
		AccumulatorSpectrum acc = new AccumulatorSpectrum();
		
		if (quasars.isEmpty()) {
			return acc.getComposite();
		}
		
		report.info("z = %s, size = %s", binning.apply(quasars.get(0)),
				quasars.size());
		
		int n = 0;
		for (Quasar q : quasars) {
			
			n++;
			if (n % 100 == 0) {
				report.info("%s", n);
			}
			// Get the spectrum from the database
			Spectrum s = specDb.get(q.getID());
			
			// Use intermediate spectrum to shift, rebin, and normalise
			IntermediateSpectrum i = new IntermediateSpectrum();
			double z = q.getRedshift();
			i.shiftAndRebin(s, z);
			if (i.normalise(z)) {
			
				// Add to accumulator
				acc.combine(i);
			
			}
			
		}
		
		return acc.getComposite();
		
	}

	
	/**
	 * Write the composites to output files
	 * 
	 * @param c
	 *            a list of bins containing key values and composite spectra
	 * @param filePrefix
	 *            String with which to prefix output file names
	 * @throws FileNotFoundException
	 *             if output file cannot be created
	 */
	private void outputBinnedComposites(
			List<Map.Entry<Double, CompositeSpectrum>> c, String filePrefix) {

		for (Entry<Double, CompositeSpectrum> e : c) {
			String strFile = filePrefix + e.getKey() + ".csv";
			CompositeSpectrum spec = e.getValue();
			PrintWriter pw = SupplierX.get( () -> new PrintWriter(new File(dirOut, strFile)));
			pw.println("WL,count,mean,gmean,median,umean,umedian");
			for (CPixel pix : spec.getPixels()) {
				pw.printf("%s,%s,%s,%s,%s,%s,%s\n", pix.getWavelength(),
						pix.getCount(), pix.getArithmeticMeanFlux(), pix.getGeometricMeanFlux(),
						pix.getMedianFlux(), pix.getMeanUncertainty(), pix.getMedianUncertainty());
			}
			pw.close();
		}
		
	}



}

/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import ps.db.SQL;
import ps.util.Report;
import s390.EmissionLine;
import s390.Quasar;
import s390.Runner;

/**
 * Look for evidence of the "Baldwin Effect". Measure emission line equivalent
 * width versus luminosity. Data comes from the emission line tables SPZLINE,
 * of the DR10Q FITS files.
 * 
 * N.B. The results from using individual spectra were inconclusive
 * and so high SNR composite spectra were examined instead, and this programme
 * wasn't ultimately used.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class BaldwinEffect implements Runnable {

	public static void main(String[] args) {
		Runner.run(BaldwinEffect.class);
	}
	
	
	// Dependencies
	
	/**
	 * SQL for database processing
	 */
	@Inject SQL sql;
	
	
	/**
	 * Console output
	 */
	@Inject Report report;
	
	
	/**
	 * Main program
	 */
	@Override
	public void run() {

		report.info("Begin...");
		
		doBaldwinEffect();
		
		report.info("...end");
		
	}


	/**
	 * Look for evidence of the "Baldwin Effect". Measure emission line
	 * equivalent width versus luminosity. We are looking for an inverse
	 * correlation between luminosity and equivalent line width.
	 * 
	 */
	private void doBaldwinEffect() {

		// Luminosity bin size
		final double kBinSize = 0.2;
		
		// Minimum acceptable lines per bin
		final int kMinSampleSize = 200;
		
		// Maximum acceptable error in line continuum level
		// and line equivalent width
		final double kMaxLineError = 0.05;
		
		
		// Create a map of good quasars by unique id
		Map<Long, Quasar> qmap = Quasar.queryGood(sql).collect(
				Functions.mapBy(Quasar::getID));

		report.info("quasars %s", qmap.size());

		// Get a list of all emission lines for good quasars, with less than
		// 5% error in line equivalent width and line continuum level,
		// poking each emission line's corresponding quasar as we go.
		List<EmissionLine> lineList = EmissionLine.queryAll(sql)
				.filter(el -> qmap.containsKey(el.getID()))
				.filter(el -> el.getEquivalentWidthErr() / el.getEquivalentWidth() <= kMaxLineError)
				.filter(el -> el.getLineContLevelErr() / el.getLineContLevel() <= kMaxLineError)
				.peek(el -> el.setQuasar(qmap.get(el.getID())))
				.collect(Collectors.toList());

		report.info("emission lines %s", lineList.size());
		
		// Reorganise emission lines list into a map by wavelength
		Map<Double, List<EmissionLine>> lineWaveLengthMap = lineList.stream()
				.collect(Collectors.groupingBy(EmissionLine::getWavelength));
		
		report.info("distinct lines %s", lineWaveLengthMap.size());

		// Extract a convenience map of line wavelengths to line names
		Map<Double, String> lineNameMap = lineWaveLengthMap.entrySet().stream().collect(
				Functions.remapBy(e -> e.getKey(), e -> e.getValue()
						.get(0).getLineName()));
		
		// Extract a convenience list of line wavelengths sorted by wavelength
		List<Double> sortedLineList = lineNameMap.keySet().stream().sorted()
				.collect(Collectors.toList());		
		
		// Iterate over map of emission lines by line wavelength
		for (Double lineWL : sortedLineList) {
			
			List<EmissionLine> lineWLList = lineWaveLengthMap.get(lineWL);
			
			// Report the wavelength and name of the emission line, and number of quasars with that line
			report.info("%4.2f (%s) -> %s", lineWL,
					lineNameMap.get(lineWL), lineWLList.size());
			
			// Bin the lines at this wavelength by quasar luminosity
			Map<Double, List<EmissionLine>> luminosityBins = lineWLList.stream()
					.collect(
							Collectors.groupingBy(Functions.binning(kBinSize,
									emline -> emline.getQuasar()
											.getAbsoluteMagnitude())));
			
			// Remove any emission line bins that don't have at least 200 quasars
			luminosityBins = luminosityBins
					.entrySet()
					.stream()
					.filter( e -> e.getValue().size() >= kMinSampleSize)
					.collect(Functions.remapBy(e -> e.getKey(), e-> e.getValue()));

			// Get the average equivalent line width in each luminosity bin
			Map<Double, Double> luminosityEW = luminosityBins
					.entrySet()
					.stream()
					.collect(Functions.remapBy(e -> e.getKey(), e-> e.getValue().stream()
							.mapToDouble(EmissionLine::getEquivalentWidth)
							.average().getAsDouble()));

			// Calculate the Pearson correlation and Spearman rank of luminosity vs. equiv. width
			report.info("    Pearson %s, Spearman %s",
					Functions.PearsonTest(
							() -> luminosityEW.entrySet().stream()
									.mapToDouble(e -> e.getKey()),
							() -> luminosityEW.entrySet().stream()
									.mapToDouble(e -> e.getValue())),

					Functions.SpearmanRankTest(
							() -> luminosityEW.entrySet().stream()
									.mapToDouble(e -> e.getKey()),
							() -> luminosityEW.entrySet().stream()
									.mapToDouble(e -> e.getValue()))
			);									
			
			// Get a sorted list of bin luminosity values
			List<Double> binValues = luminosityBins.keySet().stream().sorted().collect(Collectors.toList());
			
			// For each luminosity bin, in sorted order, show the luminosity value,
			// number of quasars in bin, and average equivalent line width.
			report.info("    Magnitude (Count) -> Average line equivalent width");
			for (double binValue : binValues) {
				
				report.info("    %4.1f (%s) -> %4.1f", binValue, luminosityBins
						.get(binValue).size(), luminosityEW.get(binValue));
				
			}
			
		}
		
	}

}

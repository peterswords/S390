/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import ps.util.ModuleProperties;
import ps.util.Report;
import s390.Runner;
import s390.sdss.spec.SpecPixel;
import s390.sdss.spec.Spectrum;
import s390.sdss.spec.SpectrumDbReader;

/**
 * Test performance of spectrum disk index file.
 * Reads sequentially forward, then backward. The results demonstrate
 * that it is far more efficient (by a factor of ~10) to read forward.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class TestSpectrumDbPerformance implements Runnable {

	public static void main(String[] args) throws IOException {
		
		Runner.run(TestSpectrumDbPerformance.class);
		
	}
	
	// Dependencies
	
	/**
	 * Properties/parameters
	 */
	@Inject ModuleProperties props;

	
	/**
	 * Spectrum database
	 */
	@Inject SpectrumDbReader specIndex;
	
	
	/**
	 * Progress reporting to console
	 */
	@Inject Report report;

	
	/**
	 * Main program.
	 * 
	 */
	@Override
	public void run() {
		
		try {
			readSpecIndex(true);
			readSpecIndex(false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	

	/**
	 * Read the spectrum database in the specified direction.
	 * 
	 * @param readForward
	 *            boolean indicating whether to read forward (true) or backward
	 *            (false).
	 * @throws IOException
	 *             if an I/O exception occurs during processing.
	 */
	public void readSpecIndex(boolean readForward) throws IOException {

		report.info("Begin...");
		
		// Get the set of all spectrum object ids.
		List<Long> allObjID = specIndex.idStream().collect(Collectors.toList());
		
		// Counters
		int nq = 0;
		int nrows = 0;
		
		// Index of first and last spectrum, and increment, depending on direction
		int first = readForward? 0 : allObjID.size() - 1;
		int last = readForward? allObjID.size() : -1;
		int incr = readForward? 1 : -1;
		
		// Iterate from first to last spectrum
		for (int i = first; i != last; i += incr) {
			
			nq++; // counter
			
			// Load spectrum and interate over its pixel rows
			Spectrum spectrum = specIndex.get(allObjID.get(i));
			for (@SuppressWarnings("unused") SpecPixel sample : spectrum.getPixels()) {
				nrows++;
				if (nrows % 10000000 == 0) {
					report.info("%s", nrows/1000000);
				}
				
			}
			
		}
		
		// Final diagnostics
		report.info("");
		report.info("Samples: %s", nrows);
		report.info("Quasars: %s", nq);
	}


}

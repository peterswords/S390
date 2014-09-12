/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process;

import java.util.function.Function;

import s390.Quasar;
import s390.Runner;

/**
 * Make composite spectra. The input spectra are binned by luminosity
 * and then composited, producing a set of composite output spectra.
 * 
 * @see CompositeSpectra
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class CompositeByLuminosity extends CompositeSpectra {

	/**
	 * Main entry point.
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {
		Runner.run(CompositeByLuminosity.class);
	}
	
	
	/**
	 * Main program
	 */
	@Override
	public void run() {

		super.run();
		
	}


	/**
	 * Declare a grouping function to bin quasars in luminsoity increments of
	 * 0.5, i.e. floor(M * 2)/2
	 * 
	 * @return binning function
	 */
	Function<Quasar, Double> getBinningFunction() {
		
		Function<Quasar, Double> luminosityBinning =
				q -> ((Math.floor(q.getAbsoluteMagnitude()*2))/2);
		
		return luminosityBinning;
		
	}
	
	
	/**
	 * Get the prefix used for output files.
	 * 
	 * @return prefix
	 */
	String getFilePrefix() {
		
		return "M";

	}
	
	
}

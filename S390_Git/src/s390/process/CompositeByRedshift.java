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
 * Make composite spectra. The input spectra are binned by redshift
 * and then composited, producing a set of composite output spectra.
 * 
 * @see CompositeSpectra
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class CompositeByRedshift extends CompositeSpectra {

	/**
	 * Main entry point.
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {
		Runner.run(CompositeByRedshift.class);
	}
	
	
	/**
	 * Main program
	 */
	@Override
	public void run() {

		super.run();
		
	}


	/**
	 * Declare a grouping function to bin quasars in red shift increments of
	 * 0.2, i.e. floor(z * 5)/5
	 * 
	 * @return binning function
	 */
	Function<Quasar, Double> getBinningFunction() {
		
		Function<Quasar, Double> redshiftBinning =
				q -> ((Math.floor(q.getRedshift()*5))/5);
		
		return redshiftBinning;
		
	}
	
	
	/**
	 * Get the prefix used for output files.
	 * 
	 * @return prefix
	 */
	String getFilePrefix() {
		
		return "z";

	}
	
	
}

/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.composite;

/**
 * Pixel of a {@link CompositeSpectrum}.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class CPixel {

	/**
	 * Pixel wavelength
	 */
	private final int wavelength;

	
	/**
	 * Count of contributing input pixels
	 */
	private final int count;
	
	
	/**
	 * Arithmetic mean flux
	 */
	private final double arithmeticMeanFlux;
	
	
	/**
	 * Geometric mean flux
	 */
	private final double geometricMeanFlux;
	
	
	/**
	 * Median flux
	 */
	private final double medianFlux;
	
	
	/**
	 * Uncertainty in arithmetic mean flux
	 */
	private final double meanUncertainty;
	
	
	/**
	 * Uncertainty in median flux
	 */
	private final double medianUncertainty;

	
	/**
	 * Constructor
	 * @param wl wavelength
	 * @param c count
	 * @param amean arithmetic mean flux
	 * @param gmean geometric mean flux
	 * @param median median flux
	 * @param umean uncertainty in arithmetic mean flux
	 * @param umedian uncertainty in median flux
	 */
	CPixel(int wl, int c, double amean, double gmean, double median, double umean, double umedian) {
		wavelength = wl;
		count = c;
		arithmeticMeanFlux = amean;
		geometricMeanFlux = gmean;
		medianFlux = median;
		meanUncertainty = umean;
		medianUncertainty = umedian;
	}
	
	
	/**
	 * Get count of input pixels
	 * 
	 * @return count
	 */
	public int getCount() {
		return count;
	}

	
	/**
	 * Get arithmetic mean input flux
	 * 
	 * @return mean
	 */
	public double getArithmeticMeanFlux() {
		return arithmeticMeanFlux;
	}

	
	/**
	 * Get geometric mean flux
	 * 
	 * @return mean
	 */
	public double getGeometricMeanFlux() {
		return geometricMeanFlux;
	}

	
	/**
	 * Get median input flux
	 * 
	 * @return median
	 */
	public double getMedianFlux() {
		return medianFlux;
	}

	
	/**
	 * Uncertainty in median
	 * 
	 * @return uncertainty
	 */
	public double getMedianUncertainty() {
		return medianUncertainty;
	}

	
	/**
	 * Uncertainty in arithmetic mean
	 * 
	 * @return uncertainty
	 */
	public double getMeanUncertainty() {
		return meanUncertainty;
	}

	
	/**
	 * Get wavelength
	 * @return wavelength
	 */
	public int getWavelength() {
		return wavelength;
	}

}

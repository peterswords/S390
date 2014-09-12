/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.composite;

import java.util.List;

/**
 * Composite spectrum.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class CompositeSpectrum {

	/** 
	 * Pixels
	 */
	private final List<CPixel> pixels;
	
	
	/**
	 * Constructor
	 * 
	 * @param pixels
	 *            input pixels
	 */
	CompositeSpectrum(List<CPixel> pixels) {
		this.pixels = pixels;
	}
	
	
	/**
	 * Get pixels of composite
	 * 
	 * @return pixels
	 */
	public List<CPixel> getPixels() {
		return pixels;
	}
	
	
	/**
	 * Get wavelength range lo
	 * @return wavelength lo
	 */
	public int getWavelengthLo() {
		return pixels.get(0).getWavelength();
	}

	
	/**
	 * Get wavelength range hi
	 * 
	 * @return wavelength hi
	 */
	public int getWavelengthHi() {
		return pixels.get(pixels.size() - 1).getWavelength();
	}
	
}

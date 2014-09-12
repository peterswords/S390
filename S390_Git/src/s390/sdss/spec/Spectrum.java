/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.sdss.spec;

import java.util.stream.Stream;

/**
 * Represents an SDSS BOSS spectrum.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public interface Spectrum {

	/**
	 * The maximum wavelength handled by the BOSS spectrograph
	 */
	int kMaxWavelength = 10500;
	
	
	/**
	 * The maximum "useful" wavelength of the BOSS spectrograph
	 */
	int kMaxUsefulWavelength = 10000;

	
	/**
	 * The minimum "useful" wavelength of the BOSS spectrograph
	 */
	int kMinUsefulWavelength = 3700;

	/**
	 * Get object unique ID
	 * 
	 * @return id
	 */
	long getObjID();
	
	
	/**
	 * Get number of pixels in spectrum
	 * 
	 * @return the number of pixels
	 */
	int getNumPixels();
	
	
	/**
	 * Get spectrum pixels
	 * 
	 * @return iterable over pixels
	 */
	Iterable<SpecPixel> getPixels();

	
	/**
	 * Get spectrum pixels as a {@link Stream}
	 * 
	 * @return pixel stream
	 */
	Stream<SpecPixel> stream();
	
}

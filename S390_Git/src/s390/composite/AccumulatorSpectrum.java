/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.composite;

import java.util.ArrayList;
import java.util.List;

import s390.sdss.spec.Spectrum;

/**
 * Accumulates the inputs to a composite spectrum. Allows
 * the composite to be produced at the end of processing.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class AccumulatorSpectrum {

	/**
	 * Wavelength range lo
	 */
	int wavelengthLo;
	
	/**
	 * Wavelength range hi
	 */
	int wavelengthHi;
	
	
	/**
	 * Pixels of the accumulated spectrum.
	 * These are stored in blocks which are allocated as required.
	 */
	final APixel pixels[][];
	
	
	/**
	 * Pixel allocation block size
	 */
	private static final int kPixelBlockSize = 100;
	
	
	/**
	 * 
	 * Constructor.
	 * 
	 */
	public AccumulatorSpectrum() {
		
		int numBlocks = Spectrum.kMaxUsefulWavelength / kPixelBlockSize + 1;
		
		pixels = new APixel[numBlocks][];
	}

	
	/**
	 * Get pixel at the specified wavelength.
	 * @param wavelength
	 * @return pixel
	 */
	APixel getPixel(int wavelength) {
		
		// Allocate new block if necessary
		int blockNum = wavelength / kPixelBlockSize;
		int blockOffset = wavelength % kPixelBlockSize;
		APixel[] block = pixels[blockNum];
		if (block == null) {
			block = new APixel[kPixelBlockSize];
			for (int i = 0; i < block.length; i++) {
				block[i] = new APixel();
			}
			pixels[blockNum] = block;
		}
		return block[blockOffset];
	}
	
	
	/**
	 * Add an intermediate spectrum to this composite.
	 * 
	 * @param ispec
	 *            spectrum to add
	 * @return the combined output (i.e. this)
	 */
	public AccumulatorSpectrum combine(IntermediateSpectrum ispec) {
		
		// Get the wavelength range -- we discard the first and last pixels
		int rangeLo = ispec.getWavelengthLo() + 1;
		int rangeHi = ispec.getWavelengthHi() - 1;
		
		// Accumulate pixels - ignore NaNs.
		for (int i = rangeLo; i <= rangeHi; i++) {
			float f = (float)ispec.getPixel(i);
			APixel pixel = getPixel(i);
			if (!Float.isNaN(f)) {
				pixel.addPixel(f);
			}
		}
		
		// Expand range of accumulator if necessary
		wavelengthLo = Math.min(wavelengthLo, rangeLo);
		wavelengthHi = Math.max(wavelengthHi, rangeHi);
		
		return this;
		
	}

	
	/**
	 * Get accumulator spectrum wavelength range lo
	 * 
	 * @return lowest wavelength
	 */
	public int getWavelengthLo() {
		return wavelengthLo;
	}


	/**
	 * Get accumulator spectrum wavelength range hi
	 * 
	 * @return highest wavelength
	 */
	public int getWavelengthHi() {
		return wavelengthHi;
	}
	
	
	/**
	 * Process accumulated spectrum to produce a composite.
	 * 
	 * @return composite spectrum
	 */
	public CompositeSpectrum getComposite() {
				
		int numPixels = Math.max(0, getWavelengthHi() - getWavelengthLo() + 1);
		
		// Allocate composite pixels
		List<CPixel> list = new ArrayList<CPixel>(numPixels);
		
		if (numPixels > 0) {
			// Compute each composite pixel
			for (int i = getWavelengthLo(); i <= getWavelengthHi(); i++) {
				CPixel s = getPixel(i).getComposite(i);
				list.add(s);
			}
		}
		
		// Construct composite spectrum
		return new CompositeSpectrum(list);
		
	}

		
}

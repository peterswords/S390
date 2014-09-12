/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.composite;

import java.util.Arrays;

/**
 * Pixel of an accumulator spectrum.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class APixel {

	/**
	 * Number of spectra accumulated
	 */
	private int count;

	
	/**
	 * Accumulated flux values of input spectra
	 */
	private final float fluxes[][];

	
	/**
	 * Flux allocation block size
	 */
	private static final int kFluxBlockSize = 1000;
	
	
	/**
	 * Max fluxes
	 */
	private static final int kMaxFluxes = 40000;
	
	
	/**
	 * Create a pixel capable of accumulated the maximum number of
	 * input spectra.
	 * 
	 */
	public APixel() {
		
		// Allocate flux blocks
		int numBlocks = kMaxFluxes / kFluxBlockSize + 1;
		
		fluxes = new float[numBlocks][];
		
	}
	
	
	/**
	 * Get an input flux value.
	 * 
	 * @param n
	 *            the flux item required
	 * @return fluxes
	 */
	public float getFlux(int n) {
		
		int blockNum = n / kFluxBlockSize;
		int blockOffset = n % kFluxBlockSize;
		return fluxes[blockNum][blockOffset];
		
	}

	
	/**
	 * Add an additional input pixel
	 * 
	 * @param flux
	 *            input pixel flux value
	 */
	public void addPixel(float flux) {
		
		if (Float.isNaN(flux)) {
			// Don't record NaN values
			return;
		}
		
		int blockNum = count / kFluxBlockSize;
		int blockOffset = count % kFluxBlockSize;
		float[] block = fluxes[blockNum];
		if (block == null) {
			block = new float[kFluxBlockSize];
			fluxes[blockNum] = block;
		}
		
		// save flux and increment the count for the output pixel
		block[blockOffset] = flux;
		count++;
		
	}

	
	/**
	 * Compute composite pixel from the accumulated inputs.
	 * 
	 * @param wavelength
	 *            wavelength of output pixel
	 * @return composite pixel
	 */
	public CPixel getComposite(int wavelength) {
		
		// If there were no accumulated inputs
		if (count == 0) {
			return new CPixel(wavelength, count, 0, 0, 0, 0, 0);
		}
		
		// Get all non-NaN pixel flux values as a single array
		float fluxArray[] = new float[count];
		for (int i = 0; i < count; i++) {
			fluxArray[i] = getFlux(i);
		}
		
		// Sort the input fluxes so we can calculate median
		Arrays.sort(fluxArray);
		
		// Get statistical values for the output composite pixel
		return new CPixel(wavelength, count, getArithmeticMean(fluxArray), getGeometricMean(fluxArray),
				getMedian(fluxArray), getMeanUncertainty(fluxArray), getMedianUncertainty(fluxArray));
		
	}
	
	/**
	 * Get uncertainty in the arithmetic mean pixel. This is the sample standard
	 * deviation of the input flux values.
	 * 
	 * @return uncertainty.
	 */
	private double getMeanUncertainty(float[] fluxArrray) {
		
		// Special cases 
		if (count < 2) {
			if (count == 0) {
				return 0;
			}
			if (count == 1) {
				// uncertainty is 100% i.e. same as the single input flux value
				return getFlux(0);
			}
		}
		
		double meanFlux = getArithmeticMean(fluxArrray);
		double meanUncertainty = 0;
		
		for (int i = 0; i < count; i++) {
			// accumulate squares of differences from the mean
			double diff = getFlux(i) - meanFlux;
			meanUncertainty += (diff * diff);
		}
		
		meanUncertainty /= (count - 1); // sample variance
		meanUncertainty = Math.sqrt(meanUncertainty); // sample std dev
		meanUncertainty /= Math.sqrt(count); // uncertainty in the mean
		
		return meanUncertainty;

	}

	
	/**
	 * Get the median input flux value in the specified pixel range. This is a
	 * utility routine that also supports calculating quartiles.
	 * 
	 * @param lower
	 *            pixel number
	 * @param upper
	 *            pixel number
	 * @param fluxArray
	 *            sorted flux values
	 * @return median of the values between lower and upper pixel ranges
	 */
	private double calcMedian(int lower, int upper, float[] fluxArray) {
		
		int n = upper - lower;
		int mid = n/2 + lower;
		boolean odd = n % 2 == 1;
		double median = odd? fluxArray[mid] : (fluxArray[mid] + fluxArray[mid - 1]) / 2;
		return median;
		
	}
	
	
	/**
	 * Get the input pixel flux value
	 * 
	 * @param fluxArray
	 *            sorted flux values
	 * 
	 * @return median
	 */
	private double getMedian(float[] fluxArray) {
		
		// Get median across whole array
		return calcMedian(0, count, fluxArray);

	}

	
	/**
	 * Get the uncertainty in the median pixel value. Uncertainty in the median
	 * is 68% of the semi-interquartile range divided by the square root of the
	 * number of input pixels.
	 * 
	 * @return uncertainty in the median
	 */
	private double getMedianUncertainty(float[] fluxArray) {
		
		// Special cases 
		if (count < 2) {
			if (count == 0) {
				return 0;
			}
			if (count == 1) {
				// uncertainty is 100% i.e. same as the single input flux value
				return getFlux(0);
			}
		}
		
		// Median and quartiles. The median is in the centre array entry if count is odd.
		// Otherwise it is the average of two centre entries. The lower quartile is the
		// median of the lower half, which includes the median if count is odd. The upper
		// quartile is the median of the upper half.

		int mid = count / 2;
		double q1 = calcMedian(0, mid + (count % 2), fluxArray);
		double q3 = calcMedian(mid, count, fluxArray);
		
		// Uncertainty in the median is 68% of the semi-interquartile range divided by the
		// square root of the count.
		
		return 0.68 * ((q3 - q1) / 2) / Math.sqrt(count);

	}

	
	/**
	 * Get the geometric mean of the input flux values. This is calculated as
	 * the antilog of the mean of the logs of the input values. Negative and
	 * zero input flux values are ignored for the purpose of calculating the
	 * geometric mean.
	 * @param fluxArray 
	 * 
	 * @return geometric mean
	 */
	private double getGeometricMean(float[] fluxArray) {
		
		// Sum of the logs of the input fluxes
		double totalLogFlux = 0;
		
		// Number of input fluxes with positive values
		int positiveFluxCount = 0;
		
		for (int i = 0; i < count; i++) {
			double flux = fluxArray[i];
			// Must ignore zero and negative values for geometric mean
			if (flux > 0) {
				totalLogFlux += Math.log(flux);
				positiveFluxCount++;
			}
		}
		
		if (positiveFluxCount == 0) {
			// Special case
			return 0;
		} else {
			// Get average of the logs
			double avgLog = totalLogFlux / positiveFluxCount;
			// return the antilog of the average log
			return Math.exp(avgLog);
		}

	}

	
	/**
	 * Get the arithmetic mean of the input flux values.
	 * @param fluxArray sorted array of flux values
	 * 
	 * @return arithmetic mean
	 */
	private double getArithmeticMean(float[] fluxArray) {
		
		if (count == 0) {
			// Special case
			return 0;
		}
		
		double totalFlux = 0;
		for (int i = 0; i < count; i++) {
			totalFlux += fluxArray[i];
		}
		return totalFlux / count;
		
	}

	
	/**
	 * Get the number of input flux values.
	 * 
	 * @return count
	 */
	public int getCount() {
		return count;
	}

	
}

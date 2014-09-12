/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.composite;

import java.util.Iterator;
import java.util.stream.DoubleStream;

import s390.normalisation.NormalisationRange;
import s390.sdss.spec.SpecPixel;
import s390.sdss.spec.Spectrum;

/**
 * Intermediate spectrum used for red shifting, rebinning and normalisation,
 * before adding to a composite spectrum.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class IntermediateSpectrum {

	/**
	 * Pixel array. Note that wavelength range must start at zero since
	 * we do not know how highly redshifted an object will be.
	 */
	double pixels[] = new double[Spectrum.kMaxWavelength];

	
	/**
	 * Wavelength range lo
	 */
	int wavelengthLo;

	
	/**
	 * Wavelength range hi
	 */
	int wavelengthHi;
	
	
	/**
	 * Perform normalisation
	 * 
	 * @param z
	 *            redshift
	 *            
	 *  @return false if normalisation could not be performed due to extreme redshift, otherwise true
	 */
	public boolean normalise(double z) {
		
		// Check if normalisation is possible, i.e. wavelengths inside
		// normalisable range
		NormalisationRange nr = NormalisationRange.getNormalisation(z);
		if (nr == null) {
			return false;
			 // throw new RuntimeException("Can't normalise z = " + z);
		}
		
		// Get the average pixel value in the normalisation range
		DoubleStream.Builder b = DoubleStream.builder(); 
		for (int i = nr.getStartWL(); i <= nr.getEndWL(); i++) {
			b.add(pixels[i]);
		}
		double avg = b.build().average().getAsDouble();
		
		// Get the ratio of the average pixel value and the standardised value
		// Note, if avg was zero, then divide-by-zero here results in NaN.
		double normScale = nr.getNormFactor() / avg;
		
		// Scale all pixels by the normalisation ratio
		// If normscale was NaN from previous, then all pixels become NaN.
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] *= normScale;
		}
		
		return true;
	}

	
	/**
	 * Shift input spectrum to rest frame wavelengths. Rebin wavelengths
	 * from logarithmic to Angstrom intervals.
	 * @param spec input spectrum
	 * @param z red shift
	 */
	public void shiftAndRebin(Spectrum spec, double z) {
		
		// Temporary storage for bin fractions, i.e the fractional distribution
		// of a single input bin over output bins. There are never more than 4
		// output bins for one input bin.
		double fractions[] = new double[4];
		
		Iterator<SpecPixel> i = spec.getPixels().iterator();
		SpecPixel s = i.next(); // first sample
		boolean first = true;
		
		while (i.hasNext()) {
			
			// Get the next pixel to give us an interval between previous and next
			SpecPixel next = i.next();
			
			// Check wavelength within useful SDSS range
			double input_lo = Math.pow(10, s.getLoglam());
			double input_hi = Math.pow(10, next.getLoglam());
			if ((input_lo < Spectrum.kMinUsefulWavelength)
					|| (input_hi > Spectrum.kMaxUsefulWavelength)) {
				s = next;
				continue; // skip pixel
			}
			
			// lo and hi range of input wavelength interval in rest frame
			input_lo /= (z + 1);
			input_hi /= (z + 1);
			double input_range = input_hi - input_lo;
			
			// start wavelength of first and last 1-Angstrom output bin, and number of bins
			// N.B. output_bin_range is actually one less than number of bins.
			int output_bin_lo = (int)Math.floor(input_lo);
			int output_bin_hi = (int)Math.floor(input_hi);
			int output_bin_range = output_bin_hi - output_bin_lo;
			if (first) {
				wavelengthLo = output_bin_lo;
				first = false;
			}
			wavelengthHi = output_bin_hi;
			
			if (output_bin_range < 0) {
				throw new RuntimeException("Wavelength binning error");
			}
			if (output_bin_range == 0) {
				// all flux goes in one bin
				pixels[output_bin_lo] += s.getFlux();
			} else {
				// Calc fraction of one Angstrom in first and last bins
				fractions[0] = 1 - (input_lo - output_bin_lo);
				fractions[output_bin_range] = input_hi - output_bin_hi;
				// Then put one Angstrom in each bin in between first and last
				for (int j = 1; j < output_bin_range; j++) {
					fractions[j] = 1; // one unit in each full bin
				}
				// Now distribute flux according to fraction in each bin. In each
				// of our fractions we have a fraction of one Angstrom. Divide by
				// total wl range to get fraction of flux going in each bin.
				for (int j = 0; j <= output_bin_range; j++) {
					double fraction = fractions[j] / input_range;
					pixels[output_bin_lo + j] += (s.getFlux() * fraction);
				}
			}
			
			s = next; // next sample
		}
		
	}

	
	/**
	 * Get wavelength range hi
	 * 
	 * @return wavelength hi
	 */
	public int getWavelengthLo() {
		return wavelengthLo;
	}

	
	/**
	 * Get wavelength range lo
	 * 
	 * @return wavelength lo
	 */
	public int getWavelengthHi() {
		return wavelengthHi;
	}
	
	
	/**
	 * Get a pixel
	 * 
	 * @param wavelength
	 *            wavelength of the desired pixel
	 * @return pixel at specified wavelength
	 */
	public double getPixel(int wavelength) {
		return pixels[wavelength];
	}

	
	/**
	 * Get pixel array
	 * 
	 * @return pixels
	 */
	public double[] getPixels() {
		return pixels;
	}

	
}

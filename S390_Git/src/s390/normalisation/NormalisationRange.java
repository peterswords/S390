/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.normalisation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import s390.sdss.spec.Spectrum;

/**
 * NormalisationRange represents a small range of wavelengths that
 * avoids prominent emission lines in quasar spectra. These quiescent
 * portions of the spectrum can be used as standard continuum flux levels
 * for the purpose of scaling spectra to a common reference.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class NormalisationRange {

	/**
	 * Start wavelength of normalisation range.
	 */
	private final int startWL;
	

	/**
	 * End wavelength of normalisation range.
	 */
	private final int endWL;
	private final double normRatio;
	private final double normFactor;
	
	private static final List<NormalisationRange> list;

	static public NormalisationRange getNormalisation(double redShift) {
		NormalisationRange best = null;
		double closest = 100;
		for (int i = 1; i < list.size(); i++) {
			NormalisationRange r = list.get(i);
			double z1 = r.getStartZ(), z2 = r.getEndZ();
			if ((redShift >= z1) && (redShift <= z2)) {
				double cmp = Math.pow(redShift - (z1 + z2)/2, 2);
				if (cmp < closest) {
					closest = cmp;
					best = r;
				}
			}
		}
		return best;
	}
	
	static public List<NormalisationRange> getList() {
		return list;
	}
	
	static {
		NormalisationRange r1 = new NormalisationRange(5400, 5500, 1.0, 15.0);
		NormalisationRange r2 = new NormalisationRange(3500, 3650, 3.0, r1);
		NormalisationRange r3 = new NormalisationRange(3000, 3150, 1.5, r2);
		NormalisationRange r4 = new NormalisationRange(2450, 2600, 1.7, r3);
		NormalisationRange r5 = new NormalisationRange(1950, 2100, 1.6, r4);
		list = Collections.unmodifiableList(Arrays.asList(r1, r2, r3, r4, r5));
	}
	
	/**
	 * Private constructor.
	 * @param start start wavelength of normalisation range
	 * @param end end wavelength of normalisation range
	 * @param ratio
	 * @param factor
	 */
	private NormalisationRange(int start, int end, double ratio, double factor) {
		startWL = start;
		endWL = end;
		normRatio = ratio;
		normFactor = factor;
	}

	private NormalisationRange(int start, int end, double ratio, NormalisationRange prevInterval) {
		startWL = start;
		endWL = end;
		normRatio = ratio;
		normFactor = ratio * prevInterval.getNormFactor();
	}

	public int getStartWL() {
		return startWL;
	}

	public int getEndWL() {
		return endWL;
	}
	
	public double getStartZ() {
		double min = Spectrum.kMinUsefulWavelength;
		return (min / getStartWL()) - 1;
	}

	public double getEndZ() {
		double max = Spectrum.kMaxUsefulWavelength;
		return (max / getEndWL()) - 1;
	}
	
	public double getMidZ() {
		return (getStartZ() + getEndZ()) / 2;
	}
	
	public int getMidWL() {
		return (getStartWL() + getEndWL()) / 2;
	}

	public double getNormRatio() {
		return normRatio;
	}

	public double getNormFactor() {
		return normFactor;
	}
}


//Range num	Start	End	Norm Ratio	Norm factor		Range mid	Norm factor		3700	10000

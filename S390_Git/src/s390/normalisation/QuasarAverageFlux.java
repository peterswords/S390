/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.normalisation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ps.db.SQL;
import ps.util.Report;
import s390.Quasar;
import s390.composite.IntermediateSpectrum;
import s390.sdss.spec.SpectrumDbReader;

/**
 * Computes the average flux in specific wavelength ranges for a quasar spectrum.
 * The ranges are determined by {@link NormalisationRange#getList()}.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class QuasarAverageFlux implements Serializable {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Quasar being tested
	 */
	private final Quasar q;

	
	/**
	 * Array of average flux values for multiple wavelength ranges
	 */
	private final double avgRange[];

	
	/**
	 * Constructor. Computes the average flux in multiple wavelength ranges, as
	 * determined by the {@link NormalisationRange} class.
	 * 
	 * @param q
	 *            the quasar being tested
	 * @param spec
	 *            the spectrum for the quasar under test. This is assumed to
	 *            have been rebinned to one Angstrom wavelength intervals and
	 *            shifted to rest frame wavelengths.
	 */
	public QuasarAverageFlux(Quasar q, IntermediateSpectrum spec) {
		
		this.q = q;
		
		// Get required normalisation ranges to be averaged over
		List<NormalisationRange> ranges = NormalisationRange.getList();
		
		// Allocate storage for flux averages
		avgRange = new double[ranges.size()];
		
		// Calculate averages
		int i = 0;
		for (NormalisationRange range : ranges) {
			avgRange[i] = computeRangeAverage(spec, range.getStartWL(), range.getEndWL());
			i++;
		}
	}
	
	
	/**
	 * Get associated quasar
	 * @return quasar
	 */
	public Quasar getQuasar() {
		return q;
	}
	
	
	/**
	 * Get average flux value for each wavelength range.
	 * 
	 * @return flux value array
	 */
	public double[] getAvgRange() {
		return avgRange;
	}
	
	
	/**
	 * Compute average flux in a specified wavelength range.
	 * 
	 * @param spec
	 *            spectrum to test
	 * @param start
	 *            wavelength range start
	 * @param end
	 *            wavelength range end
	 * @return computed average flux
	 */
	private double computeRangeAverage(IntermediateSpectrum spec, int start, int end) {
		
		// If spectrum does not fully overlap with range, return sentinel -1 value
		if ((spec.getWavelengthLo() > start) || (spec.getWavelengthHi() < end)) {
			return -1;
		}
		
		// Get total flux in range
		double total = 0;
		for (int wl = start; wl <= end; wl++) {
			total += spec.getPixel(wl);
		}
		
		// Compute average
		int len = end - start + 1;
		return total / len;
		
	}
	

	/**
	 * Calculate average flux values in specific ranges for all non-BAL quasars.
	 * 
	 * @param sql
	 *            SQL object for reading database
	 * @param specDb
	 *            the spectrum database
	 * @param report
	 *            for writing diagnostics to console
	 * @return a list of {@link QuasarAverageFlux}es sorted by quasar object id
	 */
	public static List<QuasarAverageFlux> getQuasarAverageFluxes(SQL sql,
			SpectrumDbReader specDb, Report report) {

		// Get all non-BAL quasars into a map by object id
		Map<Long, Quasar> quasarsByObjID = Quasar.queryAll(sql, false).collect(
				HashMap<Long, Quasar>::new, (r, q) -> r.put(q.getObjID(), q),
				(r1, r2) -> r1.putAll(r2));
				
		// Create list to hold quasar average fluxes, with capacity equal to quasar list
		List<QuasarAverageFlux> listQuasarFlux = new ArrayList<>(quasarsByObjID.size());
		
		int counter = 0; // counter for reporting
		
		// Read all quasar spectra from spectrum database in efficient object id order
		for (long objID : specDb.getAllObjID()) {
			
			// Ignore BALs by checking if quasar in original list
			Quasar q = quasarsByObjID.get(objID);
			
			if (q != null) {
				
				// Shift spectrum to rest frame and rebin in 1-Ang intervals
				IntermediateSpectrum ispec = new IntermediateSpectrum();
				ispec.shiftAndRebin(specDb.get(objID), q.getRedshift());
				
				// Calculate average fluxes in normalisation ranges
				listQuasarFlux.add(new QuasarAverageFlux(q, ispec));				
				
			}
			
			// Reporting
			counter++;
			if (counter % 1000 == 0) {
				if (report != null) {
					report.info("%s", counter);
				}
			}
			
		}
		
		return listQuasarFlux;
		
	}
		
}

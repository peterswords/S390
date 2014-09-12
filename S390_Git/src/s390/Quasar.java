/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390;

import java.io.Serializable;
import java.util.Comparator;
import java.util.stream.Stream;

import ps.db.SQL;
import ps.db.SQLResult;
import s390.sdss.Naming;

/**
 * Represents a Quasar from the DR10Q SDSS Quasar catalogue.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class Quasar implements Serializable {
	
	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Subset of columns from the SDSS Quasar catalog FITS file that are used.
	 */
	public static final String QCAT_FIELDS = "OBJ_ID, RA, DEC, Z_VI, MI, BAL_FLAG_VI, PSFMAG4, EXTINCTION4, PLATE, MJD, FIBERID";


	/**
	 * 
	 * Get a stream of all quasars from database, optionally including BALs.
	 * 
	 * @param sql
	 *            SQL database access object
	 * @param includeBAL
	 *            whether to include BALs (Broad Absorption Line quasars)
	 * @return stream of quasars
	 */
	public static Stream<Quasar> queryAll(SQL sql, boolean includeBAL) {
		
		// Set up the query to retrieve a list of quasars
		
		String predicate = includeBAL? "" : "where BAL_FLAG_VI = 0";
		
		String strQuery = String.format(
				"select %s from QCAT %s", Quasar.QCAT_FIELDS, predicate);
		
		return sql.query(strQuery).stream().map(Quasar::new);
		
	}

	
	/**
	 * 
	 * Get a stream of all quasars from database.
	 * 
	 * @param sql
	 *            SQL database access object
	 * @return stream of quasars
	 */
	public static Stream<Quasar> queryAll(SQL sql) {
		
		return queryAll(sql, true);
		
	}

	
	/**
	 * Get a comparator to sort/compare quasars by object id.
	 * 
	 * @return comparator
	 */
	public static Comparator<Quasar> comparingByID() {
		
		return Comparator.comparing(Quasar::getObjID);

	}
	
	
	/**
	 * Initialize a Quasar from relational database
	 * @param r an sql query result
	 */
	public Quasar(SQLResult r) {
		obj_id = r.get("OBJ_ID");
		ra = r.get("RA");
		dec = r.get("DEC");
		z_vi = r.get("Z_VI");
		magMiz2 = r.get("MI");
		magPSF_i = (float)r.get("PSFMAG4");
		ext_i = (float)r.get("EXTINCTION4");
		short tmp1 = r.get("BAL_FLAG_VI");
		bal_flag_vi = tmp1 == 1;
		plate = r.get("PLATE");
		mjd = r.get("MJD");
		fiber = r.get("FIBERID");
		
	}

	
	/**
	 * Override equals() to be compatible with comparison by object id
	 */
	@Override
	public boolean equals(Object obj) {
		
		return (this == obj) || ((obj instanceof Quasar) && ((Quasar)obj).getObjID() == getObjID());

	}
	
	
	/**
	 * Standard value of quasar spectral index used in luminosity calculations
	 */
	static final double kStdSpectralIndex = -0.5;
	
	
	/**
	 * Standard offset to K Correction which makes the K Correction equal
	 * to zero at a red shift of 2. This is used in M[z=2] absolute magnitude
	 * calculations.
	 */
	static final double kCorrectionOffsetZ2 = getKCorrection(2);
	
	
	/**
	 * Quasar unique object id
	 */
	private final long obj_id;
	
	/**
	 * Right ascension in degrees
	 */
	private final double ra;
	
	
	/**
	 * Declination in degrees
	 */
	private final double dec;
	
	
	/**
	 * Red shift from SDSS visual inspection
	 */
	private final double z_vi;
	
	
	/**
	 * Broad absorption line quasar (BAL) flag
	 */
	private final boolean bal_flag_vi;
	
	
	/**
	 * Absolute i-band magnitude adjusted to z=2
	 * as calculated by the SDSS processing pipeline
	 */
	private final double magMiz2;

	
	/**
	 * PSF i-band apparent magnitude.
	 */
	private final double magPSF_i;

	
	/**
	 * Extinction in i-band.
	 */
	private final double ext_i;

	
	/**
	 * Plate ID of original exposure.
	 */
	private final int plate;

	
	/**
	 * Modified Julian date of observation.
	 */
	private final int mjd;

	
	/**
	 * Fiber ID of original exposure.
	 */
	private final int fiber;

	
	/**
	 * Get quasar unique object id
	 * @return object id
	 */
	public long getObjID() {return obj_id;}

	
	/**
	 * Get right ascension
	 * @return right ascension in degrees
	 */
	public double getRA() {return ra;}

	
	/**
	 * Get declination
	 * @return declination in degrees
	 */
	public double getDec() {return dec;}

	
	/**
	 * Get the IAU-designated quasar name.
	 * @return IAU name string
	 */
	public String getNameIAU() {
		return Naming.getNameIAU(getRA(), getDec());
	}
	
	
	/**
	 * Get visual inspection red shift
	 * @return red shift (z) value
	 */
	public double getRedshift() {
		return z_vi;
	}

	
	/**
	 * Get quasar i-band absolute magnitude Mi[z=2] as calculated by SDSS pipeline.
	 * @return absolute magnitude
	 */
	public double getAbsoluteMagnitude() {
		return magMiz2;
	}
	
	
	/**
	 * Get plate ID of original exposure.
	 * @return plate ID
	 */
	public int getPlate() {
		return plate;
	}
	
	
	/**
	 * Get modified Julian date of original exposure.
	 * @return mjd
	 */
	public int getMJD() {
		return mjd;
	}
	
	
	/**
	 * Get fiber ID of original exposure.
	 * @return fiber ID
	 */
	public int getFiber() {
		return fiber;
	}
	
	
	/**
	 * Get quasar absolute magnitude Mi[z=2] as calculated in the specified
	 * cosmology from apparent magnitude with extinction and K corrections.
	 * This is a simplified calculation compared to the SDSS pipeline value returned
	 * by {@link #getAbsoluteMagnitude()}. It treats the spectrum as a power-law
	 * function  with  constant  frequency spectral index as described in
	 * <a href="http://adsabs.harvard.edu/abs/2000A%26A...353..861W">Wisotzki, 2000</a>
	 * @param c Cosmology parameters.
	 * @return absolute magnitude
	 */
	public double getAbsoluteMagnitude(Cosmology c) {
		
		double apparentMag = getMagnitude();
		double extinction = getExtinction();
		double z = getRedshift();
		double distanceModulus = c.getDistanceModulus(z);
		final double kcorrection = getKCorrection() - kCorrectionOffsetZ2;
		
		double magAbs = apparentMag + extinction - distanceModulus - kcorrection;
		return magAbs;
		
	}
	
	/**
	 * Get the K Correction for this quasar, based on an assumption of spectral index = -0.5.
	 * Note that the K Correction does NOT include an offset to z=2. For z=2 absolute magnitude
	 * calculations you should further substract {@link #kCorrectionOffsetZ2}.
	 * @return K Correction.
	 */
	public double getKCorrection() {
		
		return getKCorrection(getRedshift());
		
	}

	
	/**
	 * Get the K Correction for the specified red shift, based on an assumption of spectral index = -0.5.
	 * Note that the K Correction does NOT include an offset to z=2. For z=2 absolute magnitude
	 * calculations you should further substract {@value #kCorrectionOffsetZ2}.
	 * @return K Correction.
	 */
	private static double getKCorrection(double z) {
		
		return -2.5 * (1 + kStdSpectralIndex) * Math.log10(1 + z);
		
	}

	
	/**
	 * Get quasar PSF i-band apparent magnitude.
	 * @return apparent magnitude
	 */
	public double getMagnitude() {
		
		return magPSF_i;
		
	}
	
	
	/**
	 * Get quasar i-band extinction.
	 * @return extinction in magnitudes
	 */
	public double getExtinction() {
		
		return ext_i;
		
	}

	
	/**
	 * Indicates if this is a broad line absorption quasar.
	 * @return true if quasar is a BAL
	 */
	public boolean isBAL() {
		
		return bal_flag_vi;
		
	}
	
	
}

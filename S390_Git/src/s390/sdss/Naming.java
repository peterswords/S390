/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.sdss;

/**
 * Algorithms for naming and storage of SDSS objects.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class Naming {
	
	/**
	 * Given its J2000 epoch coordinates, gets the IAU designation of an SDSS
	 * object. This has the format: "SDSS JHHMMSS.ss+DD:MM:SS.s", where
	 * HHMMSS.ss are the hours, minutes and seconds (to two decimal places) of
	 * right acension, and DDMMSS.s are the degree, minutes and seconds (to one
	 * decimal place) of declination. The plus sign becomes a minus for negative
	 * declinations. The seconds of RA and dec are truncated (not rounded) to 2
	 * and 1 decimal places respectively.
	 * 
	 * @see <a href="http://www.sdss2.org/dr7/coverage/IAU.html">SDSS IAU Designations</a>
	 * 
	 * @param ra
	 *            Right Acension of object in floating point degrees format
	 * @param dec
	 *            Declination of object in floating point degrees format
	 * @return a string containing the IAU designation.
	 */
	public static String getNameIAU(double ra, double dec) {
		
		double f = ra  / 15; // fractional ra hours
		int raHH = (int)Math.floor(f); // integer ra hours
		f = (f - raHH) * 60; // fractional ra minutes
		int raMM = (int)Math.floor(f); // integer ra minutes
		double raSS = (f - raMM) * 60; // fractional ra seconds
		raSS = Math.floor(raSS * 100) / 100; // truncate ra seconds to 2 places
		
		char sign = dec < 0? '-' : '+';
		
		f = Math.abs(dec); // fractional degrees as a positive number
		int decDD = (int)Math.floor(f); // integer dec degrees
		f = (f - decDD) * 60; // fractional dec minutes
		int decMM = (int)Math.floor(f); // integer dec minutes
		double decSS = (f - decMM) * 60; // fractional dec seconds
		decSS = Math.floor(decSS * 10) / 10; // truncate dec seconds to 1 place
		
		return String.format("SDSS J%02d%02d%05.2f%c%02d%02d%04.1f",
				raHH, raMM, raSS, sign, decDD, decMM, decSS);
	}
	

	/**
	 * Construct a relative spectrum FITS file name based on plate, mjd, fiber.
	 * This is the structure under which the spectrum FITS files are stored on
	 * the SDSS CAS server.
	 * 
	 * @param plate
	 *            the plate number
	 * @param mjd
	 *            the modified Julian date of the observation
	 * @param fiber
	 *            the id of the fiber on the plate
	 * @return a file name string with the format
	 *         pppp/spec-pppp-mmmmm-ffff.fits, where pppp is the 4-digit plate
	 *         number, mmmmm is the 5-digit mjd date, and ffff is the 4-digit
	 *         fiber id.
	 */
	public static String getSpecFileName(int plate, int mjd, int fiber) {
		
		return String.format("%04d/spec-%04d-%05d-%04d.fits", plate, plate, mjd, fiber);
		
	}
	
	
}

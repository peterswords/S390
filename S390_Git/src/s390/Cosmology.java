/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390;


/**
 * Encapsulates cosmology parameters for luminosity distance calculations. Uses
 * <a href="http://www.star.bristol.ac.uk/~mbt/stilts/">STILTS</a> under LGPL.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class Cosmology {

	/**
	 * Hubble parameter
	 */
	private double H0;
	
	/**
	 * Mass density parameter
	 */
	private double omegaM;
	
	/**
	 * Dark energy density parameter
	 */
	private double omegaLambda;
	
	/**
	 * Constructor: Initialise cosmology parameters.
	 * @param h0 Hubble parameter
	 * @param omegaM mass density
	 * @param omegaLambda dark energy density
	 */
	Cosmology(double h0, double omegaM, double omegaLambda) {
		this.H0 = h0;
		this.omegaM = omegaM;
		this.omegaLambda = omegaLambda;
	}
	

	/**
	 * Calculate the <a
	 * href="http://en.wikipedia.org/wiki/Distance_modulus">distance modulus</a>
	 * 5 * log10( dL(z) / 10) for red shift z where dL(z) is the <a
	 * href="http://en.wikipedia.org/wiki/Luminosity_distance">luminosity
	 * distance</a>.
	 * 
	 * @param z
	 *            red shift
	 * @return distance modulus
	 */
	public double getDistanceModulus(double z) {
		
		double dL = getLuminosityDistance(z);
		
		// dL is in units of megaparsecs (Mpc). We need it in units of 10 pc, to calculate the distance modulus.
		// So we multiply by 10^6 and divide by 10, thus multiply by 10^5.
		double dM = 5 * Math.log10(dL * 1E5);
		
		return dM;
	}
	
	
	/**
	 * Calculate a <a
	 * href="http://en.wikipedia.org/wiki/Luminosity_distance">luminosity
	 * distance</a> from a red shift for this cosmology.
	 * 
	 * @param z
	 *            red shift
	 * @return the luminosity distance in megaparsecs
	 */
	public double getLuminosityDistance(double z) {
		
		// Luminosity distance from STILTS, used under LGPL, http://www.star.bristol.ac.uk/~mbt/stilts/
		double dL = uk.ac.starlink.ttools.func.Distances.luminosityDistance(z, H0, omegaM, omegaLambda);

		return dL;
		
	}
	
}

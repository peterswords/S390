/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.sdss.spec;

/**
 * A single pixel of a spectrum. Based on the HDU1 "COADD" Binary Table from the
 * SDSS III <a href=
 * "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
 * >Spec data model</a>
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public interface SpecPixel {

	/**
	 * Get coadded calibrated flux [10-17 ergs/s/cm2/Å]
	 * @return flux
	 */
	float getFlux();
	
	
	/**
	 * Get log10(wavelength [Å])
	 * @return loglam
	 */
	float getLoglam();
	
	
	/**
	 * Get inverse variance of flux
	 * @return ivar
	 */
	float getIvar();
	
	
	/**
	 * Get AND mask
	 * @return AND mask
	 */
	int getAndMask();

	
	/**
	 * Get OR mask
	 * @return OR mask
	 */
	int getOrMask();
	
	
	/**
	 * Get wavelength dispersion in pixel=dloglam units
	 * @return wavelength dispersion
	 */
	float getWdisp();
	
	
	/**
	 * Get subtracted sky flux [10-17 ergs/s/cm2/Å].
	 * Note: this has already been subtracted from flux value.
	 * @return sky flux
	 */
	float getSky();
	
	
	/**
	 * Get pipeline best model fit used for classification and redshift
	 * @return model
	 */
	float getModel();
	
}

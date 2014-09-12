/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.sdss.spec;

import java.util.stream.Stream;


/**
 * Spectrum database reader interface. This database provides the same data for
 * a spectrum as specified in the COADD HDU1 table from the SDSS <a href=
 * "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
 * >Spec data model</a>
 * 
 * @see Spectrum
 * @see SpecPixel
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public interface SpectrumDbReader {

	/**
	 * Get an iterator over all object ids on the database
	 * in ascending order of object id.
	 * 
	 * @return Iterable
	 */
	public Iterable<Long> getAllObjID();
	
	
	/**
	 * Get a stream of all object ids on the database
	 * in ascending order of object id.
	 * 
	 * @return stream
	 */
	Stream<Long> idStream();

	
	/**
	 * Retrieve a spectrum from the database.
	 * 
	 * @param objID
	 *            the unique object id of the spectrum to retrieve.
	 * @return the retrieved spectrum or null if none found
	 */
	public Spectrum get(long objID);

}

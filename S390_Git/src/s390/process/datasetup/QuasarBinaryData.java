/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.datasetup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;

import s390.Quasar;
import s390.fits.FITSBinaryFile;
import s390.fits.FITSTable;
import s390.sdss.Naming;

/**
 * Helper class for loading quasar data from FITS binary tables.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class QuasarBinaryData implements Serializable {
	
	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;

	
	/**
	 * Directory for SDSS DR10Q raw spectrum FITS files
	 */
	static File dirSpecFiles;
	
	
	/**
	 * Access to FITS binary table data
	 */
	FITSBinaryFile tables;
	
	
	/**
	 * Associated quasar.
	 */
	Quasar q;
	
	
	/**
	 * Constructor. Construct a quasar from a relational database row.
	 * @param r SQLResult containing database row
	 */
	QuasarBinaryData(Quasar q) {
		this.q = q;
	}
	
	
	/**
	 * Get associated quasar.
	 * 
	 * @return quasar
	 */
	Quasar getQuasar() {
		return q;
	}
	
	/**
	 * Open raw FITS spectrum file to access binary table data.
	 * @return a set of FITS tables
	 * @throws FileNotFoundException if the raw spectrum file is not found.
	 */
	private FITSBinaryFile getFITSTables() throws FileNotFoundException {
		if (tables == null) {
			String specFileName = Naming.getSpecFileName(q.getPlate(), q.getMJD(), q.getFiber());
			File specFile = new File(dirSpecFiles, specFileName);
			tables = new FITSBinaryFile(new FileInputStream(specFile));
		}
		return tables;
	}
	
	
	/**
	 * Get the COADD binary FITS table for this quasar's spectrum.
	 * 
	 * @return COADD binary table
	 * @throws FileNotFoundException
	 *             if the spectrum file cannot be found
	 */
	FITSTable getCoaddTable() throws FileNotFoundException {
		return getFITSTables().getTable("COADD");
	}
	
	
	/**
	 * Get the SPZLINE binary FITS table for this quasar's spectrum.
	 * 
	 * @return SPZLINE binary table
	 * @throws FileNotFoundException
	 *             if the spectrum file cannot be found
	 */
	FITSTable getSpzLineTable() throws FileNotFoundException {
		return getFITSTables().getTable("SPZLINE");
	}
	
	
	/**
	 * Nullify tables to allow garbage collection
	 */
	void close() {
		tables = null;
	}

	
}

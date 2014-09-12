/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.fits;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ps.util.TextUtils;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

/**
 * Provides simple access to FITS format binary tables from a FITS-compliant file.
 * @see <a href="http://fits.gsfc.nasa.gov/">NASA FITS documentation</a>
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class FITSBinaryFile {

	/** 
	 * Cached headers 
	 **/
	private Header[] header;
	
	
	/** 
	 * Cached Header Data Units 
	 **/
	private BasicHDU[] hdu;
	
	
	/** 
	 * Cached binary tables 
	 **/
	private BinaryTable[] table;

	
	/**
	 * Construct from input stream (typically a {@link FileInputStream} from a
	 * FITS format file).
	 * 
	 * @param in
	 *            an input stream containing the FITS table data
	 */
	public FITSBinaryFile(InputStream in) {
		
		try {
						
			// Use NASA FITS library to read the input stream
			Fits fits = new Fits(in);
			hdu = fits.read();
			header = new Header[hdu.length];
			table = new BinaryTable[hdu.length];
			
		} catch (FitsException ex) {
			throw new RuntimeException(ex);
		}
		
	}

	
	/**
	 * Get a binary FITS table by HDU number.
	 * 
	 * @param hduNum
	 *            the HDU number of the required table. Numbers start at zero.
	 * @return the binary FITS table, or null if the specified table was any
	 *         format other than a binary table.
	 * @throws IndexOutOfBoundsException if hduNum is out of range.
	 */
	public FITSTable getTable (int hduNum) {
		
		BinaryTable t = getInternalTable(hduNum);
		return t == null? null : new FITSTable(getHeader(hduNum), t);
		
	}

	
	/**
	 * Get a binary FITS table by name. Names are taken from the "EXTNAME"
	 * field of the table header.
	 * 
	 * @param tableName
	 *            the name of the required table.
	 * @return the binary FITS table, or null if the specified table was any
	 *         format other than a binary table.
	 * @throws IndexOutOfBoundsException if named table does not exist.
	 */
	public FITSTable getTable (String tableName) {
		
		int hduNum = getHduNumber(tableName);
		return getTable(hduNum);
		
	}
	
	
	/**
	 * Get names of all tables in this FITS file.
	 * 
	 * @return List of table names (based on EXTNAME attribute of each HDU)
	 */
	public List<String> getTableNames() {
		
		List<String> names = new ArrayList<String>(hdu.length);
		
		for (int i = 0; i < hdu.length; i++) {
			
			Header header = getHeader(i);
			String extName = header.getStringValue("EXTNAME");
			names.add(TextUtils.nullOrEmpty(extName)? "Unnamed" + i : extName);
			
		}
		
		return names;
		
	}
	

	/**
	 * Convert a table name into a corresponding hdu number.
	 * 
	 * @param tableName
	 *            the table name
	 * @return the hdu number or -1 if the named table is not found
	 */
	int getHduNumber(String tableName) {
		
		for (int i = 0; i < hdu.length; i++) {
			String extName = getHeader(i).getStringValue("EXTNAME");
			if (tableName.equals(extName)) {
				return (i);
			}
		}
		
		// Not found
		return -1;
		
	}

	
	/**
	 * Accessor for table header
	 * 
	 * @param hduNum
	 *            hdu number of table
	 * @return the table header object
	 */
	Header getHeader(int hduNum) {
		if (header[hduNum] == null) {
			header[hduNum] = hdu[hduNum].getHeader();		
		}
		return header[hduNum];
	}
	
	
	/**
	 * Get a binary FITS table by HDU number, returning the FITS library
	 * internal BinaryTable format.
	 * 
	 * @param hduNum
	 *            the HDU number of the required table. Numbers start at zero.
	 * @return the binary FITS table, or null if the specified table was any
	 *         format other than a binary table.
	 * @throws IndexOutOfBoundsException if hduNum is out of range.
	 */
	public BinaryTable getInternalTable (int hduNum) {
		
		if (table[hduNum] == null) {
			// if not already cached, check if binary table exists
			Data data = hdu[hduNum].getData();
			if (data instanceof BinaryTable) {
				table[hduNum] = (BinaryTable)data;
			}
		}
		return table[hduNum];
		
	}

	
	
}

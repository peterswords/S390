/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.fits;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import nom.tam.fits.BinaryTable;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import ps.util.TextUtils;

/**
 * Provides simple access to a single FITS format binary table.
 * @see <a href="http://fits.gsfc.nasa.gov/">NASA FITS documentation</a>
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class FITSTable {

	/**
	 * Table header
	 */
	private Header header;

	
	/**
	 * Internal FITS library BinaryTable 
	 */
	private BinaryTable table;

	
	/**
	 * Create from input stream (typically a FileInputStream from a FITS format
	 * file).
	 * 
	 * @param in
	 *            the input stream containing the FITS tables
	 * @param hduNum
	 *            number of hdu containing table
	 */
	public FITSTable(InputStream in, int hduNum) {
		
		FITSBinaryFile t = new FITSBinaryFile(in);
		header = t.getHeader(hduNum);
		table = t.getInternalTable(hduNum);
		
	}
	

	/**
	 * Create from input stream (typically a FileInputStream from a FITS format
	 * file).
	 * 
	 * @param in
	 *            the input stream containing the FITS tables
	 * @param tableName
	 *            name ("EXTNAME") of hdu containing table
	 */
	public FITSTable(InputStream in, String tableName) {
		
		FITSBinaryFile t = new FITSBinaryFile(in);
		int hduNum = t.getHduNumber(tableName);
		header = t.getHeader(hduNum);
		table = t.getInternalTable(hduNum);
		
	}

	
	/**
	 * 
	 * Protected constructor for use by FitsTables.
	 * 
	 * @param h
	 *            table header
	 * @param t
	 *            binary table
	 */
	FITSTable(Header h, BinaryTable t) {
		
		header = h;
		table = t;
		
	}

	
	/**
	 * Dump header to stdout for diagnostic purposes.
	 */
	public void dumpHeader() {
		header.dumpHeader(System.out);
	}
	

	/**
	 * Get number of columns in this table.
	 * 
	 * @return number of columns in table
	 */
	public int getNCols() {
		
		return table.getNCols();
		
	}
	

	/**
	 * Get number of rows in this table.
	 * 
	 * @return number of rows in table
	 */
	public int getNRows() {
		
		return table.getNRows();
		
	}
	
	
	/**
	 * Returns tables in row order. The table is a list of rows. Each row is a
	 * list of columns represented as boxed primitives. Note that the entire
	 * table is read into memory in order to return the list of rows. Very large
	 * tables may need the use of lower-level FITS access for efficiency.
	 * 
	 * @return List of rows, each containing a list of primitive columns
	 */
	public List<List<Object>> getTableInRowOrder() {
		
		// Get number of rows and columns
		int nRows = table.getNRows(), nCols = table.getNCols();
		
		// Allocate an array of column values and get from FITS file
		Object[] colValue = new Object[nCols];
		try {
			for (int col = 0; col < nCols; col++) {
				colValue[col]= table.getColumn(col);
			}
		} catch (FitsException ex) {
			throw new RuntimeException(ex);
		}
		
		// Flatten the list of columns into a list of rows x columns
		List<List<Object>> rowList = new ArrayList<List<Object>>(nRows);

		for (int row = 0; row < nRows; row++) {
			
			List<Object> colList = new ArrayList<Object>(nCols);
			rowList.add(colList);
			for (int col = 0; col < nCols; col++) {
				colList.add(Array.get(colValue[col], row));
			}
			
		}
		
		return rowList;
		
	}

	/**
	 * Get a single column from a FITS table.
	 * 
	 * @param colNum
	 *            column number
	 * @return Object of primitive array type representing this column
	 */
	public Object getColumn(int colNum) {
		
		try {
			
			return table.getColumn(colNum);
			
		} catch (FitsException ex) {
			System.err.println("err: accessed col " + colNum + " ... dumping header:");
			header.dumpHeader(System.err);
			throw new RuntimeException(ex);
		}
	}

	
	/**
	 * Get a single column from a FITS table by column name.
	 * 
	 * @param colName
	 *            column name (corresponds to TTYPE field in header)
	 * @return Object of primitive array type representing this column
	 */
	public Object getColumn(String colName) {
		
		int colNum = 0;
		
		for (String s : getColumnNames()) {
			
			if (colName.equals(s)) break;
			colNum++;

		}
		
		try {
			
			return table.getColumn(colNum);
			
		} catch (FitsException ex) {
			throw new RuntimeException(ex);
		}
		
	}

	
	/**
	 * Get names of table columns
	 * 
	 * @return list of column names
	 */
	public List<String> getColumnNames() {
		
		return getNumberedHeaderCards("TTYPE", true, null);
		
	}
	
		
	/**
	 * Get data types of table columns
	 * 
	 * @return list of column types
	 */
	public List<String> getColumnTypes() {
		
		return getNumberedHeaderCards("TFORM", false, null);
		
	}

	
	/**
	 * Get units of table columns
	 * 
	 * @return list of column units
	 */
	public List<String> getColumnUnits() {
		
		return getNumberedHeaderCards("TUNIT", false, "-");
		
	}

	
	/**
	 * Get comments from table columns
	 * 
	 * @return list of comments
	 */
	public List<String> getColumnComments() {
		
		return getNumberedHeaderCards("TCOMM", false, null);
		
	}
		
	
	/**
	 * 
	 * Get column metadata from header cards. The type of metadata is set by
	 * baseName, e.g. if baseName is TTYPE the method will find cards with keys
	 * of TTYPE1, TTYPE2 etc.
	 * 
	 * @param baseName
	 *            base name of metadata e.g. TTYPE
	 * @param useDefaults
	 *            if true, and card value is empty, use card key as default
	 *            value. For instance, if baseName is TTYPE and the third card
	 *            with key TTYPE3 has an empty value, the card value is returned
	 *            as TTYPE3.
	 * @param nullInd
	 *            a sentinel value representing null -- if a card has this
	 *            value return an empty string instead.
	 * @return list of string values
	 */
	private List<String> getNumberedHeaderCards(String baseName, boolean useDefaults, String nullInd) {
		
		int nCols = table.getNCols();	
		List<String> result = new ArrayList<String>(nCols);
		
		for (int col = 0; col < nCols; col++) {
			
			String defaultName = baseName + (col + 1);
			String cardValue = header.getStringValue(defaultName);
			
			if (TextUtils.nullOrEmpty(cardValue)) {
				
				cardValue = useDefaults? defaultName : cardValue;
				
			} else {
				
				cardValue = cardValue.equals(nullInd)? "" : cardValue;
				
			}
			
			result.add(cardValue);
			
		}
		
		return result;
		
	}
		
}

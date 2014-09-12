/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.sdss.spec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import s390.fits.FITSTable;
import s390.fits.FITSBinaryFile;

/**
 * Converts an SDSS FITS spectrum file for use in a spreadsheet. The coadded
 * spectrum can either be output in csv format, or the spectrum plus additional
 * information from other FITS tables in the spectrum file can be output as an
 * Excel workbook with three worksheets.
 * 
 * The csv file just contains the contents of the COADD table, whereas the Excel
 * workbook additionally contains the SPZLINE table and ancillary
 * information.
 * 
 * See the SDSS <a href=
 *      "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
 *      > Spec data model</a> for further information.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class Spectrum2Spreadsheet {

	/**
	 * Input stream containing FITS format data
	 */
	InputStream fits;
	
	/**
	 * Input stream to which either cvs or Excel data will be written.
	 */
	OutputStream out;
	
	
	/**
	 * Constructor.
	 * 
	 * @param fits
	 *            an input stream containing FITS format data
	 * 
	 * @param out
	 *            an output stream to which either csv or Excel data will be
	 *            written.
	 */
	public Spectrum2Spreadsheet(InputStream fits, OutputStream out) {
		
		this.fits = fits;
		this.out = out;
		
	}
	
	/**
	 * Convert coadded spectrum to csv format.
	 */
	public void csv() {
		
		// Get the COADD table from HDU1 of the FITS file
		FITSTable table = new FITSBinaryFile(fits).getTable(1);
		
		// Open a PrintWriter on the output stream
		PrintWriter pw = new PrintWriter(out);

		// Print the csv column headings
		String sep = "";
		for (String s : table.getColumnNames()) {
			pw.print(sep);
			sep = ",";
			pw.print(s);
		}
		pw.print('\n');
		
		// Print csv data
		for (Collection<Object> row : table.getTableInRowOrder()) {
			sep = "";
			for (Object o : row) {
				pw.print(sep);
				sep = ",";
				pw.print(o);
			}
			pw.print('\n');
		}
		
		pw.close();
		
	}
	
	
	/**
	 * Convert FITS spectrum to Excel workbook.
	 * 
	 * @throws IOException if an I/O exception occurs
	 */
	public void xls() throws IOException {

		// Open input FITS file
		FITSBinaryFile tables = new FITSBinaryFile(fits);
		
		// Create output workbook
		HSSFWorkbook workbook = new HSSFWorkbook();
		
		// Convert each of the three input binary tables to a separate worksheet
		table2Sheet(tables.getTable(1), workbook, "Spectrum");
		table2Sheet(tables.getTable(2), workbook, "Info");
		table2Sheet(tables.getTable(3), workbook, "Lines");
		
		// Write out the workbooks contents
		workbook.write(out);
		
	}
	
	
	/**
	 * Convert an FITS binary table to an Excel worksheet.
	 * 
	 * @param table
	 *            the input FITS binary table.
	 * @param workbook
	 *            the workbook in which the sheet will be created
	 * @param sheetName
	 *            the name of the worksheet to create
	 */
	private void table2Sheet(FITSTable table, HSSFWorkbook workbook, String sheetName) {

		// Create worksheet
		HSSFSheet sheet = workbook.createSheet(sheetName);
		
		// Row and column counters.
		int xrowNum = 0, xcolNum = 0;
		
		// Create the header row for column names
		HSSFRow xrow = sheet.createRow(xrowNum);
		for (String s : table.getColumnNames()) {
			HSSFCell xcol = xrow.createCell(xcolNum);
			xcol.setCellValue(s);
			xcolNum++;
		}
		
		// Iterate over rows and columns outputting cells to sheet.
		for (Collection<Object> row : table.getTableInRowOrder()) {
			xrowNum++; xcolNum = 0;
			xrow = sheet.createRow(xrowNum);
			for (Object o : row) {
				HSSFCell xcol = xrow.createCell(xcolNum);
				setCell(xcol, o);
				xcolNum++;
			}
		}
	}
	
	
	/**
	 * Set value of a spreadsheet cell.
	 * 
	 * @param cell
	 *            the cell to set
	 * @param o
	 *            an object containing the value
	 */
	private void setCell(HSSFCell cell, Object o) {
		
		// Handle strings first
		if (o instanceof String) {
			cell.setCellValue((String)o);
			return;
		}
		
		// Cast any numeric types to double.
		// Handles Long, Integer, Byte, Float or Double.
		// All other types are converted to Double.NaN.
		double d = o instanceof Long ? (Long) o
				: o instanceof Integer ? (Integer) o
						: o instanceof Short ? (Short) o
								: o instanceof Byte ? (byte) o & 0xff
										: o instanceof Float ? (Float) o
												: o instanceof Double ? (Double) o
														: Double.NaN;
												
		// If we have a non-NaN value, set that and finish
		if (!Double.isNaN(d)) {
			cell.setCellValue(d);
			return;
		}
		
		// Handle array types by saying we don't handle them.
		if (o.getClass().getName().startsWith("[")) {
			cell.setCellValue("#Array value ignored");
			return;
		}
		
		// We know here that the object is not a string or numeric type,
		// so the following cast will intentionally fail with a ClassCastException.
		cell.setCellValue((double)o);
		
	}
	
	
}

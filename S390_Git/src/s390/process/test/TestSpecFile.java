/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

import org.junit.Ignore;
import org.junit.Test;

import s390.sdss.spec.Spectrum2Spreadsheet;

/**
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class TestSpecFile {

	final static String wdir = "C:/Users/peter/Documents/Open University/SXP390/Coding/";
	final static String ndir = "http://data.sdss3.org/sas/dr10/boss/spectro/redux/v5_5_12/spectra/lite/";
	final static String nfile = "%s%04d/spec-%04d-%05d-%04d.fits";
	final static String nout = "%sspec-%04d-%05d-%04d.xls";

	@Test public void t() {
		for (int j = 1; j <= 100; j++){
			int m = 0, n = 0;
			for (int i = 1; i <= j; i++) {
				m += i;
				n += i*i;
			}
		m *= m;
		System.out.printf("m=%s, n=%s, m-n = %s, (m-n)/j = %s%n", m, n, m-n, (m-n)/j);
		}
		//System.out.printf("m=%s, n=%s, m-n = %s%n", m, n, m-n);
	}
	
	
	@Test
	@Ignore
	public void testy() throws FitsException, IOException {
		
		int plate = 3586, mjd = 55181, fiber = 1;
		
		final String strFITS = String.format(nfile, ndir, plate, plate, mjd, fiber);
		final String strXls = String.format(nout, wdir, plate, mjd, fiber);
		InputStream fits = (new URL(strFITS)).openStream();
		OutputStream xls = new FileOutputStream(strXls);
		new Spectrum2Spreadsheet(fits, xls).xls();;
		
	}

	@Test
	@Ignore
	public void testx() throws FitsException, IOException {
		final String strFITS =
				wdir + "spec-4549-55556-0022.fits";
		final String strXls =
				wdir + "spec-4549-55556-0022.xls";
		InputStream fits = new FileInputStream(strFITS);
		OutputStream xls = new FileOutputStream(strXls);
		new Spectrum2Spreadsheet(fits, xls).xls();;
	}
		
	@Test
	@Ignore
	public void test1() throws FitsException {
		final String strFITS =
			//wdir + "spec-0515-52051-0126.fits";
				wdir + "spec-4549-55556-0022.fits";
		
		Fits fits = new Fits(new File(strFITS));
		BasicHDU[] hdu = fits.read();
		System.out.printf("FITS file %s\n", strFITS);
		doData(hdu[1], hdu1cols);
		doData(hdu[3], hdu3cols);
	}

	final boolean f = false;
	final boolean t = true;
	final boolean hdu1cols[] = new boolean[] {t,t,t,t,t,t,t,t};
	final boolean hdu3cols[] = new boolean[] {f,f,f,t,t,t,t,t,t,t,t,t,t,t,t,t,t,t,t};
	
	private void doData(BasicHDU hdu, boolean[] useCol) throws FitsException {
		int nCols = useCol.length;
		Header header = hdu.getHeader();
		String extName = header.getStringValue("EXTNAME");
		System.out.printf("%s %s\n", extName, hdu.getClass().getName());
		//printHeader(header);
		
		BinaryTable table = (BinaryTable)hdu.getData();		
		//char ttype[] = table.getTypes();
		String colName[] = new String[nCols];
		Object colValue[] = new Object[nCols];
		for (int col = 0; col < nCols; col++) {
			if (useCol[col]) {
				colName[col] = header.getStringValue("TTYPE"+(col+1));
				colValue[col]= table.getColumn(col);
			}
			//System.out.printf("%s %s %s\n", col, ttype[col], colName[col]);
		}
		
		// Print col headers
		boolean first = true;
		for (int col = 0; col < nCols; col++) {
			if (useCol[col]) {
				if (!first) {System.out.print(",");} first = false;
				System.out.print(colName[col]);
			}
		}
		System.out.println();
		
		// Print row data
		int nRows = table.getNRows();
		for (int row = 0; row < nRows; row++) {
			first = true;
			for (int col = 0; col < nCols; col++) {
				if (useCol[col]) {
					if (!first) {System.out.print(",");} first = false;
					System.out.print(Array.get(colValue[col], row));
				}
			}
			System.out.println();
		}
	}

	@SuppressWarnings("unused")
	private void printHeader(Header header) {
		int j = 0;
		for (Cursor c = header.iterator(); c.hasNext(); j++) {
			HeaderCard hc = (HeaderCard)c.next();
			System.out.println("  " + hc.toString());
		}
		System.out.println("  Cards: " + j);
	}
	
}

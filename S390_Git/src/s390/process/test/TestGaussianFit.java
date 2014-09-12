/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.test;

import java.io.File;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

import org.junit.Test;

/**
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class TestGaussianFit {

	@Test
	public void test1() throws FitsException {
		final String strFITS =
			//"C:/Users/peter/Documents/Open University/SXP390/Coding/spec-0515-52051-0126.fits";
			"C:/Users/peter/Documents/Open University/SXP390/Coding/spec-4549-55556-0022.fits";
		
		Fits fits = new Fits(new File(strFITS));
		BasicHDU[] hdu = fits.read();
		System.out.printf("FITS file %s\n", strFITS);
		doData(hdu[1]);
		doData(hdu[3]);
	}

	private void doData(BasicHDU hdu) throws FitsException {
		Header header = hdu.getHeader();
		String extName = header.getStringValue("EXTNAME");
		System.out.printf("%s %s\n", extName, hdu.getClass().getName());
		//printHeader(header);
		
		BinaryTable table = (BinaryTable)hdu.getData();		
		char ttype[] = table.getTypes();
		String colName[] = new String[ttype.length];
		for (int i = 0; i < colName.length; i++) {
			colName[i] = header.getStringValue("TTYPE"+(i+1));
			System.out.printf("%s %s %s\n", i, ttype[i], colName[i]);
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

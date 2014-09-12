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

import javax.inject.Inject;

import ps.util.FileUtils;
import ps.util.ModuleProperties;
import s390.Runner;
import s390.sdss.Naming;
import s390.sdss.spec.Spectrum2Spreadsheet;

/**
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class OneOffTest implements Runnable {

	public static void main(String[] args) {
		Runner.run(OneOffTest.class);
	}
	
	
	@Inject ModuleProperties props;
	
	
	@Override
	public void run() {

		try {
			
			test1();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public void test1() throws IOException {
		
		// obj_id	1237679320164401417	

		int plate = 5648, mjd = 55923, fiber = 808;

		String strOut = FileUtils.expandUserHome(props.get("dirOut"));
		String strDir = FileUtils.expandUserHome(props.get("dirSpectrumFITS"));
		String strFile = Naming.getSpecFileName(plate, mjd, fiber);
		FileInputStream fi = new FileInputStream(new File(strDir, strFile));
		FileOutputStream fo = new FileOutputStream(new File(strOut, "xx.csv"));
		Spectrum2Spreadsheet s = new Spectrum2Spreadsheet(fi, fo);
		s.csv();
	}
	
	
}

/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process;

import java.io.File;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.inject.Named;

import ps.db.SQL;
import ps.util.SupplierX;
import s390.Cosmology;
import s390.Quasar;
import s390.Runner;

/**
 * Output quasar luminosity and redshift in order to make a scatter plot.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class LuminosityRedshiftPlane implements Runnable {

	// Dependencies
	
	/**
	 * SQL for database processing
	 */
	@Inject SQL sql;
	
	
	/**
	 * Cosmology parameters
	 */
	@Inject Cosmology cosmology;
	
	
	/**
	 * Parameter: output directory.
	 */
	@Inject @Named("dirOut") File dirOut;
	
		
	/**
	 * Main entry point.
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {
		Runner.run(LuminosityRedshiftPlane.class);
	}
	
	
	/**
	 * Main program
	 */
	@Override
	public void run() {

		PrintWriter pw = SupplierX.get( () -> new PrintWriter(new File(dirOut, "QLumZ.csv")));
		
		pw.println("z,M,Mcalc");
		
		Quasar.queryGood(sql).forEach(
				q -> {
					pw.printf("%s,%s,%s\n", q.getRedshift(),
							q.getAbsoluteMagnitude(),
							q.getAbsoluteMagnitudeEmission(cosmology));
				});
		
		pw.close();
		
	}


}

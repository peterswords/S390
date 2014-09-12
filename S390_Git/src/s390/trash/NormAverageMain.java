/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.trash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import ps.db.SQL;
import ps.util.Report;
import s390.Quasar;
import s390.Runner;
import s390.composite.IntermediateSpectrum;
import s390.normalisation.NormalisationRange;
import s390.normalisation.QuasarAverageFlux;
import s390.sdss.spec.SpectrumDbReader;

/**
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class NormAverageMain implements Runnable {

	/**
	 * Main entry point
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {
		Runner.run(NormAverageMain.class);
	}
	
	
	/**
	 * SQL for database manipulation
	 */
	@Inject SQL sql;
	
	
	/**
	 * Spectrum database
	 */
	@Inject SpectrumDbReader specDb;
	
	
	/**
	 * Diagnostic reporting
	 */
	@Inject Report report;
	
	
	/**
	 * Parameter: output directory.
	 */
	@Inject @Named("dirOut") File dirOut;
	
		
	/**
	 * Main program
	 */
	@Override
	public void run() {

		try {
			
			doNormalisation();
			
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
	}


	/**
	 * Calculate average flux values in specified ranges for all quasars.
	 * @throws FileNotFoundException 
	 */
	void doNormalisation() throws FileNotFoundException {

		// Create list of quasar average fluxes for all non-BAL quasars
		List<QuasarAverageFlux> listQuasarFlux = QuasarAverageFlux
				.getQuasarAverageFluxes(sql, specDb, report);
		
		// Output the results
		report.info("Finishing...");
		PrintWriter pw = new PrintWriter(new File(dirOut, "QuasarAverageFlux.csv"));

		for (QuasarAverageFlux ns : listQuasarFlux) {
			Quasar q = ns.getQuasar();
			double z = q.getRedshift();
			double m = q.getAbsoluteMagnitude();
			NormalisationRange nr = NormalisationRange.getNormalisation(z);
			double mz = nr == null? -1 : nr.getMidZ();
			pw.printf("%s,%s,%s",z,mz,m);
			for (double d : ns.getAvgRange()) {
				pw.printf(",%s",d);
			}
			pw.println();
		}
		pw.close();
		
	}
	

}

/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.datasetup;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import ps.db.SQL;
import ps.util.FileUtils;
import ps.util.ModuleProperties;
import ps.util.SupplierX;
import s390.Quasar;
import s390.Runner;
import s390.sdss.Naming;

/**
 * Quick and dirty utility program for generating Bash shell scripts to download
 * SDSS DR10 spectrum files. Reads in the list of quasars from SQL database
 * and generates a WGET command for each corresponding spectrum FITS file. The WGETs
 * are batched into script files containing at least 1,000 commands apiece -- this
 * allows them to be run in parallel, since the SDSS CAS server limits the bandwidth
 * it will devote to any single connection. Without parallelisation, downloading the
 * full quarter terabyte of spectrum data would take an unreasonably long time.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class SpecBatFiles implements Runnable {

	/**
	 * Program main entry point
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String args[]) {
		
		Runner.run(SpecBatFiles.class);
		
	}
	
	
	/**
	 * SQL for database access
	 */
	@Inject SQL sql;

	
	/**
	 * Property file containing program settings
	 */
	@Inject ModuleProperties properties;

	
	/**
	 * Main program.
	 * 
	 */
	@Override
	public void run() {
		
		// Validate output directory for created batch files
		final String strDirBat = FileUtils.expandUserHome(properties.get("dirSpecBatFile"));
		File dOut = new File(strDirBat);
		if (!dOut.isDirectory()) {
			throw new RuntimeException("dirBat property must be existing output directory");
		}
		
		
		// List of quasars sorted by plate/mjd/fiber
		List<Quasar> quasars = Quasar.queryAll(sql)
				.sorted(Comparator.comparing(Quasar::getPlate))
				.collect(Collectors.toList());
		

		// Partitition into lists containing at least kBatchSize spectra,
		// but which must contain whole plates (for neatness).
		final int kBatchSize = 1000;
		List<List<Quasar>> batches = new ArrayList<>();
		List<Quasar> currentBatch = new ArrayList<>();
		int currentPlate = -1;
		int numPlates = 0;
		int numSpectra = 0;
		for (Quasar q : quasars) {
			if (currentPlate != q.getPlate()) {
				// new plate
				currentPlate = q.getPlate();
				numPlates++;
				if (numSpectra >= kBatchSize) {
					// batch is full
					batches.add(currentBatch);
					currentBatch = new ArrayList<Quasar>();
					numSpectra = 0;
				}
			}
			numSpectra++;
			currentBatch.add(q);
		}
		if (currentBatch.size() > 0) {
			// Add final batch is necessary
			batches.add(currentBatch);
		}
		
		
		// Create the output batch files. One overall file contains wget commands with batches of
		// file names for downloading in separate text files.
		PrintWriter pwAll = new PrintWriter(SupplierX.get( () -> new FileWriter(new File(dOut, "getq.bat"))));
		for (List<Quasar> batch : batches) {
			// Output file format for a batch is qpppp.txt where pppp is plate number
			String strPlate = String.format("q%04d.txt", batch.get(0).getPlate());
			// Command in overall file to wget the named files in a batch
			pwAll.printf("$WGETCMD -i $WGETQ/%s\n", strPlate);
			PrintWriter pw = new PrintWriter(SupplierX.get( () -> new FileWriter(new File(dOut, strPlate))));
			// Spectrum file names in batch
			for (Quasar q : batch) {
				String specFileName = Naming.getSpecFileName(q.getPlate(), q.getMJD(), q.getFiber());
				pw.printf("%s\n", specFileName);
			}
			pw.close();
		}
		pwAll.close();
		System.out.printf("%s spectra on %s plates\n", quasars.size(), numPlates);
	}
	

}

/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;

import javax.inject.Inject;

import ps.db.SQL;
import ps.util.FileUtils;
import ps.util.ModuleProperties;
import s390.Cosmology;
import s390.Quasar;
import s390.Runner;

/**
 * Test the accuracy of absolute magnitude calculations by a number of different
 * methods. Creates a csv format output that can be graphed in Excel. Output
 * columns are:
 * 
 * z (red shift), m (apparent magnitude), M (absolute magnitude as calculated by
 * SDSS processing pipeline), MC (absoluted magnitude calculated using a
 * continuum-only K Correction based on assumption of frequency spectral index
 * of -0.5), MT (absolute magnitude based on continuum and emission K
 * Corrections using Table 4 from Richards et al, 2006 SDSS QLF), e (SDSS
 * extinction value), d (distance modulus, calculated using luminosity distance
 * via STILTS), k (the K Correction using in MC), k2 (the K Correction used in
 * MT).
 * 
 * The columns can be combined in Excel to graph differences between the different
 * luminosity calculation methods.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class TestMagnitudeAccuracy implements Runnable {

	public static void main(String[] args) {
		Runner.run(TestMagnitudeAccuracy.class);
	}
	
	
	@Inject SQL sql;
	@Inject Cosmology c;
	@Inject ModuleProperties props;
	
	
	@Override
	public void run() {

		try {
			
			testMagnitudes();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public void testMagnitudes() throws IOException {
		
		// Create the output csv file
		String strDirOut = FileUtils.expandUserHome(props.get("dirOut"));
		PrintWriter pw = new PrintWriter(new File(strDirOut, "CmpMag1.csv"));
		
		// Column headings (see class documentation above)
		pw.printf("z,m,M,MC,MT,e,d,k,k2\n");

		// Get "good" quasars sorted by red shift and print columns to spreadsheet.
		Quasar.queryGood(sql)
				.sorted(Comparator.comparing(Quasar::getRedshift))
				.forEach(
						q -> {
							
							double z = q.getRedshift();
							double m = q.getMagnitude();
							double M = q.getAbsoluteMagnitude();
							double MC = q.getAbsoluteMagnitudeContinuum(c);
							double MT = q.getAbsoluteMagnitudeEmission(c);
							double extinction = q.getExtinction();
							double distanceModulus = c.getDistanceModulus(z);
							double k = q.getKCorrection();
							double k2 = q.getTableKCorrection();
							
							pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s\n", z, m, M, MC, MT, extinction, distanceModulus, k, k2);
						});
		
		pw.close();
		
	}
	
	
}

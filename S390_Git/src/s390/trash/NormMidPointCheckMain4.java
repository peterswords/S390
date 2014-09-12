/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.trash;

import ps.util.Report;
import s390.Runner;
import s390.normalisation.NormalisationRange;

/**
 * Check normalisation mid points used for different z
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class NormMidPointCheckMain4 implements Runnable {

	public static void main(String[] args) {
		Runner.run(NormMidPointCheckMain4.class);
	}
	
	Report report = new Report();
	
	@Override
	public void run() {

		doNormalisation();
		
	}


	void doNormalisation() {

		for (double z = 0; z < 4; z += 0.1) {
			NormalisationRange nr = NormalisationRange.getNormalisation(z);
			double mz = nr == null? -1 : nr.getMidZ();
			System.out.printf("%s,%s\n",z,mz);
		}
		
	}
	

}

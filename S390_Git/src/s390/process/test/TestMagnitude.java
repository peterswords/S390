/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.test;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import ps.db.SQL;
import ps.util.MainModule;
import ps.util.Report;
import s390.Cosmology;
import s390.CommonDependencies;
import s390.Quasar;

/**
 * Test how closely our computed absolute magnitude matches the SDSS one.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class TestMagnitude implements Runnable {
	
	public static void main(String[] args) {
		MainModule.run(TestMagnitude.class, new CommonDependencies());
	}
	
	@Inject SQL sql;
	@Inject Cosmology cosmology;
	Report report = new Report();
	
	@Override
	public void run() {

		// Get list of non-BAL quasars
		String strQuery = String.format("select %s from QCAT", Quasar.QCAT_FIELDS);
		
		List<Quasar> qlist = sql.query(strQuery).stream().map(Quasar::new)
				.filter(q -> !q.isBAL()).collect(Collectors.toList());

		long count;
		
		count = qlist.stream()
		.filter(q -> Math.abs(q.getAbsoluteMagnitude(cosmology) - q.getAbsoluteMagnitude()) > 0.2)
		.count();
		
		System.out.println(count);
		
		count = qlist.stream()
		.filter(q -> Math.abs(q.getAbsoluteMagnitude(cosmology) - q.getAbsoluteMagnitude()) <= 0.2)
		.count();
		
		System.out.println(count);
		
		qlist.stream()
		.limit(0)
		.limit(1000)
		.sorted((q1, q2) -> (int)Math.signum(q1.getRedshift() - q2.getRedshift()))
		.forEach(
				q -> System.out.printf("%.2f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n",
						q.getRedshift(), q.getAbsoluteMagnitude(),
						q.getAbsoluteMagnitude(cosmology),
						q.getAbsoluteMagnitude() - q.getAbsoluteMagnitude(cosmology),
						q.getKCorrection(), q.getExtinction()));
	}

}

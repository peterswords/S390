/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.test;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import ps.db.SQL;
import ps.db.SQLModule;
import ps.db.SQLResult;
import s390.process.datasetup.Qcat2db;

import com.google.inject.Guice;

/**
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class CurrentTest {

	final static String outdir = "wgetq";
	final static String strConn = "jdbc:h2:tcp://localhost/C:/Users/Peter/Documents/SXP390/db/DR10;USER=sa";
	final String strIndexPath = "C:/Users/peter/Documents/SXP390/dbf/SpecIndex.dbf";

	@Test
	@Ignore
	public void test1() throws IOException {
		final String strFITS = "C:/Users/peter/Documents/Open University/SXP390/Coding/"
				+"spec-4549-55556-0022.fits";
		//DumpFitsMetadata.main(new String[] {strFITS});
	}
	
	@Test
	@Ignore
	public void testqcat2db() throws IOException {
		String wdir = "C:/Users/peter/Documents/SXP390/Data/";
		String qfile = wdir + "DR10Q_v2.fits";
		String specDir = wdir + "spectra/"; 
		Qcat2db.main(new String[] {
				strConn, qfile, specDir, "true", "200"
		});
	}
	
	@Test
	@Ignore
	public void testFetchAllSpecSamples() throws IOException {
		long t0 = System.currentTimeMillis();
		System.out.println("0: Begin...");
		SQL sql = Guice.createInjector(new SQLModule(strConn)).getInstance(SQL.class);
		System.out.println((System.currentTimeMillis() - t0) + ": Rows...");
		int nrows = 0;
		int nq = 0;
		for (SQLResult qcat : sql.query("select OBJ_ID from QCAT").iter()) {
			nq++;
			long objID = qcat.get("OBJ_ID");
			for (SQLResult coadd : sql.query("select FLUX from COADD where OBJ_ID = ? order by SEQ", objID).iter()) {
				float flux = coadd.get("FLUX");
				nrows++;
				if (nrows % 10000 == 0) {
					System.out.print('.');
				}
				if (nrows % 1000000 == 0) {
					System.out.println(nrows);
				}
			}
		}
		System.out.println(nrows);
		System.out.println(nq);
		System.out.println((System.currentTimeMillis() - t0));
	}
	
	@Test
	//@Ignore
	public void testReadSpecIndex() throws IOException {
		TestSpectrumDbPerformance.main(new String[] {strConn, strIndexPath});
	}
	
}

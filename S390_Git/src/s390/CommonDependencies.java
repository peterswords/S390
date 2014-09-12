/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import javax.inject.Provider;

import ps.db.SQLModule;
import ps.util.BundleModule;
import ps.util.FileUtils;
import ps.util.SupplierX;
import ps.util.TextUtils;
import s390.sdss.spec.SpectrumDbReader;
import s390.sdss.spec.impl.SpectrumDbReaderImpl;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

/**
 * Configure dependencies using Google Guice. These can then be
 * acquired by dependency injection wherever they are needed.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class CommonDependencies extends BundleModule {

	/**
	 * Configure this Guice module.
	 */
	@Override
	public void configure(Binder binder) {
		
		// Configure relational database
		binder.install(new SQLModule(getString("db")));
		
		// Configure spectrum database
		binder.bind(SpectrumDbReader.class).toProvider(getSpecDb())
				.in(Scopes.SINGLETON);
		
		// Configure cosmology parameters
		binder.bind(Cosmology.class).toInstance(new Cosmology(
				Double.valueOf(getString("H0")),
				Double.valueOf(getString("Omega_M")),
				Double.valueOf(getString("Omega_Lambda"))
		));
		
		// Standard output directory
		String strDirOut = "dirOut";
		File fDirOut = new File(FileUtils.expandUserHome(getString(strDirOut)));
		binder.bind(File.class).annotatedWith(Names.named(strDirOut))
				.toInstance(fDirOut);
		
	}


	/**
	 * Get a Provider which can load the spectrum database on demand
	 * @return Provider for spectrum database
	 */
	Provider<SpectrumDbReader> getSpecDb() {

		// Configuration says whether to use "lite" or normal version of spectrum db
		// Lite version omits certain information for speed of processing
		String strLite = getString("useLite");
		boolean lite = !TextUtils.nullOrEmpty(strLite) && Boolean.valueOf(strLite);
		
		// Path name to spectrum database is configured in properties		
		String strIndexProp = lite? "spectrumDbLite" : "spectrumDb";
		String strSpecDb = FileUtils.expandUserHome(getString(strIndexProp));
		Path specDbPath = FileSystems.getDefault().getPath(strSpecDb);
		
		return SupplierX.getProvider( () -> new SpectrumDbReaderImpl(specDbPath));
		
	}


}

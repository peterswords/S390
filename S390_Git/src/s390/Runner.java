/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390;

import ps.util.MainModule;


/**
 * Run a module with common dependencies.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class Runner {

	/**
	 * Run the specified Runnable class
	 * @param clazz the Runnable to run
	 */
	public static void run(Class<? extends Runnable> clazz) {
		
		MainModule.run(clazz, new CommonDependencies());
		
	}
	
}

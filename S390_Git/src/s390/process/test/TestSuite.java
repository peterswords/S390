/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import ps.test.TestSQL;

@SuiteClasses({
	TestGaussianFit.class,
	TestSQL.class
})

/**
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
@RunWith(Suite.class)
public class TestSuite {

}

/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import ps.util.KeyValuePair;

/**
 * Miscellaneous useful functions and lambdas.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class Functions {

	/**
	 * Equivalent to binning(false, binSize, binSelector).
	 * 
	 * @see #binning(boolean, double, Function)
	 * 
	 * @param binSize
	 *            size of bin intervals
	 * @param binSelector
	 *            a function which returns the value to be binned
	 * @return binning function
	 */
	public static <T> Function<T, Double> binning(double binSize,
			Function<T, Double> binSelector) {

		return binning(false, binSize, binSelector);

	}	
	
	
	/**
	 * Create a binning function to allocate values across fixed size bins.
	 * Values may be either rounded to bin centres, or truncated to bin
	 * floors.
	 * 
	 * @param centreBin
	 *            if true, input values are rounded to the nearest bin start,
	 *            otherwise they are truncated to the next lowest bin start. For
	 *            instance, with a bin size of one, 0.6 would end up in the bin
	 *            starting at 1 if centre binning is used, otherwise it would
	 *            end up in the bin starting at 0.
	 * @param binSize
	 *            size of bin intervals
	 * @param binSelector
	 *            a function which returns the value to be binned
	 * @return binning function
	 */
	public static <T> Function<T, Double> binning(boolean centreBin, double binSize,
			Function<T, Double> binSelector) {

		return centreBin?
				
				t -> (((Double)(Math.round(binSelector.apply(t)) / binSize)) * binSize) :

				t -> ((Math.floor(binSelector.apply(t) / binSize)) * binSize);

	}	
	
	
	/**
	 * Collector which returns a map with values of some type T, keyed by a key
	 * created from T by the supplied key extractor function.
	 * 
	 * @param keyExtractor
	 *            takes an object of type T and produces a key for the map
	 * @return map of T keyed by extracted key
	 */
	public static <K, T> Collector<T, Map<K, T>, Map<K, T>> mapBy(
			Function<? super T, ? extends K> keyExtractor) {
		
		return mapBy(keyExtractor, t -> t);

	}

	
	/**
	 * Collector which returns a map with keys and values both of which are extracted
	 * from an input type T by the supplied key and value extractor functions.
	 * 
	 * @param keyExtractor
	 *            takes an object of type T and produces a key for the map
	 * @param valueExtractor
	 *            takes an object of type T and produces a value for the map
	 * @return map of V keyed by extracted key
	 */
	public static <K, V, T> Collector<T, Map<K, V>, Map<K, V>> mapBy(
			Function<? super T, ? extends K> keyExtractor,
			Function<? super T, ? extends V> valueExtractor) {
		
		return Collector.of(
				HashMap::new,
				(map, t) -> map.put(keyExtractor.apply(t),
						valueExtractor.apply(t)), (map1, map2) -> {
					map1.putAll(map2);
					return map1;
				}, Characteristics.CONCURRENT, Characteristics.UNORDERED,
				Characteristics.IDENTITY_FINISH);

	}

	
	/**
	 * Collector which returns a map with keys and values both of which are extracted
	 * from the map entry values of another map by the supplied key and value extractor functions.
	 * 
	 * @param keyExtractor
	 *            takes an object of type T and produces a key for the map
	 * @param valueExtractor
	 *            takes an object of type T and produces a value for the map
	 * @return map of extracted values keyed by extracted key
	 */
	public static <KR, VR, KT, VT> Collector<Map.Entry<KT, VT>, Map<KR, VR>, Map<KR, VR>> remapBy(
			Function<? super Map.Entry<KT, VT>, ? extends KR> keyExtractor,
			Function<? super Map.Entry<KT, VT>, ? extends VR> valueExtractor) {
		
		return mapBy(keyExtractor, valueExtractor);

	}
	

	/**
	 * Perform a <a href=
	 * "http://en.wikipedia.org/wiki/Pearson_product-moment_correlation_coefficient"
	 * >Pearson correlation test</a> on two sequences of numbers. The two
	 * sequences must be of equal length and of length greater than one.
	 * 
	 * @param x
	 *            first number sequence
	 * @param y
	 *            second number sequence
	 * @return the Pearson correlation coefficient.
	 */
	public static OptionalDouble PearsonTest(Supplier<DoubleStream> x, Supplier<DoubleStream> y) {
		
		DoubleSummaryStatistics xs = x.get().summaryStatistics();
		DoubleSummaryStatistics ys = y.get().summaryStatistics();
		
		// Must have x and y sequences the same length, longer than 1, and with real averages.
		if (xs.getCount() < 2 || xs.getCount() != ys.getCount()) {
			return OptionalDouble.empty();
		}
		double xa = xs.getAverage(), ya = ys.getAverage();
		if (Double.isNaN(xa) || Double.isNaN(ya)) {
			return OptionalDouble.empty();
		}
		
		// Extract Pearson correlation
		PrimitiveIterator.OfDouble xi = x.get().iterator();
		PrimitiveIterator.OfDouble yi = y.get().iterator();
		double totXYdiff = 0, totX2diff = 0, totY2diff = 0;
		while (xi.hasNext()) {
			double dx = xi.nextDouble();
			double dy = yi.nextDouble();
			double xdiff = (dx - xa);
			double ydiff = (dy - ya);
			totXYdiff += ( xdiff * ydiff );
			totX2diff += ( xdiff * xdiff );
			totY2diff += ( ydiff * ydiff );
		}
		double result = totXYdiff / Math.sqrt( totX2diff * totY2diff );
		return OptionalDouble.of(result);

	}
	
	
	/**
	 * Perform a <a href=
	 * "http://en.wikipedia.org/wiki/Spearman%27s_rank_correlation_coefficient"
	 * >Spearman Rank test</a> on two sequences of numbers. The sequences must
	 * be of equal length, and of length greater than one.
	 * 
	 * @param x
	 *            first sequence
	 * @param y
	 *            second sequence
	 * @return Spearman rank correlation coefficient.
	 */
	public static OptionalDouble SpearmanRankTest(Supplier<DoubleStream> x, Supplier<DoubleStream> y) {

		// Create key/value pairs of the x and y sequences, where the keys are the
		// x and y quantities and the values are their ranks. The ranks are initially
		// unassigned.
		List<KeyValuePair<Double, Double>> xpairs = x.get()
				.mapToObj(d -> new KeyValuePair<>(d, 0.0))
				.collect(Collectors.toList());
		
		List<KeyValuePair<Double, Double>> ypairs = y.get()
				.mapToObj(d -> new KeyValuePair<>(d, 0.0))
				.collect(Collectors.toList());
		
		// Must have x and y sequences the same length, and longer than 1.
		if (xpairs.size() < 2 || xpairs.size() != ypairs.size()) {
			return OptionalDouble.empty();
		}
		
		// Declare a function which takes a list of key/value pairs of equal
		// keys, computes the average of the ranks, and sets all the ranks to
		// be equal to this average. If list size < 2 does nothing and returns false;
		Function<List<KeyValuePair<Double, Double>>, Boolean> avgRank = lst -> {
			if (lst.size() < 2)
				return false;
			double avg = lst.stream().mapToDouble(v -> v.getValue()).average()
					.getAsDouble();
			for (KeyValuePair<Double, Double> v : lst) {
				v.setValue(avg);
			}
			return true;
		};
	
		// Flag to indicate if either x or y sequence has any equal ranks
		boolean equalRanks = false;
		
		// Sort the two sequences and place sorted lists in an array
		Object[] lists = new Object[] {
				xpairs.stream().sorted(KeyValuePair.comparingByKey())
						.collect(Collectors.toList()),
						
				ypairs.stream().sorted(KeyValuePair.comparingByKey())
						.collect(Collectors.toList())
				
		};
		
		// For each of the sequences
		for (Object o : lists) {
			
			@SuppressWarnings("unchecked")
			List<KeyValuePair<Double, Double>> pairsSort = (List<KeyValuePair<Double, Double>>)o;
			
			// Rank the values of the sorted sequences
			int rank = 0;
			for (KeyValuePair<Double, Double> i : pairsSort) {
				i.setValue(rank + 1.0);
				rank++;
			}
			
			// Look for blocks of equal key values. Start by adding the first key
			// value to the block.
			List<KeyValuePair<Double, Double>> equalBlock = new ArrayList<>();
			equalBlock.add(pairsSort.get(0));
			
			for (int i = 1; i < pairsSort.size(); i++) {
				if (pairsSort.get(i).getKey() != pairsSort.get(i-1).getKey()) {
					// Value different ...  check the previous block
					equalRanks = equalRanks || avgRank.apply(equalBlock);
					equalBlock.clear();
				}
				equalBlock.add(pairsSort.get(i));
			}
			// Check last block
			equalRanks = equalRanks || avgRank.apply(equalBlock);
			
		}
		
		if (equalRanks) {
			// If either sequence had successive equal values so that we had to
			// average their ranks, then we have to use Pearson test on ranks
			return PearsonTest(
					() -> xpairs.stream().mapToDouble(v -> v.getValue()),
					() -> ypairs.stream().mapToDouble(v -> v.getValue()));
		}
		
		// Otherwise use the simplified Spearman test.
		
		// Total the differences in ranks between two sequences.
		double totalDiff = 0;
		int n = xpairs.size();
		for (int i = 0; i < n; i++) {
			double diff = xpairs.get(i).getValue() - ypairs.get(i).getValue();
			totalDiff += (diff * diff);
		}
		
		// Spearman rank:
		double result = 1 - ( (6 * totalDiff) / (n * (n * n - 1)) );
		
		return OptionalDouble.of(result);
		
	}
	
	
}

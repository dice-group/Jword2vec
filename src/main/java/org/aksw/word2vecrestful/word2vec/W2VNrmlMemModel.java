package org.aksw.word2vecrestful.word2vec;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.word2vecrestful.subset.DataSubsetProvider;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dice_research.topicmodeling.commons.sort.AssociativeSort;

import nikit.test.TimeLogger;

/**
 * Class to encapsulate word2vec in-memory model and expose methods to perform
 * search on the model
 * 
 * @author Nikit
 *
 */
public class W2VNrmlMemModel implements GenWord2VecModel {
	public static Logger LOG = LogManager.getLogger(GenWord2VecModel.class);

	private Map<String, float[]> word2vec;
	private int vectorSize;
	private Map<Integer, Float> sdMap;
	/**
	 * Limit to the multiplier of area in which nearby vectors are to be looked
	 */
	private static final int EXHAUSTION_MULT = 100;
	/**
	 * Multiplier for the standard deviation
	 */
	private static final int SIGMA_MULT = 3;
	/**
	 * Divisor for the standard deviation's value
	 */
	private static final int AREA_DIVISOR = 100;
	private DataSubsetProvider dataSubsetProvider;
	/**
	 * Contains the sorted dimensional values mapped to their words
	 */
	private Object[][] indexesArr;

	private String[] gWordArr;
	private float[][] gVecArr;

	// TODO : Remove this
	private TimeLogger tl = new TimeLogger();

	public W2VNrmlMemModel(final Map<String, float[]> word2vec, final int vectorSize) {
		this.word2vec = word2vec;
		this.vectorSize = vectorSize;
		this.dataSubsetProvider = new DataSubsetProvider();
		this.initArrays();
		// Calculate sd*3/10 and save in map
		// Initialize indexesArr unsorted
		// LOG.info("Initializing indexes and calculating standard deviation");
		this.setModelVals(word2vec, vectorSize);
		// LOG.info("Sorting indexes");
		// Sort the indexes
		this.sortIndexes();
		// LOG.info("Sorting completed");
	}

	private void initArrays() {
		this.indexesArr = new Object[vectorSize][2];
		this.gWordArr = new String[word2vec.size()];
		this.gVecArr = new float[word2vec.size()][vectorSize];
		int i = 0;
		for (Entry<String, float[]> entry : word2vec.entrySet()) {
			gWordArr[i] = entry.getKey();
			gVecArr[i] = entry.getValue();
			i++;
		}
	}

	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector
	 *            - vector to find closest word to
	 * 
	 * @return closest word to the given vector alongwith it's vector
	 */
	@Override
	public Map<String, float[]> getClosestEntry(float[] vector) {
		return getClosestEntry(vector, null);
	}

	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector
	 *            - vector to find closest word to
	 * @param subKey
	 *            - key to subset if any
	 * @return closest word to the given vector alongwith it's vector
	 */
	@Override
	public Map<String, float[]> getClosestSubEntry(float[] vector, String subKey) {
		return getClosestEntry(vector, subKey);
	}

	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector
	 *            - vector to find closest word to
	 * @param subKey
	 *            - key to subset if any
	 * @return closest word to the given vector alongwith it's vector
	 */
	private Map<String, float[]> getClosestEntry(float[] vector, String subKey) {
		Set<String> wordSet = null;
		Map<String, float[]> closestVec = null;
		try {
			if (subKey == null) {
				wordSet = word2vec.keySet();
			} else {
				tl.logTime(1);
				wordSet = dataSubsetProvider.fetchSubsetWords(subKey);
				tl.printTime(1, "fetchSubsetWords");
			}
			// LOG.info("Normalizing input vector");
			// Normalize incoming vector
			vector = Word2VecMath.normalize(vector);
			// LOG.info("fetching nearby vectors");
			// Find nearby vectors
			tl.logTime(2);
			Map<String, float[]> nearbyVecs = fetchNearbyVectors(vector, wordSet, true);
			tl.printTime(2, "fetchNearbyVectors");
			// LOG.info("found the following nearby words: " + nearbyVecs.keySet());
			// Select the closest vector
			tl.logTime(3);
			closestVec = Word2VecMath.findClosestVecInNearbyVecs(nearbyVecs, vector);
			tl.printTime(3, "findClosestVecInNearbyVecs");
		} catch (IOException e) {
			// LOG.error(e.getStackTrace());
		}
		// LOG.info("Closest word found is " + closestVec.keySet());
		return closestVec;
	}

	/**
	 * Method to find standard deviation for each dimension of word vector and store
	 * the operated value next to the dimension's index in sdMap
	 * 
	 * @param word2vecMap
	 *            - mapping of words alongwith their vectors
	 * @param vectorSize
	 *            - size of each vector
	 */
	public void setModelVals(Map<String, float[]> word2vecMap, int vectorSize) {
		Map<Integer, Float> resMap = new HashMap<>();
		int totSize = word2vecMap.size();
		// loop all dimensions
		for (int i = 0; i < vectorSize; i++) {
			// loop through all the words
			float[] dimsnArr = new float[totSize];
			int[] idArr = new int[totSize];
			float sum = 0;
			for (int j = 0; j < gWordArr.length; j++) {
				float val = gVecArr[j][i];
				sum += val;
				idArr[j] = j;
				dimsnArr[j] = val;
			}
			// Setting value in indexArr
			Object[] dimValWordMap = new Object[2];
			dimValWordMap[0] = idArr;
			dimValWordMap[1] = dimsnArr;
			this.indexesArr[i] = dimValWordMap;
			// LOG.info("Dimension " + (i) + " index stored to memory");
			// mean
			float mean = sum / dimsnArr.length;
			sum = 0;
			for (int j = 0; j < dimsnArr.length; j++) {
				sum += Math.pow(dimsnArr[j] - mean, 2);
			}
			float variance = sum / dimsnArr.length;
			Double sd = Math.sqrt(variance);
			resMap.put(i, sd.floatValue() * SIGMA_MULT / AREA_DIVISOR);
		}
		// Set as sdMap
		this.sdMap = resMap;
	}

	/**
	 * Method to fetch nearby vectors for a given vector in a particular word set
	 * 
	 * @param vector
	 *            - vector to look nearby vectors for
	 * @param wordSet
	 *            - word set to look into for nearby vectors
	 * @return - mapping of nearby words alongwith with their vector values
	 */
	private Map<String, float[]> fetchNearbyVectors(float[] vector, Set<String> wordSet, boolean indxd) {
		Map<String, float[]> nearbyVecMap = new HashMap<>();
		boolean mapEmpty = true;
		boolean notExhausted = true;
		float[][] minMaxVec;
		int mult = 1;
		while (mapEmpty && notExhausted) {
			minMaxVec = getMinMaxVec(vector, mult);
			tl.printTime(2, "getMinMaxVec");
			if (indxd) {
				tl.logTime(4);
				putNearbyVecsIndxd(minMaxVec, wordSet, nearbyVecMap);
				tl.printTime(4, "putNearbyVecsIndxd");
			} else {
				putNearbyVecsNonIndxd(minMaxVec, wordSet, nearbyVecMap);
			}
			if (nearbyVecMap.size() > 0) {
				mapEmpty = false;
			} else {
				++mult;
				if (mult > EXHAUSTION_MULT) {
					notExhausted = false;
				}
				// LOG.info("MinMax multiplier incremented to " + mult);
			}
		}
		return nearbyVecMap;
	}

	private void putNearbyVecsNonIndxd(float[][] minMaxVec, Set<String> wordSet, Map<String, float[]> nearbyVecMap) {
		for (String word : wordSet) {
			float[] entryVec = word2vec.get(word);
			if (isVectorInArea(entryVec, minMaxVec)) {
				nearbyVecMap.put(word, entryVec);
			}
		}
	}

	private void putNearbyVecsIndxd(float[][] minMaxVec, Set<String> wordSet, Map<String, float[]> nearbyVecMap) {
		// init a set to hold words
		Set<String> nearbyWords = new HashSet<>();
		float[] minVec = minMaxVec[0];
		float[] maxVec = minMaxVec[1];
		BitSet finBitSet = null;
		BitSet tempBitSet;
		tl.logTime(5);
		for (int i = 0; i < vectorSize; i++) {
			tempBitSet = new BitSet(word2vec.size());
			// LOG.info("Searching inside dimension " + (i) + "'s index");
			float minVal = minVec[i];
			float maxVal = maxVec[i];
			// LOG.info("MinVal and MaxVal for the current dimension: " + minVal + " " +
			// maxVal);
			Object[] entryArr = indexesArr[i];
			int[] idArr = (int[]) entryArr[0];
			float[] dimsnValArr = (float[]) entryArr[1];
			int from = Arrays.binarySearch(dimsnValArr, minVal);
			// LOG.info("From value of dimension array: " + from);
			if (from < 0) {
				// To select the insertion point
				from = -1 - from;
			}
			// LOG.info("Final From value of current dimension array: " + from);
			int to = Arrays.binarySearch(dimsnValArr, maxVal);
			// LOG.info("To value of dimension array: " + to);
			if (to < 0) {
				// To select the insertion point
				to = -1 - to;
			} else {
				// Because binarySearch returns the exact index if element exists
				to++;
			}
			// LOG.info("Final To value of current dimension array: " + to);
			// LOG.info("Setting bits for the words between 'from' and 'to' indexes");
			for (int j = from; j < to; j++) {
				tempBitSet.set(idArr[j], true);
			}
			if (i == 0) {
				finBitSet = tempBitSet;
			} else {
				finBitSet.and(tempBitSet);
			}
			if (finBitSet.isEmpty()) {
				// LOG.info("Word not found in the current min-max area.");
				break;
			}
		}
		// LOG.info("Extracting words for set bits");
		tl.printTime(5, "Setting Bitset");
		tl.logTime(6);
		for (int i = finBitSet.nextSetBit(0); i >= 0; i = finBitSet.nextSetBit(i + 1)) {
			// operate on index i here
			nearbyWords.add(gWordArr[i]);
			if (i == Integer.MAX_VALUE) {
				break; // or (i+1) would overflow
			}
		}
		tl.printTime(6, "extracting words");
		// LOG.info("Nearby words size before retainAll from wordset: " +
		// nearbyWords.size());
		// Clear all the words not in wordset
		tl.logTime(7);
		nearbyWords.retainAll(wordSet);
		tl.printTime(7, "retaining words");
		// LOG.info("Nearby words size after retainAll from wordset: " +
		// nearbyWords.size());
		for (String word : nearbyWords) {
			nearbyVecMap.put(word, word2vec.get(word));
		}
	}

	/**
	 * Method to check if vector falls in a particular area
	 * 
	 * @param entryVec
	 *            - vector to be verified
	 * @param minMaxVec
	 *            - min vec and max vec as area's boundary
	 * @return - if the given vector is inside min and max vec's range
	 */
	private boolean isVectorInArea(float[] entryVec, float[][] minMaxVec) {
		boolean isValid = true;
		float[] minVec = minMaxVec[0];
		float[] maxVec = minMaxVec[1];
		for (int i = 0; i < entryVec.length; i++) {
			if (entryVec[i] < minVec[i] || entryVec[i] > maxVec[i]) {
				isValid = false;
				break;
			}
		}
		return isValid;
	}

	/**
	 * Method to generate two vectors from a given vector by adding and subtracting
	 * value in sdMap from the given vector
	 * 
	 * @param vector
	 *            - input vector to perform operation on
	 * @return - min vector at index 0 and max vector at index 1
	 */
	private float[][] getMinMaxVec(float[] vector, int mult) {
		float[][] resVec = new float[2][vector.length];
		for (int i = 0; i < vector.length; i++) {
			float diff = mult * sdMap.get(i);
			// MinVec
			resVec[0][i] = vector[i] - diff;
			// MaxVec
			resVec[1][i] = vector[i] + diff;
		}
		return resVec;
	}

	private void sortIndexes() {
		for (int i = 0; i < indexesArr.length; i++) {
			// LOG.info("Sorting index " + i);
			Object[] entryArr = indexesArr[i];
			int[] idArr = (int[]) entryArr[0];
			float[] dimsnValArr = (float[]) entryArr[1];
			AssociativeSort.quickSort(dimsnValArr, idArr);
			// LOG.info("Sorting completed for index " + i);
		}
	}

	/**
	 * Method to fetch vectorSize
	 * 
	 * @return - vectorSize
	 */
	@Override
	public int getVectorSize() {
		return this.vectorSize;
	}

	/**
	 * Method to fetch word2vec map
	 * 
	 * @return - word2vec map
	 */
	public Map<String, float[]> getWord2VecMap() {
		return this.word2vec;
	}

}

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
	private static final int EXHAUSTION_MULT = 10;
	/**
	 * Multiplier for the standard deviation
	 */
	private static final int SIGMA_MULT = 3;
	/**
	 * Divisor for the standard deviation's value
	 */
	private static final int AREA_DIVISOR = 10;
	private DataSubsetProvider dataSubsetProvider;
	/**
	 * Contains the sorted dimensional values mapped to their words
	 */
	private Object[][] indexesArr;

	private String[] gWordArr;
	private float[][] gVecArr;

	public W2VNrmlMemModel(final Map<String, float[]> word2vec, final int vectorSize) {
		this.word2vec = word2vec;
		this.vectorSize = vectorSize;
		this.dataSubsetProvider = new DataSubsetProvider();
		this.initArrays();
		// Calculate sd*3/10 and save in map
		// Initialize indexesArr unsorted
		LOG.info("Initializing indexes and calculating standard deviation");
		this.setModelVals(word2vec, vectorSize);
		LOG.info("Sorting indexes");
		// Sort the indexes
		this.sortIndexes();
		LOG.info("Sorting completed");
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
				wordSet = dataSubsetProvider.fetchSubsetWords(subKey);
			}
			LOG.info("Normalizing input vector");
			// Normalize incoming vector
			vector = Word2VecMath.normalize(vector);
			LOG.info("fetching nearby vectors");
			// Find nearby vectors
			Map<String, float[]> nearbyVecs = fetchNearbyVectors(vector, wordSet, true);
			LOG.info("found the following nearby words: " + nearbyVecs.keySet());
			// Select the closest vector
			closestVec = Word2VecMath.findClosestVecInNearbyVecs(nearbyVecs, vector);
		} catch (IOException e) {
			LOG.error(e.getStackTrace());
		}
		LOG.info("Closest word found is " + closestVec.keySet());
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
				dimsnArr[j++] = val;
			}
			// Setting value in indexArr
			Object[] dimValWordMap = new Object[2];
			dimValWordMap[0] = idArr;
			dimValWordMap[1] = dimsnArr;
			this.indexesArr[i] = dimValWordMap;
			LOG.info("Dimension " + (i) + " index stored to memory");
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
		float[][] minMaxVec = getMinMaxVec(vector);
		int mult = 1;
		while (mapEmpty && notExhausted) {
			if (mult > 1) {
				minMaxVec = multMinMaxVec(minMaxVec, mult);
			}
			if (indxd) {
				putNearbyVecsIndxd(minMaxVec, wordSet, nearbyVecMap);
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
		for (int i = 0; i < vectorSize; i++) {
			tempBitSet = new BitSet(word2vec.size());
			LOG.info("Searching inside dimension " + (i) + "'s index");
			float minVal = minVec[i];
			float maxVal = maxVec[i];
			LOG.info("MinVal and MaxVal for the current dimension: "+minVal+" "+maxVal);
			Object[] entryArr = indexesArr[i];
			int[] idArr = (int[]) entryArr[0];
			float[] dimsnValArr = (float[]) entryArr[1];
			int from = Arrays.binarySearch(dimsnValArr, minVal);
			LOG.info("From value of dimension array: " + from);
			if (from < 0) {
				// To select the insertion point
				from = -1 - from;
			} 
			LOG.info("Final From value of current dimension array: " + from);
			LOG.info("To value of dimension array: " + from);
			int to = Arrays.binarySearch(dimsnValArr, maxVal);
			if (to < 0) {
				// To select the insertion point
				to = -1-to;
			} else {
				to++;
			}
			LOG.info("Final To value of current dimension array: " + from);
			LOG.info("Setting bits for the words between 'from' and 'to' indexes");
			for (int j = from; j < to; j++) {
				tempBitSet.set(idArr[j], true);
			}
			if (i == 0) {
				finBitSet = tempBitSet;
			} else {
				finBitSet.and(tempBitSet);
			}
			if (finBitSet.isEmpty()) {
				break;
			}
		}
		LOG.info("Extracting words for set bits");
		int nextBit=0;
		
		for (int i = finBitSet.nextSetBit(0); i >= 0; i = finBitSet.nextSetBit(i+1)) {
		     // operate on index i here
			 nearbyWords.add(gWordArr[nextBit]);
		     if (i == Integer.MAX_VALUE) {
		         break; // or (i+1) would overflow
		     }
		 }
		LOG.info("Nearby words size before retainAll from wordset: "+ nearbyWords.size());
		// Clear all the words not in wordset
		nearbyWords.retainAll(wordSet);
		LOG.info("Nearby words size after retainAll from wordset: "+ nearbyWords.size());
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
	 * Multiply each element of the given multi dimensional vector with a given
	 * multiplier
	 * 
	 * @param minMaxVec
	 *            - vector at which operation is to be performed
	 * @param mult
	 *            - multiplier
	 * @return - Vector after multiplication with the multiplier
	 */
	private float[][] multMinMaxVec(float[][] minMaxVec, int mult) {
		for (int i = 0; i < minMaxVec[0].length; i++) {
			minMaxVec[0][i] = minMaxVec[0][i] * mult;
			minMaxVec[1][i] = minMaxVec[1][i] * mult;
		}
		return minMaxVec;
	}

	/**
	 * Method to generate two vectors from a given vector by adding and subtracting
	 * value in sdMap from the given vector
	 * 
	 * @param vector
	 *            - input vector to perform operation on
	 * @return - min vector at index 0 and max vector at index 1
	 */
	private float[][] getMinMaxVec(float[] vector) {
		float[][] resVec = new float[2][vector.length];
		for (int i = 0; i < vector.length; i++) {
			float diff = sdMap.get(i);
			// MinVec
			resVec[0][i] = vector[i] - diff;
			// MaxVec
			resVec[1][i] = vector[i] + diff;
		}
		return resVec;
	}

	private void sortIndexes() {
		for (int i = 0; i < indexesArr.length; i++) {
			LOG.info("Sorting index " + i);
			Object[] entryArr = indexesArr[i];
			int[] idArr = (int[]) entryArr[0];
			float[] dimsnValArr = (float[]) entryArr[1];
			AssociativeSort.quickSort(dimsnValArr, idArr);
			LOG.info("Sorting completed for index " + i);
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

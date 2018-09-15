package org.aksw.word2vecrestful.word2vec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.word2vecrestful.subset.DataSubsetProvider;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import nikit.test.TimeLogger;

/**
 * Class to encapsulate word2vec in-memory model and expose methods to perform
 * search on the model
 * 
 * @author Nikit
 *
 */
public class W2VNrmlMemModelNonIndxd implements GenWord2VecModel {
	public static Logger LOG = LogManager.getLogger(GenWord2VecModel.class);

	private Map<String, float[]> word2vec;
	private int vectorSize;
	private float[] sdArr;
	/**
	 * Limit to the multiplier of area in which nearby vectors are to be looked
	 */
	private static final int EXHAUSTION_MULT = 20;
	/**
	 * Multiplier for the standard deviation
	 */
	private static final int SIGMA_MULT = 3;
	/**
	 * Divisor for the standard deviation's value
	 */
	private static final int AREA_DIVISOR = 20;
	private DataSubsetProvider dataSubsetProvider;
	/**
	 * Contains the sorted dimensional values mapped to their words
	 */

	private String[] gWordArr;
	private float[][] gVecArr;

	// TODO : Remove this
	private TimeLogger tl = new TimeLogger();

	public W2VNrmlMemModelNonIndxd(final Map<String, float[]> word2vec, final int vectorSize) {
		this.word2vec = word2vec;
		this.vectorSize = vectorSize;
		this.dataSubsetProvider = new DataSubsetProvider();
		// Calculate sd*3/10 and save in map
		// Initialize indexesArr unsorted
		// LOG.info("Initializing indexes and calculating standard deviation");
		initArrays();
		this.setModelVals(word2vec, vectorSize);
		// LOG.info("Sorting indexes");
		// Sort the indexes
		// LOG.info("Sorting completed");
	}

	private void initArrays() {
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
			Map<String, float[]> nearbyVecs = fetchNearbyVectors(vector, wordSet);
			tl.printTime(2, "fetchNearbyVectors");
			// LOG.info("found the following nearby words: " + nearbyVecs.keySet());
			// Select the closest vector
			tl.logTime(3);
			closestVec = Word2VecMath.findClosestNormalizedVec(nearbyVecs, vector);
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
		float[] resMap = new float[vectorSize];
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
			// LOG.info("Dimension " + (i) + " index stored to memory");
			// mean
			float mean = sum / dimsnArr.length;
			sum = 0;
			for (int j = 0; j < dimsnArr.length; j++) {
				sum += Math.pow(dimsnArr[j] - mean, 2);
			}
			float variance = sum / dimsnArr.length;
			Double sd = Math.sqrt(variance);
			resMap[i] = sd.floatValue() * SIGMA_MULT / AREA_DIVISOR;
		}
		// Set as sdMap
		this.sdArr = resMap;
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
	private Map<String, float[]> fetchNearbyVectors(float[] vector, Set<String> wordSet) {
		Map<String, float[]> nearbyVecMap = new HashMap<>();
		boolean mapEmpty = true;
		boolean notExhausted = true;
		float[][] minMaxVec = getMinMaxVec(vector);
		tl.printTime(2, "getMinMaxVec");
		int mult = 1;
		while (mapEmpty && notExhausted) {
			if (mult > 1) {
				tl.logTime(8);
				incrementMinMaxVec(minMaxVec);
				tl.printTime(8, "incrementMinMaxVec");
			}
			tl.logTime(4);
			putNearbyVecsNonIndxd(minMaxVec, wordSet, nearbyVecMap);
			tl.printTime(4, "putNearbyVecsNonIndxd");
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
	private float[][] getMinMaxVec(float[] vector) {
		float[][] resVec = new float[2][vector.length];
		for (int i = 0; i < vector.length; i++) {
			// TODO: change sdmap to array
			float diff = sdArr[i];
			// MinVec
			resVec[0][i] = vector[i] - diff;
			// MaxVec
			resVec[1][i] = vector[i] + diff;
		}
		return resVec;
	}

	private void incrementMinMaxVec(float[][] minMaxVec) {
		float[] minVec = minMaxVec[0];
		float[] maxVec = minMaxVec[1];
		for (int i = 0; i < vectorSize; i++) {
			float diff = sdArr[i];
			// MinVec
			minVec[i] -= diff;
			// MaxVec
			maxVec[i] += diff;
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

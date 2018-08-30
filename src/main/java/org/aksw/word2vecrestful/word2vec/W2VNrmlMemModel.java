package org.aksw.word2vecrestful.word2vec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.aksw.word2vecrestful.subset.DataSubsetProvider;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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

	public W2VNrmlMemModel(final Map<String, float[]> word2vec, final int vectorSize) {
		this.word2vec = word2vec;
		this.vectorSize = vectorSize;
		this.dataSubsetProvider = new DataSubsetProvider();
		// Calculate sd*3/10 and save in map
		setModelSd(word2vec, vectorSize);
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
			// Normalize incoming vector
			vector = Word2VecMath.normalize(vector);
			Map<String, float[]> nearbyVecs = fetchNearbyVectors(vector, wordSet);
			closestVec = findClosestVecInNearbyVecs(nearbyVecs, vector);
		} catch (IOException e) {
			LOG.error(e.getStackTrace());
		}
		return closestVec;
	}

	private Map<String, float[]> findClosestVecInNearbyVecs(Map<String, float[]> nearbyVecs, float[] vector) {
		Map<String, float[]> closestVec = new HashMap<>();
		TreeMap<Double, String> cosineSimMap = new TreeMap<>();
		for (String word : nearbyVecs.keySet()) {
			cosineSimMap.put(Word2VecMath.cosineSimilarity(vector, nearbyVecs.get(word)), word);
		}
		String closestWord = cosineSimMap.lastEntry().getValue();
		closestVec.put(closestWord, nearbyVecs.get(closestWord));
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
	public void setModelSd(Map<String, float[]> word2vecMap, int vectorSize) {
		Map<Integer, Float> resMap = new HashMap<>();
		Set<Entry<String, float[]>> entries = word2vecMap.entrySet();
		int totSize = word2vecMap.size();
		// loop all dimensions
		for (int i = 0; i < vectorSize; i++) {
			// loop through all the words
			int j = 0;
			float[] dimsnArr = new float[totSize];
			float sum = 0;
			for (Entry<String, float[]> entry : entries) {
				float[] vecArr = entry.getValue();
				float val = vecArr[i];
				sum += val;
				dimsnArr[j++] = val;
			}
			// mean
			float mean = sum / dimsnArr.length;
			sum = 0;
			for (j = 0; j < dimsnArr.length; j++) {
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
	private Map<String, float[]> fetchNearbyVectors(float[] vector, Set<String> wordSet) {
		Map<String, float[]> nearbyVecMap = new HashMap<>();
		boolean mapEmpty = true;
		boolean notExhausted = true;
		float[][] minMaxVec = getMinMaxVec(vector);
		int mult = 1;
		while (mapEmpty && notExhausted) {
			if (mult > 1) {
				minMaxVec = multMinMaxVec(minMaxVec, mult);
			}
			for (String word : wordSet) {
				float[] entryVec = word2vec.get(word);
				if (isVectorInArea(entryVec, minMaxVec)) {
					nearbyVecMap.put(word, entryVec);
				}
			}
			if (nearbyVecMap.size() > 0) {
				mapEmpty = false;
			} else if (mult > EXHAUSTION_MULT) {
				notExhausted = false;
			}

		}
		return nearbyVecMap;
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

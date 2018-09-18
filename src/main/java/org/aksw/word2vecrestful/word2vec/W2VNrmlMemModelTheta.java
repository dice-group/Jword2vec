package org.aksw.word2vecrestful.word2vec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
public class W2VNrmlMemModelTheta implements GenWord2VecModel {
	public static Logger LOG = LogManager.getLogger(GenWord2VecModel.class);

	private Map<String, float[]> word2vec;
	private int vectorSize;
	private Map<Integer, List<String>> cosineIndxMap;
	private float[] comparisonVec = null;
	private float gMultiplier = 10;

	public W2VNrmlMemModelTheta(final Map<String, float[]> word2vec, final int vectorSize) {
		this.word2vec = word2vec;
		this.vectorSize = vectorSize;
		// Setting mean as comparison vec
		setComparisonVec(word2vec, vectorSize);
		// Generating index bucket for degrees
		generateCosineIndxMap();
		// TODO: Remove this
		printBucketSize();
	}

	public void updateGMultiplier(float gMult) {
		if (this.gMultiplier == gMult) {
			return;
		}
		this.gMultiplier = gMult;
		// Generating index bucket for degrees
		generateCosineIndxMap();
		// TODO: Remove this
		printBucketSize();
	}

	private void printBucketSize() {
		for (int i : cosineIndxMap.keySet()) {
			LOG.info("Bucket " + i + " has the size: " + cosineIndxMap.get(i).size());
		}
	}

	public void setComparisonVec(Map<String, float[]> word2vecMap, int vectorSize) {
		float[] meanArr = new float[vectorSize];
		int totSize = word2vecMap.size();
		// loop all dimensions
		for (int i = 0; i < vectorSize; i++) {
			// loop through all the words
			float[] dimsnArr = new float[totSize];
			float sum = 0;
			for (float[] vecEntry : word2vecMap.values()) {
				float val = vecEntry[i];
				sum += val;
			}
			// mean
			float mean = sum / dimsnArr.length;
			meanArr[i] = mean;
		}
		this.comparisonVec = meanArr;
	}

	private void generateCosineIndxMap() {
		cosineIndxMap = new HashMap<>();
		float[] curVec;
		for (String word : word2vec.keySet()) {
			curVec = word2vec.get(word);

			Long cosineIndx = Math
					.round(Word2VecMath.cosineSimilarityNormalizedVecs(comparisonVec, curVec) * gMultiplier);
			int intIndxVal = cosineIndx.intValue();
			List<String> wordsBucket = cosineIndxMap.get(intIndxVal);
			if (wordsBucket == null) {
				wordsBucket = new ArrayList<>();
				cosineIndxMap.put(intIndxVal, wordsBucket);
			}
			wordsBucket.add(word);
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
	public String getClosestEntry(float[] vector) {
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
	public String getClosestSubEntry(float[] vector, String subKey) {
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
	private String getClosestEntry(float[] vector, String subKey) {
	    String closestVec = null;
		try {
			// Normalize incoming vector
			vector = Word2VecMath.normalize(vector);
			// calculate cosine similarity of all distances
			double cosSimMultVal = Word2VecMath.cosineSimilarityNormalizedVecs(comparisonVec, vector) * gMultiplier;
			Double dMinIndx = Math.floor(cosSimMultVal);
			Double dMaxIndx = Math.ceil(cosSimMultVal);
			int minIndx = dMinIndx.intValue();
			int maxIndx = dMaxIndx.intValue();
			Set<String> nearbyWords = new HashSet<>();
			List<String> minWordList = cosineIndxMap.get(minIndx);
			if (minWordList != null) {
				nearbyWords.addAll(minWordList);
			}
			List<String> maxWordList = cosineIndxMap.get(maxIndx);
			if (maxWordList != null) {
				nearbyWords.addAll(maxWordList);
			}
			Map<String, float[]> nearbyVecMap = createNearbyVecMap(nearbyWords);
			closestVec = Word2VecMath.findClosestNormalizedVec(nearbyVecMap, vector);
		} catch (Exception e) {
			LOG.error(e.getStackTrace());
		}
		return closestVec;
	}

	private Map<String, float[]> createNearbyVecMap(Collection<String> wordCol) {
		Map<String, float[]> vecMap = new HashMap<>();
		for (String word : wordCol) {
			vecMap.put(word, word2vec.get(word));
		}
		return vecMap;
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

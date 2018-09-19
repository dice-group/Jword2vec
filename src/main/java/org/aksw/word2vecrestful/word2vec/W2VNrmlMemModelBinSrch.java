package org.aksw.word2vecrestful.word2vec;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

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
public class W2VNrmlMemModelBinSrch implements GenWord2VecModel {
	public static Logger LOG = LogManager.getLogger(GenWord2VecModel.class);

	private Map<String, float[]> word2vec;
	private int vectorSize;
	private float[][] comparisonVecs = null;
	private String[] wordArr;
	private float[][] vecArr;
	private int[] indxArr;
	private double[] simValArr;
	private int compareVecCount = 4;
	private int bucketCount = 10;
	private BitSet[][] csBucketContainer;
	// TODO : Remove this
	private TimeLogger tl = new TimeLogger();
	public W2VNrmlMemModelBinSrch(final Map<String, float[]> word2vec, final int vectorSize) {
		this.word2vec = word2vec;
		this.vectorSize = vectorSize;
		comparisonVecs = new float[compareVecCount][vectorSize];
		csBucketContainer = new BitSet[compareVecCount][bucketCount];
		// Setting mean as comparison vec
		setMeanComparisonVec(word2vec, vectorSize);
		// Initialize Arrays
		processCosineSim();
		// Set other comparison vecs
		setAllComparisonVecs();

	}

	private void setBucketVals(int compVecIndex, float[] comparisonVec) {
		BitSet[] comparisonVecBuckets = csBucketContainer[compVecIndex];
		double cosSimVal;
		int i = 0;
		for (String word : word2vec.keySet()) {
			cosSimVal = Word2VecMath.cosineSimilarityNormalizedVecs(comparisonVec, word2vec.get(word));
			// Setting bitset for the comparison vec
			setValToBucket(i, cosSimVal, comparisonVecBuckets);
			i++;
		}
	}

	private void setAllComparisonVecs() {
		int diff = (word2vec.size() / compareVecCount) - 1;
		int curIndx = diff;
		for (int i = 1; i < compareVecCount; i++) {
			comparisonVecs[i] = vecArr[indxArr[curIndx]];
			setBucketVals(i, comparisonVecs[i]);
			curIndx += diff;
		}
	}

	private int getBucketIndex(double cosineSimVal) {
		Double dIndx = ((bucketCount-1d) / 2d) * (cosineSimVal + 1d);
		return Math.round(dIndx.floatValue());
	}

	private void processCosineSim() {
		this.wordArr = new String[word2vec.size()];
		this.vecArr = new float[word2vec.size()][vectorSize];
		this.indxArr = new int[word2vec.size()];
		this.simValArr = new double[word2vec.size()];
		int i = 0;
		BitSet[] meanComparisonVecBuckets = csBucketContainer[0];
		for (String word : word2vec.keySet()) {
			wordArr[i] = word;
			float[] vec = word2vec.get(word);
			vecArr[i] = vec;
			indxArr[i] = i;
			simValArr[i] = Word2VecMath.cosineSimilarityNormalizedVecs(comparisonVecs[0], vec);
			// Setting bitset for the first comparison vec
			setValToBucket(i, simValArr[i], meanComparisonVecBuckets);
			i++;
		}
		AssociativeSort.quickSort(simValArr, indxArr);
	}

	private void setValToBucket(int wordIndex, double cosSimVal, BitSet[] meanComparisonVecBuckets) {
		int bucketIndex = getBucketIndex(cosSimVal);
		BitSet bitset = meanComparisonVecBuckets[bucketIndex];
		if (bitset == null) {
			bitset = new BitSet(word2vec.size());
			meanComparisonVecBuckets[bucketIndex] = bitset;
		}
		bitset.set(wordIndex);
	}

	private void setMeanComparisonVec(Map<String, float[]> word2vecMap, int vectorSize) {
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
		this.comparisonVecs[0] = meanArr;
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
			float[] curCompVec;
			BitSet finBitSet = null;
			tl.logTime(1);
			for (int i = 0; i < compareVecCount; i++) {
				curCompVec = comparisonVecs[i];
				double cosSimVal = Word2VecMath.cosineSimilarityNormalizedVecs(curCompVec, vector);
				int indx = getBucketIndex(cosSimVal);
				BitSet curBs = new BitSet(word2vec.size());
				curBs.or(csBucketContainer[i][indx]);
				int temIndx = indx + 1;
				if (temIndx < csBucketContainer[i].length) {
					curBs.or(csBucketContainer[i][temIndx]);
				}
				temIndx = indx - 1;
				if (temIndx > -1) {
					curBs.or(csBucketContainer[i][temIndx]);
				}
				if (i == 0) {
					finBitSet = curBs;
				} else {
					finBitSet.and(curBs);
				}
			}
			tl.printTime(1, "Setting Bits");
			tl.logTime(1);
			Map<String, float[]> nearbyVecs = new HashMap<>();
			for (int i = finBitSet.nextSetBit(0); i >= 0; i = finBitSet.nextSetBit(i + 1)) {
				// operate on index i here
				nearbyVecs.put(wordArr[i], vecArr[i]);
				if (i == Integer.MAX_VALUE) {
					break; // or (i+1) would overflow
				}
			}
			tl.printTime(1, "Extracting words");
			tl.logTime(1);
			closestVec = Word2VecMath.findClosestNormalizedVec(nearbyVecs, vector);
			tl.printTime(1, "finding closest word");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return closestVec;
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

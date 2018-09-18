package org.aksw.word2vecrestful.word2vec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
public class W2VNrmlMemModelBruteForce implements GenWord2VecModel {
	public static Logger LOG = LogManager.getLogger(GenWord2VecModel.class);

	private Map<String, float[]> word2vec;
	private int vectorSize;
	private DataSubsetProvider dataSubsetProvider;
	// TODO : Remove this
	private TimeLogger tl = new TimeLogger();

	public W2VNrmlMemModelBruteForce(final Map<String, float[]> word2vec, final int vectorSize) {
		this.word2vec = word2vec;
		this.vectorSize = vectorSize;
		this.dataSubsetProvider = new DataSubsetProvider();
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
		Set<String> wordSet = null;
		String closestVec = null;
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
			// calculate cosine similarity of all distances
			String[] wordArr = new String[wordSet.size()];
			int[] idArr = new int[wordSet.size()];
			double[] cosineArr = new double[wordSet.size()];
			int i = 0;
			for (String word : wordSet) {
				wordArr[i] = word;
				idArr[i] = i;
				float[] wordVec = word2vec.get(word);
				cosineArr[i] = Word2VecMath.cosineSimilarityNormalizedVecs(wordVec, vector);
				i++;
			}
			cosineArr = AssociativeSort.quickSort(cosineArr, idArr);
			double maxVal = cosineArr[cosineArr.length - 1];
			for (int j = cosineArr.length - 1; j >= 0; j--) {
				if (cosineArr[j] == maxVal) {
					int closestWordId = idArr[j];
					String closestWord = wordArr[closestWordId];
					closestVec = closestWord;
				}else {
					break;
				}
			}

		} catch (IOException e) {
			LOG.error(e.getStackTrace());
		}
		// LOG.info("Closest word found is " + closestVec.keySet());
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

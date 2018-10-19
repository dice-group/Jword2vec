package org.aksw.word2vecrestful;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelBruteForce;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelBinSrch;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

public class NrmlzdThetaMdlPrfmncTester {
	static {
		PropertyConfigurator.configure(Cfg.LOG_FILE);
	}
	public static Logger LOG = LogManager.getLogger(NrmlzdThetaMdlPrfmncTester.class);
	public static final float[][] TEST_CENTROIDS = { TestConst.CENT1, TestConst.CENT2, TestConst.CENT3, TestConst.CENT4,
			TestConst.CENT5, TestConst.CENT6, TestConst.CENT7, TestConst.CENT8, TestConst.CENT9, TestConst.CENT10,
			TestConst.CENT11, TestConst.CENT12, TestConst.CENT13, TestConst.CENT14, TestConst.CENT15, TestConst.CENT16,
			TestConst.CENT17, TestConst.CENT18, TestConst.CENT19, TestConst.CENT20 };
	public static final String[] TEST_WORDS = { "cat", "dog", "airplane", "road", "kennedy", "rome", "human", "disney",
			"machine", "intelligence", "palaeontology", "surgeon", "amazon", "jesus", "gold", "atlantis", "ronaldo",
			"pele", "scissors", "lizard" };

	@Test
	public void testNbmTime() throws IOException {
		long startTime, diff;
		long totTime = 0;
		LOG.info("Starting InMemory Theta Model test!");
		Word2VecModel nbm = Word2VecFactory.getNormalBinModel();
		float[][] centroids = TEST_CENTROIDS;
		// float[][] centroids = fetchWordsVec(TEST_WORDS, nbm);
		LOG.info("Starting BruteForce-Model Test");
		List<String> correctWords = getCorrectWords(centroids, nbm);
		LOG.info("Correct Words are :" + correctWords);
		LOG.info("Initializing W2VNrmlMemModelBinSrch Model");
		final W2VNrmlMemModelBinSrch memModel = new W2VNrmlMemModelBinSrch(nbm.word2vec, nbm.vectorSize);
		memModel.process();
		List<String> lrModelWords = new ArrayList<>();

		LOG.info("Starting W2VNrmlMemModelBinSrch Test");

		for (int i = 0; i < centroids.length; i++) {
			LOG.info("Sending query for Centroid " + (i + 1));
			startTime = System.currentTimeMillis();
			lrModelWords.add(memModel.getClosestEntry(centroids[i]));
			diff = System.currentTimeMillis() - startTime;
			totTime += diff;
			LOG.info("Query time recorded for Centroid " + (i + 1) + " is " + diff + " milliseconds.");
		}
		LOG.info("Average query time for W2VNrmlMemModelBinSrch is : " + (totTime / centroids.length) + " milliseconds");
		LOG.info("Predicted Words are :" + lrModelWords);
		float percVal = calcPercScore(correctWords, lrModelWords);
		LOG.info("Score for Test is : " + percVal + "%");
		lrModelWords.clear();
	}

	private static float[][] fetchWordsVec(String[] words, Word2VecModel nbm) {
		float[][] resVec = new float[words.length][300];
		for (int i = 0; i < words.length; i++) {
			resVec[i] = nbm.word2vec.get(words[i]);
		}
		return resVec;
	}

	public static float calcPercScore(List<String> correctWordSet, List<String> lrModelWords) {
		float percScore = 0;
		int len = correctWordSet.size();
		float lenInv = 100f / len;
		for (int i = 0; i < len; i++) {
			if (correctWordSet.get(i).equals(lrModelWords.get(i))) {
				percScore += lenInv;
			}
		}
		return percScore;

	}

	public static List<String> getCorrectWords(float[][] centroids, Word2VecModel nbm) {
		List<String> wordSet = new ArrayList<>();
		W2VNrmlMemModelBruteForce bruteForce = new W2VNrmlMemModelBruteForce(nbm.word2vec, nbm.vectorSize);
		long startTime, diff;
		long totTime = 0;
		for (int i = 0; i < centroids.length; i++) {
			LOG.info("Sending query for Centroid " + (i + 1));
			startTime = System.currentTimeMillis();
			String closestWord = bruteForce.getClosestSubEntry(centroids[i], null);
			diff = System.currentTimeMillis() - startTime;
			totTime += diff;
			wordSet.add(closestWord);
			LOG.info("Query time recorded for Centroid " + (i + 1) + " is " + diff + " milliseconds.");
		}
		LOG.info("Average query time for BruteForce is : " + (totTime / centroids.length) + " milliseconds");
		return wordSet;
	}
}

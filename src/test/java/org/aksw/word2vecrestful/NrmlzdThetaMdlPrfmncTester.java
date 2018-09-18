package org.aksw.word2vecrestful;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelBruteForce;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelTheta;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

import nikit.test.TestConst;

public class NrmlzdThetaMdlPrfmncTester {
	static {
		PropertyConfigurator.configure(Cfg.LOG_FILE);
	}
	public static Logger LOG = LogManager.getLogger(NrmlzdThetaMdlPrfmncTester.class);

	@Test
	public void testNbmTime() {
		long startTime, diff;
		long totTime = 0;
		LOG.info("Starting InMemory Theta Model test!");
		Word2VecModel nbm = Word2VecFactory.getNormalBinModel();
		float[][] centroids = { TestConst.CENT1, TestConst.CENT2, TestConst.CENT3, TestConst.CENT4, TestConst.CENT5 };
		LOG.info("Starting BruteForce-Model Test");
		List<Set<String>> correctWords = getCorrectWords(centroids, nbm);
		LOG.info("Correct Words are :" + correctWords);
		LOG.info("Initializing Theta Model");
		final W2VNrmlMemModelTheta memModel = new W2VNrmlMemModelTheta(nbm.word2vec, nbm.vectorSize);
		List<String> lrModelWords = new ArrayList<>();

		LOG.info("Starting Theta-Model Test");
		for (int mult = 10; mult < 1000; mult += 10) {
			LOG.info("Testing with multplier: " + mult);
			memModel.updateGMultiplier(mult);
			
			for (int i = 0; i < centroids.length; i++) {
				LOG.info("Sending query for Centroid " + (i + 1));
				startTime = System.currentTimeMillis();
				lrModelWords.addAll(memModel.getClosestEntry(centroids[i]).keySet());
				diff = System.currentTimeMillis() - startTime;
				totTime += diff;
				LOG.info("Query time recorded for Centroid " + (i + 1) + " is " + diff + " milliseconds.");
			}
			LOG.info("Average query time for W2VNrmlMemModelTheta is : " + (totTime / centroids.length)
					+ " milliseconds");
			LOG.info("Predicted Words are :" + lrModelWords);
			float percVal = calcPercScore(correctWords, lrModelWords);
			LOG.info("Score for Test is : " + percVal + "%");
			lrModelWords.clear();
		}
	}

	private float calcPercScore(List<Set<String>> correctWordSet, List<String> lrModelWords) {
		float percScore = 0;
		int len = correctWordSet.size();
		float lenInv = 100f / len;
		for (int i = 0; i < len; i++) {
			if (correctWordSet.get(i).contains(lrModelWords.get(i))) {
				percScore += lenInv;
			}
		}
		return percScore;

	}

	public List<Set<String>> getCorrectWords(float[][] centroids, Word2VecModel nbm) {
		List<Set<String>> wordSet = new ArrayList<>();
		W2VNrmlMemModelBruteForce bruteForce = new W2VNrmlMemModelBruteForce(nbm.word2vec, nbm.vectorSize);
		long startTime, diff;
		long totTime = 0;
		for (int i = 0; i < centroids.length; i++) {
			LOG.info("Sending query for Centroid " + (i + 1));
			startTime = System.currentTimeMillis();
			Map<String, float[]> closestWord = bruteForce.getClosestSubEntry(centroids[i], null);
			diff = System.currentTimeMillis() - startTime;
			totTime += diff;
			wordSet.add(closestWord.keySet());
			LOG.info("Query time recorded for Centroid " + (i + 1) + " is " + diff + " milliseconds.");
		}
		LOG.info("Average query time for BruteForce is : " + (totTime / centroids.length) + " milliseconds");
		return wordSet;
	}
}

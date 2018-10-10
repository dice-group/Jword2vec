package org.aksw.word2vecrestful;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelNonIndxd;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Assert;
import org.junit.Test;

import nikit.test.TestConst;

public class NormalizedInMemModelTest {
	static {
		PropertyConfigurator.configure(Cfg.LOG_FILE);
	}
	public static Logger LOG = LogManager.getLogger(NormalizedInMemModelTest.class);

	/*
	 * @Test public void testNormalizedModel() {
	 * LOG.info("Starting InMemory indexed model test!"); final W2VNrmlMemModel
	 * memModel = Word2VecFactory.getNormalizedBinModel();
	 * LOG.info("Indexed Model instance created"); Map<String, String> wordKeyMap =
	 * new HashMap<>(); wordKeyMap.put("cat", null); wordKeyMap.put("dog", null);
	 * wordKeyMap.put("airplane", null); wordKeyMap.put("road", null);
	 * 
	 * long startTime, diff; long totTime = 0; for (String word :
	 * wordKeyMap.keySet()) { LOG.info("Sending query for word :" + word); startTime
	 * = System.currentTimeMillis(); float[] vec =
	 * memModel.getWord2VecMap().get(word); Map<String, float[]> closestWord =
	 * memModel.getClosestSubEntry(vec, wordKeyMap.get(word));
	 * Assert.assertTrue(closestWord.containsKey(word)); diff =
	 * System.currentTimeMillis() - startTime; totTime += diff;
	 * LOG.info("Query time recorded for the word: '" + word + "' and subset: '" +
	 * wordKeyMap.get(word) + "' is " + diff + " milliseconds."); }
	 * 
	 * LOG.info("Average query time: " + (totTime / wordKeyMap.size()) +
	 * " milliseconds");
	 * 
	 * }
	 */

	/*
	 * @Test public void testNbmTime() {
	 * LOG.info("Starting InMemory indexed model test!"); final
	 * W2VNrmlMemModelNonIndxd memModel =
	 * Word2VecFactory.getNormalizedBinNonIndxdModel(); float[][] centroids =
	 * {TestConst.CENT1, TestConst.CENT2, TestConst.CENT3, TestConst.CENT4,
	 * TestConst.CENT5};
	 * 
	 * long startTime, diff; long totTime = 0; for (int i=0;i<centroids.length;i++)
	 * { LOG.info("Sending query for Centroid " + (i+1) ); startTime =
	 * System.currentTimeMillis(); String closestWord = memModel.getClosestSubEntry(
	 * centroids[i], null); diff = System.currentTimeMillis() - startTime; totTime
	 * += diff; LOG.info(closestWord); LOG.info("Query time recorded for Centroid "
	 * + (i+1) + " is " + diff + " milliseconds."); }
	 * 
	 * LOG.info("Average query time: " + (totTime / centroids.length) +
	 * " milliseconds");
	 * 
	 * }
	 */

	@Test
	public void testNbmTime() {
		LOG.info("Starting InMemory indexed model test!");
		Word2VecModel nbm = Word2VecFactory.getNormalBinModel();
		float[][] centroids = { TestConst.CENT1, TestConst.CENT2, TestConst.CENT3, TestConst.CENT4, TestConst.CENT5,
				TestConst.CENT6, TestConst.CENT7, TestConst.CENT8, TestConst.CENT9, TestConst.CENT10, TestConst.CENT11,
				TestConst.CENT12, TestConst.CENT13, TestConst.CENT14, TestConst.CENT15, TestConst.CENT16,
				TestConst.CENT17, TestConst.CENT18, TestConst.CENT19, TestConst.CENT20 };
		List<String> correctWords = NrmlzdMdlPrfmncTester.getCorrectWords(centroids, nbm);
		LOG.info("Correct Words are :" + correctWords);

		long startTime, diff;
		long totTime = 0;
		List<String> wordSet = new ArrayList<>();
		for (int i = 0; i < centroids.length; i++) {
			LOG.info("Sending query for Centroid " + (i + 1));
			startTime = System.currentTimeMillis();
			Map<String, float[]> closestWordMap = nbm.getClosestEntry(centroids[i]);
			diff = System.currentTimeMillis() - startTime;
			totTime += diff;
			wordSet.addAll(closestWordMap.keySet());
			LOG.info("Query time recorded for Centroid " + (i + 1) + " is " + diff + " milliseconds.");
		}

		LOG.info("Average query time: " + (totTime / centroids.length) + " milliseconds");
		float percVal = NrmlzdMdlPrfmncTester.calcPercScore(correctWords, wordSet);
		LOG.info("Score for the Test is " + percVal + "%");
	}
}

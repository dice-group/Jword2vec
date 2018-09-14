package org.aksw.word2vecrestful;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelBruteForce;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelIndxdLR;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dice_research.topicmodeling.commons.sort.AssociativeSort;
import org.junit.Test;

import nikit.test.TestConst;

public class NrmlzdMdlPrfmncTester {
	static {
		PropertyConfigurator.configure(Cfg.LOG_FILE);
	}
	public static Logger LOG = LogManager.getLogger(NrmlzdMdlPrfmncTester.class);

	@Test
	public void testNbmTime() {
		LOG.info("Starting InMemory indexed model test!");
		Word2VecModel nbm = Word2VecFactory.getNormalBinModel();
		float[][] centroids = { TestConst.CENT1, TestConst.CENT2, TestConst.CENT3, TestConst.CENT4, TestConst.CENT5 };
		List<String> correctWords = getCorrectWords(centroids, nbm);
		LOG.info("Correct Words are :" + correctWords);
		int kStrt = 100;
		int kEnd = 250;
		int sigStrt = 3;
		int sigEnd = 5;
		int arDivStrt = 1;
		int arDivEnd = 10;
		int indx = 0;
		float[] percScore = new float[(kEnd - kStrt + 1) * (sigEnd - sigStrt + 1) * (arDivEnd - arDivStrt + 1)];
		int[] idArr = new int[percScore.length];
		final W2VNrmlMemModelIndxdLR memModel = new W2VNrmlMemModelIndxdLR(nbm.word2vec, nbm.vectorSize);
		for (int a = kStrt; a <= kEnd; a++) {
			for (int b = arDivStrt; b <= arDivEnd; b++) {
				for (int c = sigStrt; c <= sigEnd; c++) {
					LOG.info("Starting LR-Model Test with config: kVal=" + a + " and sigMult=" + c + " and arDiv=" + b);
					List<String> lrModelWords = runLRMemModel(centroids, memModel, a, b, c);
					LOG.info("Predicted Words are :" + lrModelWords);
					float percVal = calcPercScore(correctWords, lrModelWords);
					idArr[indx] = indx + 1;
					percScore[indx] = percVal;
					LOG.info("Percentage Score for Test id: " + (++indx) + " is" + percVal);
				}
			}
		}
		AssociativeSort.quickSort(percScore, idArr);
		LOG.info("Highest Score ("+percScore[percScore.length-1]+"%) is achieved by the test id: " + idArr[idArr.length - 1]);
	}

	private float calcPercScore(List<String> correctWords, List<String> lrModelWords) {
		float percScore = 0;
		int len = correctWords.size();
		float lenInv = 100f / len;
		for (int i = 0; i < len; i++) {
			if (correctWords.get(i).equals(lrModelWords.get(i))) {
				percScore += lenInv;
			}
		}
		return percScore;

	}

	private List<String> runLRMemModel(float[][] centroids, W2VNrmlMemModelIndxdLR memModel, int k, int arDiv,
			int sigMult) {
		memModel.setK(k);
		memModel.updateSdArr(sigMult, arDiv);
		List<String> wordSet = new ArrayList<>();
		long startTime, diff;
		long totTime = 0;
		for (int i = 0; i < centroids.length; i++) {
			LOG.info("Sending query for Centroid " + (i + 1));
			startTime = System.currentTimeMillis();
			Map<String, float[]> closestWord = memModel.getClosestSubEntry(centroids[i], null);
			diff = System.currentTimeMillis() - startTime;
			totTime += diff;
			wordSet.addAll(closestWord.keySet());
			LOG.info("Query time recorded for Centroid " + (i + 1) + " is " + diff + " milliseconds.");
		}

		LOG.info(
				"Average query time for W2VNrmlMemModelIndxdLR is : " + (totTime / centroids.length) + " milliseconds");
		return wordSet;
	}

	public List<String> getCorrectWords(float[][] centroids, Word2VecModel nbm) {
		List<String> wordSet = new ArrayList<>();
		W2VNrmlMemModelBruteForce bruteForce = new W2VNrmlMemModelBruteForce(nbm.word2vec, nbm.vectorSize);
		long startTime, diff;
		long totTime = 0;
		for (int i = 0; i < centroids.length; i++) {
			LOG.info("Sending query for Centroid " + (i + 1));
			startTime = System.currentTimeMillis();
			Map<String, float[]> closestWord = bruteForce.getClosestSubEntry(centroids[i], null);
			diff = System.currentTimeMillis() - startTime;
			totTime += diff;
			wordSet.addAll(closestWord.keySet());
			LOG.info("Query time recorded for Centroid " + (i + 1) + " is " + diff + " milliseconds.");
		}
		LOG.info("Average query time for BruteForce is : " + (totTime / centroids.length) + " milliseconds");
		return wordSet;
	}
}

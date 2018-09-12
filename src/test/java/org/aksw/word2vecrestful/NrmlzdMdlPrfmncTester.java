package org.aksw.word2vecrestful;

import java.util.Map;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelIndxdLR;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
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
		final W2VNrmlMemModelIndxdLR memModel = new W2VNrmlMemModelIndxdLR(nbm.word2vec, nbm.vectorSize);
		float[][] centroids = { TestConst.CENT1, TestConst.CENT2, TestConst.CENT3, TestConst.CENT4, TestConst.CENT5 };

		long startTime, diff;
		long totTime = 0;
		for (int i = 0; i < centroids.length; i++) {
			LOG.info("Sending query for Centroid " + (i + 1));
			startTime = System.currentTimeMillis();
			Map<String, float[]> closestWord = memModel.getClosestSubEntry(centroids[i], null);
			diff = System.currentTimeMillis() - startTime;
			totTime += diff;
			LOG.info(closestWord.keySet());
			LOG.info("Query time recorded for Centroid " + (i + 1) + " is " + diff + " milliseconds.");
		}

		LOG.info("Average query time: " + (totTime / centroids.length) + " milliseconds");

	}
}

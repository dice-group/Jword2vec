package org.aksw.word2vecrestful;

import java.util.HashMap;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModel;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Assert;
import org.junit.Test;

public class NormalizedInMemModelTest {
	static {
		PropertyConfigurator.configure(Cfg.LOG_FILE);
	}
	public static Logger LOG = LogManager.getLogger(NormalizedInMemModelTest.class);

	@Test
	public void testNormalizedModel() {
		LOG.info("Starting InMemory indexed model test!");
		final W2VNrmlMemModel memModel = Word2VecFactory.getNormalizedBinModel();
		LOG.info("Indexed Model instance created");
		Map<String, String> wordKeyMap = new HashMap<>();
		/*
		 * wordKeyMap.put("WesternOne", "ns#country-name");
		 * wordKeyMap.put("Donald_O._Schnuck",
		 * "ontology#ConferenceVenuePlacerdf-schema#label"); wordKeyMap.put("Skyytek",
		 * "icaltzd#summary"); wordKeyMap.put("Sungai_Muar",
		 * "ontology#Presenterrdf-schema#label");
		 */
		wordKeyMap.put("cat", null);
		wordKeyMap.put("dog", null);
		wordKeyMap.put("airplane", null);

		wordKeyMap.put("road", null);
		long startTime, diff;
		long totTime = 0;
		for (String word : wordKeyMap.keySet()) {
			LOG.info("Sending query for word :"+ word);
			startTime = System.currentTimeMillis();
			float[] vec = memModel.getWord2VecMap().get(word);
			Map<String, float[]> closestWord = memModel.getClosestSubEntry(vec, wordKeyMap.get(word));
			Assert.assertTrue(closestWord.containsKey(word));
			diff = System.currentTimeMillis() - startTime;
			totTime += diff;
			LOG.info("Query time recorded for the word: '" + word + "' and subset: '" + wordKeyMap.get(word) + "' is "
					+ diff + " milliseconds.");
		}

		LOG.debug("Average query time: " + (totTime / wordKeyMap.size()) + " milliseconds");

	}

	public static void main(String[] args) {
		LOG.info("Starting test!");
		NormalizedInMemModelTest inMemModelTest = new NormalizedInMemModelTest();
		inMemModelTest.testNormalizedModel();
		LOG.info("Test finished!");
	}
}

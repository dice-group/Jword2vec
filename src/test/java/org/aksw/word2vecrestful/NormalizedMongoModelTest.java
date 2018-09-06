package org.aksw.word2vecrestful;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.aksw.word2vecrestful.word2vec.W2VNrmlMongoDbModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import nikit.test.TestConst;

public class NormalizedMongoModelTest {
	public static Logger LOG = LogManager.getLogger(NormalizedMongoModelTest.class);
	static {
		TestConst.VEC_MAP.put("cat", TestConst.CAT);
		TestConst.VEC_MAP.put("dog", TestConst.DOG);
		TestConst.VEC_MAP.put("airplane", TestConst.AIRPLANE);
		TestConst.VEC_MAP.put("road", TestConst.ROAD);
	}
	@Test
	public void testNormalizedModel() throws IOException {

		//final W2VNrmlMemModel memModel = Word2VecFactory.getNormalizedBinModel();
		final W2VNrmlMongoDbModel mongoModel = new W2VNrmlMongoDbModel(300);
		Map<String, String> wordKeyMap = new HashMap<>();
		wordKeyMap.put("cat", null);
		wordKeyMap.put("dog", null);
		wordKeyMap.put("airplane", null);
		wordKeyMap.put("road", null);
		long startTime, diff;
		long totTime = 0;
		for (String word : wordKeyMap.keySet()) {
			startTime = System.currentTimeMillis();
			float[] vec = TestConst.VEC_MAP.get(word);
			Map<String, float[]> closestWord = mongoModel.getClosestSubEntry(vec, wordKeyMap.get(word));
			Assert.assertNotNull(closestWord);
			Assert.assertTrue(closestWord.containsKey(word));
			diff = System.currentTimeMillis() - startTime;
			totTime += diff;
			LOG.info("Query time recorded for the word: '" + word + "' and subset: '" + wordKeyMap.get(word) + "' is "
					+ diff + " milliseconds.");
		}

		LOG.info("Average query time: " + (totTime / wordKeyMap.size()) + " milliseconds");

	}
}

package nikit.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import com.fasterxml.jackson.core.JsonProcessingException;

public class Word2VecTester {
	public static void main(final String[] a) throws JsonProcessingException, FileNotFoundException, IOException {

	    // loads model in memory
	    final Word2VecModel model = Word2VecFactory.get();
	    /* final float[] vecCat = model.word2vec.get("cat");
	    final float[] vecDog = model.word2vec.get("dog");
	    final float[] vecItaly = model.word2vec.get("Italy");
	    final float[] vecFrance = model.word2vec.get("France");
	    final float[] vecInstead = model.word2vec.get("instead");
	    final float[] vecWhether = model.word2vec.get("whether");
	    final float[] vecHello = model.word2vec.get("hello");
	    final float[] vecBand = model.word2vec.get("band");
	    final float[] vecBeer = model.word2vec.get("beer");
	    final float[] vecBear = model.word2vec.get("bear");
	    final float[] vecBang = model.word2vec.get("bang");
	    final float[] vecBang = model.word2vec.get("bang");
	    vecCat[34] = 234;
	    vecDog[12] = 342;
	    vecItaly[80] = 12;
	    vecItaly[23] = 212;
	    vecFrance[76] = 12;
	    vecFrance[26] = 34;
	    vecFrance[35] = 1231;
	    vecInstead[37] = 35;
	    vecWhether[29] = 67;
	    vecHello[78] = 34;
	    vecBand[76] = -12;
	    vecBeer[23] = 32;
	    vecBear[56] = -8;
	    vecBang[13] = 23;
	    
	    System.out.println(model.getClosestEntry(vecCat));
	    System.out.println(model.getClosestEntry(vecDog));
	    System.out.println(model.getClosestEntry(vecItaly));
	    System.out.println(model.getClosestEntry(vecFrance));
	    System.out.println(model.getClosestEntry(vecInstead));
	    System.out.println(model.getClosestEntry(vecWhether));
	    System.out.println(model.getClosestEntry(vecHello));
	    System.out.println(model.getClosestEntry(vecBand));
	    System.out.println(model.getClosestEntry(vecBeer));
	    System.out.println(model.getClosestEntry(vecBear));
	    System.out.println(model.getClosestEntry(vecBang));
	    
	    final double sim = Word2VecMath.cosineSimilarity(vecCat, vecDog);
	    System.out.println(sim);*/
		// test 1
		float[] vec1 = model.word2vec.get("WesternOne");
		Map<String, float[]> closestWord = model.getClosestEntryInSub(vec1, "ns#country-name");
		System.out.println(closestWord.keySet());
		// test 2
		vec1 = model.word2vec.get("Donald_O._Schnuck");
		closestWord = model.getClosestEntryInSub(vec1, "ontology#ConferenceVenuePlacerdf-schema#label");
		System.out.println(closestWord.keySet());
		// test 3
		vec1 = model.word2vec.get("Ð°n_Ô_ÑÎ¿mÐµ");
		closestWord = model.getClosestEntryInSub(vec1, "ontology#ConferenceVenuePlacerdf-schema#label");
		System.out.println(closestWord.keySet());
	  }

}

package nikit.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import com.fasterxml.jackson.core.JsonProcessingException;

public class Word2VecTester {
	public static void main(final String[] a) throws JsonProcessingException, FileNotFoundException, IOException {

	    // loads model in memory
	    final Word2VecModel model = Word2VecFactory.get();
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

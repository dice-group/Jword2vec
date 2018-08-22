package org.aksw.word2vecrestful.subset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Class to help generate and persist the subsets of a word2vec model
 * 
 * @author Nikit
 *
 */
public class DataSubsetGenerator {

	public static final JsonNodeFactory JSON_FACTORY = JsonNodeFactory.instance;
	public static final String DATA_LABEL = "data";
	public static final String KEY_LABEL = "key";
	public static final String CENTROID_LABEL = "centroid";
	public static final String SD_LABEL = "sd";
	public static final ObjectMapper OBJ_MAPPER = new ObjectMapper();
	public static final ObjectReader OBJ_READER = OBJ_MAPPER.reader();
	public static final ObjectWriter OBJ_WRITER = OBJ_MAPPER.writer(new DefaultPrettyPrinter());
	/**
	 * Method to generate subset json files for a given configuration and word2vec model
	 * @param subsetConfig - configuration json file
	 * @param outputFileDir - output directory for the subset files
	 * @param word2vec - word2vec model map
	 * @param vectorSize - size of the vectors in model
	 * @throws JsonProcessingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void generateSubsetFiles(File subsetConfig, String outputFileDir, Map<String, float[]> word2vec,
			int vectorSize) throws JsonProcessingException, FileNotFoundException, IOException {
		// Read file into a json
		ObjectNode inpObj = (ObjectNode) OBJ_READER.readTree(new FileInputStream(subsetConfig));
		ArrayNode inpDt = (ArrayNode) inpObj.get(DATA_LABEL);
		// Traverse the json for keys
		Iterator<JsonNode> inpIt = inpDt.iterator();
		float[] maxlim = new float[vectorSize];
		float[] minlim = new float[vectorSize];
		while (inpIt.hasNext()) {

			JsonNode curNode = inpIt.next();
			// fetch value of key
			String key = curNode.get(KEY_LABEL).asText();
			// fetch value of centroid
			ArrayNode centroid = (ArrayNode) curNode.get(CENTROID_LABEL);
			// fetch value of standard deviation
			ArrayNode stndrdDev = (ArrayNode) curNode.get(SD_LABEL);
			// initiate a list of words
			ArrayNode wordArr = OBJ_MAPPER.createArrayNode();
			// Initialize result json
			ObjectNode resObj = new ObjectNode(JSON_FACTORY);
			// insert a mapping of key to list in the result json
			resObj.set(key, wordArr);
			boolean limitNotSet = true;
			// loop through the model
			for (Entry<String, float[]> wordEntry : word2vec.entrySet()) {
				String word = wordEntry.getKey();
				float[] wordvec = wordEntry.getValue();
				boolean isValid = true;
				for (int i = 0; i < centroid.size(); i++) {
					if (limitNotSet) {
						float centVal = centroid.get(i).floatValue();
						float sdVal = stndrdDev.get(i).floatValue();
						// maxlim = add sd to centroid
						maxlim[i] = centVal + 3 * sdVal;
						// minlim = subtract sb from centroid
						minlim[i] = centVal - 3 * sdVal;
					}
					// check if values of all the dimensions are under maxlim and minlim
					float curVal = wordvec[i];
					if (curVal > maxlim[i] || curVal < minlim[i]) {
						isValid = false;
						break;
					}
				}
				limitNotSet = false;
				if (isValid) {
					// store the word in the list
					wordArr.add(word);
				}
			}
			// Persist each json file
			File outputJsonFile = new File(outputFileDir + "/" + key + ".json");
			outputJsonFile.getParentFile().mkdirs();
			OBJ_WRITER.writeValue(outputJsonFile, resObj);
		}
	}
	/**
	 * Method to demonstrate example usage
	 * @param args
	 * @throws JsonProcessingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws JsonProcessingException, FileNotFoundException, IOException {
		File subsetConfig = new File(".\\word2vec-dump\\subsetconfig2.json");
		Word2VecModel model = Word2VecFactory.get();
		generateSubsetFiles(subsetConfig, Cfg.get("org.aksw.word2vecrestful.Application.subsetfiledir"), model.word2vec,
				model.vectorSize);
	}
}

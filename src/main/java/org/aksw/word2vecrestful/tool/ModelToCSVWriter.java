package org.aksw.word2vecrestful.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.CSVWriter;
/**
 * Class to help write a word2vec model to a csv file
 * @author Nikit
 *
 */
public class ModelToCSVWriter {
	/**
	 * Method to write a word2vecmodel to a csv file
	 * @param filePath - path to csv file
	 * @param word2vec - word2vec model map
	 * @param vectorSize - size of the vectors in model
	 */
	public static void writeDataLineByLine(String filePath, Map<String, float[]> word2vec, int vectorSize) {
		// first create file object for file placed at location
		// specified by filepath
		File file = new File(filePath);
		try {
			// create FileWriter object with file as parameter
			FileWriter outputfile = new FileWriter(file);

			// create CSVWriter object filewriter object as parameter
			CSVWriter writer = new CSVWriter(outputfile);
			String[] rowStr = new String[vectorSize + 1];
			for (Entry<String, float[]> wordEntry : word2vec.entrySet()) {
				rowStr[0] = wordEntry.getKey();
				float[] value = wordEntry.getValue();
				for (int i = 0; i < vectorSize; i++) {
					rowStr[i + 1] = String.valueOf(value[i]);
				}
				writer.writeNext(rowStr);
			}

			// closing writer connection
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
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
		Word2VecModel model = Word2VecFactory.get();
		writeDataLineByLine(".\\word2vec-dump\\word2vec-model.csv", model.word2vec,
				model.vectorSize);
	}

}

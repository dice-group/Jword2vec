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

public class ModelToCSVWriter {

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

	public static void main(String[] args) throws JsonProcessingException, FileNotFoundException, IOException {
		Word2VecModel model = Word2VecFactory.get();
		writeDataLineByLine("D:\\Nikit\\DICE-Group\\word2vec-dump\\word2vec-model.csv", model.word2vec,
				model.vectorSize);
	}

}

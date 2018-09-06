package org.aksw.word2vecrestful.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModel;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.CSVWriter;

public class ModelStatsWriter {
	public static Logger LOG = LogManager.getLogger(ModelStatsWriter.class);

	// Method to write the results to a csv
	public static Map<Integer, Float> writeModelStats(Map<String, float[]> word2vecMap, int vectorSize, File outputFile)
			throws IOException {
		Map<Integer, Float> resMap = new HashMap<>();
		outputFile.getParentFile().mkdirs();
		// create FileWriter object with file as parameter
		FileWriter fileWriter = new FileWriter(outputFile);
		// create CSVWriter object filewriter object as parameter
		CSVWriter writer = new CSVWriter(fileWriter);
		// Writer Header
		String[] header = { "dimension_index", "min", "max", "mean", "variance", "sd" };
		writer.writeNext(header);
		Set<Entry<String, float[]>> entries = word2vecMap.entrySet();
		int totSize = word2vecMap.size();
		// loop all dimensions
		for (int i = 0; i < vectorSize; i++) {
			// loop through all the words
			Float min = null;
			Float max = null;
			int j = 0;
			float[] dimsnArr = new float[totSize];
			float sum = 0;
			for (Entry<String, float[]> entry : entries) {
				float[] vecArr = entry.getValue();
				float val = vecArr[i];
				if (min == null || val < min) {
					min = val;
				}
				if (max == null || val > max) {
					max = val;
				}
				sum += val;
				dimsnArr[j++] = val;
			}
			// mean
			float mean = sum / dimsnArr.length;
			sum = 0;
			for (j = 0; j < dimsnArr.length; j++) {
				sum += Math.pow(dimsnArr[j] - mean, 2);
			}
			float variance = sum / dimsnArr.length;
			Double sd = Math.sqrt(variance);
			resMap.put(i, sd.floatValue());
			// Write values to file
			writeValues(i, min, max, mean, variance, sd, writer);
		}
		writer.close();
		return resMap;
	}

	private static void writeValues(int index, float min, float max, float mean, float variance, double sd,
			CSVWriter writer) {
		String[] row = { String.valueOf(index + 1), String.valueOf(min), String.valueOf(max), String.valueOf(mean),
				String.valueOf(variance), String.valueOf(sd) };
		writer.writeNext(row);
	}

	/**
	 * Method to demonstrate example usage
	 * 
	 * @param args
	 * @throws JsonProcessingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws JsonProcessingException, FileNotFoundException, IOException {
		// Get the normalized model
		W2VNrmlMemModel model = Word2VecFactory.getNormalizedBinModel();
		writeModelStats(model.getWord2VecMap(), model.getVectorSize(), new File("data/normal/stat/normal-model-stats.csv"));
	}
}

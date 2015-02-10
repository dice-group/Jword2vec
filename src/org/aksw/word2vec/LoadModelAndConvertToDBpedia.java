package org.aksw.word2vec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class LoadModelAndConvertToDBpedia {
	public static void main(String[] args) {
		Model model = ModelLoader.loadModel(new File("freebase-vectors-skipgram1000.bin"));
		try {

			System.out.println("finished reading freebase");
			TTLReader reader = new TTLReader();
			Map<String, String> freebase2DBpedia = reader.loadDBpediaMappings();
			System.out.println("finished reading dbpedia mappings");

			BufferedWriter bw = new BufferedWriter(new FileWriter("dbpedia-vectors-skipgram1000.txt"));

			for (String freebase : model.word2vec.keySet()) {
				bw.write(freebase2DBpedia.get(freebase) + "\t" + Arrays.toString(model.word2vec.get(freebase)));
			}
			bw.close();

			System.out.println("finished writing vectors");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

package org.aksw.word2vecrestful.subset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.aksw.word2vecrestful.utils.Cfg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DataSubsetProvider {

	public static String fileDir = Cfg.get("org.aksw.word2vecrestful.Application.subsetfiledir");
	public static final ObjectMapper OBJ_MAPPER = new ObjectMapper();
	public static final ObjectReader OBJ_READER = OBJ_MAPPER.reader();

	public static List<String> fetchSubsetWords(String subsetKey)
			throws JsonProcessingException, FileNotFoundException, IOException {
		List<String> resList = new ArrayList<>();
		// logic to fetch the words from the stored subsets
		ObjectNode inpObj = null;
		File dir = new File(fileDir);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				// Do something with child
				if (child.getName().equalsIgnoreCase(getJsonExtension(subsetKey))) {
					inpObj = (ObjectNode) OBJ_READER.readTree(new FileInputStream(child));
					break;
				}
			}
		}
		if(inpObj!=null) {
			ArrayNode wordArr = (ArrayNode) inpObj.get(subsetKey);
			for(int i=0;i<wordArr.size();i++) {
				resList.add(wordArr.get(i).asText());
			}
		}
		return resList;
	}

	public static String getJsonExtension(String name) {
		return name + ".json";
	}

}

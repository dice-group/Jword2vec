package org.aksw.word2vecrestful.subset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Cfg;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Class to help retrieve the list of words from a subset stored on the disk
 * 
 * @author Nikit
 *
 */
public class DataSubsetProvider {

	public static String fileDir = Cfg.get("org.aksw.word2vecrestful.Application.subsetfiledir");
	public static final Map<String, List<String>> SUBSET_MODELS = new HashMap<>();

	/**
	 * Method to fetch the list of words in a subset
	 * 
	 * @param subsetKey
	 *            - key to identify the subset
	 * @return - a list of words in the related subset
	 * @throws JsonProcessingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static List<String> fetchSubsetWords(String subsetKey)
			throws IOException {
		// fetch from cache
		List<String> resList = SUBSET_MODELS.get(subsetKey);
		// if not in cache then read from file and add to cache
		if (resList == null) {
			resList = new ArrayList<>();
			// logic to fetch the words from the stored subsets
			File dir = new File(fileDir);
			// declare inputstream
			BufferedReader bReader = null;
			File[] directoryListing = dir.listFiles();
			if (directoryListing != null) {
				for (File child : directoryListing) {
					// Do something with child
					if (child.getName().equalsIgnoreCase(appendFileExtension(subsetKey))) {
						bReader = new BufferedReader(new FileReader(child));
						break;
					}
				}
			}
			if (bReader != null) {
				while (true) {
					String word = bReader.readLine();
					if (word == null) {
						break;
					}
					resList.add(word);
				}
			}
			SUBSET_MODELS.put(subsetKey, resList);
		}
		return resList;
	}

	/**
	 * Method to append txt extension at the end of a key
	 * 
	 * @param name
	 *            - key
	 * @return key appended with txt extension
	 */
	public static String appendFileExtension(String name) {
		return name + ".txt";
	}

}

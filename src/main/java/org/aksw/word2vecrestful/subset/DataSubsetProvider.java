package org.aksw.word2vecrestful.subset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aksw.word2vecrestful.utils.Cfg;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Class to help retrieve the list of words from a subset stored on the disk
 * 
 * @author Nikit
 *
 */
public class DataSubsetProvider {

	private String fileDir = Cfg.get("org.aksw.word2vecrestful.Application.subsetfiledir");
	private final Map<String, Set<String>> SUBSET_MODELS = new HashMap<>();

	/**
	 * Method to fetch the set of words in a subset
	 * 
	 * @param subsetKey
	 *            - key to identify the subset
	 * @return - a list of words in the related subset
	 * @throws JsonProcessingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Set<String> fetchSubsetWords(String subsetKey) throws IOException {
		// fetch from cache
		Set<String> resList = SUBSET_MODELS.get(subsetKey);
		// if not in cache then read from file and add to cache
		if (resList == null) {
			// logic to fetch the words from the stored subsets
			File file1 = new File(fileDir + "/" + appendFileExtension(subsetKey));
			if (file1.exists()) {
				resList = new HashSet<>();
				resList.addAll(FileUtils.readLines(file1, StandardCharsets.UTF_8));
				SUBSET_MODELS.put(subsetKey, resList);
			}
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

package org.aksw.word2vecrestful.word2vec;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.aksw.word2vecrestful.db.mongo.MongoDbHandler;
import org.aksw.word2vecrestful.subset.DataSubsetProvider;
import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.opencsv.CSVReader;

public class W2VNrmlMongoDbModel implements GenWord2VecModel {

	public static Logger LOG = LogManager.getLogger(W2VNrmlMongoDbModel.class);

	private String wordColName = "word";
	private String vecColPrefix = "val";

	private int indexSize = 63;

	private int vectorSize;
	private MongoDbHandler dbHandler;
	private String dbName = "word2vec";
	private String collctnName = "mainCollection";
	private Map<Integer, Float> sdMap;
	private static String STAT_FILE_PATH = Cfg.get("org.aksw.word2vecrestful.word2vec.stats.sdfile");
	/**
	 * Limit to the multiplier of area in which nearby vectors are to be looked
	 */
	private static final int EXHAUSTION_MULT = 10;
	/**
	 * Multiplier for the standard deviation
	 */
	private static final int SIGMA_MULT = 3;
	/**
	 * Divisor for the standard deviation's value
	 */
	private static final int AREA_DIVISOR = 10;
	private DataSubsetProvider dataSubsetProvider;

	public W2VNrmlMongoDbModel(int vectorSize) throws IOException {
		this.vectorSize = vectorSize;
		this.dbHandler = new MongoDbHandler(this.dbName, null, null);
		this.dataSubsetProvider = new DataSubsetProvider();
		// Calculate sd*3/10 and save in map
		setModelSd(new File(STAT_FILE_PATH));
	}

	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector
	 *            - vector to find closest word to
	 * @param subKey
	 *            - key to subset if any
	 * @return closest word to the given vector alongwith it's vector
	 */
	private Map<String, float[]> getClosestEntry(float[] vector, String subKey) {
		Set<String> wordSet = null;
		Map<String, float[]> closestVec = null;
		try {
			dbHandler.connect();
			if (subKey == null) {
				wordSet = null;
			} else {
				wordSet = dataSubsetProvider.fetchSubsetWords(subKey);
			}
			// Normalize incoming vector
			vector = Word2VecMath.normalize(vector);
			// Find nearby vectors
			Map<String, float[]> nearbyVecs = fetchNearbyVectors(vector, wordSet);
			// Select the closest vector
			closestVec = Word2VecMath.findClosestVecInNearbyVecs(nearbyVecs, vector);
		} catch (Exception e) {
			LOG.error(e.getStackTrace());
		} finally {
			dbHandler.close();
		}
		return closestVec;
	}

	/**
	 * Method to fetch nearby vectors for a given vector in a particular word set
	 * 
	 * @param vector
	 *            - vector to look nearby vectors for
	 * @param wordSet
	 *            - word set to look into for nearby vectors
	 * @return - mapping of nearby words alongwith with their vector values
	 */
	private Map<String, float[]> fetchNearbyVectors(float[] vector, Set<String> wordSet) {
		Map<String, float[]> nearbyVecMap = null;
		boolean mapEmpty = true;
		boolean notExhausted = true;
		Map<Integer, float[]> minMaxVecMap = getMinMaxVecMap(vector);
		int mult = 1;
		while (mapEmpty && notExhausted) {
			if (mult > 1) {
				multMinMaxVecMap(minMaxVecMap, mult);
			}
			// Query Nearby
			nearbyVecMap = queryNearbyVecs(vector, minMaxVecMap, wordSet);
			if (nearbyVecMap.size() > 0) {
				mapEmpty = false;
			} else if (++mult > EXHAUSTION_MULT) {
				notExhausted = false;
			}

		}
		return nearbyVecMap;
	}

	private Map<String, float[]> queryNearbyVecs(float[] vector, Map<Integer, float[]> minMaxVecMap,
			Set<String> wordSet) {
		Map<String, float[]> resMap = new HashMap<>();

		BasicDBObject queryObj = new BasicDBObject();
		MongoCollection<Document> mongoCollection = dbHandler.getDatabase().getCollection(this.collctnName);
		if (wordSet != null && wordSet.size() > 0) {
			queryObj.put(wordColName, wordSet);
		}
		for (Integer vecDim : minMaxVecMap.keySet()) {
			float[] minMaxVal = minMaxVecMap.get(vecDim);
			BasicDBObject filterObj = new BasicDBObject("$gt", minMaxVal[0]).append("$lt", minMaxVal[1]);
			queryObj.put(vecColPrefix + vecDim, filterObj);
		}
		FindIterable<Document> findIt = mongoCollection.find(queryObj);
		MongoCursor<Document> it = findIt.iterator();
		while (it.hasNext()) {
			Document doc = it.next();
			String word = doc.getString(wordColName);
			float[] vec = new float[vectorSize];
			for (int i = 0; i < vectorSize; i++) {
				Double dVal = doc.getDouble(vecColPrefix + (i + 1));
				vec[i] = dVal.floatValue();
			}
			resMap.put(word, vec);
		}

		return resMap;

	}

	/**
	 * Multiply each element of the given multi dimensional vector with a given
	 * multiplier
	 * 
	 * @param minMaxVecMap
	 *            - vector at which operation is to be performed
	 * @param mult
	 *            - multiplier
	 * @return - Vector after multiplication with the multiplier
	 */
	private void multMinMaxVecMap(Map<Integer, float[]> minMaxVecMap, int mult) {
		for (Integer vecDim : minMaxVecMap.keySet()) {
			float[] minMaxVal = minMaxVecMap.get(vecDim);
			minMaxVal[0] *= mult;
			minMaxVal[1] *= mult;
			minMaxVecMap.put(vecDim, minMaxVal);
		}
	}

	/**
	 * Method to generate two vectors from a given vector by adding and subtracting
	 * value in sdMap from the given vector
	 * 
	 * @param vector
	 *            - input vector to perform operation on
	 * @return - min vector at index 0 and max vector at index 1
	 */
	private Map<Integer, float[]> getMinMaxVecMap(float[] vector) {
		Map<Integer, float[]> resMap = new HashMap<>();
		float[] minMaxVal;
		for (Integer vecDim : sdMap.keySet()) {
			minMaxVal = new float[2];
			float diff = sdMap.get(vecDim);
			minMaxVal[0] = vector[vecDim] - diff;
			minMaxVal[1] = vector[vecDim] + diff;
			resMap.put(vecDim, minMaxVal);
		}
		return resMap;
	}

	@Override
	public int getVectorSize() {
		return this.vectorSize;
	}

	@Override
	public Map<String, float[]> getClosestEntry(float[] vector) {
		return getClosestEntry(vector, null);
	}

	@Override
	public Map<String, float[]> getClosestSubEntry(float[] vector, String subKey) {
		return getClosestEntry(vector, subKey);
	}

	/**
	 * Method to find standard deviation for each dimension of word vector and store
	 * the operated value next to the dimension's index in sdMap
	 * 
	 * @param file
	 *            - file with standard deviation values of vectors in descending
	 *            order
	 * @throws IOException
	 */
	public void setModelSd(File file) throws IOException {
		Map<Integer, Float> resMap = new HashMap<>();
		CSVReader csvReader = new CSVReader(new FileReader(file));
		// Reading header
		csvReader.readNext();
		// loop all dimensions
		for (int i = 0; i < indexSize; i++) {
			String[] entry = csvReader.readNext();

			Double sd = Double.parseDouble(entry[1]);
			resMap.put(Integer.parseInt(entry[0]), sd.floatValue() * SIGMA_MULT / AREA_DIVISOR);
		}
		csvReader.close();
		// Set as sdMap
		this.sdMap = resMap;
	}

}

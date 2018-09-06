package org.aksw.word2vecrestful.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.aksw.word2vecrestful.db.mongo.MongoDbHandler;
import org.aksw.word2vecrestful.word2vec.Word2VecModelLoader;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.Indexes;
import com.opencsv.CSVReader;

public class MongoDBModelGenerator {

	public static Logger LOG = LogManager.getLogger(MongoDBModelGenerator.class);

	private String wordColName = "word";
	private String vecColPrefix = "val";
	private String[] vecColNames;
	private MongoDbHandler mongoDbHandler;
	private MongoCollection<Document> mongoCollection;
	private String collctnName;

	public MongoDBModelGenerator(String collctnName, String dbName, String host, Integer port) {
		this.mongoDbHandler = new MongoDbHandler(dbName, host, port);
		this.collctnName = collctnName;
	}

	private Document createWord2VecDoc(String word, float[] vector) {
		Document doc = new Document();
		doc.put(wordColName, word);
		for (int i = 0; i < vector.length; i++) {
			doc.put(vecColNames[i], vector[i]);
		}
		return doc;
	}

	private String[] createVecColNames(int vectorSize) {
		String[] colnames = new String[vectorSize];
		for (int i = 0; i < vectorSize; i++) {
			colnames[i] = vecColPrefix + (i + 1);
		}
		return colnames;
	}

	private void createIndexes() {
		List<IndexModel> indexList = new ArrayList<>();
		IndexModel indexModel = new IndexModel(Indexes.text(wordColName));
		indexList.add(indexModel);
		for (int i = 0; i < vecColNames.length; i++) {
			indexModel = new IndexModel(Indexes.ascending(vecColNames[i]));
			indexList.add(indexModel);
		}
		this.mongoCollection.createIndexes(indexList);
	}

	private void createIndexes(List<String> fieldNames) {
		List<IndexModel> indexList = new ArrayList<>();
		IndexModel indexModel;
		for (String field : fieldNames) {
			indexModel = new IndexModel(Indexes.ascending(field));
			indexList.add(indexModel);
		}
		this.mongoCollection.createIndexes(indexList);
	}

	/**
	 * Use ModelStatsWriter Instead
	 * 
	 * @param n
	 * @param vectorSize
	 * @return
	 */
	@Deprecated
	private List<String> getNImportantFieldNames(int n, int vectorSize) {
		List<String> fieldNames = new ArrayList<>();
		TreeMap<Double, String> stdDevMap = new TreeMap<>(Collections.reverseOrder());
		for (int i = 0; i < vectorSize; i++) {
			String colName = vecColPrefix + (i + 1);
			AggregateIterable<org.bson.Document> aggregate = mongoCollection.aggregate(Arrays.asList(Aggregates
					.group("_id", new BsonField("stdDev", new BsonDocument("$stdDevPop", new BsonString(colName))))));
			Document result = aggregate.first();
			Double stdDevVal = result.getDouble("stdDev");
			stdDevMap.put(stdDevVal, colName);
		}
		int count = 0;
		for (Entry<Double, String> entry : stdDevMap.entrySet()) {
			fieldNames.add(entry.getValue());
			if (++count == n) {
				break;
			}
		}
		return fieldNames;
	}

	private List<String> getNImportantFieldNames(int n) throws IOException {
		List<String> fieldNames = new ArrayList<>();
		File file = new File("D:\\Nikit\\DICE-Group\\Jword2vec\\data\\normal\\stat\\normal-model-sd.csv");
		CSVReader csvReader = new CSVReader(new FileReader(file));
		// Reading header
		csvReader.readNext();
		for (int i = 0; i < n; i++) {
			String fieldName = vecColPrefix + csvReader.readNext()[0];
			fieldNames.add(fieldName);
		}
		csvReader.close();
		return fieldNames;
	}

	public void generateTopIndexes(int vectorSize) {
		this.mongoDbHandler.connect();
		this.mongoCollection = this.mongoDbHandler.getDatabase().getCollection(this.collctnName);
		try {
			// Find top vectors
			List<String> fieldNames = getNImportantFieldNames(63);
			// Generate indexes
			createIndexes(fieldNames);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.mongoDbHandler.close();
		}

	}

	public void persistWord2VecModel(File inputFile) {
		LOG.info("Starting insertion of records to MongoDB..");
		FileInputStream fin = null;
		this.mongoDbHandler.connect();
		this.mongoCollection = mongoDbHandler.createCollection(collctnName);
		// this.mongoCollection =
		// this.mongoDbHandler.getDatabase().getCollection(this.collctnName);
		try {
			fin = new FileInputStream(inputFile);
			String word = Word2VecModelLoader.readWord(fin);
			int words = Integer.parseInt(word);
			word = Word2VecModelLoader.readWord(fin);
			int vectorSize = Integer.parseInt(word);
			List<Document> docList = new ArrayList<>();
			int recCount = 0;
			this.vecColNames = createVecColNames(vectorSize);
			// Insert records
			for (int w = 0; w < words; ++w) {
				word = Word2VecModelLoader.readWord(fin);
				// LOG.info(word);
				float[] vector = Word2VecModelLoader.readVector(fin, vectorSize);
				docList.add(createWord2VecDoc(word, vector));
				recCount++;
				if (recCount % 10000 == 0) {
					this.mongoCollection.insertMany(docList);
					docList.clear();
					LOG.info((recCount) + " Records inserted.");
				}
			}
			// Insert leftover records
			if (docList.size() > 0) {
				this.mongoCollection.insertMany(docList);
			}
			LOG.info("Records insertion successfully completed.");
			LOG.info("Starting creation of indexes");
			// create indexes
			createIndexes();
			LOG.info("Indexes creation successfully completed.");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.mongoDbHandler.close();
		}
	}

	public static void main(String[] args) throws JsonProcessingException, FileNotFoundException, IOException {
		/*
		 * String inputModel =
		 * (Cfg.get("org.aksw.word2vecrestful.word2vec.normalizedbinmodel.model")); File
		 * inputFile = new File(inputModel);
		 */
		MongoDBModelGenerator modelGenerator = new MongoDBModelGenerator("mainCollection", "word2vec", null, null);
		/* modelGenerator.persistWord2VecModel(inputFile); */
		modelGenerator.generateTopIndexes(300);
	}
}

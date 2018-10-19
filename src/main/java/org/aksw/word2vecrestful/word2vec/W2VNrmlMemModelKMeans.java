package org.aksw.word2vecrestful.word2vec;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.utils.ClusterableVec;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

/**
 * Class to encapsulate word2vec in-memory model and expose methods to perform
 * search on the model. (Only works with Normalized Model)
 * 
 * This class selects {@link W2VNrmlMemModelKMeans#compareVecCount} vectors
 * (centroids of the KMeans result on the model vectors) and then calculates the
 * cosine similarity of all words in model to those vectors.
 * 
 * It uses the knowledge about pre-processed similarities with
 * {@link W2VNrmlMemModelKMeans#comparisonVecs} to narrow down the search of
 * closest word for the user specified vector.
 * 
 * @author Nikit
 *
 */
public class W2VNrmlMemModelKMeans extends W2VNrmlMemModelBinSrch {
	public static Logger LOG = LogManager.getLogger(GenWord2VecModel.class);

	private int kMeansMaxItr = 5;
	private String vecFilePath = Cfg.get(W2VNrmlMemModelKMeans.class.getName().concat(".filepath"));

	public W2VNrmlMemModelKMeans(final Map<String, float[]> word2vec, final int vectorSize) throws IOException {
		super(word2vec, vectorSize);
	}

	@Override
	public void process() throws IOException {
		LOG.info("Process from KMeans called");
		fetchComparisonVectors();
		// Initialize Arrays
		processCosineSim();
	}

	private void fetchComparisonVectors() throws IOException {
		File vecFile = new File(vecFilePath);
		if (vecFile.exists()) {
			LOG.info("Reading Comparsion vectors from the file.");
			// read the persisted vectors
			comparisonVecs = readVecsFromFile(vecFile);
		} else {
			LOG.info("Starting Generation of comparison vectors!");
			// Fetch comparison vectors
			generateComparisonVectors();
			// persist the generated vectors

			writeVecsToFile(comparisonVecs, vecFile);
		}
		LOG.info("Comparison vectors generated/fetched. Building buckets.");
	}

	private void generateComparisonVectors() {
		KMeansPlusPlusClusterer<ClusterableVec> clusterer = new KMeansPlusPlusClusterer<>(compareVecCount,
				kMeansMaxItr);
		List<ClusterableVec> vecList = new ArrayList<>();
		for (float[] vec : word2vec.values()) {
			vecList.add(getClusterablePoint(vec));
		}
		List<CentroidCluster<ClusterableVec>> compVecList = clusterer.cluster(vecList);
		int i = 0;
		for (CentroidCluster<ClusterableVec> entry : compVecList) {
			Clusterable centroid = entry.getCenter();
			LOG.info("Number of points in the cluster " + (i + 1) + " are: " + entry.getPoints().size());
			float[] fCentroid = Word2VecMath.convertDoublesToFloats(centroid.getPoint());
			comparisonVecs[i] = fCentroid;
			i++;
		}
	}

	public static ClusterableVec getClusterablePoint(float[] vec) {
		return new ClusterableVec(vec);
	}

	private void processCosineSim() {
		double cosSimVal;
		this.wordArr = new String[word2vec.size()];
		this.vecArr = new float[word2vec.size()][vectorSize];
		int i = 0;
		for (String word : word2vec.keySet()) {
			wordArr[i] = word;
			float[] vec = word2vec.get(word);
			vecArr[i] = vec;
			for (int j = 0; j < compareVecCount; j++) {
				BitSet[] comparisonVecBuckets = csBucketContainer[j];
				cosSimVal = Word2VecMath.cosineSimilarityNormalizedVecs(comparisonVecs[j], vec);
				// Setting bitset for the comparison vec
				setValToBucket(i, cosSimVal, comparisonVecBuckets);
			}
			i++;
		}
	}

	public static float[][] readVecsFromFile(File inputFile) throws IOException {
		float[][] vecArr = null;
		FileReader fileReader;
		CSVReader reader = null;
		try {
			fileReader = new FileReader(inputFile);
			reader = new CSVReader(fileReader);
			List<String[]> vecList = reader.readAll();
			vecArr = new float[vecList.size()][vecList.get(0).length];
			for (int i = 0; i < vecList.size(); i++) {
				vecArr[i] = convertToFloatArr(vecList.get(i));
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				// closing writer connection
				reader.close();
			}
		}
		return vecArr;
	}

	public static void writeVecsToFile(float[][] vecArr, File outputFile) throws IOException {
		outputFile.getParentFile().mkdirs();
		CSVWriter writer = null;
		try {
			// create FileWriter object with file as parameter
			FileWriter fileWriter = new FileWriter(outputFile);

			// create CSVWriter object filewriter object as parameter
			writer = new CSVWriter(fileWriter);
			for (int i = 0; i < vecArr.length; i++) {
				float[] vec = vecArr[i];
				String[] line = convertToStrArr(vec);
				writer.writeNext(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				// closing writer connection
				writer.close();
			}
		}
	}

	public static String[] convertToStrArr(float[] vec) {
		String[] resArr = new String[vec.length];
		for (int i = 0; i < resArr.length; i++) {
			resArr[i] = String.valueOf(vec[i]);
		}
		return resArr;
	}

	public static float[] convertToFloatArr(String[] vec) {
		float[] resArr = new float[vec.length];
		for (int i = 0; i < resArr.length; i++) {
			resArr[i] = Float.parseFloat(vec[i]);
		}
		return resArr;
	}

	// Getter and Setters
	public int getkMeansMaxItr() {
		return kMeansMaxItr;
	}

	public void setkMeansMaxItr(int kMeansMaxItr) {
		this.kMeansMaxItr = kMeansMaxItr;
	}

	public String getVecFilePath() {
		return vecFilePath;
	}

	public void setVecFilePath(String vecFilePath) {
		this.vecFilePath = vecFilePath;
	}

}

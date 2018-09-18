package org.aksw.word2vecrestful.tool;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModelLoader;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ModelNormalizer {
	public static Logger LOG = LogManager.getLogger(ModelNormalizer.class);
	public static final byte[] END_LINE_BA = "\n".getBytes(StandardCharsets.UTF_8);
	public static final byte[] WHITESPACE_BA = " ".getBytes(StandardCharsets.UTF_8);

	/**
	 * Method to normalize a bin word2vec model line
	 * 
	 * @param line
	 *            - line from a bin model to be normalized
	 * @param vectorSize
	 *            - size of the vector
	 * @return - normalized line
	 */
	public static String getNormalizedVecLine(String word, float[] vector) {
		StringBuffer resStr = new StringBuffer();
		resStr.append(word);
		vector = Word2VecMath.normalize(vector);
		for (int i = 0; i < vector.length; i++) {
			resStr.append(" ").append(String.valueOf(vector[i]));
		}
		return resStr.toString();
	}

	public static byte[] getNormalizedVecBA(float[] vector) {
		vector = Word2VecMath.normalize(vector);
		ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < vector.length; i++) {
			buffer.putFloat(vector[i]);
		}
		return buffer.array();
	}

	/**
	 * Method to generate a normalized model for a word2vec bin model
	 * 
	 * @param inputFile
	 *            - word2vec file of the model to be normalized
	 * @param outputFile
	 *            - output file for normalized model
	 * @throws IOException
	 */
	public void generateNormalizedModel(File inputFile, File outputFile) throws IOException {
		// ensure directory creation
		outputFile.getParentFile().mkdirs();
		// open an output stream
		BufferedWriter bWriter = null;
		FileInputStream fin = null;
		try {
			// reads file header
			fin = new FileInputStream(inputFile);
			String word = Word2VecModelLoader.readWord(fin);
			int words = Integer.parseInt(word);
			word = Word2VecModelLoader.readWord(fin);
			int vectorSize = Integer.parseInt(word);
			bWriter = new BufferedWriter(new FileWriterWithEncoding(outputFile, StandardCharsets.UTF_8));
			bWriter.write(words + " " + vectorSize);
			LOG.info("Expecting " + words + " words with " + vectorSize + " values per vector.");
			for (int w = 0; w < words; ++w) {
				word = Word2VecModelLoader.readWord(fin);
				// LOG.info(word);
				float[] vector = Word2VecModelLoader.readVector(fin, vectorSize);
				bWriter.newLine();
				bWriter.write(getNormalizedVecLine(word, vector));
				if (w % 10000 == 0) {
					bWriter.flush();
				}
			}
		} catch (final IOException e) {
			LOG.error(e.getLocalizedMessage(), e);
		} finally {
			fin.close();
			bWriter.close();
		}
	}

	/**
	 * Method to persist a normalized model for a word2vec bin model
	 * 
	 * @param inputFile
	 *            - word2vec file of the model to be normalized
	 * @param dbName
	 *            - name of the database
	 * @param tablName
	 *            - name of the table to store the data in
	 * @throws IOException
	 * @throws SQLException
	 */
	public void persistNormalizedModel(File inputFile, String dbName, String tblName) throws IOException, SQLException {
		// intialize handler instance
		NormalizedDBModelGenerator dbHandler = null;

		FileInputStream fin = null;
		try {
			// reads file header
			fin = new FileInputStream(inputFile);
			String word = Word2VecModelLoader.readWord(fin);
			int words = Integer.parseInt(word);
			word = Word2VecModelLoader.readWord(fin);
			int vectorSize = Integer.parseInt(word);
			dbHandler = new NormalizedDBModelGenerator(dbName, tblName, vectorSize);
			// open connection
			dbHandler.connect();
			LOG.info("Expecting " + words + " words with " + vectorSize + " values per vector.");
			// create preparedstatement
			PreparedStatement ps = dbHandler.generateMainTblInsrtStmnt();
			for (int w = 0; w < words; ++w) {
				word = Word2VecModelLoader.readWord(fin);
				// LOG.info(word);
				float[] vector = Word2VecModelLoader.readVector(fin, vectorSize);
				// dbHandler.insertMainTblRecord(word, vector);
				dbHandler.addMainTblInsrtBatch(word, Word2VecMath.normalize(vector), ps);
				if ((w + 1) % 50000 == 0) {
					dbHandler.executeBatchCommit(ps);
					LOG.info((w + 1) + " Records inserted.");
				}
			}
			dbHandler.executeBatchCommit(ps);
			// Generate Index on completion
			dbHandler.makeIndex();
		} catch (final IOException e) {
			LOG.error(e.getLocalizedMessage(), e);
		} finally {
			fin.close();
			dbHandler.disconnect();
		}
	}

	/**
	 * Method to generate a normalized model for a word2vec bin model
	 * 
	 * @param inputFile
	 *            - word2vec file of the model to be normalized
	 * @param outputFile
	 *            - output file for normalized model
	 * @throws IOException
	 */
	public void generateNormalizedBinModel(File inputFile, File outputFile) throws IOException {
		// ensure directory creation
		outputFile.getParentFile().mkdirs();
		// open an output stream
		BufferedOutputStream bOutStrm = null;
		FileInputStream fin = null;
		try {
			bOutStrm = new BufferedOutputStream(new FileOutputStream(outputFile));
			// reads file header
			fin = new FileInputStream(inputFile);
			String word = Word2VecModelLoader.readWord(fin);
			bOutStrm.write(word.getBytes(StandardCharsets.UTF_8));
			bOutStrm.write(WHITESPACE_BA);
			Integer words = Integer.parseInt(word);
			word = Word2VecModelLoader.readWord(fin);
			bOutStrm.write(word.getBytes(StandardCharsets.UTF_8));
			Integer vectorSize = Integer.parseInt(word);
			bOutStrm.write(END_LINE_BA);
			LOG.info("Expecting " + words + " words with " + vectorSize + " values per vector.");
			for (int w = 0; w < words; ++w) {
				word = Word2VecModelLoader.readWord(fin);
				// LOG.info(word);
				float[] vector = Word2VecModelLoader.readVector(fin, vectorSize);
				
				bOutStrm.write(word.getBytes(StandardCharsets.UTF_8));
				bOutStrm.write(WHITESPACE_BA);
				bOutStrm.write(getNormalizedVecBA(vector));

				if ((w + 1) % 10000 == 0) {
					bOutStrm.flush();
					LOG.info((w + 1) + " Records inserted.");
				}
			}
		} catch (final IOException e) {
			LOG.error(e.getLocalizedMessage(), e);
		} finally {
			fin.close();
			bOutStrm.close();
		}
	}

	/*
	 * public static void main(String[] args) throws IOException { String
	 * cfgKeyModel = Word2VecFactory.class.getName().concat(".model"); String model
	 * = (Cfg.get(cfgKeyModel)); ModelNormalizer modelNormalizer = new
	 * ModelNormalizer(); File inputFile = new File(model); File outputFile = new
	 * File(
	 * "D:\\Nikit\\DICE-Group\\Jword2vec\\data\\normal\\GoogleNews-vectors-negative300-normalized.txt"
	 * ); modelNormalizer.generateNormalizedModel(inputFile, outputFile); }
	 */

	/*
	 * public static void main(String[] args) throws IOException, SQLException {
	 * String cfgKeyModel = Word2VecFactory.class.getName().concat(".model"); String
	 * model = (Cfg.get(cfgKeyModel)); ModelNormalizer modelNormalizer = new
	 * ModelNormalizer(); File inputFile = new File(model); //
	 * modelNormalizer.generateNormalizedModel(inputFile, outputFile);
	 * modelNormalizer.persistNormalizedModel(inputFile,
	 * "data/nrmldb/word2vecmodel", "wordtovec"); }
	 */

	public static void main(String[] args) throws IOException, SQLException {
		String cfgKeyModel = Word2VecFactory.class.getName().concat(".model");
		String model = (Cfg.get(cfgKeyModel));
		ModelNormalizer modelNormalizer = new ModelNormalizer();
		File inputFile = new File(model);
		// "org.aksw.word2vecrestful.word2vec.normalizedbinmodel.model"
		String outputModel = (Cfg.get("org.aksw.word2vecrestful.word2vec.normalizedbinmodel.model"));
		File outputFile = new File(outputModel);
		// modelNormalizer.generateNormalizedModel(inputFile, outputFile);
		modelNormalizer.generateNormalizedBinModel(inputFile, outputFile);
	}

}

package org.aksw.word2vecrestful.word2vec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author MichaelRoeder
 * @author rspeck
 *
 */
public class Word2VecModelLoader {
  static {
    PropertyConfigurator.configure(Cfg.CFG_FILE);
  }

  public static Logger LOG = LogManager.getLogger(Word2VecModelLoader.class);
  public static final String CFG_KEY_BIN = Word2VecModelLoader.class.getName().concat(".bin");
  protected boolean binModel = Boolean.parseBoolean(Cfg.get(CFG_KEY_BIN));

  public static void main(final String[] a) {

    // loads model in memory
    final Word2VecModel model = Word2VecFactory.get();
    final float[] vecCat = model.word2vec.get("cat");
    final float[] vecDog = model.word2vec.get("dog");

    final double sim = Word2VecMath.cosineSimilarity(vecCat, vecDog);
    LOG.info("sim: " + sim);
  }

  /**
   *
   * @param file
   * @return
   * @throws IOException
   * @throws FileNotFoundException
   */
  public Word2VecModel loadModel(final File file) {
    int vectorSize = -1;
    int words = -1;

    String word;

    final Map<String, float[]> word2Vector = new HashMap<String, float[]>();

    if (!binModel) {

      try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        final String header = br.readLine();
        words = Integer.parseInt(header.split(" ")[0]);
        vectorSize = Integer.parseInt(header.split(" ")[1]);
        LOG.info("Expecting " + words + " words with " + vectorSize + " values per vector.");

        for (String line; (line = br.readLine()) != null;) {
          final float vector[] = new float[vectorSize];
          final String[] split = line.split(" ");
          for (int i = 1; i < split.length; i++) {
            vector[i - 1] = Float.parseFloat(split[i]);
          }
          word2Vector.put(split[0], vector);

        }
      } catch (final IOException e) {
        LOG.error(e.getLocalizedMessage(), e);
      }
    } else {

      FileInputStream fin = null;
      try {
        // reads file header
        fin = new FileInputStream(file);
        word = readWord(fin);
        words = Integer.parseInt(word);
        word = readWord(fin);
        vectorSize = Integer.parseInt(word);
        LOG.info("Expecting " + words + " words with " + vectorSize + " values per vector.");
        float vector[] = new float[vectorSize];
        for (int w = 0; w < words; ++w) {
          word = readWord(fin);
          // LOG.info(word);
          vector = readVector(fin, vectorSize);
          word2Vector.put(word, vector);
        }

      } catch (final Exception e) {
        LOG.error(e.getLocalizedMessage(), e);
        return null;
      } finally {
        if (fin != null) {
          try {
            fin.close();
          } catch (final IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
          }
        }
      }
    }
    return new Word2VecModel(word2Vector, vectorSize);
  }

  public static float[] readVector(final FileInputStream fin, final int vectorSize)
      throws IOException {
    final byte bytes[] = new byte[vectorSize * 4];
    fin.read(bytes);
    final ByteBuffer buffer = ByteBuffer.wrap(bytes);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    final float vector[] = new float[vectorSize];
    for (int i = 0; i < vectorSize; i++) {
      vector[i] = buffer.getFloat();
    }
    return vector;
  }

  public static String readWord(final FileInputStream fin) throws IOException {
    char c;
    final StringBuffer buffer = new StringBuffer();
    c = (char) fin.read();
    while ((c != ' ') && (c != '\n')) {
      buffer.append(c);
      c = (char) fin.read();
    }
    return buffer.toString();
  }

}

package org.aksw.word2vecrestful.db;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.word2vecrestful.utils.MapUtil;
import org.aksw.word2vecrestful.utils.Serialize;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.Logger;

public class InMemoryDB implements IDatabase {

  public static Logger LOG = Logger.getLogger(InMemoryDB.class);

  final Word2VecModel model = Word2VecFactory.get();

  @Override
  public LinkedHashMap<String, Double> getNClosest(final float[] vec, final int n) {
    final LinkedHashMap<String, Double> nbest = new LinkedHashMap<>();
    try {
      final Map<String, Double> map = new HashMap<>();

      for (final Entry<String, float[]> entry : model.word2vec.entrySet()) {
        final String modelWord = entry.getKey();
        final float[] modelVec = entry.getValue();
        final float[] inputVec = vec;
        if ((modelVec != null) && (inputVec != null)) {
          map.put(modelWord, Word2VecMath.cosineSimilarityNormalizedVecs(
              Word2VecMath.normalize(inputVec), Word2VecMath.normalize(modelVec)));
        }
      }
      final LinkedHashMap<String, Double> lmap = (LinkedHashMap) MapUtil.reverseSortByValue(map);
      // put n best to result map nbest
      int i = 0;
      for (final Entry<String, Double> e : lmap.entrySet()) {
        nbest.put(e.getKey(), e.getValue());
        i++;
        if (i >= n) {
          break;
        }
      }
    } catch (final Exception e) {
      LOG.error(e.getLocalizedMessage(), e);
    }

    return nbest;
  }

  @Override
  public LinkedHashMap<String, Double> getNBest(final String word, final int n) {
    final LinkedHashMap<String, Double> nbest = new LinkedHashMap<>();
    try {
      final Map<String, Double> map = new HashMap<>();

      for (final Entry<String, float[]> entry : model.word2vec.entrySet()) {

        final String modelWord = entry.getKey();
        if (modelWord.equals(word)) {
          continue;
        }

        final float[] modelVec = entry.getValue();
        final float[] inputVec = getVec(word);
        if ((modelVec != null) && (inputVec != null)) {
          map.put(modelWord, Word2VecMath.cosineSimilarityNormalizedVecs(
              Word2VecMath.normalize(inputVec), Word2VecMath.normalize(modelVec)));
        }
      }

      final LinkedHashMap<String, Double> lmap =
          (LinkedHashMap<String, Double>) MapUtil.reverseSortByValue(map);
      // put n best to result map nbest
      int i = 0;
      for (final Entry<String, Double> e : lmap.entrySet()) {
        nbest.put(e.getKey(), e.getValue());
        i++;
        if (i >= n) {
          break;
        }
      }
    } catch (final Exception e) {
      LOG.error(e.getLocalizedMessage(), e);
    }

    return nbest;
  }

  @Override
  public byte[] getByteVec(final String word) {
    byte[] b = null;
    try {
      b = Serialize.toByte(getVec(word));
    } catch (final IOException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
    return b;
  }

  @Override
  public byte[] getByteNormVec(final String word) {
    byte[] b = null;
    try {
      b = Serialize.toByte(getNormVec(word));
    } catch (final IOException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
    return b;
  }

  @Override
  public float[] getVec(final String word) {
    return model.word2vec.get(word);
  }

  @Override
  public float[] getNormVec(final String word) {
    return Word2VecMath.normalize(getVec(word));
  }
}

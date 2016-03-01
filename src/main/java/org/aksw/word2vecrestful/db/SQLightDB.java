package org.aksw.word2vecrestful.db;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.word2vecrestful.utils.Serialize;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

public class SQLightDB extends AbstractSQLightDB implements IDatabase {

  public SQLightDB() {
    createTable();
  }

  public void saveModeltoDB(Word2VecModel model) throws IOException, SQLException {
    if (connect()) {
      for (final Entry<String, float[]> e : model.word2vec.entrySet()) {
        final float[] v = e.getValue();
        final String word = e.getKey();
        saveModeltoDatabase(word, Serialize.toByte(v), Serialize.toByte(Word2VecMath.normalize(v)));
      }
      disconnect();
    }
    model = null;
  }

  protected void saveModeltoDatabase(final String word, final byte[] v, final byte[] nv)
      throws SQLException {
    final String sql = "insert into " + textTable + " (word,vec,normVec) values (?,?,?)";
    if (connection != null) {
      final PreparedStatement prep = connection.prepareStatement(sql);
      prep.setString(1, word);
      prep.setBytes(2, v);
      prep.setBytes(3, nv);
      prep.execute();
      prep.close();
    }
  }

  @Override
  public LinkedHashMap<String, Double> getNClosest(final float[] vec, final int n) {
    final float[] in = Word2VecMath.normalize(vec);

    final LinkedHashMap<String, Double> map = new LinkedHashMap<>();
    if (connect()) {
      try {
        final String sql = "select word, normVec from " + textTable;
        final PreparedStatement prep = connection.prepareStatement(sql);
        final ResultSet resultSet = prep.executeQuery();
        int i = 0;
        while (resultSet.next()) {
          final byte[] b = resultSet.getBytes("normVec");
          final String word = resultSet.getString("word");
          final float[] comp = (b == null) ? null : (float[]) Serialize.fromByte(b);
          if (comp != null) {

            final double dis = Word2VecMath.cosineSimilarityNormalizedVecs(in, comp);

            if (i < n) {
              map.put(word, dis);
            } else {
              final List<Double> values = new ArrayList<>(map.values());
              Collections.sort(values);
              final double min = values.get(0);
              if (dis > min) {
                final Set<String> remove = new HashSet<>();
                for (final Entry<String, Double> entry : map.entrySet()) {
                  if (entry.getValue() == min) {
                    /* could be more than one! how to handle? */
                    remove.add(entry.getKey());
                    break;
                  }
                }
                for (final String w : remove) {
                  map.remove(w);
                }
                map.put(word, dis);
              }
            }
            i++;
          }
        }
        prep.close();
        disconnect();
      } catch (final Exception e) {
        LOG.error(e.getLocalizedMessage(), e);
      }
    }
    return map;
  }

  @Override
  public LinkedHashMap<String, Double> getNBest(String word, final int n) {

    final float[] in = getNormVec(word);

    final LinkedHashMap<String, Double> map = new LinkedHashMap<>();
    if (connect()) {
      try {
        final String sql = "select word, normVec from " + textTable;
        final PreparedStatement prep = connection.prepareStatement(sql);
        final ResultSet resultSet = prep.executeQuery();
        int i = 0;
        final String input = word;
        while (resultSet.next()) {
          final byte[] b = resultSet.getBytes("normVec");
          word = resultSet.getString("word");
          if (word.equals(input)) {
            continue;
          }
          final float[] comp = (b == null) ? null : (float[]) Serialize.fromByte(b);
          if (comp != null) {

            final double dis = Word2VecMath.cosineSimilarityNormalizedVecs(in, comp);

            if (i < n) {
              map.put(word, dis);
            } else {
              final List<Double> values = new ArrayList<>(map.values());
              Collections.sort(values);
              final double min = values.get(0);
              if (dis > min) {
                final Set<String> remove = new HashSet<>();
                for (final Entry<String, Double> entry : map.entrySet()) {
                  if (entry.getValue() == min) {
                    /* could be more than one! how to handle? */
                    remove.add(entry.getKey());
                    break;
                  }
                }
                for (final String w : remove) {
                  map.remove(w);
                }
                map.put(word, dis);
              }
            }
            i++;
          }
        }
        prep.close();
        disconnect();
      } catch (final Exception e) {
        LOG.error(e.getLocalizedMessage(), e);
      }
    }
    return map;
  }

  private byte[] _getByteVec(final String word, final String field) {
    byte[] b = null;
    try {
      if (connect()) {
        final String sql = "select " + field + " from " + textTable + " where word=?";
        final PreparedStatement prep = connection.prepareStatement(sql);
        prep.setString(1, word);
        final ResultSet resultSet = prep.executeQuery();

        if (resultSet.next()) {
          b = resultSet.getBytes(field);
        }
        prep.close();
        disconnect();
      }
    } catch (final SQLException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
    return b;

  }

  @Override
  public byte[] getByteVec(final String word) {
    return _getByteVec(word, "vec");
  }

  @Override
  public byte[] getByteNormVec(final String word) {
    return _getByteVec(word, "normVec");
  }

  @Override
  public float[] getVec(final String word) {
    float[] vec = null;
    try {
      vec = (float[]) Serialize.fromByte(getByteVec(word));
    } catch (ClassNotFoundException | IOException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
    return vec;
  }

  @Override
  public float[] getNormVec(final String word) {
    float[] vec = null;
    try {
      vec = (float[]) Serialize.fromByte(getByteNormVec(word));
    } catch (ClassNotFoundException | IOException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
    return vec;
  }
  /**
   * <code>
    &#64;Override
    public LinkedHashMap<String, Double> getNBest(final String word, final int n) {
      final LinkedHashMap<String, Double> nbest = new LinkedHashMap<>();
      Map<String, Double> map = new HashMap<>();
      final float[] inputVec = getNormVec(word);
      try {
        if (connect()) {
          final String sql = "select word, normVec from " + textTable;
          final PreparedStatement prep = connection.prepareStatement(sql);
          final ResultSet resultSet = prep.executeQuery();
          while (resultSet.next()) {

            final String modelWord = resultSet.getString("word");
            if (modelWord.equals(word)) {
              continue;
            }
            final byte[] modelNormVec = resultSet.getBytes("normVec");
            if (modelNormVec != null) {

              final float[] modelVec = (float[]) Serialize.fromByte(modelNormVec);
              if ((inputVec != null) && (modelVec != null)) {
                map.put(modelWord, Word2VecMath.cosineSimilarityNormalizedVecs(inputVec, modelVec));
              }
            }
          }
          prep.close();
          disconnect();
        }

        map = MapUtil.reverseSortByValue(map);
        // put n best to result map nbest
        int i = 0;
        for (final Entry<String, Double> e : map.entrySet()) {
          nbest.put(e.getKey(), e.getValue());
          i++;
          if (i > n) {
            break;
          }
        }
      } catch (final Exception e) {
        LOG.error(e.getLocalizedMessage(), e);
      }

      return nbest;
    }
    </code>
   */
}

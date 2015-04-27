package org.aksw.word2vecrestful.db;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.word2vecrestful.utils.Serialize;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

public class DatabaseExtended extends Database {

    /**
     * Test
     * 
     * @param a
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static void main(String[] a) throws IOException, SQLException, ClassNotFoundException {
        // DatabaseExtended w = new DatabaseExtended();
        // w.saveModeltoDB();

        // byte[] b = w.get("Acciona_Energia");
        // float[] f = (float[]) Serialize.fromByte(b);
    }

    /**
     * 
     */
    public DatabaseExtended() {
        createTable();

    }

    public void saveModeltoDB() throws IOException, SQLException {
        Word2VecModel model = Word2VecFactory.get();
        if (connect()) {
            for (Entry<String, float[]> e : model.word2vec.entrySet()) {
                float[] v = e.getValue();
                String word = e.getKey();
                saveModeltoDatabase(
                        word,
                        Serialize.toByte(v),
                        Serialize.toByte(Word2VecMath.normalize(v)));
            }
            disconnect();
        }
        model = null;
    }

    protected void saveModeltoDatabase(String word, byte[] v, byte[] nv) throws SQLException {
        String sql = "insert into " + textTable + " (word,vec,normVec) values (?,?,?)";
        if (connection != null) {
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, word);
            prep.setBytes(2, v);
            prep.setBytes(3, nv);
            prep.execute();
            prep.close();
        }
    }

    public byte[] getVec(String word) throws SQLException {
        byte[] b = null;
        if (connect()) {
            String sql = "select vec from " + textTable + " where word=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, word);
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) {
                b = resultSet.getBytes("vec");
            }

            prep.close();
            disconnect();
        }
        return b;
    }

    public byte[] getNormVec(String word) throws SQLException {
        byte[] b = null;
        if (connect()) {
            String sql = "select normVec from " + textTable + " where word=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, word);
            ResultSet resultSet = prep.executeQuery();
            if (resultSet.next()) {
                b = resultSet.getBytes("normVec");
            }
            prep.close();
            disconnect();
        }
        return b;
    }

    public Map<String, Double> getNBest(int n, String word) throws SQLException, ClassNotFoundException, IOException {

        byte[] b = getNormVec(word);
        float[] in = (b == null) ? null : (float[]) Serialize.fromByte(b);

        Map<String, Double> map = new HashMap<>();
        if (connect()) {
            String sql = "select word, normVec from " + textTable;
            PreparedStatement prep = connection.prepareStatement(sql);
            ResultSet resultSet = prep.executeQuery();
            int i = 0;
            while (resultSet.next()) {
                b = resultSet.getBytes("normVec");
                word = resultSet.getString("word");
                float[] comp = (b == null) ? null : (float[]) Serialize.fromByte(b);
                if (comp != null) {

                    double dis = Word2VecMath.cosineSimilarityNormalizedVecs(
                            Word2VecMath.normalize(in),
                            Word2VecMath.normalize(comp));

                    if (i < n) {
                        map.put(word, dis);
                    }
                    else {
                        List<Double> values = new ArrayList<>(map.values());
                        Collections.sort(values);
                        double min = values.get(0);
                        if (dis > min) {
                            Set<String> remove = new HashSet<>();
                            for (Entry<String, Double> entry : map.entrySet()) {
                                if (entry.getValue() == min) {
                                    remove.add(entry.getKey()); /* could be more than one! how to handle? */
                                    break; // remove break?
                                }
                            }
                            for (String w : remove)
                                map.remove(w);
                            map.put(word, dis);
                        }
                    }
                    i++;
                }
            }
            prep.close();
            disconnect();
        }
        return map;

    }
}

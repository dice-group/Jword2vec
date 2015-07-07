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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * 
 * @author MichaelRoeder
 * @author rspeck
 *
 */
public class Word2VecModelLoader {

    public static Logger       LOG         = LogManager.getLogger(Word2VecModelLoader.class);
    public static final String CFG_KEY_BIN = Word2VecModelLoader.class.getName().concat(".bin");
    protected boolean          binModel    = Boolean.parseBoolean(Cfg.get(CFG_KEY_BIN));

    /**
     * 
     * @param file
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public Word2VecModel loadModel(File file) {
        int vectorSize = -1;
        int words = -1;

        String word;

        Map<String, float[]> word2Vector = new HashMap<String, float[]>();

        if (!binModel) {

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String header = br.readLine();
                words = Integer.parseInt(header.split(" ")[0]);
                vectorSize = Integer.parseInt(header.split(" ")[1]);
                LOG.info("Expecting " + words + " words with " + vectorSize + " values per vector.");

                for (String line; (line = br.readLine()) != null;) {
                    float vector[] = new float[vectorSize];
                    String[] split = line.split(" ");
                    for (int i = 1; i < split.length; i++)
                        vector[i - 1] = Float.parseFloat(split[i]);
                    word2Vector.put(split[0], vector);

                }
            } catch (IOException e) {
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
                    LOG.info(word);
                    vector = readVector(fin, vectorSize);
                    word2Vector.put(word, vector);
                }

            } catch (Exception e) {
                LOG.error(e.getLocalizedMessage(), e);
                return null;
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                        LOG.error(e.getLocalizedMessage(), e);
                    }
                }
            }
        }
        return new Word2VecModel(word2Vector, vectorSize);
    }

    private static float[] readVector(FileInputStream fin, int vectorSize) throws IOException {
        byte bytes[] = new byte[vectorSize * 4];
        fin.read(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        float vector[] = new float[vectorSize];
        for (int i = 0; i < vectorSize; i++) {
            vector[i] = buffer.getFloat();
        }
        return vector;
    }

    private static String readWord(FileInputStream fin) throws IOException {
        char c;
        StringBuffer buffer = new StringBuffer();
        c = (char) fin.read();
        while ((c != ' ') && (c != '\n')) {
            buffer.append(c);
            c = (char) fin.read();
        }
        return buffer.toString();
    }

}

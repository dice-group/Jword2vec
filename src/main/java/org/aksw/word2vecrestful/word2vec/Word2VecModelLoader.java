package org.aksw.word2vecrestful.word2vec;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
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
     */
    public Word2VecModel loadModel(File file) {
        FileInputStream fin = null;
        String word;
        try {
            // reads file header
            fin = new FileInputStream(file);
            word = readWord(fin);
            int words = Integer.parseInt(word);
            word = readWord(fin);
            int vectorSize = Integer.parseInt(word);
            LOG.info("Expecting " + words + " words with " + vectorSize + " values per vector.");

            // reads file data
            float vector[];
            Map<String, float[]> word2Vector = new HashMap<String, float[]>();
            for (int w = 0; w < words; ++w) {
                word = readWord(fin);
                vector = readVector(fin, vectorSize, binModel);
                word2Vector.put(word, vector);
            }
            return new Word2VecModel(word2Vector, vectorSize);
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

    private static float[] readVector(FileInputStream fin, int vectorSize, boolean binModel) throws IOException {
        return binModel ? readVectorBin(fin, vectorSize) : readVector(fin, vectorSize);
    }

    private static float[] readVector(FileInputStream fin, int vectorSize) throws IOException {
        DataInputStream dis = new DataInputStream(fin);
        float vector[] = new float[vectorSize];
        for (int i = 0; i < vectorSize; i++) {
            vector[i] = dis.readFloat();
        }
        return vector;
    }

    private static float[] readVectorBin(FileInputStream fin, int vectorSize) throws IOException {
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

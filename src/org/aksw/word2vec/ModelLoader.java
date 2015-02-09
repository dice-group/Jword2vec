package org.aksw.word2vec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class ModelLoader {

    public static Model loadModel(File file) {
        FileInputStream fin = null;
        String word;
        try {
            fin = new FileInputStream(file);
            word = readWord(fin);
            int words = Integer.parseInt(word);
            word = readWord(fin);
            int vectorSize = Integer.parseInt(word);
            System.out.println("Expecting " + words + " words with " + vectorSize + " values per vector.");
            float vector[];
            Map<String, float[]> word2Vector = new HashMap<String, float[]>();
            for (int w = 0; w < words; ++w) {
                word = readWord(fin);
                vector = readVector(fin, vectorSize);
                word2Vector.put(word, vector);
            }
            return new Model(word2Vector, vectorSize);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                }
            }
        }
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

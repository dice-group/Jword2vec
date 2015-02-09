package org.aksw.word2vec;

import java.util.Map;

public class Model {

    public Map<String, float[]> word2vec;
    public int vectorSize;

    public Model(Map<String, float[]> word2vec, int vectorSize) {
        this.word2vec = word2vec;
        this.vectorSize = vectorSize;
    }
}

package org.aksw.word2vecrestful.word2vec;

import java.io.File;

public class Word2VecFactory {
    static File file = new File("data/GoogleNews-vectors-negative300.bin");

    public static Word2VecModel get() {
        return Word2VecModelLoader.loadModel(file);
    }
}

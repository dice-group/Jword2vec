package org.aksw.word2vec;

import java.io.File;

public class ModelLoadTest {
    public static void main(String[] args) {
        ModelLoader.loadModel(new File("GoogleNews-vectors-negative300.bin"));
    }
}

package org.aksw.word2vecrestful.word2vec;

import java.io.File;

import org.aksw.word2vecrestful.utils.Cfg;

public class Word2VecFactory {

  public static final String CFG_KEY_MODEL = Word2VecFactory.class.getName().concat(".model");
  public static String model = (Cfg.get(CFG_KEY_MODEL));

  public static Word2VecModel get() {
    return new Word2VecModelLoader().loadModel(new File(model));
  }
}

package org.aksw.word2vecrestful.word2vec;

import java.util.Map;

/**
 *
 * @author MichaelRoeder
 *
 */
public class Word2VecModel {

  public Map<String, float[]> word2vec;
  public int vectorSize;

  public Word2VecModel(final Map<String, float[]> word2vec, final int vectorSize) {
    this.word2vec = word2vec;
    this.vectorSize = vectorSize;
  }
}

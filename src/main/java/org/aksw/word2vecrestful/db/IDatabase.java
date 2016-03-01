package org.aksw.word2vecrestful.db;

import java.util.LinkedHashMap;

public interface IDatabase {

  public abstract byte[] getByteVec(final String word);

  public abstract byte[] getByteNormVec(final String word);

  public abstract float[] getVec(final String word);

  public abstract float[] getNormVec(final String word);

  public abstract LinkedHashMap<String, Double> getNBest(String word, final int n);

  public abstract LinkedHashMap<String, Double> getNClosest(float[] vec, final int n);
}

package org.aksw.word2vecrestful.word2vec;

import java.util.Map;

public interface GenWord2VecModel {
	public int getVectorSize();
	public Map<String, float[]> getClosestEntry(float[] vector);
	public Map<String, float[]> getClosestSubEntry(float[] vector, String subKey);
}

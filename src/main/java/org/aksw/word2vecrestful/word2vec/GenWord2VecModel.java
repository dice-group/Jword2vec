package org.aksw.word2vecrestful.word2vec;

import java.io.IOException;

public interface GenWord2VecModel {
	public int getVectorSize();
	public String getClosestEntry(float[] vector);
	public String getClosestSubEntry(float[] vector, String subKey);
	public void process() throws IOException;
}

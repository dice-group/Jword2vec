package org.aksw.word2vecrestful.utils;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class ClusterableVec implements Clusterable {
	private float[] vec;

	public ClusterableVec(float[] vec) {
		this.vec = vec;
	}

	@Override
	public double[] getPoint() {
		return Word2VecMath.convertFloatsToDoubles(vec);
	}
	
	public float[] getVec() {
		return this.vec;
	}

}

package org.aksw.word2vecrestful.word2vec;

public class VectorDimension implements Comparable<VectorDimension>{
	
	private int id;
	private float minVal;
	private float maxVal;
	private float range;
	public VectorDimension(int id, float minVal, float maxVal) {
		super();
		this.id = id;
		this.minVal = minVal;
		this.maxVal = maxVal;
		this.range = Math.abs(maxVal-minVal);
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public float getMinVal() {
		return minVal;
	}
	public void setMinVal(float minVal) {
		this.minVal = minVal;
	}
	public float getMaxVal() {
		return maxVal;
	}
	public void setMaxVal(float maxVal) {
		this.maxVal = maxVal;
	}
	public float getRange() {
		return range;
	}
	public void setRange(float range) {
		this.range = range;
	}
	@Override
	public int compareTo(VectorDimension other) {
		return (int) Math.floor(this.getRange() - other.getRange());
	}
	
}

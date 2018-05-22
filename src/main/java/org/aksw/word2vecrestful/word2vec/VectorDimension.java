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
		double rangeDiff = Math.floor(this.getRange() - other.getRange());
		int s;
		if(rangeDiff>0)
			s = 1;
		else if(rangeDiff<0)
			s = -1;
		else {
			int indxDiff = this.getId()-other.getId();
			s = indxDiff>0?1:-1;
		}
		return s;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + Float.floatToIntBits(range);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VectorDimension other = (VectorDimension) obj;
		if (id != other.id)
			return false;
		if (Float.floatToIntBits(range) != Float.floatToIntBits(other.range))
			return false;
		return true;
	}
	
}

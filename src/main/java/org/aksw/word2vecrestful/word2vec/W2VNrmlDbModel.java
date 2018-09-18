package org.aksw.word2vecrestful.word2vec;

import java.util.Map;

import org.aksw.word2vecrestful.db.SQLiteDBHandler;

public class W2VNrmlDbModel implements GenWord2VecModel{
	
	private String mainTblName= "wordtovec";
	private String wordColName = "word";
	private String vecColPrefix = "val";
	
	private int vectorSize;
	private SQLiteDBHandler dbHandler;
	
	private Map<Integer, Float> sdMap;
	public W2VNrmlDbModel(String dbName, int vectorSize) {
		this.vectorSize = vectorSize;
		this.dbHandler = new SQLiteDBHandler(dbName);
		// Calculate sd*3/10 and save in map
	}
	@Override
	public int getVectorSize() {
		return this.vectorSize;
	}

	@Override
	public String getClosestEntry(float[] vector) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClosestSubEntry(float[] vector, String subKey) {
		// TODO Auto-generated method stub
		return null;
	}

}

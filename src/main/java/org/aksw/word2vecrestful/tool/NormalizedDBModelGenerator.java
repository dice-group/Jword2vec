package org.aksw.word2vecrestful.tool;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.aksw.word2vecrestful.db.SQLiteDBHandler;

public class NormalizedDBModelGenerator extends SQLiteDBHandler {

	private String mainTblName;
	private int vectorSize;
	private String insertQuery;

	public NormalizedDBModelGenerator(String dbName, String mainTblName, int vectorSize) {
		super(dbName);
		this.mainTblName = mainTblName;
		this.vectorSize = vectorSize;
		this.insertQuery = this.createInsertQuery();
		createMainTable();
	}

	public void createMainTable() {
		StringBuilder sqlStr = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
		sqlStr.append(this.mainTblName);
		sqlStr.append(" ( word text ");
		for (int i = 0; i < vectorSize; i++) {
			sqlStr.append(", val").append(i + 1).append(" float NOT NULL ");
		}
		sqlStr.append(");");
		executeStatement(sqlStr.toString());
	}

	private String createInsertQuery() {
		StringBuilder insrtStr = new StringBuilder();
		insrtStr.append("insert into ").append(this.mainTblName).append(" values ( ?");
		for (int i = 0; i < vectorSize; i++) {
			insrtStr.append(", ?");
		}
		insrtStr.append(") ;");
		return insrtStr.toString();
	}
	
	public PreparedStatement generateMainTblInsrtStmnt() throws SQLException {
		PreparedStatement prep = connection.prepareStatement(this.insertQuery);
		connection.setAutoCommit(false);
		return prep;
	}
	
	 /**
	   * Creates an index.
	   */
	  public void makeIndex() {
	      final String sql = "CREATE INDEX Idx1 ON " + this.mainTblName + "(word)";
	      if (connection != null) {
	        try {
	          final PreparedStatement prep = connection.prepareStatement(sql);
	          prep.execute();
	          prep.close();
	          commit();
	        } catch (final SQLException e) {
	          LOG.error(e.getLocalizedMessage(), e);
	        }
	      }
	  }
	
	public void addMainTblInsrtBatch(String word, float[] vector, PreparedStatement ps) throws SQLException {
		ps.setString(1, word);
		for (int i = 0; i < this.vectorSize; i++) {
			ps.setFloat(i + 2, vector[i]);
		}
		ps.addBatch();
	}
	
	public int[] executeBatchCommit(PreparedStatement ps) throws SQLException {
		int[] res = ps.executeBatch();
		connection.commit();
		return res;
	}

	public boolean insertMainTblRecord(String word, float[] vector) throws SQLException {
		boolean recInserted = false;
		PreparedStatement prep = connection.prepareStatement(this.insertQuery);
		prep.setString(1, word);
		for (int i = 0; i < this.vectorSize; i++) {
			prep.setFloat(i + 2, vector[i]);
		}
		recInserted = prep.execute();
		prep.close();
		return recInserted;
	}
}

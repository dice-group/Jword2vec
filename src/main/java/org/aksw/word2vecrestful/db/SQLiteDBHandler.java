package org.aksw.word2vecrestful.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.sqlite.SQLiteConfig;

public class SQLiteDBHandler {

	public static Logger LOG = Logger.getLogger(AbstractSQLightDB.class);

	private static int queryTimeout = 30;

	private String db;
	protected Connection connection = null;
	protected Statement statement = null;

	public SQLiteDBHandler(String dbName) {
		try {
			this.db = dbName.concat(".db");
			Class.forName("org.sqlite.JDBC");
		} catch (final ClassNotFoundException e) {
			LOG.error(e.getLocalizedMessage(), e);
		}

	}
	
	public void commit() throws SQLException {
		if (connection != null) {
			connection.commit();
		}
	}

	/**
	 * Executes the given query on database and returns the numbers of rows updated
	 * 
	 * @param query
	 *            - statement to be executed
	 * @return - numbers of rows updated
	 */
	protected boolean executeStatement(String query) {
		boolean res = false;
		if (connect()) {
			try {
				statement = connection.createStatement();
				statement.setQueryTimeout(queryTimeout);
				res = statement.execute(query);
			} catch (final SQLException e) {
				LOG.error(e.getLocalizedMessage(), e);
			} finally {
				disconnect();
			}
		}
		return res;
	}

	/**
	 * Disconnect DB.
	 */
	public void disconnect() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (final SQLException e) {
			LOG.error("\n", e);
		}
	}

	/**
	 * Connect DB.
	 */
	public boolean connect() {
		final SQLiteConfig config = new SQLiteConfig();
		// config.setEncoding(SQLiteConfig.Encoding.UTF8);
		return connect(config);
	}

	/**
	 * Connect DB.
	 *
	 * @param config
	 * @return true if connected
	 */
	protected boolean connect(final SQLiteConfig config) {
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:".concat(db), config.toProperties());
		} catch (final SQLException e) {
			LOG.error("\n", e);
			statement = null;
		}
		return connection == null ? false : true;
	}

}

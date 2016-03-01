package org.aksw.word2vecrestful.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.sqlite.SQLiteConfig;

public abstract class AbstractSQLightDB {

  public static Logger LOG = Logger.getLogger(AbstractSQLightDB.class);

  public static String dbName = "data/word2vec";
  public static String textTable = "google";
  public static int queryTimeout = 30;

  protected static String db = dbName.concat(".db");
  protected Connection connection = null;
  protected Statement statement = null;

  public AbstractSQLightDB() {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (final ClassNotFoundException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }

  }

  public static boolean dbExists() {
    return new File(db).exists();
  }

  /**
   * Creates the tables: {@link #textTable} and {@link #feedbackTable} if not exists.
   *
   */
  public void createTable() {
    if (connect()) {
      try {
        final String sb = new StringBuffer().append("create table if not exists ").append(textTable)
            .append(" ( ").append("word text not null, ").append("vec blob not null,")
            .append("normVec blob not null").append(" )").toString();
        statement.executeUpdate(sb);
      } catch (final SQLException e) {
        LOG.error(e.getLocalizedMessage(), e);
      } finally {
        disconnect();
      }
    }
  }

  /**
   * Creates an index.
   */
  public void makeIndex() {
    if (connect()) {
      final String sql = "CREATE INDEX Idx1 ON " + textTable + "(word)";
      if (connection != null) {
        try {
          final PreparedStatement prep = connection.prepareStatement(sql);
          prep.execute();
          prep.close();
        } catch (final SQLException e) {
          LOG.error(e.getLocalizedMessage(), e);
        }
      }
      disconnect();
    }
  }

  /**
   * Disconnect DB.
   */
  protected void disconnect() {
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
  protected boolean connect() {
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
      statement = connection.createStatement();
      statement.setQueryTimeout(queryTimeout);
    } catch (final SQLException e) {
      LOG.error("\n", e);
      statement = null;
    }
    return statement == null ? false : true;
  }
}

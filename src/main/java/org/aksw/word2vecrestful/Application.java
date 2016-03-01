package org.aksw.word2vecrestful;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;

import org.aksw.word2vecrestful.data.Data;
import org.aksw.word2vecrestful.db.AbstractSQLightDB;
import org.aksw.word2vecrestful.db.SQLightDB;
import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * Runs a REST service. Check config file cfg.properties for more settings.
 *
 * @author rspeck
 *
 */
@SpringBootApplication
public class Application {
  static {
    PropertyConfigurator.configure(Cfg.LOG_FILE);
  }

  public static Logger LOG = Logger.getLogger(Application.class);
  public static ApplicationContext context;

  public final static boolean inmem =
      Boolean.valueOf(Cfg.get(Application.class.getName().concat(".inmemory")));

  /**
   *
   * @param args
   */
  public static void main(final String[] args) {

    writeShutDownFile("close");

    if (inmem) {
      // in memory use
      downloadandextract();

    } else {

      // db usage
      if (!AbstractSQLightDB.dbExists()) {
        downloadandextract();

        SQLightDB databaseExtended = new SQLightDB();
        try {
          LOG.info("Creating database ...");
          databaseExtended.saveModeltoDB(Word2VecFactory.get());
          LOG.info("Making index ...");
          databaseExtended.makeIndex();
          databaseExtended = null;
        } catch (IOException | SQLException e) {
          LOG.error(e.getLocalizedMessage(), e);
        }
      }
    }

    context = SpringApplication.run(Application.class, args);
  }

  /**
  *
  */
  public static void downloadandextract() {
    if (!new File(Word2VecFactory.model).exists()) {
      if (!new File(Data.filename_zip).exists()) {
        LOG.info("Downloading file (1.5G) ...");
        Data.downloadFile();
      }
      if (new File(Data.filename_zip).exists()) {
        LOG.info("Unzip file ...");
        Data.gunzipIt();
      }
    }
  }

  /**
   * Gives the applications process id.
   *
   * @return applications process id
   */
  public static synchronized String getProcessId() {

    final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
    final int index = jvmName.indexOf('@');
    if (index < 1) {
      return null;
    }
    try {
      return Long.toString(Long.parseLong(jvmName.substring(0, index)));
    } catch (final NumberFormatException e) {
      return null;
    }
  }

  /**
   * Writes a system depended file to shut down the application with kill cmd and process id.
   *
   * @return true if the file was written
   */
  public static synchronized boolean writeShutDownFile(final String fileName) {

    // get process Id
    final String id = getProcessId();
    if (id == null) {
      return false;
    }

    String cmd = "";
    String fileExtension = "";

    cmd = "kill " + id + System.getProperty("line.separator") + "rm " + fileName + ".sh";
    fileExtension = "sh";
    LOG.info(fileName + "." + fileExtension);

    final File file = new File(fileName + "." + fileExtension);
    try {
      final BufferedWriter out = new BufferedWriter(new FileWriter(file));
      out.write(cmd);
      out.close();
    } catch (final Exception e) {
      LOG.error(e.getMessage());
    }
    file.setExecutable(true, false);
    file.deleteOnExit();
    return true;
  }

  public static synchronized boolean shutDown() {
    try {
      Runtime.getRuntime().exec("kill ".concat(getProcessId()));
    } catch (final IOException e) {
      LOG.error(e.getLocalizedMessage(), e);
      return false;
    }
    return true;
  }
}

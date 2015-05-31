package org.aksw.word2vecrestful.web;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.aksw.word2vecrestful.data.Data;
import org.aksw.word2vecrestful.db.Database;
import org.aksw.word2vecrestful.db.DatabaseExtended;
import org.aksw.word2vecrestful.utils.Cfg;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
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

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {

        if (!Database.dbExists()) {

            if (!new File(Data.filename_zip).exists()) {
                if (!new File(Data.filename).exists()) {
                    LOG.info("Downloading file (1.5G) ...");
                    Data.downloadFile();
                }
                if (new File(Data.filename_zip).exists()) {
                    LOG.info("Unzip file ...");
                    Data.gunzipIt();
                }
            }

            DatabaseExtended databaseExtended = new DatabaseExtended();
            try {
                LOG.info("Creating database ...");
                databaseExtended.saveModeltoDB();
                LOG.info("Making index ...");
                databaseExtended.makeIndex();
                databaseExtended = null;
            } catch (IOException | SQLException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }
        } else {
            SpringApplication.run(Application.class, args);
        }
    }
}

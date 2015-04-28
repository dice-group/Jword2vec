package org.aksw.word2vecrestful.web;

import java.io.IOException;
import java.sql.SQLException;

import org.aksw.word2vecrestful.db.Database;
import org.aksw.word2vecrestful.db.DatabaseExtended;
import org.aksw.word2vecrestful.utils.Cfg;
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

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        PropertyConfigurator.configure(Cfg.LOG_FILE);
        // SpringApplication.run(Application.class, args);

        if (!Database.dbExists()) {
            // make database
            DatabaseExtended databaseExtended = new DatabaseExtended();
            try {
                databaseExtended.saveModeltoDB();
                databaseExtended = null;
            } catch (IOException | SQLException e) {
                DatabaseExtended.LOG.error(e.getLocalizedMessage(), e);
            }
        }
        if (Database.dbExists()) {
            SpringApplication.run(Application.class, args);
        }
    }
}

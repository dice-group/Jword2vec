package org.aksw.word2vecrestful.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;

import org.aksw.word2vecrestful.data.Data;
import org.aksw.word2vecrestful.db.Database;
import org.aksw.word2vecrestful.db.DatabaseExtended;
import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

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
    public static Logger             LOG = Logger.getLogger(Application.class);
    public static ApplicationContext context;

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {

        if (!Database.dbExists()) {
            if (!new File(Word2VecFactory.model).exists()) {
                if (!new File(Data.filename_zip).exists()) {
                    LOG.info("Downloading file (1.5G) ...");
                    Data.downloadFile();

                    if (new File(Data.filename_zip).exists()) {
                        LOG.info("Unzip file ...");
                        Data.gunzipIt();
                    }
                }
            }

            DatabaseExtended databaseExtended = new DatabaseExtended();
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
        writeShutDownFile("close");
        context = SpringApplication.run(Application.class, args);
    }

    /**
     * Gives the applications process id.
     * 
     * @return applications process id
     */
    public static synchronized String getProcessId() {

        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');
        if (index < 1)
            return null;
        try {
            return Long.toString(Long.parseLong(jvmName.substring(0, index)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Writes a system depended file to shut down the application with kill cmd
     * and process id.
     * 
     * @return true if the file was written
     */
    public static synchronized boolean writeShutDownFile(String fileName) {

        // get process Id
        String id = getProcessId();
        if (id == null)
            return false;

        String cmd = "";
        String fileExtension = "";

        cmd = "kill " + id + System.getProperty("line.separator") + "rm " + fileName + ".sh";
        fileExtension = "sh";
        LOG.info(fileName + "." + fileExtension);

        File file = new File(fileName + "." + fileExtension);
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(cmd);
            out.close();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        file.setExecutable(true, false);
        file.deleteOnExit();
        return true;
    }

    public static synchronized boolean shutDown() {
        try {
            Runtime.getRuntime().exec("kill ".concat(getProcessId()));
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
            return false;
        }
        return true;
    }

}

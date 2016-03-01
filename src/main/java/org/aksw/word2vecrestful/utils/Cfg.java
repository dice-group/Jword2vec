package org.aksw.word2vecrestful.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Cfg {

  public static final Logger LOG = LogManager.getLogger(Cfg.class);;

  public static final String LOG_FILE = "config/log4j.properties";
  public static final String CFG_FILE = "config/cfg.properties";
  protected static Properties properties = null;

  /**
   * Loads a given file to use as properties.
   *
   * @param cfgFile properties file
   */
  public static boolean loadFile(final String cfgFile) {
    boolean loaded = false;
    LOG.info("Loads cfg ...");

    properties = new Properties();
    FileInputStream in = null;
    try {
      in = new FileInputStream(cfgFile);
    } catch (final FileNotFoundException e) {
      LOG.error("file: " + cfgFile + " not found!");
    }
    if (in != null) {
      try {
        properties.load(in);
        loaded = true;
      } catch (final IOException e) {
        LOG.error("Can't read `" + cfgFile + "` file.");
      }
      try {
        in.close();
      } catch (final Exception e) {
        LOG.error("Something went wrong.\n", e);
      }
    } else {
      LOG.error("Can't read `" + cfgFile + "` file.");
    }

    return loaded;
  }

  /**
   * Gets a property.
   *
   * @param key property key
   * @return property value
   */
  public static String get(final String key) {
    if (properties == null) {
      loadFile(CFG_FILE);
    }
    return properties.getProperty(key);
  }
}

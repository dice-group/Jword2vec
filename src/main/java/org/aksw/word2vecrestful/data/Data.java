package org.aksw.word2vecrestful.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Data {

  public static Logger LOG = LogManager.getLogger(Data.class);
  public static String filename = "data/GoogleNews-vectors-negative300.bin";
  public static String filename_zip = filename + ".gz";

  public static String download_url =
      "https://docs.google.com/uc?export=download&id=0B7XkCwpI5KDYNlNUTTlSS21pQmM";

  public static void downloadFile() {
    try {
      // connect to get confirm
      final URL authUrl = new URL(download_url);
      final HttpsURLConnection authCon = (HttpsURLConnection) authUrl.openConnection();
      authCon.connect();

      String content;
      final Scanner scanner = new Scanner(authCon.getInputStream());
      scanner.useDelimiter("\\A");
      content = scanner.hasNext() ? scanner.next() : "";
      scanner.close();

      // find confirm
      String confirm = "";
      if (!content.isEmpty()) {
        final String start = content.substring(content.indexOf("confirm=") + "confirm=".length());
        confirm = start.substring(0, start.indexOf("&amp;"));
        // LOG.info(confirm);
      }

      // get cookies
      if (!confirm.isEmpty()) {

        final StringBuilder cookieHeaders = new StringBuilder();
        final List<String> cookies = authCon.getHeaderFields().get("Set-Cookie");
        if (cookies != null) {
          for (final String cookie : cookies) {
            if (cookieHeaders.length() > 0) {
              cookieHeaders.append("; ");
            }
            final String value = cookie.split(";")[0];
            cookieHeaders.append(value);
          }
        }

        // connect with confirm and cookies
        final URL regUrl = new URL(download_url + "&confirm=" + confirm);
        final HttpsURLConnection regCon = (HttpsURLConnection) regUrl.openConnection();
        regCon.setRequestProperty("Cookie", cookieHeaders.toString());
        regCon.connect();

        // download file
        final boolean done = downloadFile(regCon.getInputStream());
        if (!done) {
          LOG.warn("Could not download " + download_url);
        }
      }
    } catch (final Exception e) {
      LOG.error(e.getLocalizedMessage(), e);
    }

  }

  public static boolean downloadFile(final String url) {
    try {
      return downloadFile(new URL(url));
    } catch (final MalformedURLException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
    return false;
  }

  public static boolean downloadFile(final InputStream is) {
    try {
      final ReadableByteChannel rbc = Channels.newChannel(is);
      final FileOutputStream fos = new FileOutputStream(filename_zip);
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

      fos.close();
      rbc.close();
      return true;
    } catch (final Exception e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
    return false;
  }

  public static boolean downloadFile(final URL url) {
    try {
      return downloadFile(url.openStream());
    } catch (final Exception e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
    return false;
  }

  public static void gunzipIt() {
    gunzipIt(filename_zip, filename);
  }

  public static void gunzipIt(final String zip, final String unzip) {
    final byte[] buffer = new byte[1024];
    try {
      final GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(zip));
      final FileOutputStream out = new FileOutputStream(unzip);

      int len;
      while ((len = gzis.read(buffer)) > 0) {
        out.write(buffer, 0, len);
      }

      gzis.close();
      out.close();

    } catch (final IOException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }
}

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

    public static Logger LOG          = LogManager.getLogger(Data.class);
    public static String filename     = "data/GoogleNews-vectors-negative300.bin";
    public static String filename_zip = filename + ".gz";

    public static String download_url = "https://docs.google.com/uc?export=download&id=0B7XkCwpI5KDYNlNUTTlSS21pQmM";

    public static void downloadFile() {
        try {
            // connect to get confirm
            URL authUrl = new URL(download_url);
            HttpsURLConnection authCon = (HttpsURLConnection) authUrl.openConnection();
            authCon.connect();

            String content;
            Scanner scanner = new Scanner(authCon.getInputStream());
            scanner.useDelimiter("\\A");
            content = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            // find confirm
            String confirm = "";
            if (!content.isEmpty()) {
                String start = content.substring(content.indexOf("confirm=") + "confirm=".length());
                confirm = start.substring(0, start.indexOf("&amp;"));
                LOG.info(confirm);
            }

            // get cookies
            if (!confirm.isEmpty()) {

                StringBuilder cookieHeaders = new StringBuilder();
                List<String> cookies = authCon.getHeaderFields().get("Set-Cookie");
                if (cookies != null) {
                    for (String cookie : cookies) {
                        if (cookieHeaders.length() > 0) {
                            cookieHeaders.append("; ");
                        }
                        String value = cookie.split(";")[0];
                        cookieHeaders.append(value);
                    }
                }

                // connect with confirm and cookies
                URL regUrl = new URL(download_url + "&confirm=" + confirm);
                HttpsURLConnection regCon = (HttpsURLConnection) regUrl.openConnection();
                regCon.setRequestProperty("Cookie", cookieHeaders.toString());
                regCon.connect();

                // download file
                LOG.info("Downloading file (1.5G), please wait");
                boolean done = downloadFile(regCon.getInputStream());
                if (!done)
                    LOG.warn("Could not download " + download_url);
            }
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
        }

    }

    public static boolean downloadFile(String url) {
        try {
            return downloadFile(new URL(url));
        } catch (MalformedURLException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
        return false;
    }

    public static boolean downloadFile(InputStream is) {
        try {
            ReadableByteChannel rbc = Channels.newChannel(is);
            FileOutputStream fos = new FileOutputStream(filename_zip);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            fos.close();
            rbc.close();
            return true;
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
        return false;
    }

    public static boolean downloadFile(URL url) {
        try {
            return downloadFile(url.openStream());
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
        return false;
    }

    public static void gunzipIt() {
        byte[] buffer = new byte[1024];
        try {
            GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(filename_zip));
            FileOutputStream out = new FileOutputStream(filename);

            int len;
            while ((len = gzis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            gzis.close();
            out.close();

        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }
}

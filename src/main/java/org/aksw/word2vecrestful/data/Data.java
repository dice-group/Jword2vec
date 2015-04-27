package org.aksw.word2vecrestful.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

// TODO
public class Data {

    public static Logger LOG          = LogManager.getLogger(Data.class);
    public static String filename     = "data/GoogleNews-vectors-negative300.bin.gz";
    public static String filename_zip = filename + ".gz";

    /*
     * TODO
     * 
     * confirm needed!!!!
     */
    public static String url          = "https://docs.google.com/uc?export=download&confirm=Sr5U&id=0B7XkCwpI5KDYNlNUTTlSS21pQmM";

    public static void main(String[] a) {
        Data.downloadFile(url);
    }

    /*
     * https://developers.google.com/drive/web/manage-downloads
     *
     * Download a file's content.
     *
     * @param service
     *            Drive API service instance.
     * @param file
     *            Drive File instance.
     * @return InputStream containing the file's content if successful,
     *         {@code null} otherwise.

    private static InputStream downloadFile(Drive service, File file) {
        if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
            try {
                // uses alt=media query parameter to request content
                return service.files().get(file.getId()).executeMediaAsInputStream();
            } catch (IOException e) {
                // An error occurred.
                e.printStackTrace();
                return null;
            }
        } else {
            // The file doesn't have any content stored on Drive.
            return null;
        }
    }
    */
    public static boolean downloadFile(String url) {
        try {
            return downloadFile(new URL(url));
        } catch (MalformedURLException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
        return false;
    }

    public static boolean downloadFile(URL url) {
        try {
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
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

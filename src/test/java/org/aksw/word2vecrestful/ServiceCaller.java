package org.aksw.word2vecrestful;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.Scanner;
import java.util.concurrent.Callable;

public class ServiceCaller implements Callable<SimpleEntry<String, String>> {

    String url;
    String word;

    public ServiceCaller(String word, String url) {
        this.word = word;
        this.url = url;
    }

    @Override
    public SimpleEntry<String, String> call() throws Exception {
        URL myurl = new URL(url.concat(word));
        HttpURLConnection con = (HttpURLConnection) myurl.openConnection();
        con.connect();

        String content;
        Scanner scanner = new Scanner(con.getInputStream());
        scanner.useDelimiter("\\A");
        content = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return new SimpleEntry<String, String>(word, content);
    }
}

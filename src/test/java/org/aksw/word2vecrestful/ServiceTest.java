package org.aksw.word2vecrestful;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.web.Application;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Assert;
import org.junit.Test;

public class ServiceTest {
    static {
        PropertyConfigurator.configure(Cfg.LOG_FILE);
    }
    public static Logger LOG = Logger.getLogger(ServiceTest.class);

    // run server
    static {
        Application.main(new String[0]);
    }

    public static String url = "http://localhost:4441/word2vec/vector?&apikey=1234&a=";
    public static String cat = null;
    public static String dog = null;

    class Animal implements Callable<SimpleEntry<String, String>> {

        String name;

        public Animal(String name) {
            this.name = name;
        }

        @Override
        public SimpleEntry<String, String> call() throws Exception {
            URL myurl = new URL(url.concat(name));
            HttpURLConnection con = (HttpURLConnection) myurl.openConnection();
            con.connect();

            String content;
            Scanner scanner = new Scanner(con.getInputStream());
            scanner.useDelimiter("\\A");
            content = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
            return new SimpleEntry<String, String>(name, content);
        }
    }

    @Test
    public void requestDogTest() {

        boolean allTestDone = false;
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            CompletionService<SimpleEntry<String, String>> completionService =
                    new ExecutorCompletionService<>(executorService);

            final int n = 400;
            int nn = 0;
            for (int i = 0; i < n; i++) {
                completionService.submit(new Animal("dog"));
                nn++;
                completionService.submit(new Animal("cat"));
                nn++;
            }
            executorService.shutdown();

            for (int i = 0; i < nn; ++i) {

                Future<SimpleEntry<String, String>> future = completionService.take();
                SimpleEntry<String, String> result = future.get();

                if (result != null) {
                    if (result.getKey().equals("dog")) {
                        if (dog == null)
                            dog = result.getValue();
                        else
                            Assert.assertTrue(dog.equals(result.getValue()));
                    }
                    if (result.getKey().equals("cat")) {
                        if (cat == null)
                            cat = result.getValue();
                        else
                            Assert.assertTrue(cat.equals(result.getValue()));
                    }

                } else {
                    Assert.assertTrue(false);
                }

            }
            allTestDone = true;
        } catch (Exception e) {
            Assert.assertTrue(allTestDone);
            LOG.error(e.getLocalizedMessage());
        }
    }

}

package org.aksw.word2vecrestful;

import java.util.AbstractMap.SimpleEntry;
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

    static String        url = "http://localhost:4441/word2vec/vector?&apikey=1234&a=";

    // run server
    static {
        Application.main(new String[0]);
    }

    public static String cat = null;
    public static String dog = null;

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
                completionService.submit(new ServiceCaller("dog", url));
                nn++;
                completionService.submit(new ServiceCaller("cat", url));
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
            LOG.error(e.getLocalizedMessage());
            Assert.assertTrue(allTestDone);
        }
    }

}

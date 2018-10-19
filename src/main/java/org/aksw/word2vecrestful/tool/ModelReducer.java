package org.aksw.word2vecrestful.tool;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelReducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelReducer.class);

    public static final int REDUCED_WORDS = 100000;
    public static final String OUPUT_FILE = "reducedModel-" + REDUCED_WORDS + "-normalized.bin";

    public static void main(String[] args) throws IOException {
        LOGGER.info("Reading model...");
        Word2VecModel w2vModel = Word2VecFactory.getNormalBinModel();
        Map<String, float[]> model = w2vModel.word2vec;
        int vectorSize = w2vModel.vectorSize;
        // we don't need the model anymore
        w2vModel = null;

        LOGGER.info("Preparing random ids...");
        Random random = new Random();
        int wordCount = model.size();
        // Use a set to make sure that no ID is chosen twice
        Set<Integer> idSet = new HashSet<Integer>();
        while(idSet.size() < REDUCED_WORDS) {
            idSet.add(random.nextInt(wordCount));
        }
        int[] wordsKept = new int[REDUCED_WORDS];
        Iterator<Integer> idIter = idSet.iterator();
        for (int i = 0; i < wordsKept.length; i++) {
            wordsKept[i] = idIter.next();
        }
        idSet = null;
        idIter = null;
        // sort the array ascending
        Arrays.sort(wordsKept);
        // Iterate over all words and remove them if they are not listed in the
        // wordsKept array
        LOGGER.info("Starting to remove words...");
        int idPos = 0;
        int count = 0;
        Iterator<String> iterator = model.keySet().iterator();
        while ((idPos < REDUCED_WORDS) && (iterator.hasNext())) {
            iterator.next();
            if (count == wordsKept[idPos]) {
                ++idPos;
            } else {
                // remove the word
                iterator.remove();
            }
            ++count;
        }
        LOGGER.info("Kept " + idPos + " words. Writing output file...");
        writeModel(model, vectorSize, new File(OUPUT_FILE));
    }

    public static void writeModel(Map<String, float[]> model, int vectorSize, File outputFile) throws IOException {
        // ensure directory creation
        if(outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        // open an output stream
        BufferedOutputStream bOutStrm = null;
        try {
            bOutStrm = new BufferedOutputStream(new FileOutputStream(outputFile));
            String words = Integer.toString(model.size());
            bOutStrm.write(words.getBytes(StandardCharsets.UTF_8));
            bOutStrm.write(ModelNormalizer.WHITESPACE_BA);
            bOutStrm.write(Integer.toString(vectorSize).getBytes(StandardCharsets.UTF_8));
            bOutStrm.write(ModelNormalizer.END_LINE_BA);
            LOGGER.info("Expecting " + words + " words with " + vectorSize + " values per vector.");
            for (String word : model.keySet()) {
                bOutStrm.write(word.getBytes(StandardCharsets.UTF_8));
                bOutStrm.write(ModelNormalizer.WHITESPACE_BA);
                float[] vector = model.get(word);
                bOutStrm.write(ModelNormalizer.getNormalizedVecBA(vector));
            }
        } catch (final IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        } finally {
            if (bOutStrm != null) {
                bOutStrm.close();
            }
        }
    }
}

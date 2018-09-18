package org.aksw.word2vecrestful.word2vec;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.IntStream;

import org.dice_research.topicmodeling.commons.collections.TopIntIntCollection;

public class W2VNrmlMemModelIndxdLRMulti extends W2VNrmlMemModelIndxdLR {

    public W2VNrmlMemModelIndxdLRMulti(final Map<String, float[]> word2vec, final int vectorSize) {
        super(word2vec, vectorSize);
    }

    public W2VNrmlMemModelIndxdLRMulti(Map<String, float[]> word2vec, int vectorSize, int sigmaMult, int areaDivisor) {
        super(word2vec, vectorSize, sigmaMult, areaDivisor);
    }

    protected void putNearbyVecs(float[][] minMaxVec, Map<String, float[]> nearbyVecMap) {
        // init score array
        AtomicIntegerArray scoreArr = new AtomicIntegerArray(gWordArr.length);
        float[] minVec = minMaxVec[0];
        float[] maxVec = minMaxVec[1];
        // loop through each dimension and increment the score of words in that area
        IntStream.range(0, vectorSize).parallel().forEach(i -> {
            float minVal = minVec[i];
            float maxVal = maxVec[i];
            Object[] entryArr = indexesArr[i];
            int[] idArr = (int[]) entryArr[0];
            float[] dimsnValArr = (float[]) entryArr[1];
            int from = Arrays.binarySearch(dimsnValArr, minVal);
            // LOG.info("From value of dimension array: " + from);
            if (from < 0) {
                // To select the insertion point
                from = -1 - from;
            }
            // LOG.info("Final From value of current dimension array: " + from);
            int to = Arrays.binarySearch(dimsnValArr, maxVal);
            // LOG.info("To value of dimension array: " + to);
            if (to < 0) {
                // To select the insertion point
                to = -1 - to;
            } else {
                // Because binarySearch returns the exact index if element exists
                to++;
            }
            // LOG.info("Setting scores for the words between 'from' and 'to' indexes:\t" +
            // from + " " + to);
            // tl.logTime(9);
            for (int j = from; j < to; j++) {
                scoreArr.incrementAndGet(idArr[j]);
            }
            // tl.printTime(9, "Score set for index " + i);
        });
        // find the index of the words with highest score and add them to nearbyVecMap
        for (int wordId : getMaxIdList(scoreArr)) {
            nearbyVecMap.put(gWordArr[wordId], gVecArr[wordId]);
        }
    }

    private int[] getMaxIdList(AtomicIntegerArray scoreArr) {
        TopIntIntCollection collection = new TopIntIntCollection(k, false);
        for (int i = 0; i < scoreArr.length(); i++) {
            collection.add(scoreArr.get(i), i);
        }
        return collection.getObjects();
    }
}

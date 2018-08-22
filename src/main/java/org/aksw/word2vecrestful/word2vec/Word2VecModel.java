package org.aksw.word2vecrestful.word2vec;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.aksw.word2vecrestful.subset.DataSubsetProvider;
import org.aksw.word2vecrestful.utils.Word2VecMath;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * @author MichaelRoeder
 *
 */
public class Word2VecModel {

  public Map<String, float[]> word2vec;
  public int vectorSize;
  public TreeSet<VectorDimension> sortedVecDimns;
  public int[] dimRngIndxDesc;
  public Word2VecModel(final Map<String, float[]> word2vec, final int vectorSize) {
    this.word2vec = word2vec;
    this.vectorSize = vectorSize;
    this.sortedVecDimns = new TreeSet<>();
    //fetch vector dimension details
    fetchVectorDimensions();
  }
  /**
   * Method to fetch and store range of each dimension in the model
   */
  private void fetchVectorDimensions() {
	  float[] dimVals = new float[word2vec.size()];;
	  VectorDimension vectorDimension;
	  //for each dimension
	  for(int i=0;i<vectorSize;i++) {
		  //fetch all the values
		  int j=0;
		  for(float[] entryVec : word2vec.values()) {
			  dimVals[j++] =  entryVec[i];
		  }
		  //init and set dimension det
		  vectorDimension = new VectorDimension(i, Word2VecMath.getMin(dimVals), Word2VecMath.getMax(dimVals));
		  this.sortedVecDimns.add(vectorDimension);
	  }
	  this.dimRngIndxDesc = new int[vectorSize];
	  Iterator<VectorDimension> descIt = sortedVecDimns.descendingIterator();
	  int k = 0;
	  while(descIt.hasNext()) {
		  this.dimRngIndxDesc[k++] = descIt.next().getId();
	  }
  }
  /**
   * Method to fetch a single closest word entry to the passed input vector
   * @param inpvec - input vector to find closest word for
   * @return Map of the single closest word and its vector
   */
  public Map<String, float[]> getClosestEntry(float[] inpvec){
	  Map<String, float[]> resMap = new HashMap<>();
	  double minDist = -2;
	  String minWord = null;
	  float[] minVec = null;
	  double tempDist;
	  for(Entry<String, float[]> wordEntry : word2vec.entrySet()) {
		  String word = wordEntry.getKey();
		  float[] wordvec = wordEntry.getValue();
		  tempDist = getSqEucDist(inpvec, wordvec, minDist);
		  if(tempDist != -1) {
			  minWord = word;
			  minVec = wordvec;
			  minDist = tempDist;
		  }
	  }
	  resMap.put(minWord, minVec);
	  return resMap;
  }
  
  /**
   * Method to fetch a single closest word entry to the passed input vector 
   * inside the subset of word2vec model
   * @param inpvec - input vector to find closest word for
   * @param subsetKey - key to identify the subset model
   * @return Map of the single closest word and its vector
 * @throws IOException 
 * @throws FileNotFoundException 
 * @throws JsonProcessingException 
   */
  public Map<String, float[]> getClosestEntryInSub(float[] inpvec, String subsetKey) throws JsonProcessingException, FileNotFoundException, IOException{
	  Map<String, float[]> resMap = new HashMap<>();
	  double minDist = -2;
	  String minWord = null;
	  float[] minVec = null;
	  double tempDist;
	  // Loop on the subset
	  for(String word : DataSubsetProvider.fetchSubsetWords(subsetKey)) {
		  float[] wordvec = word2vec.get(word);
		  tempDist = getSqEucDist(inpvec, wordvec, minDist);
		  if(tempDist != -1) {
			  minWord = word;
			  minVec = wordvec;
			  minDist = tempDist;
		  }
	  }
	  resMap.put(minWord, minVec);
	  return resMap;
  }
  /**
   * Method to find the squared value of euclidean distance between two vectors if it is less
   * than the provided minimum distance value, otherwise return -1
   * @param arr1 - first vector
   * @param arr2 - second vector
   * @param minDist - minimum distance constraint
   * @return squared euclidean distance between two vector or -1
   */
  private double getSqEucDist(float[] arr1, float[] arr2, double minDist) {
	  double dist = 0;
	  for(int i: dimRngIndxDesc) {
		  dist+= Math.pow(arr1[i]-arr2[i], 2);
		  if(minDist!=-2 && dist>minDist)
			  return -1;
	  }
	  return dist;
  }
}

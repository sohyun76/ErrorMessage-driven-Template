/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.joshua.corpus;

/**
 * Corpus is an interface that contains methods for accessing the information within a monolingual
 * corpus.
 * 
 * @author Chris Callison-Burch
 * @since 7 February 2005
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */

public interface Corpus { // extends Externalizable {

  // ===============================================================
  // Attribute definitions
  // ===============================================================

  /**
   * @param position the position at which we want to obtain a word ID
   * @return the integer representation of the Word at the specified position in the corpus.
   */
  int getWordID(int position);


  /**
   * Gets the sentence index associated with the specified position in the corpus.
   * 
   * @param position Index into the corpus
   * @return the sentence index associated with the specified position in the corpus.
   */
  int getSentenceIndex(int position);


  /**
   * Gets the sentence index of each specified position.
   * 
   * @param positions Index into the corpus
   * @return array of the sentence indices associated with the specified positions in the corpus.
   */
  int[] getSentenceIndices(int[] positions);

  /**
   * Gets the position in the corpus of the first word of the specified sentence. If the sentenceID
   * is outside of the bounds of the sentences, then it returns the last position in the corpus + 1.
   * 
   * @param sentenceID a specific sentence to obtain a position for
   * @return the position in the corpus of the first word of the specified sentence. If the
   *         sentenceID is outside of the bounds of the sentences, then it returns the last position
   *         in the corpus + 1.
   */
  int getSentencePosition(int sentenceID);

  /**
   * Gets the exclusive end position of a sentence in the corpus.
   * 
   * @param sentenceID a specific sentence to obtain an end position for
   * @return the position in the corpus one past the last word of the specified sentence. If the
   *         sentenceID is outside of the bounds of the sentences, then it returns one past the last
   *         position in the corpus.
   */
  int getSentenceEndPosition(int sentenceID);

  /**
   * Gets the specified sentence as a phrase.
   * 
   * @param sentenceIndex Zero-based sentence index
   * @return the sentence, or null if the specified sentence number doesn't exist
   */
  Phrase getSentence(int sentenceIndex);


  /**
   * Gets the number of words in the corpus.
   * 
   * @return the number of words in the corpus.
   */
  int size();


  /**
   * Gets the number of sentences in the corpus.
   * 
   * @return the number of sentences in the corpus.
   */
  int getNumSentences();


  // ===========================================================
  // Methods
  // ===========================================================


  /**
   * Compares the phrase that starts at position start with the subphrase indicated by the start and
   * end points of the phrase.
   * 
   * @param corpusStart the point in the corpus where the comparison begins
   * @param phrase the superphrase that the comparsion phrase is drawn from
   * @param phraseStart the point in the phrase where the comparison begins (inclusive)
   * @param phraseEnd the point in the phrase where the comparison ends (exclusive)
   * @return an int that follows the conventions of {@link java.util.Comparator#compare(Object, Object)}
   */
  int comparePhrase(int corpusStart, Phrase phrase, int phraseStart, int phraseEnd);


  /**
   * Compares the phrase that starts at position start with the phrase passed in. Compares the
   * entire phrase.
   * 
   * @param corpusStart position start
   * @param phrase {@link org.apache.joshua.corpus.Phrase} to compare against
   * @return an int that follows the conventions of {@link java.util.Comparator#compare(Object, Object)}
   */
  int comparePhrase(int corpusStart, Phrase phrase);

  /**
   * Compares the suffixes starting a positions index1 and index2.
   * 
   * @param position1 the position in the corpus where the first suffix begins
   * @param position2 the position in the corpus where the second suffix begins
   * @param maxComparisonLength a cutoff point to stop the comparison
   * @return an int that follows the conventions of {@link java.util.Comparator#compare(Object, Object)}
   */
  int compareSuffixes(int position1, int position2, int maxComparisonLength);

  /**
   * 
   * @param startPosition start position for phrase
   * @param endPosition end position for phrase
   * @return the {@link org.apache.joshua.corpus.ContiguousPhrase}
   */
  ContiguousPhrase getPhrase(int startPosition, int endPosition);

  /**
   * Gets an object capable of iterating over all positions in the corpus, in order.
   * 
   * @return An object capable of iterating over all positions in the corpus, in order.
   */
  Iterable<Integer> corpusPositions();

  // void write(String corpusFilename, String vocabFilename, String charset) throws IOException;
}

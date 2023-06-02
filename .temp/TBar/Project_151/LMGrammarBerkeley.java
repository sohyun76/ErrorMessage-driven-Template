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
package org.apache.joshua.decoder.ff.lm.berkeley_lm;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.ff.lm.DefaultNGramLanguageModel;

import com.google.common.annotations.VisibleForTesting;

import edu.berkeley.nlp.lm.ArrayEncodedNgramLanguageModel;
import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.WordIndexer;
import edu.berkeley.nlp.lm.cache.ArrayEncodedCachingLmWrapper;
import edu.berkeley.nlp.lm.io.LmReaders;
import edu.berkeley.nlp.lm.util.StrUtils;
import org.slf4j.LoggerFactory;

/**
 * This class wraps Berkeley LM.
 *
 * @author adpauls@gmail.com
 */
public class LMGrammarBerkeley extends DefaultNGramLanguageModel {

  public static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LMGrammarBerkeley.class);

  private ArrayEncodedNgramLanguageModel<String> lm;

  private static final Logger logger = Logger.getLogger(LMGrammarBerkeley.class.getName());

  private int[] vocabIdToMyIdMapping;

  private final ThreadLocal<int[]> arrayScratch = new ThreadLocal<int[]>() {

    @Override
    protected int[] initialValue() {
      return new int[5];
    }
  };

  private int mappingLength = 0;

  private final int unkIndex;

  private static boolean logRequests = false;

  private static Handler logHandler = null;

  public LMGrammarBerkeley(int order, String lm_file) {
    super(order);
    vocabIdToMyIdMapping = new int[10];

    if (!new File(lm_file).exists()) {
      throw new RuntimeException("Can't read lm_file '" + lm_file + "'");
    }

    if (logRequests) {
      logger.addHandler(logHandler);
      logger.setLevel(Level.FINEST);
      logger.setUseParentHandlers(false);
    }

    try { // try binary format (even gzipped)
      lm = (ArrayEncodedNgramLanguageModel<String>) LmReaders.<String>readLmBinary(lm_file);
      LOG.info("Loading Berkeley LM from binary {}", lm_file);
    } catch (RuntimeException e) {
      ConfigOptions opts = new ConfigOptions();
      LOG.info("Loading Berkeley LM from ARPA file {}", lm_file);
      final StringWordIndexer wordIndexer = new StringWordIndexer();
      ArrayEncodedNgramLanguageModel<String> berkeleyLm =
          LmReaders.readArrayEncodedLmFromArpa(lm_file, false, wordIndexer, opts, order);

      lm = ArrayEncodedCachingLmWrapper.wrapWithCacheThreadSafe(berkeleyLm);
    }
    this.unkIndex = lm.getWordIndexer().getOrAddIndex(lm.getWordIndexer().getUnkSymbol());
  }

  @Override
  public boolean registerWord(String token, int id) {
    int myid = lm.getWordIndexer().getIndexPossiblyUnk(token);
    if (myid < 0) return false;
    if (id >= vocabIdToMyIdMapping.length) {
      vocabIdToMyIdMapping =
          Arrays.copyOf(vocabIdToMyIdMapping, Math.max(id + 1, vocabIdToMyIdMapping.length * 2));

    }
    mappingLength = Math.max(mappingLength, id + 1);
    vocabIdToMyIdMapping[id] = myid;

    return false;
  }
  
  @Override
  public  boolean isOov(int id) {
    // for Berkeley, we unfortunately have to temporarily convert to String
    return lm.getWordIndexer().getIndexPossiblyUnk(Vocabulary.word(id)) <= 0;
  }

  @Override
  public float sentenceLogProbability(int[] sentence, int order, int startIndex) {
    if (sentence == null) return 0;
    int sentenceLength = sentence.length;
    if (sentenceLength <= 0) return 0;

    float probability = 0;
    // partial ngrams at the begining
    for (int j = startIndex; j < order && j <= sentenceLength; j++) {
      // TODO: startIndex dependens on the order, e.g., this.ngramOrder-1 (in srilm, for 3-gram lm,
      // start_index=2. othercase, need to check)
      double logProb = ngramLogProbability_helper(sentence, 0, j, false);
      if (logger.isLoggable(Level.FINE)) {
        int[] ngram = Arrays.copyOfRange(sentence, 0, j);
        String words = Vocabulary.getWords(ngram);
        logger.fine("\tlogp ( " + words + " )  =  " + logProb);
      }
      probability += logProb;
    }

    // regular-order ngrams
    for (int i = 0; i <= sentenceLength - order; i++) {
      double logProb =  ngramLogProbability_helper(sentence, i, order, false);
      if (logger.isLoggable(Level.FINE)) {
        int[] ngram = Arrays.copyOfRange(sentence, i, i + order);
        String words = Vocabulary.getWords(ngram);
        logger.fine("\tlogp ( " + words + " )  =  " + logProb);
      }
      probability += logProb;
    }

    return probability;
  }

  @Override
  public float ngramLogProbability_helper(int[] ngram, int order) {
    return ngramLogProbability_helper(ngram, false);
  }

  protected float ngramLogProbability_helper(int[] ngram, boolean log) {
    return ngramLogProbability_helper(ngram, 0, ngram.length, log);
  }

  protected float ngramLogProbability_helper(int sentence[], int ngramStartPos, int ngramLength, boolean log) {
    int[] mappedNgram = arrayScratch.get();
    if (mappedNgram.length < ngramLength) {
      mappedNgram = new int[mappedNgram.length * 2];
      arrayScratch.set(mappedNgram);
    }
    for (int i = 0; i < ngramLength; ++i) {
      mappedNgram[i] = vocabIdToMyIdMapping[sentence[ngramStartPos + i]];
    }

    if (log && logRequests) {
      dumpBuffer(mappedNgram, ngramLength);
    }

    return lm.getLogProb(mappedNgram, 0, ngramLength);
  }

  public static void setLogRequests(Handler handler) {
    logRequests = true;
    logHandler = handler;
  }

  @Override
  public float ngramLogProbability(int[] ngram) {
    return ngramLogProbability_helper(ngram,true);
  }

  @Override
  public float ngramLogProbability(int[] ngram, int order) {
    return ngramLogProbability(ngram);
  }

  private void dumpBuffer(int[] buffer, int len) {
    final int[] copyOf = Arrays.copyOf(buffer, len);
    for (int i = 0; i < copyOf.length; ++i) {
      if (copyOf[i] < 0) {
        copyOf[i] = unkIndex;
      }
    }
    logger.finest(StrUtils.join(WordIndexer.StaticMethods.toList(lm.getWordIndexer(), copyOf)));
  }

  @VisibleForTesting
  ArrayEncodedNgramLanguageModel<String> getLM() {
    return lm;
  }
}

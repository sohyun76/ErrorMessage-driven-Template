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
package org.apache.joshua.system;

import static org.mockito.Mockito.doReturn;
import static org.testng.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.TranslationResponseStream;
import org.apache.joshua.decoder.io.TranslationRequestStream;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.mockito.Mockito;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration test for multithreaded Joshua decoder tests. Grammar used is a
 * toy packed grammar.
 *
 * @author Kellen Sunderland kellen.sunderland@gmail.com
 */
public class MultithreadedTranslationTests {

  private JoshuaConfiguration joshuaConfig = null;
  private Decoder decoder = null;
  private static final String INPUT = "A K B1 U Z1 Z2 B2 C";
  private static final String EXCEPTION_MESSAGE = "This exception should properly propagate";
  private int previousLogLevel;
  private final static long NANO_SECONDS_PER_SECOND = 1_000_000_000;

  @BeforeClass
  public void setUp() throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.search_algorithm = "cky";
    joshuaConfig.mark_oovs = false;
    joshuaConfig.pop_limit = 100;
    joshuaConfig.use_unique_nbest = false;
    joshuaConfig.include_align_index = false;
    joshuaConfig.topN = 0;
    joshuaConfig.tms.add("thrax -owner pt -maxspan 20 -path src/test/resources/wa_grammar.packed");
    joshuaConfig.tms.add("thrax -owner glue -maxspan -1 -path src/test/resources/grammar.glue");
    joshuaConfig.goal_symbol = "[GOAL]";
    joshuaConfig.default_non_terminal = "[X]";
    joshuaConfig.features.add("OOVPenalty");
    joshuaConfig.weights.add("tm_pt_0 1");
    joshuaConfig.weights.add("tm_pt_1 1");
    joshuaConfig.weights.add("tm_pt_2 1");
    joshuaConfig.weights.add("tm_pt_3 1");
    joshuaConfig.weights.add("tm_pt_4 1");
    joshuaConfig.weights.add("tm_pt_5 1");
    joshuaConfig.weights.add("tm_glue_0 1");
    joshuaConfig.weights.add("OOVPenalty 2");
    joshuaConfig.num_parallel_decoders = 500; // This will enable 500 parallel
                                              // decoders to run at once.
                                              // Useful to help flush out
                                              // concurrency errors in
                                              // underlying
                                              // data-structures.
    this.decoder = new Decoder(joshuaConfig, ""); // Second argument
                                                  // (configFile)
                                                  // is not even used by the
                                                  // constructor/initialize.

    previousLogLevel = Decoder.VERBOSE;
    Decoder.VERBOSE = 0;
  }

  @AfterClass
  public void tearDown() throws Exception {
    this.decoder.cleanUp();
    this.decoder = null;
    Decoder.VERBOSE = previousLogLevel;
  }



  // This test was created specifically to reproduce a multithreaded issue
  // related to mapped byte array access in the PackedGrammer getAlignmentArray
  // function.

  // We'll test the decoding engine using N = 10,000 identical inputs. This
  // should be sufficient to induce concurrent data access for many shared
  // data structures.

  @Test()
  public void givenPackedGrammar_whenNTranslationsCalledConcurrently_thenReturnNResults() throws IOException {
    // GIVEN

    int inputLines = 10000;
    joshuaConfig.use_structured_output = true; // Enabled alignments.
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < inputLines; i++) {
      sb.append(INPUT + "\n");
    }

    // Append a large string together to simulate N requests to the decoding
    // engine.
    TranslationRequestStream req = new TranslationRequestStream(
        new BufferedReader(new InputStreamReader(new ByteArrayInputStream(sb.toString()
        .getBytes(StandardCharsets.UTF_8)))), joshuaConfig);

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // WHEN
    // Translate all segments in parallel.
    TranslationResponseStream translationResponseStream = this.decoder.decodeAll(req);

    ArrayList<Translation> translationResults = new ArrayList<Translation>();


    final long translationStartTime = System.nanoTime();
    try {
      for (Translation t: translationResponseStream)
        translationResults.add(t);
    } finally {
      if (output != null) {
        try {
          output.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    final long translationEndTime = System.nanoTime();
    final double pipelineLoadDurationInSeconds = (translationEndTime - translationStartTime)
            / ((double)NANO_SECONDS_PER_SECOND);
    System.err.println(String.format("%.2f seconds", pipelineLoadDurationInSeconds));

    // THEN
    assertTrue(translationResults.size() == inputLines);
  }

  @Test(expectedExceptions = RuntimeException.class,
          expectedExceptionsMessageRegExp = EXCEPTION_MESSAGE)
  public void givenDecodeAllCalled_whenRuntimeExceptionThrown_thenPropagate() throws IOException {
    // GIVEN
    // A spy request stream that will cause an exception to be thrown on a threadpool thread
    TranslationRequestStream spyReq = Mockito.spy(new TranslationRequestStream(null, joshuaConfig));
    doReturn(createSentenceSpyWithRuntimeExceptions()).when(spyReq).next();

    // WHEN
    // Translate all segments in parallel.
    TranslationResponseStream translationResponseStream = this.decoder.decodeAll(spyReq);

    ArrayList<Translation> translationResults = new ArrayList<>();
    for (Translation t: translationResponseStream)
      translationResults.add(t);
  }

  private Sentence createSentenceSpyWithRuntimeExceptions() {
    Sentence sent = new Sentence(INPUT, 0, joshuaConfig);
    Sentence spy = Mockito.spy(sent);
    Mockito.when(spy.target()).thenThrow(new RuntimeException(EXCEPTION_MESSAGE));
    return spy;
  }
}

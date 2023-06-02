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
package org.apache.joshua.decoder.kbest_extraction;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.base.Charsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static org.testng.Assert.assertEquals;

/**
 * Reimplements the kbest extraction regression test
 * TODO (fhieber): this test strangely only works with StateMinimizing KenLM.
 * This is to be investigated
 */

public class KBestExtractionTest {

  private static final String CONFIG = "src/test/resources/kbest_extraction/joshua.config";
  private static final String INPUT = "a b c d e";
  private static final Path GOLD_PATH = Paths.get("src/test/resources/kbest_extraction/output.scores.gold");

  private JoshuaConfiguration joshuaConfig = null;
  private Decoder decoder = null;

  @BeforeMethod
  public void setUp() throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.readConfigFile(CONFIG);
    joshuaConfig.outputFormat = "%i ||| %s ||| %c";
    KenLmTestUtil.Guard(() -> decoder = new Decoder(joshuaConfig, ""));
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }

  @Test
  public void givenInput_whenKbestExtraction_thenOutputIsAsExpected() throws IOException {
    final String translation = decode(INPUT).toString();
    final String gold = new String(readAllBytes(GOLD_PATH), UTF_8);
    assertEquals(gold, translation);
  }

  private Translation decode(String input) {
    final Sentence sentence = new Sentence(input, 0, joshuaConfig);
    return decoder.decode(sentence);
  }

}

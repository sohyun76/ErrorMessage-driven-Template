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
package org.apache.joshua.decoder.ff.lm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.ff.lm.buildin_lm.TrieLM;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for testing ARPA language model class.
 *
 * @author Lane Schwartz
 */
public class ArpaFileTest {

  String arpaFileName;

  Vocabulary vocab;

  @Test
  public void setup() {

    vocab = new Vocabulary();
    vocab.id("a");
    vocab.id("because");
    vocab.id("boycott");
    vocab.id("of");
    vocab.id("parliament");
    vocab.id("potato");
    vocab.id("resumption");
    vocab.id("the");

    try {
      File file = File.createTempFile("testLM", "arpa");
      PrintStream out = new PrintStream(file, "UTF-8");

      out.println();
      out.println("\\data\\");
      out.println("ngram 1=8");
      out.println("ngram 2=4");
      out.println("ngram 3=1");
      out.println();

      out.println("\\1-grams:");
      out.println("-1.992672	a	-0.1195484");
      out.println("-2.713723	because	-0.4665429");
      out.println("-4.678545	boycott	-0.0902521");
      out.println("-1.609573	of	-0.1991907");
      out.println("-3.875917	parliament	-0.1274891");
      out.println("-9.753210	potato");
      out.println("-4.678545	resumption	-0.07945678");
      out.println("-1.712444	the	-0.1606644");

      out.println();
      out.println("\\2-grams:");
      out.println("-0.3552987	because of	-0.03083654");
      out.println("-1.403534	of a");
      out.println("-0.7507797	of the	-0.05237135");
      out.println("-0.7266324	resumption of");
      out.println("-3.936147	the resumption");

      out.println();
      out.println("\\3-grams:");
      out.println("-0.6309999	because of the");
      out.println();

      out.println("\\end\\");

      out.close();
      this.arpaFileName = file.getAbsolutePath();

    } catch (IOException e) {
      Assert.fail("Unable to create temporary file: " + e.toString());
    }

  }

  @Test(dependsOnMethods = { "setup" })
  public void testOrder() {
    ArpaFile arpaFile = new ArpaFile(arpaFileName, vocab);

    try {
      Assert.assertEquals(arpaFile.getOrder(), 3);
    } catch (FileNotFoundException e) {
      Assert.fail(e.toString());
    }
  }

  @Test(dependsOnMethods = { "setup" })
  public void testIteration() {

    ArpaFile arpaFile = new ArpaFile(arpaFileName, vocab);

    Map<Integer, Integer> counts = new HashMap<>();

    boolean iterationOccurred = false;

    for (ArpaNgram ngram : arpaFile) {

      iterationOccurred = true;

      int order = ngram.order();
      int count;
      if (counts.containsKey(order)) {
        count = counts.get(order) + 1;
      } else {
        count = 1;
      }

      counts.put(order, count);

    }

    Assert.assertTrue(iterationOccurred);

    Assert.assertTrue(counts.containsKey(1));
    Assert.assertTrue(counts.containsKey(2));
    Assert.assertTrue(counts.containsKey(3));

    Assert.assertEquals((int) counts.get(1), 8);
    Assert.assertEquals((int) counts.get(2), 5);
    Assert.assertEquals((int) counts.get(3), 1);

  }

  @Test(dependsOnMethods = { "setup" })
  public void testSize() {
    ArpaFile arpaFile = new ArpaFile(arpaFileName, vocab);

    Assert.assertEquals(arpaFile.size(), 14);
  }

  @Test(dependsOnMethods = { "setup", "testIteration" })
  public void testChildren() throws FileNotFoundException {
    ArpaFile arpaFile = new ArpaFile(arpaFileName, vocab);

    TrieLM lm = new TrieLM(arpaFile);
    Assert.assertNotSame(lm.getChildren().size(), 0);
  }
}

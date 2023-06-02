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
package org.apache.joshua.metrics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.joshua.util.StreamGobbler;

public class METEOR extends EvaluationMetric {
  protected String targetLanguage;
  protected boolean normalize;
  protected boolean keepPunctuation;

  public METEOR(String[] Metric_options) {
    // M_o[0]: -l language, one of {en,cz,fr,de,es}
    // M_o[1]: -normalize, one of {norm_yes,norm_no}
    // M_o[2]: -keepPunctuation, one of {keepPunc,removePunc}
    // M_o[3]: maxComputations, positive integer

    // default in meteor v0.8: en, norm_no, removePunc

    switch (Metric_options[0]) {
    case "en":
      targetLanguage = "en";
      break;
    case "cz":
      targetLanguage = "cz";
      break;
    case "fr":
      targetLanguage = "fr";
      break;
    case "de":
      targetLanguage = "de";
      break;
    case "es":
      targetLanguage = "es";
      break;
    default:
      String msg =
          "Unknown language string " + Metric_options[0] + ". Should be one of {en,cz,fr,de,es}.";
      throw new RuntimeException(msg);
    }

    switch (Metric_options[1]) {
    case "norm_yes":
      normalize = true;
      break;
    case "norm_no":
      normalize = false;
      break;
    default:
      String msg = "Unknown normalize string " + Metric_options[1]
          + ". Should be one of norm_yes or norm_no.";
      throw new RuntimeException(msg);
    }

    if (Metric_options[2].equals("keepPunc")) {
      keepPunctuation = true;
    } else if (Metric_options[1].equals("removePunk")) {
      keepPunctuation = false;
    } else {
      String msg = "Unknown keepPunctuation string " + Metric_options[1]
          + ". Should be one of keepPunc or removePunk.";
      throw new RuntimeException(msg);
    }

    int maxComputations = Integer.parseInt(Metric_options[3]);
    if (maxComputations < 1) {
      throw new RuntimeException("Maximum computations must be positive");
    }

    initialize(); // set the data members of the metric
  }

  protected void initialize() {
    metricName = "METEOR";
    toBeMinimized = false;
    suffStatsCount = 5;
  }

  public double bestPossibleScore() {
    return 1.0;
  }

  public double worstPossibleScore() {
    return 0.0;
  }

  public int[] suffStats(String cand_str, int i) {
    // this method should never be used when the metric is METEOR,
    // because METEOR.java overrides suffStats(String[],int[]) below,
    // which is the only method that calls suffStats(Sting,int).
    return null;
  }

  public int[][] suffStats(String[] cand_strings, int[] cand_indices) {
    // calculate sufficient statistics for each sentence in an arbitrary set of candidates

    int candCount = cand_strings.length;
    if (cand_indices.length != candCount) {
      System.out.println("Array lengths mismatch in suffStats(String[],int[]); returning null.");
      return null;
    }

    int[][] stats = new int[candCount][suffStatsCount];

    try {

      // 1) Create input files for meteor

      // 1a) Create hypothesis file
      FileOutputStream outStream = new FileOutputStream("hyp.txt.METEOR", false); // false: don't
      // append
      OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      BufferedWriter outFile = new BufferedWriter(outStreamWriter);

      for (String cand_string : cand_strings) {
        writeLine(cand_string, outFile);
      }

      outFile.close();

      // 1b) Create reference file
      outStream = new FileOutputStream("ref.txt.METEOR", false); // false: don't append
      outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      outFile = new BufferedWriter(outStreamWriter);

      for (int d = 0; d < candCount; ++d) {
        for (int r = 0; r < refsPerSen; ++r) {
          writeLine(refSentences[cand_indices[d]][r], outFile);
        }
      }

      outFile.close();

      // 2) Launch meteor as an external process

      String cmd_str = "./meteor hyp.txt.METEOR ref.txt.METEOR";
      cmd_str += " -l " + targetLanguage;
      cmd_str += " -r " + refsPerSen;
      if (normalize) {
        cmd_str += " -normalize";
      }
      if (keepPunctuation) {
        cmd_str += " -keepPunctuation";
      }
      cmd_str += " -ssOut";

      Runtime rt = Runtime.getRuntime();
      Process p = rt.exec(cmd_str);

      StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), 0);
      StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), 0);

      errorGobbler.start();
      outputGobbler.start();

      @SuppressWarnings("unused")
      int exitValue = p.waitFor();


      // 3) Read SS from output file produced by meteor

      BufferedReader inFile = new BufferedReader(new FileReader("TER_out.ter"));
      String line;

      line = inFile.readLine(); // skip hyp line
      line = inFile.readLine(); // skip ref line

      for (int d = 0; d < candCount; ++d) {
        line = inFile.readLine(); // read info
        String[] strA = line.split("\\s+");

        stats[d][0] = (int) Double.parseDouble(strA[0]);
        stats[d][1] = (int) Double.parseDouble(strA[1]);
        stats[d][2] = (int) Double.parseDouble(strA[2]);
        stats[d][3] = (int) Double.parseDouble(strA[3]);
        stats[d][4] = (int) Double.parseDouble(strA[4]);
      }

      inFile.close();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    return stats;
  }

  public double score(int[] stats) {
    if (stats.length != suffStatsCount) {
      throw new RuntimeException("Mismatch between stats.length and suffStatsCount (" + stats.length
          + " vs. " + suffStatsCount + ") in METEOR.score(int[])");
    }

    double sc = 0.0;

    // sc = ???

    return sc;
  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner) {
    if (oneLiner) {
      System.out.println("METEOR = METEOR(" + stats[0] + "," + stats[1] + "," + stats[2] + ","
          + stats[3] + "," + stats[4] + " = " + score(stats));
    } else {
      System.out.println("# matches = " + stats[0]);
      System.out.println("test length = " + stats[1]);
      System.out.println("ref length = " + stats[2]);
      System.out.println("# chunks = " + stats[3]);
      System.out.println("length cost = " + stats[4]);
      System.out.println("METEOR = " + score(stats));
    }
  }

  private void writeLine(String line, BufferedWriter writer) throws IOException {
    writer.write(line, 0, line.length());
    writer.newLine();
    writer.flush();
  }

}

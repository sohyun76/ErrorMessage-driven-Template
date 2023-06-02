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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.apache.joshua.util.StreamGobbler;

public class TER extends EvaluationMetric {
  private boolean caseSensitive;
  private boolean withPunctuation;
  private int beamWidth;
  private int maxShiftDist;
  private String tercomJarFileName;
  private int numScoringThreads;

  public TER(String[] Metric_options) {
    // M_o[0]: case sensitivity, case/nocase
    // M_o[1]: with-punctuation, punc/nopunc
    // M_o[2]: beam width, positive integer
    // M_o[3]: maximum shift distance, positive integer
    // M_o[4]: filename of tercom jar file
    // M_o[5]: number of threads to use for TER scoring (= number of tercom processes launched)

    // for 0-3, default values in tercom-0.7.25 are: nocase, punc, 20, 50

    switch (Metric_options[0]) {
    case "case":
      caseSensitive = true;
      break;
    case "nocase":
      caseSensitive = false;
      break;
    default:
      String msg = "Unknown case sensitivity string " + Metric_options[0]
          + ". Should be one of case or nocase.";
      throw new RuntimeException(msg);
    }

    switch (Metric_options[1]) {
    case "punc":
      withPunctuation = true;
      break;
    case "nopunc":
      withPunctuation = false;
      break;
    default:
      String msg = "Unknown with-punctuation string " + Metric_options[1]
          + ". Should be one of punc or nopunc.";
      throw new RuntimeException(msg);
    }

    beamWidth = Integer.parseInt(Metric_options[2]);
    if (beamWidth < 1) {
      throw new RuntimeException("Beam width must be positive");
    }

    maxShiftDist = Integer.parseInt(Metric_options[3]);
    if (maxShiftDist < 1) {
      throw new RuntimeException("Maximum shift distance must be positive");
    }

    tercomJarFileName = Metric_options[4];

    if (tercomJarFileName == null || tercomJarFileName.equals("")) {
      throw new RuntimeException("Problem processing tercom's jar filename");
    } else {
      File checker = new File(tercomJarFileName);
      if (!checker.exists()) {
        String msg = "Could not find tercom jar file " + tercomJarFileName
            + "(Please make sure you use the full path in the filename)";
        throw new RuntimeException(msg);
      }
    }

    numScoringThreads = Integer.parseInt(Metric_options[5]);
    if (numScoringThreads < 1) {
      throw new RuntimeException("Number of TER scoring threads must be positive");
    }


    TercomRunner.set_TercomParams(caseSensitive, withPunctuation, beamWidth, maxShiftDist,
        tercomJarFileName);


    initialize(); // set the data members of the metric
  }

  protected void initialize() {
    metricName = "TER";
    toBeMinimized = true;
    suffStatsCount = 2;
  }

  public double bestPossibleScore() {
    return 0.0;
  }

  public double worstPossibleScore() {
    return Double.POSITIVE_INFINITY;
  }

  public int[] suffStats(String cand_str, int i) {
    // this method should never be used when the metric is TER,
    // because TER.java overrides createSuffStatsFile below,
    // which is the only method that calls suffStats(String,int).
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

      // 1) Create input files for tercom

      // 1a) Create hypothesis file
      FileOutputStream outStream = new FileOutputStream("hyp.txt.TER", false); // false: don't
                                                                               // append
      OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      BufferedWriter outFile = new BufferedWriter(outStreamWriter);

      for (int d = 0; d < candCount; ++d) {
        writeLine(cand_strings[d] + " (ID" + d + ")", outFile);
      }

      outFile.close();

      // 1b) Create reference file
      outStream = new FileOutputStream("ref.txt.TER", false); // false: don't append
      outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      outFile = new BufferedWriter(outStreamWriter);

      for (int d = 0; d < candCount; ++d) {
        for (int r = 0; r < refsPerSen; ++r) {
          writeLine(refSentences[cand_indices[d]][r] + " (ID" + d + ")", outFile);
        }
      }

      outFile.close();

      // 2) Launch tercom as an external process

      runTercom("ref.txt.TER", "hyp.txt.TER", "TER_out", 500);

      // 3) Read SS from output file produced by tercom.7.25.jar

      BufferedReader inFile = new BufferedReader(new FileReader("TER_out.ter"));
      String line;

      line = inFile.readLine(); // skip hyp line
      line = inFile.readLine(); // skip ref line

      for (int d = 0; d < candCount; ++d) {
        line = inFile.readLine(); // read info
        String[] strA = line.split("\\s+");

        stats[d][0] = (int) Double.parseDouble(strA[1]);
        stats[d][1] = (int) Double.parseDouble(strA[2]);
      }

      inFile.close();
      
      // 4) Delete TER files

      File fd;
      fd = new File("hyp.txt.TER");
      if (fd.exists()) fd.delete();
      fd = new File("ref.txt.TER");
      if (fd.exists()) fd.delete();
      fd = new File("TER_out.ter");
      if (fd.exists()) fd.delete();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return stats;
  }

  public void createSuffStatsFile(String cand_strings_fileName, String cand_indices_fileName,
      String outputFileName, int maxBatchSize) {

    try {
      int batchCount = 0;

      FileInputStream inStream_cands = new FileInputStream(cand_strings_fileName);
      BufferedReader inFile_cands =
          new BufferedReader(new InputStreamReader(inStream_cands, "utf8"));

      FileInputStream inStream_indices = new FileInputStream(cand_indices_fileName);
      BufferedReader inFile_indices =
          new BufferedReader(new InputStreamReader(inStream_indices, "utf8"));

      while (true) {
        ++batchCount;
        int readCount =
            createTercomHypFile(inFile_cands, tmpDirPrefix + "hyp.txt.TER.batch" + batchCount,
                10000);
        createTercomRefFile(inFile_indices, tmpDirPrefix + "ref.txt.TER.batch" + batchCount, 10000);

        if (readCount == 0) {
          --batchCount;
          break;
        } else if (readCount < 10000) {
          break;
        }
      }

      // score the batchCount batches of candidates, in parallel, across numThreads threads
      ExecutorService pool = Executors.newFixedThreadPool(numScoringThreads);
      Semaphore blocker = new Semaphore(0);

      for (int b = 1; b <= batchCount; ++b) {
        pool.execute(new TercomRunner(blocker, tmpDirPrefix + "ref.txt.TER.batch" + b, tmpDirPrefix
            + "hyp.txt.TER.batch" + b, tmpDirPrefix + "TER_out.batch" + b, 500));
        // Each thread scores the candidates, creating a tercom output file,
        // and then deletes the .hyp. and .ref. files, which are not needed
        // for other batches.
      }

      pool.shutdown();

      try {
        blocker.acquire(batchCount);
      } catch (java.lang.InterruptedException e) {
        throw new RuntimeException(e);
      }

      PrintWriter outFile = new PrintWriter(outputFileName);
      for (int b = 1; b <= batchCount; ++b) {
        copySS(tmpDirPrefix + "TER_out.batch" + b + ".ter", outFile);
        File fd;
        fd = new File(tmpDirPrefix + "TER_out.batch" + b + ".ter");
        if (fd.exists()) fd.delete();
        // .hyp. and .ref. already deleted by individual threads
      }
      outFile.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  public int createTercomHypFile(BufferedReader inFile_cands, String hypFileName, int numCands) {
    // returns # lines read

    int readCount = 0;

    try {
      FileOutputStream outStream = new FileOutputStream(hypFileName, false); // false: don't append
      OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      BufferedWriter outFile = new BufferedWriter(outStreamWriter);

      String line_cand;

      if (numCands > 0) {
        for (int d = 0; d < numCands; ++d) {
          line_cand = inFile_cands.readLine();
          if (line_cand != null) {
            ++readCount;
            writeLine(line_cand + " (ID" + d + ")", outFile);
          } else {
            break;
          }
        }
      } else {
        line_cand = inFile_cands.readLine();
        int d = -1;
        while (line_cand != null) {
          ++readCount;
          ++d;
          writeLine(line_cand + " (ID" + d + ")", outFile);
          line_cand = inFile_cands.readLine();
        }
      }

      outFile.close();

    } catch (IOException e) {
      throw new RuntimeException("IOException in TER.createTercomHypFile(...): " + e.getMessage(), e);
    }

    return readCount;

  }

  public int createTercomRefFile(BufferedReader inFile_indices, String refFileName, int numIndices) {
    // returns # lines read

    int readCount = 0;

    try {
      FileOutputStream outStream = new FileOutputStream(refFileName, false); // false: don't append
      OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      BufferedWriter outFile = new BufferedWriter(outStreamWriter);

      String line_index;

      if (numIndices > 0) {
        for (int d = 0; d < numIndices; ++d) {
          line_index = inFile_indices.readLine();
          if (line_index != null) {
            ++readCount;
            int index = Integer.parseInt(line_index);
            for (int r = 0; r < refsPerSen; ++r) {
              writeLine(refSentences[index][r] + " (ID" + d + ")", outFile);
            }
          } else {
            break;
          }
        }
      } else {
        line_index = inFile_indices.readLine();
        int d = -1;
        while (line_index != null) {
          ++readCount;
          ++d;
          int index = Integer.parseInt(line_index);
          for (int r = 0; r < refsPerSen; ++r) {
            writeLine(refSentences[index][r] + " (ID" + d + ")", outFile);
          }
          line_index = inFile_indices.readLine();
        }
      }

      outFile.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return readCount;

  }

  public int runTercom(String refFileName, String hypFileName, String outFileNamePrefix, int memSize) {
    int exitValue;

    try {

      String cmd_str =
          "java -Xmx" + memSize + "m -Dfile.encoding=utf8 -jar " + tercomJarFileName + " -r "
              + refFileName + " -h " + hypFileName + " -o ter -n " + outFileNamePrefix;
      cmd_str += " -b " + beamWidth;
      cmd_str += " -d " + maxShiftDist;
      if (caseSensitive) {
        cmd_str += " -s";
      }
      if (!withPunctuation) {
        cmd_str += " -P";
      }
      /*
       * From tercom's README: -s case sensitivity, optional, default is insensitive -P no
       * punctuations, default is with punctuations.
       */

      Runtime rt = Runtime.getRuntime();
      Process p = rt.exec(cmd_str);

      StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), 0);
      StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), 0);

      errorGobbler.start();
      outputGobbler.start();

      exitValue = p.waitFor();

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    return exitValue;

  }

  public void copySS(String inputFileName, PrintWriter outFile) {
    try {
      BufferedReader inFile = new BufferedReader(new FileReader(inputFileName));
      String line;

      line = inFile.readLine(); // skip hyp line
      line = inFile.readLine(); // skip ref line

      line = inFile.readLine(); // read info for first line

      while (line != null) {
        String[] strA = line.split("\\s+");
        outFile
            .println((int) Double.parseDouble(strA[1]) + " " + (int) Double.parseDouble(strA[2]));
        line = inFile.readLine(); // read info for next line
      }
      
      inFile.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public double score(int[] stats) {
    if (stats.length != suffStatsCount) {
      throw new RuntimeException("Mismatch between stats.length and suffStatsCount (" + stats.length
          + " vs. " + suffStatsCount + ") in TER.score(int[])");
    }

    double sc;

    sc = stats[0] / (double) stats[1];

    return sc;
  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner) {
    if (oneLiner) {
      System.out.println("TER = " + stats[0] + " / " + stats[1] + " = " + f4.format(score(stats)));
    } else {
      System.out.println("# edits = " + stats[0]);
      System.out.println("Reference length = " + stats[1]);
      System.out.println("TER = " + stats[0] + " / " + stats[1] + " = " + f4.format(score(stats)));
    }
  }

  private void writeLine(String line, BufferedWriter writer) throws IOException {
    writer.write(line, 0, line.length());
    writer.newLine();
    writer.flush();
  }

}

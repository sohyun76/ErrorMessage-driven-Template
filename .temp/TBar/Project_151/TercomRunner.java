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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.apache.joshua.util.StreamGobbler;


public class TercomRunner implements Runnable {
  /* non-static data members */
  private final Semaphore blocker;

  private final String refFileName;
  private final String hypFileName;
  private final String outFileNamePrefix;
  private final int memSize;

  /* static data members */
  private static boolean caseSensitive;
  private static boolean withPunctuation;
  private static int beamWidth;
  private static int maxShiftDist;
  private static String tercomJarFileName;

  public static void set_TercomParams(boolean in_caseSensitive, boolean in_withPunctuation,
      int in_beamWidth, int in_maxShiftDist, String in_tercomJarFileName) {
    caseSensitive = in_caseSensitive;
    withPunctuation = in_withPunctuation;
    beamWidth = in_beamWidth;
    maxShiftDist = in_maxShiftDist;
    tercomJarFileName = in_tercomJarFileName;
  }

  public TercomRunner(Semaphore in_blocker, String in_refFileName, String in_hypFileName,
      String in_outFileNamePrefix, int in_memSize) {
    blocker = in_blocker;
    refFileName = in_refFileName;
    hypFileName = in_hypFileName;
    outFileNamePrefix = in_outFileNamePrefix;
    memSize = in_memSize;
  }

  private void real_run() {

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
       * punctuation, default is with punctuation.
       */

      Runtime rt = Runtime.getRuntime();
      Process p = rt.exec(cmd_str);

      StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), 0);
      StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), 0);

      errorGobbler.start();
      outputGobbler.start();

      p.waitFor();

      File fd;
      fd = new File(hypFileName);
      if (fd.exists()) fd.delete();
      fd = new File(refFileName);
      if (fd.exists()) fd.delete();

    } catch (IOException| InterruptedException e) {
      throw new RuntimeException(e);
    }

    blocker.release();

  }

  public void run() {
    try {
      real_run();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}

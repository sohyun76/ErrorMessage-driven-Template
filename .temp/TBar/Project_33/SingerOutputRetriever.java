/**
 * Copyright 2019 Pinterest, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.singer.e2e;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SingerOutputRetriever implements Runnable {

  private InputStream output = null;
  private Thread  thread = null;
  private boolean stopped = true;

  public SingerOutputRetriever(InputStream os) {
    this.output = os;
  }

  public void start() {
    thread = new Thread(this);
    thread.start();
  }

  public void stop() {
    this.stopped = true;
  }
  public void run() {
    try {
      stopped = false;
      BufferedReader reader = new BufferedReader(new InputStreamReader(output));
      String line = reader.readLine();
      try {
        while (!stopped && line != null) {
          System.out.println("[Singer]" + line);
          line = reader.readLine();
        }
      } catch (IOException e) {
        reader.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

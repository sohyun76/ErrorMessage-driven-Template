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
package org.apache.joshua.pro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

// sparse feature representation version
public class ClassifierPerceptron implements ClassifierInterface {

  private static final Logger LOG = LoggerFactory.getLogger(ClassifierPerceptron.class);

  @Override
  public double[] runClassifier(Vector<String> samples, double[] initialLambda, int featDim) {
    System.out.println("------- Average-perceptron training starts ------");

    int sampleSize = samples.size();
    double score = 0; // model score
    double label;
    double[] lambda = new double[featDim + 1]; // in ZMERT lambda[0] is not used
    double[] sum_lambda = new double[featDim + 1];
    String[] featVal;

    for (int i = 1; i <= featDim; i++) {
      sum_lambda[i] = 0;
      lambda[i] = initialLambda[i];
    }

    System.out.print("Perceptron iteration ");
    int numError = 0;
    // int numPosSamp = 0;
    String[] feat_info;

    for (int it = 0; it < maxIter; it++) {
      System.out.print(it + " ");
      numError = 0;
      // numPosSamp = 0;

      for (String sample : samples) {
        featVal = sample.split("\\s+");

        // only consider positive samples
        // if( featVal[featDim].equals("1") )
        // {
        // numPosSamp++;
        score = 0;
        for (int d = 0; d < featVal.length - 1; d++) {
          feat_info = featVal[d].split(":");
          score += Double.parseDouble(feat_info[1]) * lambda[Integer.parseInt(feat_info[0])];
        }

        label = Double.parseDouble(featVal[featVal.length - 1]);
        score *= label; // the last element is class label(+1/-1)

        if (score <= bias) // incorrect classification
        {
          numError++;
          for (int d = 0; d < featVal.length - 1; d++) {
            feat_info = featVal[d].split(":");
            int featID = Integer.parseInt(feat_info[0]);
            lambda[featID] += learningRate * label * Double.parseDouble(feat_info[1]);
            sum_lambda[featID] += lambda[featID];
          }
        }
        // }//if( featVal[featDim].equals("1") )
      }
      if (numError == 0) break;
    }

    System.out.println("\n------- Average-perceptron training ends ------");

    for (int i = 1; i <= featDim; i++)
      sum_lambda[i] /= maxIter;

    return sum_lambda;
  }

  @Override
  /*
   * for avg_perceptron: param[0] = maximum number of iterations param[1] = learning rate (step
   * size) param[2] = bias (usually set to 0)
   */
  public void setClassifierParam(String[] param) {
    if (param == null)
      LOG.warn("no parameters specified for perceptron classifier, using default settings.");
    else {
      maxIter = Integer.parseInt(param[0]);
      learningRate = Double.parseDouble(param[1]);
      bias = Double.parseDouble(param[2]);
    }
  }

  int maxIter = 20;
  double learningRate = 0.5;
  double bias = 0.0;
}

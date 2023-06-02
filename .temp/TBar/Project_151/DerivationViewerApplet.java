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
package org.apache.joshua.ui.tree_visualizer;

import java.awt.Color;

import javax.swing.JApplet;

import org.apache.joshua.ui.tree_visualizer.tree.Tree;

/**
 * An applet for viewing DerivationTrees. It consists of a DerivationViewer inside of the applet's
 * Panel.
 * 
 * @author Jonathan Weese
 * 
 */
@SuppressWarnings("serial")
public class DerivationViewerApplet extends JApplet {
  /**
   * Initializes the applet by getting the source sentence and the tree representation from the
   * applet tag in a web page.
   */
  public void init() {
    String source = getParameter("sourceSentence");
    String derivation = getParameter("derivationTree");
    Tree tree = new Tree(derivation);

    add(new DerivationViewer(new DerivationTree(tree, source),
        getSize(),
        Color.red,
        DerivationViewer.AnchorType.ANCHOR_ROOT));
  }
}

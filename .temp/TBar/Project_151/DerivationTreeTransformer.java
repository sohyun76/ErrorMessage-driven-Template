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

import java.awt.Dimension;
import java.awt.geom.Point2D;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;

public class DerivationTreeTransformer implements Transformer<Node, Point2D> {
  private final TreeLayout<Node, DerivationTreeEdge> treeLayout;
  private final DerivationTree graph;
  private final Node root;
  private final Node sourceRoot;

  private final boolean isAnchored;
  private Point2D anchorPoint;

  private final double Y_DIST;
  private final double X_DIST;


  public DerivationTreeTransformer(DerivationTree t, Dimension d, boolean isAnchored) {
    this.isAnchored = isAnchored;
    anchorPoint = new Point2D.Double(0, 0);
    graph = t;
    DelegateForest<Node, DerivationTreeEdge> del = new DelegateForest<>(t);
    del.setRoot(t.root);
    del.setRoot(t.sourceRoot);
    root = t.root;
    sourceRoot = t.sourceRoot;
    Y_DIST = d.getHeight() / (2 * (1 + distanceToLeaf(root)));
    int leafCount = 0;
    for (Node n : t.getVertices()) {
      if (t.outDegree(n) == 0) leafCount++;
    }
    X_DIST = d.getWidth() / leafCount;

    treeLayout = new TreeLayout<>(del, (int) Math.round(X_DIST));
  }

  public Point2D transform(Node n) {
    double x, y;
    Point2D t = treeLayout.transform(n);
    if (n.isSource) {
      x =
          /* treeLayout.transform(root).getX() + */(t.getX()
              - treeLayout.transform(sourceRoot).getX() + treeLayout.transform(root).getX());
      y = Y_DIST * (distanceToLeaf(n) + 1);
    } else {
      x = t.getX();
      y = Y_DIST * (-1) * distanceToLeaf(n);
    }
    if (isAnchored) {
      x += anchorPoint.getX();
      y += anchorPoint.getY();
    }
    return new Point2D.Double(x, y + Y_DIST * (1 + distanceToLeaf(root)));
  }

  private int distanceToLeaf(Node n) {
    if (graph.getSuccessors(n).isEmpty()) return 0;
    int result = 0;
    for (Object x : graph.getSuccessors(n)) {
      int tmp = distanceToLeaf((Node) x);
      if (tmp > result) result = tmp;
    }
    return 1 + result;
  }

  public Dimension getSize() {
    int height = (int) Math.round(2 * Y_DIST * (1 + distanceToLeaf(root)));
    int width = (int) Math.round(2 * treeLayout.transform(root).getX());
    return new Dimension(width, height);
  }

  public Point2D getAnchorPosition(DerivationViewer.AnchorType type) {
    switch (type) {
      case ANCHOR_ROOT:
        return transform(root);
      case ANCHOR_LEFTMOST_LEAF:
        Node n = root;
        while (graph.getSuccessorCount(n) != 0)
          n = (Node) graph.getSuccessors(n).toArray()[0];
        return transform(n);
      default:
        return new Point2D.Double(0, 0);
    }
  }

  public void setAnchorPoint(DerivationViewer.AnchorType type, Point2D viewerAnchor) {
    Point2D oldAnchor = getAnchorPosition(type);
    double x = viewerAnchor.getX() - oldAnchor.getX();
    double y = viewerAnchor.getY() - oldAnchor.getY();
    anchorPoint = new Point2D.Double(x, y);
  }
}

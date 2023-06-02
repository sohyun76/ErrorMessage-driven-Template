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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JLabel;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

@SuppressWarnings("serial")
public class DerivationViewer extends VisualizationViewer<Node, DerivationTreeEdge> {
  public static final int DEFAULT_HEIGHT = 500;
  public static final int DEFAULT_WIDTH = 500;
  public static final Color SRC = Color.WHITE;
  private final Color TGT;

  public static final Color HIGHLIGHT = Color.pink;

  public enum AnchorType {
    ANCHOR_ROOT, ANCHOR_LEFTMOST_LEAF
  }

  private final AnchorType anchorStyle;
  private final Point2D anchorPoint;

  public DerivationViewer(DerivationTree g, Dimension d, Color targetColor, AnchorType anchor) {
    super(new CircleLayout<>(g));
    anchorStyle = anchor;
    DerivationTreeTransformer dtt = new DerivationTreeTransformer(g, d, false);
    StaticLayout<Node, DerivationTreeEdge> derivationLayout = new StaticLayout<>(g, dtt);
    // derivationLayout.setSize(dtt.getSize());
    setGraphLayout(derivationLayout);
    scaleToLayout(new LayoutScalingControl());
    // g.addCorrespondences();
    setPreferredSize(new Dimension(DEFAULT_HEIGHT, DEFAULT_WIDTH));
    getRenderContext().setVertexLabelTransformer(new ToStringLabeller<>());

    DefaultModalGraphMouse<Node, DerivationTreeEdge> graphMouse = new DefaultModalGraphMouse<>();
    graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
    setGraphMouse(graphMouse);
    addKeyListener(graphMouse.getModeKeyListener());
    // this.setPickedVertexState(new DerivationTreePickedState(g));

    getRenderContext().setVertexFillPaintTransformer(vp);
    getRenderContext().setEdgeStrokeTransformer(es);
    getRenderContext().setVertexShapeTransformer(ns);
    getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);

    TGT = targetColor;
    anchorPoint = dtt.getAnchorPosition(anchorStyle);
  }

  public void setGraph(DerivationTree tree) {
    DerivationTreeTransformer dtt = new DerivationTreeTransformer(tree, getSize(), true);
    dtt.setAnchorPoint(anchorStyle, anchorPoint);
    setGraphLayout(new StaticLayout<>(tree, dtt));
  }

  private final Transformer<Node, Paint> vp = new Transformer<Node, Paint>() {
    public Paint transform(Node n) {
      if (n.isHighlighted) return HIGHLIGHT;
      if (n.isSource)
        return SRC;
      else
        return TGT;
    }
  };

  private static final Transformer<DerivationTreeEdge, Stroke> es = e -> {
    if (e.pointsToSource) {
      return new BasicStroke(1.0f,
                             BasicStroke.CAP_BUTT,
                             BasicStroke.JOIN_MITER,
                             10.0f,
                             new float[] {10.0f},
                             0.0f);
    } else {
      return new BasicStroke(1.0f);
    }
  };

  private static final Transformer<Node, Shape> ns = n -> {
    JLabel x1 = new JLabel();
    double len = x1.getFontMetrics(x1.getFont()).stringWidth(n.toString());
    double margin = 5.0;
    return new Rectangle2D.Double((len + margin) / (-2), 0, len + 2 * margin, 20);
  };
}

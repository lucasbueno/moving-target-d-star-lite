
package MTDStarLite;

import jason.environment.grid.*;
import java.util.*;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.JPanel;
import javax.swing.JLabel;


class MovingTargetView extends GridWorldView {

  public static final int START  = 0;
  public static final int GOAL = 1;
  public static final boolean DISPLAY_TEXT 		= false;
  public static final boolean DISPLAY_STATES 	= false;
  public static final boolean DISPLAY_PATH 		= true;

  MovingTargetDStarLite world;
  Font costsFont;
  JLabel searches, moves, expanded, deleted, runtime;

  public MovingTargetView(MovingTargetDStarLite world, MovingTargetModel model) {
    super(model, "Moving Target D* Lite", 600);
    this.world = world;
    defaultFont = new Font("Arial", Font.BOLD, 600/model.getHeight()/3); // change default font
    setVisible(true);
    getCanvas().setBackground(Color.white);
    repaint();
  }

  @Override
  public void initComponents(int width) {
    super.initComponents(width);

    JPanel s 	= new JPanel();		
    searches 	= new JLabel("Searches:");
    moves 		= new JLabel("Moves:");
    expanded 	= new JLabel("Expanded:");
    deleted 	= new JLabel("Deleted:");
    runtime 	= new JLabel("Runtime:");		

    s.add(searches);
    s.add(moves);
    s.add(expanded);
    s.add(deleted);
    s.add(runtime);

    getContentPane().add(BorderLayout.SOUTH, s);
  }

  public void setSearches(int num) {
    searches.setText("Searches: " + num);
  }
  public void setMoves(int num) {
    moves.setText("Moves: " + num);
  }
  public void setExpanded(int num) {
    expanded.setText("Expanded: " + num);
  }
  public void setDeleted(int num) {
    deleted.setText("Deleted: " + num);
  }
  public void setRuntime(long time) {
    runtime.setText("Runtime: " + time + " ms");
  }

  /** draw application objects */
  @Override
  public void draw(Graphics g, int x, int y, int object) {
    switch(object) {
      case MovingTargetModel.OPEN:
        drawOpen(g, x, y);
        break;
      case MovingTargetModel.CLOSED:
        drawClosed(g, x, y);
        break;
      case MovingTargetModel.PATH:
        drawPath(g, x, y);
        break;
      case MovingTargetModel.TEXT: 
        drawText(g, x, y);
        break;
      default: 
        break;
    }
  }

  @Override
  public void drawAgent(Graphics g, int x, int y, Color c, int id) {
    if (id == START){
      c = Color.green;
    } else{
      c = Color.red;
    }
    super.drawAgent(g, x, y, c, -1);

    g.setColor(Color.black);
    if (id == START){
      drawString(g, x, y, defaultFont, "A");
    }else if (id == GOAL){
      drawString(g, x, y, defaultFont, "T");
    }
  }

  public void drawClosed(Graphics g, int x, int y) {
    if (DISPLAY_STATES) {
      g.setColor(new Color(150, 150, 255));
      g.fillRect(x*cellSizeW, y*cellSizeH, cellSizeW, cellSizeH);
      g.setColor(Color.lightGray);
      g.drawRect(x*cellSizeW, y*cellSizeH, cellSizeW, cellSizeH);
    }
  }

  public void drawOpen(Graphics g, int x, int y) {
    if (DISPLAY_STATES) {
      g.setColor(new Color(255, 150, 150));
      g.fillRect(x*cellSizeW, y*cellSizeH, cellSizeW, cellSizeH);
      g.setColor(Color.lightGray);
      g.drawRect(x*cellSizeW, y*cellSizeH, cellSizeW, cellSizeH);
    }
  }

  public void drawPath(Graphics g, int x, int y) {
    if (DISPLAY_PATH) {
      int wsize = cellSizeW/5;
      int hsize = cellSizeH/5;
      int woffset = (cellSizeW - wsize)/2;
      int hoffset = (cellSizeH - hsize)/2;
      g.setColor(Color.cyan);
      g.fillRect(x*cellSizeW+woffset, y*cellSizeH+hoffset, wsize, hsize);
    }
  }

  public void drawText(Graphics g, int x, int y) {
    if (DISPLAY_TEXT) {
      int fSize = cellSizeH/4;
      if(costsFont == null)
        costsFont = new Font("Arial", Font.BOLD, fSize);
      g.setColor(Color.black);
      g.setFont(costsFont);
      /*if (world.getCost(x,y) >= 0)
        g.drawString(String.valueOf(world.getCost(x,y)), x*cellSizeW, y*cellSizeH+fSize);
        else
        g.drawString("inf", x*cellSizeW, y*cellSizeH+fSize);*/
    }
  }

}

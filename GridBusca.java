import jason.asSyntax.*;
import jason.environment.*;
import jason.environment.grid.*;
import java.util.logging.*;
import java.util.*;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;


public class GridBusca extends Environment {
  
  private Random r = new Random();

  public static final int N       = 11;   // largura do grid NxN (impar >= 3)
  public static final int minCost = 1;
  public static final int maxCost = 1;
  public static final int delay   = 500; // delay entre acoes em ms
  //public final int delayFactor = 10;

  public static final int ONLINE  = 0;
  public static final int OFFLINE = 1;

  public static final int TARGET =  8;
  public static final int CLOSED = 16;
  public static final int OPEN   = 32;
  public static final int TEXT   = 64;

  public int[][] maze;
  public int[][] costs;
  /*public int[][] maze = {	
    {1,1,1,1,1},
    {10,10,10,10,1},
    {1,1,1,1,1},
    {1,10,10,10,10},
    {1,1,1,1,1}
    };
    */
    public int targetPosX, targetPosY;
    

  public static final Literal percGrid = 
    Literal.parseLiteral("grid(" + N + "," + N + ")");

  private Logger logger = Logger.getLogger("Tarefa2.mas2j."+GridBusca.class.getName());

  private MarsModel model;
  private MarsView  view;


  /** Called before the MAS execution with the args informed in .mas2j */

  @Override
  public void init(String[] args) {
    int mSize = (N+1)/2;
    int tx, ty;
    //maze = new int[N][N];

    MazeGenerator m = new MazeGenerator(mSize,mSize);
    m.displayBinary();
    maze = m.getBinaryMaze();
    
    costs = new int[N][N];
    
    for(int y = 0; y < N; y++) {
      for(int x = 0; x < N; x++) {
        //int random = (int) (Math.random()*maxCost + minCost);
        //maze[y][x] = random;
        costs[x][y] = (maze[y][x] > 0) ? 0 : 1;
      }
    }
    
    do {
      tx = r.nextInt(N);
      ty = r.nextInt(N);
    } while(costs[ty][tx] == 0);
    targetPosX = tx;
    targetPosY = ty;

    int agId = Integer.valueOf(args[0]);
    model = new MarsModel(agId);
    view  = new MarsView(model);
    model.setView(view);
    updatePerceptions(agId);
  }

  @Override
  public boolean executeAction(String agName, Structure action) {
    int agId = agNameToId(agName);
    Location agLoc = model.getAgPos(agId);
    try {
      if (action.getFunctor().equals("right")) {
        Thread.sleep(delay);
        model.moveRight(agId);
      } else if (action.getFunctor().equals("left")) {
        Thread.sleep(delay);
        model.moveLeft(agId);
      } else if (action.getFunctor().equals("up")) {
        Thread.sleep(delay);
        model.moveUp(agId);
      } else if (action.getFunctor().equals("down")) {
        Thread.sleep(delay);
        model.moveDown(agId);
      } else {
        logger.info("executing: "+action+", but not implemented!");
        return false;
      }
    } catch (Exception e) {
      logger.info("exception: " + e.getMessage());
    }

    updatePerceptions(agId);

    return true;
  }

  /** Called before the end of MAS execution */

  @Override
  public void stop() {
    super.stop();
  }

  public void updatePerceptions(int ag) {
    clearPercepts();

    if(model.INITIALIZATION) {
      addPercept(percGrid);
      if(ag == OFFLINE) {
        Literal costPerc = ASSyntax.createLiteral("costs");
        for(int y = 0; y < N; y++)
          for(int x = 0; x < N; x++)
            costPerc.addTerm(ASSyntax.createNumber(costs[y][x]));
        addPercept(costPerc);
      }
      model.INITIALIZATION = false;
    }

    Location agLoc = model.getAgPos(ag);
    int x = agLoc.x;
    int y = agLoc.y;

    Literal posPerc = Literal.parseLiteral(
        "pos(" + (x+1) + "," + (y+1) + ")");
    addPercept(posPerc);
    
    Literal targetPosPerc = Literal.parseLiteral(
        "target(" + (targetPosX+1) + "," + (targetPosY+1) + ")");
    addPercept(targetPosPerc);

    if(ag == ONLINE) {
      int cUp = 0, cRight = 0, cDown = 0, cLeft = 0;
      if(x > 0) 	cLeft  = costs[y][x-1];
      if(x < N-1) cRight = costs[y][x+1];
      if(y > 0) 	cUp    = costs[y-1][x];
      if(y < N-1) cDown  = costs[y+1][x];

      Literal costPerc = Literal.parseLiteral(
          "costs("+cUp+","+cRight+","+cDown+","+cLeft+")");  
      addPercept(costPerc);
    }
  }

  public int agNameToId(String name) {
    if(name.contains("online"))
      return ONLINE;
    else if (name.equals("offline"))
      return OFFLINE;
    else
      return -1;
  }

  class MarsModel extends GridWorldModel {

    private boolean INITIALIZATION;

    private MarsModel(int ag) {
      super(N, N, 2);

      INITIALIZATION = true;

			setAgPos(ag, 0, 0);
      // posicao inicial aleatoria
			//for (int ag = 0; ag < numAgs; ag++) {
				try {
          int x, y;
          do {
            x = r.nextInt(N);
            y = r.nextInt(N);
          } while(costs[y][x] == 0);
          setAgPos(ag, x, y);
				} catch (Exception e) {
					e.printStackTrace();
				}
			//}

      for(int y = 0; y < N; y++) {
        for(int x = 0; x < N; x++) {
          if(costs[y][x] == 0)
            add(OBSTACLE, x, y);
          else
            add(GridBusca.TEXT, x, y);
        }
      }
      add(GridBusca.TARGET, targetPosX, targetPosY);

      if(ag == ONLINE)
        addNeighbors(getAgPos(ag));
    }

    @Override
    public void setAgPos(int ag, Location l) {
      add(CLOSED, l.x, l.y);
      super.setAgPos(ag, l);
    }

    @Override
    public void setAgPos(int ag, int x, int y) {
      setAgPos(ag, new Location(x, y));
    }

    void moveRight(int ag) throws Exception {

      Location aspLoc = getAgPos(ag);
      if(ag == ONLINE)
        removeNeighbors(aspLoc);
      if (aspLoc.x < N-1) {
        aspLoc.x++;
      }
      setAgPos(ag, aspLoc);
      if(ag == ONLINE)
        addNeighbors(aspLoc);
    }

    void moveLeft(int ag) throws Exception {

      Location aspLoc = getAgPos(ag);
      if(ag == ONLINE)
        removeNeighbors(aspLoc);
      if (aspLoc.x > 0) {
        aspLoc.x--;
      }
      setAgPos(ag, aspLoc);
      if(ag == ONLINE)
        addNeighbors(aspLoc);
    } 

    void moveUp(int ag) throws Exception {
      Location aspLoc = getAgPos(ag);
      if(ag == ONLINE)
        removeNeighbors(aspLoc);
      if (aspLoc.y > 0) {
        aspLoc.y--;
      }
      setAgPos(ag, aspLoc);
      if(ag == ONLINE)
        addNeighbors(aspLoc);
    }    

    void moveDown(int ag) throws Exception {
      Location aspLoc = getAgPos(ag);
      if(ag == ONLINE)
        removeNeighbors(aspLoc);
      if (aspLoc.y < N-1) {
        aspLoc.y++;
      }
      setAgPos(ag, aspLoc);
      if(ag == ONLINE)
        addNeighbors(aspLoc);
    }

    void addNeighbors(Location l) {
      if(l.x > 0)
        add(OPEN, l.x-1, l.y);
      if(l.x < N-1)
        add(OPEN, l.x+1, l.y);
      if(l.y > 0)
        add(OPEN, l.x, l.y-1);
      if(l.y < N-1)
        add(OPEN, l.x, l.y+1);
    }

    void removeNeighbors(Location l) {
      if(l.x > 0)
        remove(OPEN, l.x-1, l.y);
      if(l.x < N-1)
        remove(OPEN, l.x+1, l.y);
      if(l.y > 0)
        remove(OPEN, l.x, l.y-1);
      if(l.y < N-1)
        remove(OPEN, l.x, l.y+1);
    }
  }

  class MarsView extends GridWorldView {

    int agId;
    Font costsFont;

    public MarsView(MarsModel model) {
      super(model, "Mars World", 600);
      defaultFont = new Font("Arial", Font.BOLD, 600/GridBusca.N/3); // change default font
      setVisible(true);
      repaint();
    }

    /** draw application objects */
    @Override
    public void draw(Graphics g, int x, int y, int object) {
      switch(object) {
        case GridBusca.TARGET: drawTarget(g, x, y);
                               break;
        case GridBusca.CLOSED: drawVisited(g, x, y);
                               break;
        case GridBusca.TEXT: 	 drawText(g, x, y);
                               break;
        case GridBusca.OPEN:   drawNeighbor(g, x, y);
                               break;
      }
    }

    public void drawVisited(Graphics g, int x, int y) {
      g.setColor(Color.cyan);
      g.fillRect(x*cellSizeW, y*cellSizeH, cellSizeW, cellSizeH);
      g.setColor(Color.lightGray);
      g.drawRect(x*cellSizeW, y*cellSizeH, cellSizeW, cellSizeH);
    }

    @Override
    public void drawAgent(Graphics g, int x, int y, Color c, int id) {
      c = new Color(255, 255, 0);
      super.drawAgent(g, x, y, c, -1);
      g.setColor(Color.black);
      drawString(g, x, y, defaultFont, "A");
    }
        
    public void drawTarget(Graphics g, int x, int y) {
      g.setColor(Color.black);
      drawString(g, x, y, defaultFont, "T");
    }

    public void drawNeighbor(Graphics g, int x, int y) {			
      g.setColor(Color.green);
      g.fillOval(x*this.cellSizeW+2, y*this.cellSizeH+2,
          this.cellSizeW-4, this.cellSizeH-4);
    }

    public void drawText(Graphics g, int x, int y) {
      int fSize = cellSizeW/4;
      if(costsFont == null)
        costsFont = new Font("Arial", Font.BOLD, fSize);
      g.setColor(Color.black);
      g.setFont(costsFont);
      if (costs[y][x] >= 0)
        g.drawString(String.valueOf(costs[y][x]), x*cellSizeW, y*cellSizeH+fSize);
      else
        g.drawString("inf", x*cellSizeW, y*cellSizeH+fSize);
    }

  }

}



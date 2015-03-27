import jason.asSyntax.*;
import jason.environment.*;
import jason.environment.grid.*;
import java.util.logging.*;
import java.util.*;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;


public class GridMovingTarget extends TimeSteppedEnvironment {

  // Constantes
  public static final int N      = 15;   // largura do grid NxN 
  public static final int DELAY  = 500; // DELAY entre acoes em ms
  public static final int AGENT  = 0;
  public static final int TARGET = 1;
  public static final int CLOSED = 16;
  public static final int OPEN   = 32;
  public static final int TEXT   = 64;
  public static final boolean DISPLAY_TEXT = false;

  // Variáveis globais
  private Random        r = new Random();
  public  int[][]       costs;
  private String[]      agentsName;  
  private Logger        logger = Logger.getLogger("movingTargetSearch.mas2j."+GridMovingTarget.class.getName());
  private MarsModel     model;
  private MarsView      view;
  private ListTermImpl  path = null;

  /** Called before the MAS execution with the args informed in .mas2j */
  @Override
  public void init(String[] args) {
    super.init(new String[] { "1000" }); // set step timeout

    setOverActionsPolicy(OverActionsPolicy.ignoreSecond);
    setSleep(DELAY);

    float blockPerc = Float.parseFloat(args[0]); 
    int K = Integer.parseInt(args[1]);
    agentsName = Arrays.copyOfRange(args, 2, 4);

    costs = new int[N][N];
    model = new MarsModel(AGENT, TARGET, blockPerc, K);
    view  = new MarsView(model);
    model.setView(view);

    updateAgsPercept();    
    addPercept(Literal.parseLiteral("matriz(" + N + "," + N + ")"));
  }

  @Override
  protected void updateNumberOfAgents() {
    setNbAgs(model.getNbOfAgs());
  }

  @Override
  public boolean executeAction(String agName, Structure action) {

    int agId = agNameToId(agName);

    try {

      if (action.getFunctor().equals("right")) {
        model.moveRight(agId);
      } else if (action.getFunctor().equals("left")) {
        model.moveLeft(agId);
      } else if (action.getFunctor().equals("up")) {
        model.moveUp(agId);
      } else if (action.getFunctor().equals("down")) {
        model.moveDown(agId);
      } else if (action.getFunctor().equals("moveTo")) {
        int x = (int) ((NumberTerm) action.getTerm(0)).solve();
        int y = (int) ((NumberTerm) action.getTerm(1)).solve();
        logger.info("movendo para (" + x + "," + y + ")");
        model.moveTo(agId, x-1, y-1);
      } else if (action.getFunctor().equals("search")) {
        path = movingTargetDStar();
      } else if (action.getFunctor().equals("nextMov")) {
        model.nextMov(agId);
      }
    } catch (Exception e) {
      logger.info("exception: " + e.getMessage());
    }

    return true;
  } 

  private ListTermImpl movingTargetDStar() {

    Location agLoc = model.getAgPos(AGENT);
    Location taLoc = model.getAgPos(TARGET);
    ListTermImpl path = new ListTermImpl();
    
    for (int x = 0; x < N; x++) {
      for (int y = 0; y < N; y++) {
        costs[y][x] = Math.abs(x - taLoc.x) + Math.abs(y - taLoc.y);
      }
    }

    while (agLoc.x != taLoc.x || agLoc.y != taLoc.y) {

      if (agLoc.x < taLoc.x) {
        agLoc.x++;
        path.add(ASSyntax.createLiteral("right"));
      } else if (agLoc.x > taLoc.x) {
        agLoc.x--;
        path.add(ASSyntax.createLiteral("left"));
      } else if (agLoc.y < taLoc.y) {
        agLoc.y++;
        path.add(ASSyntax.createLiteral("down"));
      } else if (agLoc.y > taLoc.y) {
        agLoc.y--;
        path.add(ASSyntax.createLiteral("up"));
      }      

      //path.add(ASSyntax.createLiteral("moveTo", 
      //      ASSyntax.createNumber(agLoc.x+1),
      //      ASSyntax.createNumber(agLoc.y+1)
      //      ));
    }
    return path;
  }  

  /** Called before the end of MAS execution */
  @Override
  public void stop() {
    super.stop();
  }

  @Override
  public void updateAgsPercept() {
    for (int i = 0; i < model.getNbOfAgs(); i++) {
      updateAgPercept(i);
    }
    model.changeWorld();
  }

  public void updateAgPercept(int agId) {
    Location agLoc, tLoc;

    clearPercepts(agentsName[agId]);

    agLoc = model.getAgPos(agId);
    Literal posPerc = Literal.parseLiteral(
        "pos(" + (agLoc.x+1) + "," + (agLoc.y+1) + ")");
    addPercept(agentsName[agId], posPerc);

    if(agId == AGENT)
      tLoc = model.getAgPos(TARGET);
    else
      tLoc = model.getAgPos(AGENT);
    Literal targetPosPerc = Literal.parseLiteral(
        "target(" + (tLoc.x+1) + "," + (tLoc.y+1) + ")");
    addPercept(agentsName[agId], targetPosPerc);

    if (agId == AGENT && path != null) {
      addPercept(agentsName[agId], ASSyntax.createLiteral("novoPlano", path));
      path = null;
    }
  }

  public int agNameToId(String name) {
    if(name.contains("agent"))
      return AGENT;
    else if (name.equals("target"))
      return TARGET;
    else
      return -1;
  }  

  class MarsModel extends GridWorldModel {

    private int K;
    
    
    private MarsModel(int idAgent, int idTarget, float blockPerc, int K) {
      super(N, N, 2);

      this.K = K;

      generateNewWorld(blockPerc);

      // posicao inicial aleatoria
      setAgPos(idAgent, getFreePos());
      setAgPos(idTarget, getFreePos());
    }

    public void generateNewWorld(float blockPerc) {
    
      for (int x = 0; x < N; x++) {
        for (int y = 0; y < N; y++) {
          add(TEXT, x, y);
        }
      }

      int numBlocks = (int)(N*N * blockPerc);
      for (int i = 0; i < numBlocks; i++) { 
        Location l = getFreePos();
        add(OBSTACLE, l);
        remove(TEXT, l);
      }
    }

    public void changeWorld() {
      for (int i = 0; i < K; i++) { 
        // block states
        Location blockLoc = getFreePos();
        add(OBSTACLE, blockLoc); 
        remove(TEXT, blockLoc); 

        // unblock states
        Location unblockLoc = null;
        while (unblockLoc == null) {
          int x = r.nextInt(N);
          int y = r.nextInt(N);
          if (hasObject(OBSTACLE, x, y))
            unblockLoc = new Location(x, y);
        }
        remove(OBSTACLE, unblockLoc);
        add(TEXT, unblockLoc);
      }
    }

    void moveTo(int ag, int x, int y) throws Exception {
      Location agLoc = getAgPos(ag);
      if (agLoc.x < x) {
        moveRight(ag);
      } else if (agLoc.x > x) {
        moveLeft(ag);
      } else if (agLoc.y < y) {
        moveDown(ag);
      } else if (agLoc.y > y) {
        moveUp(ag);
      }
    }

    boolean moveRight(int ag) throws Exception {
      Location agLoc = getAgPos(ag);
      if (agLoc.x < N-1 && isFreeOfObstacle(agLoc.x+1, agLoc.y)) {
        agLoc.x++;
        setAgPos(ag, agLoc);
        return true;
      }
      return false;
    }

    boolean moveLeft(int ag) throws Exception {
      Location agLoc = getAgPos(ag);
      if (agLoc.x > 0 && isFreeOfObstacle(agLoc.x-1, agLoc.y)) {
        agLoc.x--;
        setAgPos(ag, agLoc);
        return true;
      }
      return false;
    } 

    boolean moveUp(int ag) throws Exception {
      Location agLoc = getAgPos(ag);
      if (agLoc.y > 0 && isFreeOfObstacle(agLoc.x, agLoc.y-1)) {
        agLoc.y--;
        setAgPos(ag, agLoc);
        return true;
      }
      return false;
    }    

    boolean moveDown(int ag) throws Exception {
      Location agLoc = getAgPos(ag);
      if (agLoc.y < N-1 && isFreeOfObstacle(agLoc.x, agLoc.y+1)) {
        agLoc.y++;
        setAgPos(ag, agLoc);
        return true;
      }
      return false;
    }

    boolean nextMov(int ag) throws Exception {
      boolean ok = false;
      while(!ok) { 
        int nextPos = r.nextInt(4);    
        switch (nextPos) {     
          case 0:
            ok = moveUp(ag);
            break;
          case 1:
            ok = moveDown(ag);
            break;
          case 2:
            ok = moveLeft(ag);
            break;
          case 3:
            ok = moveRight(ag);
            break;         
        }
      }
      return true;
    }

  }

  class MarsView extends GridWorldView {

    Font costsFont;

    public MarsView(MarsModel model) {
      super(model, "Moving Target D* Lite", 600);
      defaultFont = new Font("Arial", Font.BOLD, 18); // change default font
      setVisible(true);
      repaint();
    }

    /** draw application objects */
    @Override
    public void draw(Graphics g, int x, int y, int object) {
      switch(object) {
        case GridMovingTarget.CLOSED: 
          drawClosed(g, x, y);
          break;
        case GridMovingTarget.TEXT: 
          drawText(g, x, y);
          break;
        default: 
          break;
      }
    }

    public void drawClosed(Graphics g, int x, int y) {
      g.setColor(Color.cyan);
      g.fillRect(x*cellSizeW, y*cellSizeH, cellSizeW, cellSizeH);
      g.setColor(Color.lightGray);
      g.drawRect(x*cellSizeW, y*cellSizeH, cellSizeW, cellSizeH);
    }

    @Override
    public void drawAgent(Graphics g, int x, int y, Color c, int id) {
      if (id == GridMovingTarget.AGENT){
        c = Color.green;
      }else{
        c = Color.red;
      }
      super.drawAgent(g, x, y, c, -1);

      g.setColor(Color.black);
      if (id == GridMovingTarget.AGENT){
        drawString(g, x, y, defaultFont, "A");
      }else if (id == GridMovingTarget.TARGET){
        drawString(g, x, y, defaultFont, "T");
      }
    }
    
    public void drawText(Graphics g, int x, int y) {
      if (DISPLAY_TEXT) {
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

}


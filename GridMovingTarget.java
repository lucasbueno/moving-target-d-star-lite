import jason.asSyntax.*;
import jason.environment.*;
import jason.environment.grid.*;
import java.util.logging.*;
import java.util.*;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;


public class GridMovingTarget extends Environment {
  
  // Constantes
  public static final int N       = 15;   // largura do grid NxN 
  public static final int DELAY   = 500; // DELAY entre acoes em ms
  public static final int AGENT  = 0;
  public static final int TARGET = 1;
  public static final int CLOSED = 16;
  public static final int OPEN   = 32;
  public static final int TEXT   = 64;

  // Variáveis globais
  private Random r = new Random();
  public int[][] costs;
  private String[] agentsName;  
  private Logger logger = Logger.getLogger("movingTargetSearch.mas2j."+GridMovingTarget.class.getName());
  private MarsModel model;
  private MarsView  view;

  /** Called before the MAS execution with the args informed in .mas2j */
  @Override
  public void init(String[] args) {
	
	agentsName = (String[]) args.clone();
	
    generateNewWorld();

    model = new MarsModel(AGENT, TARGET);
    view  = new MarsView(model);
    model.setView(view);
    
    updatePerceptions();    
	addPercept(Literal.parseLiteral("matriz(" + N + "," + N + ")"));
  }

  @Override
  public boolean executeAction(String agName, Structure action) {
    
	boolean ok = true;
    int agId = agNameToId(agName);
	ListTermImpl path = new ListTermImpl();
    
    try {
      Thread.sleep(DELAY);
      
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
	  }
    } catch (Exception e) {
      logger.info("exception: " + e.getMessage());
    }
    updateAgPercept(agId);    
	
	if (path.size() > 0) {
		Location taLoc = model.getAgPos(TARGET);
		addPercept(agName, ASSyntax.createLiteral("novoPlano", path));
	}
    return true;
  } 
  
  private ListTermImpl movingTargetDStar() {
		
    Location agLoc = model.getAgPos(AGENT);
    Location taLoc = model.getAgPos(TARGET);
    ListTermImpl path = new ListTermImpl();
    
	while (agLoc.x != taLoc.x || agLoc.y != taLoc.y) {
		if (agLoc.x < taLoc.x) {
			agLoc.x++;
		} else if (agLoc.x > taLoc.x) {
				agLoc.x--;
		} else if (agLoc.y < taLoc.y) {
				agLoc.y++;
		} else if (agLoc.y > taLoc.y) {
				agLoc.y--;
		}      
		
		path.add(ASSyntax.createLiteral("moveTo", 
			ASSyntax.createNumber(agLoc.x+1),
			ASSyntax.createNumber(agLoc.y+1)
		));
	}
    return path;
  }  

  /** Called before the end of MAS execution */
  @Override
  public void stop() {
    super.stop();
  }
  
  public void updatePerceptions(){
	
	updateAgPercept(-1);	
  }

  public void updateAgPercept(int agId) {
	 
	clearPercepts();
	
	Location agLoc = model.getAgPos(AGENT);
	Literal posPerc = Literal.parseLiteral(
	"pos(" + (agLoc.x+1) + "," + (agLoc.y+1) + ")");
	addPercept(agentsName[AGENT], posPerc);

	Location tLoc = model.getAgPos(TARGET);
	Literal targetPosPerc = Literal.parseLiteral(
	"target(" + (tLoc.x+1) + "," + (tLoc.y+1) + ")");
	addPercept(targetPosPerc);
	
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

    private MarsModel(int idAgent, int idTarget) {
      super(N, N, 2);

      Location aLoc, tLoc;

      // posicao inicial aleatoria
      try {
        aLoc = generateRandomLoc();
        tLoc = generateRandomLoc();
        setAgPos(idAgent, aLoc.x, aLoc.y);
        setAgPos(idTarget, tLoc.x, tLoc.y);
      } catch (Exception e) {
        e.printStackTrace();
      }

      for(int y = 0; y < N; y++) {
        for(int x = 0; x < N; x++) {
          if(costs[y][x] == 0)
            add(OBSTACLE, x, y);
          else
            add(OPEN, x, y);
        }
      }
    }
     
    Location generateRandomLoc() {
      int x,y;
      do {
        x = r.nextInt(N);
        y = r.nextInt(N);
      } while(costs[y][x] == 0);
      return new Location(x,y);
    }

    @Override
    public void setAgPos(int ag, Location l) {
      super.setAgPos(ag, l);
    }

    @Override
    public void setAgPos(int ag, int x, int y) {
      setAgPos(ag, new Location(x, y));
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
      if (agLoc.x < N-1 && costs[agLoc.y][agLoc.x+1] > 0) {
        agLoc.x++;
        setAgPos(ag, agLoc);
        return true;
      }
      return false;
    }

    boolean moveLeft(int ag) throws Exception {
      Location agLoc = getAgPos(ag);
      if (agLoc.x > 0 && costs[agLoc.y][agLoc.x-1] > 0) {
        agLoc.x--;
        setAgPos(ag, agLoc);
        return true;
      }
      return false;
    } 

    boolean moveUp(int ag) throws Exception {
      Location agLoc = getAgPos(ag);
      if (agLoc.y > 0 && costs[agLoc.y-1][agLoc.x] > 0) {
        agLoc.y--;
        setAgPos(ag, agLoc);
        return true;
      }
      return false;
    }    

    boolean moveDown(int ag) throws Exception {
      Location agLoc = getAgPos(ag);
      if (agLoc.y < N-1 && costs[agLoc.y+1][agLoc.x] > 0) {
        agLoc.y++;
        setAgPos(ag, agLoc);
        return true;
      }
      return false;
    }
  }

  class MarsView extends GridWorldView {

    int agId;
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
	
  }
  
  public void generateNewWorld(){
	  
	  costs = new int[N][N];    
    for(int y = 0; y < N; y++) {
      for(int x = 0; x < N; x++) {
		  costs[x][y] = 1;		  
      }
    }
	
	int numBlocks = (int)(N*N * 0.25);
	for (int i = 0; i < numBlocks; i++) { 
	  int x = r.nextInt(N); 
	  int y = r.nextInt(N);
	  
	  
	  while(costs[x][y] == 0){
		x = r.nextInt(N); 
		y = r.nextInt(N);
	  }
	  
	  costs[x][y] = 0;
	}
  }
  
}



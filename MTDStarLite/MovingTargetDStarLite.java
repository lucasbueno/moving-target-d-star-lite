
package MTDStarLite;

import jason.asSyntax.*;
import jason.environment.*;
import jason.environment.grid.*;
import java.util.logging.*;
import java.util.*;


public class MovingTargetDStarLite extends TimeSteppedEnvironment {

  // Constantes
  public static final int DELAY  = 500; // DELAY entre acoes em ms
  public static final int START  = 0;
  public static final int GOAL = 1;
  public static final int OO = Integer.MAX_VALUE;

  // Variáveis globais
	private int								N;   // largura do grid NxN 
	private int 							K;	
	private int								searches, moves, expandedStates, deletedStates;
	private long 							runtimeSum;
  private int[][]       		costs;	
  private String[]      		agentsName; 
  private Logger        		logger = Logger.getLogger("movingTargetSearch.mas2j."+MovingTargetDStarLite.class.getName());
  private MovingTargetModel	model;
  private MovingTargetView 	view;
	
	// Variáveis do algortimo de busca
  private LinkedList<State> 		path = new LinkedList<State>();
  private PriorityQueue<State> 	OPEN = new PriorityQueue<State>();
  private LinkedList<State> 		CLOSED = new LinkedList<State>();
  private LinkedList<State> 		DELETED = new LinkedList<State>();
  private LinkedList<State> 		OTHERS = new LinkedList<State>();
  private State 								start = null;
  private State 								goal = null;
  private State 								lastStart = null;
  private State 								lastGoal = null;
	private int 									km;
  private int[][] 							visited;

  /** Called before the MAS execution with the args informed in .mas2j */
  @Override
  public void init(String[] args) {
    super.init(new String[] { String.valueOf(DELAY*5) }); // set step timeout
	
    setOverActionsPolicy(OverActionsPolicy.ignoreSecond);
    setSleep(DELAY);

		N = Integer.parseInt(args[0]);
    float blockPerc = Float.parseFloat(args[1]); 
    K = Integer.parseInt(args[2]);
    agentsName = Arrays.copyOfRange(args, 3, 5);

		searches = 0;
		moves = 0;
		expandedStates = 0;
		deletedStates = 0;
		runtimeSum = 0;
    costs = new int[N][N];
    model = new MovingTargetModel(START, GOAL, N, blockPerc, K);
    view  = new MovingTargetView(this, model);
    model.setView(view);

		initializeVariables();
    updateAgsPercept();    
    addPercept(Literal.parseLiteral("matriz(" + N + "," + N + ")"));	
  }
	
	/** Called before the end of MAS execution */
  @Override
  public void stop() {
    super.stop();
  }
  
  @Override
  protected void updateNumberOfAgents() {
    setNbAgs(model.getNbOfAgs());
  }

  @Override
  public boolean executeAction(String agName, Structure action) {

		boolean result = true;
    int agId = agNameToId(agName);

    try {

      if (action.getFunctor().equals("right")) {
        result = model.moveRight(agId);
      } else if (action.getFunctor().equals("left")) {
        result = model.moveLeft(agId);
      } else if (action.getFunctor().equals("up")) {
        result = model.moveUp(agId);
      } else if (action.getFunctor().equals("down")) {
        result = model.moveDown(agId);
      } else if (action.getFunctor().equals("moveTo")) {
				int x = (int) ((NumberTerm) action.getTerm(0)).solve();
				int y = (int) ((NumberTerm) action.getTerm(1)).solve();
				result = model.moveTo(agId, x-1, y-1);
				moves++;
			} else if (action.getFunctor().equals("search")) {
        //path = aStar();
      } else if (action.getFunctor().equals("nextMov")) {
        result = model.nextMov(agId);
      } else if (action.getFunctor().equals("end")) {
        view.setSearches(searches);
				view.setMoves(moves);
				view.setExpanded(expandedStates);
				view.setDeleted(deletedStates);
				view.setRuntime(runtimeSum/searches);
      }
    } catch (Exception e) {
      logger.info("exception: " + e.getMessage());
    }
    return true;
  } 

  @Override
  public void updateAgsPercept() {
    changeWorld();
    for (int i = 0; i < model.getNbOfAgs(); i++) {
      updateAgPercept(i);
    }
  }

  public void updateAgPercept(int agId) {
    Location agLoc, taLoc;

    clearPercepts(agentsName[agId]);

    agLoc = model.getAgPos(agId);
    Literal posPerc = Literal.parseLiteral(
        "pos(" + (agLoc.x+1) + "," + (agLoc.y+1) + ")");
    addPercept(agentsName[agId], posPerc);

    if(agId == START)
      taLoc = model.getAgPos(GOAL);
    else
      taLoc = model.getAgPos(START);
    Literal targetPosPerc = Literal.parseLiteral(
        "target(" + (taLoc.x+1) + "," + (taLoc.y+1) + ")");
    addPercept(agentsName[agId], targetPosPerc);

    if (agId == START) {
			long tStart, tEnd;
			ListTermImpl agentPath = new ListTermImpl();
			
			//removeLastStatesFromModel();
			removeLastPathFromModel();
			
			tStart = System.currentTimeMillis();
			path = movingTargetDStarLite(agLoc, taLoc);
			tEnd = System.currentTimeMillis();	
			
			if(path != null) {
				for(State s : path) {
					Term step = (Term) ASSyntax.createLiteral("moveTo", 
						ASSyntax.createNumber(s.getX()+1),
						ASSyntax.createNumber(s.getY()+1)
						);
					agentPath.add(step);
					model.add(MovingTargetModel.PATH, s.getX(), s.getY());	
				}
			}
			
			searches++;
			runtimeSum += (tEnd-tStart);	
			
      addPercept(agentsName[agId], ASSyntax.createLiteral("novoPlano", (ListTermImpl) agentPath.reverse()));
    }
  }

  public int agNameToId(String name) {
    if(name.contains("agent"))
      return START;
    else if (name.equals("target"))
      return GOAL;
    else
      return -1;
  }  
	
	
	public void initializeVariables(){
	
		Location agLoc = model.getAgPos(START);
		Location taLoc = model.getAgPos(GOAL);
		
		OPEN.clear();
		CLOSED.clear();
		DELETED.clear();
		OTHERS.clear();
		visited = new int[N][N];
		
		for(int i = 0; i < N; i++){
			for(int j = 0; j < N; j++){
				if (model.isFreeOfObstacle(i, j)) {
					//int c = model.isFreeOfObstacle(i, j) ? 1 : OO;
					OTHERS.add(new State(i, j, 0));
				}
			}
		}
				
		km = 0;
		
		start = OTHERS.get(OTHERS.indexOf(new State(agLoc.x, agLoc.y, 0)));
		start.setG(0);
		start.setRHS(0);
		goal =  OTHERS.get(OTHERS.indexOf(new State(taLoc.x, taLoc.y, 0)));
		
		start = calculateKey(start);		
		OPEN.add(start);
		//removeFromOthers(start);
		
		lastStart = start;
		lastGoal = goal;
  }
  	
  
  private void computeCostMinimalPath(){
		
		LinkedList<State> succ = new LinkedList<State>();
		LinkedList<State> pred = new LinkedList<State>();
			  
		if(OPEN.isEmpty()) return;
		
		//System.out.println("> ComputeCostMinimalPath");
				
		while(true) {
			
			if(OPEN.isEmpty()) {
				//System.out.println("  OPEN empty");
				break;
			}
			if(!((OPEN.peek().compareTo(calculateKey(goal)) < 0) // menor
					|| goal.getRHS() > goal.getG())) {
				//System.out.println("  peek " + OPEN.peek());
				//System.out.println("  goal " + goal);
				break;
			}
			
			State u = OPEN.poll();
			State uNew = calculateKey(new State(u));
			
			//System.out.println("OPEN " + u);
			
			if(u.compareTo(uNew) < 0) { // u está desatualizado
				//System.out.println("  1 ... ");
				OPEN.add(u);
				expandedStates++; 
			} else if(u.getG() > u.getRHS()) { // melhorou o caminho
				u.setG(u.getRHS());
				//System.out.println("  2 ... " + u);
				//OPEN.remove(u);
				CLOSED.add(u);
				succ = findSucc(u);
				for(State s : succ) {
					/*if(!s.equals(start) && (s.getRHS() > u.getG() + u.getC())) {
						s.setParent(u);
						s.setRHS(u.getG() + u.getC());
						updateState(s);
					}*/
					updateState(s);
					//System.out.println("  expand " + s);
				}				
				model.remove(MovingTargetModel.OPEN, u.getX(), u.getY());
				model.add(MovingTargetModel.CLOSED, u.getX(), u.getY());
			} else {	// g <= rhs, piorou
				//System.out.println("  3 ... ");
				u.setG(OO);
				succ = findSucc(u);
				succ.add(u);
				for(State s : succ) {
					/*if(!s.equals(start) &&  u.equals(s.getParent())) {
						s.setRHS(OO);
						pred = findPred(u);
						for(State p : pred) {
							if(p.getG() + 1 < s.getRHS()) {
								s.setRHS(p.getG() + 1);
								s.setParent(p);
							}
						}
						updateState(s);							
					}*/
					//System.out.println("  expand " + s);
					updateState(s);
				}
			}
			//System.out.println("  next");
		}
			//System.out.println("END");
  }
	
	public void basicDeletion() {
		
		LinkedList<State> pred = new LinkedList<State>();
		
		start.setParent(null);		
		
		lastStart.setRHS(OO);
		lastStart.setParent(null);
		pred = findSucc(lastStart);
		for(State p : pred) {
			int val = (p.getG() == OO) ? OO : p.getG() + 1;
			if(val < lastStart.getRHS()) {
				lastStart.setRHS(val);
				lastStart.setParent(p);
			}
		}
		
		updateState(lastStart);		
	}
	
	public LinkedList<State> movingTargetDStarLite(Location agLoc, Location taLoc) {
		
		LinkedList<State> newPath = null;	
		
		if(start.equals(goal))
			return null;
				
		start = getFromOthers(agLoc.x, agLoc.y);
		goal = getFromOthers(taLoc.x, taLoc.y);
		
		if(K == 0 && path.contains(goal)) {
			
			System.out.println("Target still on the path.");
			path.removeLast();
			return path;
			
		} else {
			
			System.out.println("Target moved out the path.");
			
			km += heuristic(lastGoal);
			
			if(!lastStart.equals(start)) {
				basicDeletion();
				//optimizedDeletion();
			}
			
			//initializeVariables();
			computeCostMinimalPath();		
				
			newPath = new LinkedList<State>();
			if(goal.getRHS() < OO) {
				State cur = goal;
				while (cur != start) {
					newPath.add(cur);
					cur = cur.getParent();
				}
			} else {
				System.out.println(">> Path not found.");
			}
			
			lastStart = start;
			lastGoal = goal;		
		}
	
    return newPath;		
	}
		
	public State getFromOthers(int x, int y){		
		for(int i = 0; i < OTHERS.size(); i++){
			State s = OTHERS.get(i);
			if(s.getX() == x && s.getY() == y){
				return s;
			}
		}
		return null;
	}
	
	public State removeFromOthers(int x, int y){		
		for(int i = 0; i < OTHERS.size(); i++){		
			State s = OTHERS.get(i);
			if(s.getX() == x && s.getY() == y){
				OTHERS.remove(i);
				return s;
			}
		}
		return null;
	}
	
	public boolean isOnOthers(int x, int y){		
		for(int i = 0; i < OTHERS.size(); i++){		
			State s = OTHERS.get(i);
			if(s.getX() == x && s.getY() == y){
				return true;
			}
		}
		return false;
	}
	
	
	public void updateState(State u) {
		/*if(u.getG() != u.getRHS() && OPEN.contains(u)) {
			OPEN.remove(u);
			u = calculateKey(u);
			OPEN.add(u);
			expandedStates++; 
		} else if(u.getG() != u.getRHS() && !OPEN.contains(u)) {
			OPEN.add(u);
			expandedStates++; 
			model.add(MovingTargetModel.OPEN, u.getX(), u.getY());
			model.remove(MovingTargetModel.CLOSED, u.getX(), u.getY());
		} else if(u.getG() == u.getRHS() && OPEN.contains(u)) {
			OPEN.remove(u);
			CLOSED.add(u);
			expandedStates++; 
			model.add(MovingTargetModel.OPEN, u.getX(), u.getY());
			model.add(MovingTargetModel.CLOSED, u.getX(), u.getY());
		}*/
		LinkedList<State> pred = new LinkedList<State>();
		
		if(!u.equals(start)) {
			u.setRHS(OO);
			pred = findSucc(u);
			for(State p : pred) {
				//System.out.print("    pred " + p);
				int val = (p.getG() == OO) ? OO : p.getG() + 1;
				if(val < u.getRHS()) {
					u.setRHS(val);
					u.setParent(p);
					//System.out.println(" <- set parent ");	
				} else {
					//System.out.println();
				}
			}			
		}
		if(OPEN.contains(u)) {
			//System.out.println("  remove " + u);
			OPEN.remove(u);
			CLOSED.add(u);
			model.remove(MovingTargetModel.OPEN, u.getX(), u.getY());
			model.add(MovingTargetModel.CLOSED, u.getX(), u.getY());
		}
		if(u.getG() != u.getRHS()) {
			//System.out.println("  add " + u);
			u = calculateKey(u);
			OPEN.add(u);
			expandedStates++; 
			CLOSED.remove(u);
			model.add(MovingTargetModel.OPEN, u.getX(), u.getY());
			model.remove(MovingTargetModel.CLOSED, u.getX(), u.getY());
		}
	}
	
	
  public LinkedList<State> findSucc(State u){	  
		int i;
		int x = u.getX();
	  int y = u.getY();
		
		LinkedList<State> succ = new LinkedList<State>();
				
		State s = getFromOthers(x-1, y);
		if(s != null) { succ.add(s); }
		s = getFromOthers(x+1, y);
		if(s != null) { succ.add(s); }
		s = getFromOthers(x, y-1);
		if(s != null) { succ.add(s); }
		s = getFromOthers(x, y+1);
		if(s != null) { succ.add(s); }
		
		return succ;
  }
	
	public LinkedList<State> findPred(State u){
	  
		int i;
		int x = u.getX();
	  int y = u.getY();
		
		LinkedList<State> pred = new LinkedList<State>();
				
		i = OTHERS.indexOf(new State(x-1, y, 0));
		if(i >= 0 && u.equals(OTHERS.get(i).getParent())) { pred.add(OTHERS.get(i)); }
		i = OTHERS.indexOf(new State(x+1, y, 0));
		if(i >= 0 && u.equals(OTHERS.get(i).getParent())) { pred.add(OTHERS.get(i)); }
		i = OTHERS.indexOf(new State(x, y-1, 0));
		if(i >= 0 && u.equals(OTHERS.get(i).getParent())) { pred.add(OTHERS.get(i)); }
		i = OTHERS.indexOf(new State(x, y+1, 0));
		if(i >= 0 && u.equals(OTHERS.get(i).getParent())) { pred.add(OTHERS.get(i)); }
		
		return pred;
  }
	
  	
	private int heuristic(State u) {
		int h = Math.abs(u.getX() - goal.getX()) + Math.abs(u.getY() - goal.getY());
		return h;
	}
	
	private State calculateKey(State u)	{
		int val2 = Math.min(u.getRHS(), u.getG());
		int val1 = (val2 == OO) ? OO : (val2 + heuristic(u) + km);		
		u.getK().setFirst(val1);
		u.getK().setSecond(val2);
		return u;
	}
	
	private int getCost(int x, int y) {
		return costs[y][x];		
	}
	
	private void removeLastPathFromModel() {
		if(path != null) {
			for(State s : path) {
				model.remove(MovingTargetModel.PATH, s.getX(), s.getY());
			}
		}
	}
	
	private void removeLastStatesFromModel() {
		for(State s : OPEN) {
			model.remove(MovingTargetModel.OPEN, s.getX(), s.getY());
		}
		for(State s : CLOSED) {
			model.remove(MovingTargetModel.CLOSED, s.getX(), s.getY());
		}
	}
	
	public void changeWorld() {
		
		LinkedList<State> succ = new LinkedList<State>();
			
		for (int i = 0; i < K; i++) { 
			// block states
			Location blockLoc = model.nextFreeLoc();
			model.add(MovingTargetModel.OBSTACLE, blockLoc); 
			model.remove(MovingTargetModel.TEXT, blockLoc);
			
			State u = removeFromOthers(blockLoc.x, blockLoc.y);			
			if(!u.equals(start)) {
				u.setRHS(OO);
				u.setG(OO);
				u.setParent(null);
				succ = findSucc(u);
				for(State s : succ) {
					updateState(s);
				}			
			}
			u = null;
			
			// unblock states
			Location unblockLoc = null;
			while (unblockLoc == null) {
				int x = model.getRandom().nextInt(N);
				int y = model.getRandom().nextInt(N);
				if (model.hasObject(MovingTargetModel.OBSTACLE, x, y))
					unblockLoc = new Location(x, y);
			}
			model.remove(MovingTargetModel.OBSTACLE, unblockLoc);
			model.add(MovingTargetModel.TEXT, unblockLoc);
			
			u = new State(unblockLoc.x, unblockLoc.y, 0);
			OTHERS.add(u);
		}
	}

	
  class State  implements Comparable {
	  int x;
	  int y;
	  private int c = 1; // custo
	  private int h; // heuristica
	  private int g = OO; // custo para se chegar até aqui do estado inicial
	  private State parent = null;
	  private int rhs = OO;
		Pair<Integer, Integer> k = new Pair(OO,OO);
	  
		
	  public State(int x, int y, int h){
		  this.x = x;
		  this.y = y;
		  this.h = h;
	  }
		
	  public State(int x, int y, int h, int c){
		  this.x = x;
		  this.y = y;
		  this.h = h;
			this.c = c;
	  }
		
	  public State(int x, int y, int h, Pair k){
		  this.x = x;
		  this.y = y;
			this.c = c;
		  this.h = h;
			this.k = k;
	  }
		
	  public State(State s){
		  this.x = s.x;
		  this.y = s.y;
			this.c = s.c;
		  this.h = s.h;
			this.k = s.k;
			this.g = s.g;
			this.parent = s.parent;
			this.rhs = s.rhs;
			this.k = s.k;
	  }
	  
	  public int getX(){
		  return x;
	  }
	  
	  public int getY(){
		  return y;
	  }
	  
	  public int getH(){
		  return heuristic(this);
	  }
		
	  public int getC(){
		  return c;
	  }
	  
	  public int getG(){
		  return g;
	  }
		
		public Pair getK() {
			return k;
		}
	  
	  public State getParent(){
		  return parent;
	  }
		
	  public void setPos(int x, int y){
		  this.x = x;
			this.y = y;
	  }
		
	  public void setC(int c){
		  this.c = c;
	  }
	  
	  public void setG(int g){
		  this.g = g;
	  }
		
		public void setK(Pair k) {
			this.k = k;
		}
	  
	  public void setParent(State parent){
		  this.parent = parent;
	  }

	  public int getF(){
		return g + h;
		//return Math.min(g, (rhs + h));
	  }	  
	  
	  public int getRHS(){
		  return this.rhs;
	  }
	  
	  public void setRHS(int rhs){
		  this.rhs = rhs;
	  }
		
	  public void setH(int h){
		  this.h = h;
	  }
		
		
		@Override
		public boolean equals(Object obj) {
			if ( !(obj instanceof State) ) return false;
			State s = (State) obj;
			return (x == s.getX() && y == s.getY());
		}
		
		public int compareTo(Object obj) {
			State s = (State) obj;
			if (k.first() > s.k.first()) return 1;
			else if (k.first() < s.k.first()) return -1;
			if (k.second() > s.k.second()) return 1;
			else if (k.second() < s.k.second()) return -1;
			return 0;
		}
		
		//Override the CompareTo function for the HashMap usage
		@Override
		public int hashCode()
		{
			return this.x + 34245*this.y;
		}
	  
		public String toString() {
			return "s(" + x + "," + y + "): parent=" + ((parent != null) ? parent.displayName() : "null") + displayVars();
		}
		
		public String displayVars() {
			return " c=" + c + " g=" + g + " h=" + heuristic(this) + " rhs=" + rhs + " k<" + k.first() + "," + k.second() + ">";
		}
		public String displayName() {
			return "s(" + x + "," + y + ")";
		}
  }

}


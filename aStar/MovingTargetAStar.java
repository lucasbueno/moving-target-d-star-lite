
package aStar;

import jason.asSyntax.*;
import jason.environment.*;
import jason.environment.grid.*;
import java.util.logging.*;
import java.util.*;


public class MovingTargetAStar extends TimeSteppedEnvironment {

  // Constantes
  public static final int DELAY  = 500; // DELAY entre acoes em ms
  public static final int START  = 0;
  public static final int GOAL = 1;
  public static final int OO = 99999;

  // Variáveis globais
  private int								N;   // largura do grid NxN 
  private int								searches, moves, expandedStates, deletedStates;
  private long 							runtimeSum;
  private int[][]       		costs;
  private String[]      		agentsName; 
  private Logger        		logger = Logger.getLogger("movingTargetSearch.mas2j."+MovingTargetAStar.class.getName());
  private MovingTargetModel	model;
  private MovingTargetView 	view;
  private List<State> path = new ArrayList<State>();
  private List<State> OPEN = new ArrayList<State>();
  private List<State> CLOSED = new ArrayList<State>();
  private List<State> DELETED = new ArrayList<State>();
  private List<State> OTHERS = new ArrayList<State>();
  private State start = null;
  private State goal = null;
  private int[][] visited;

  /** Called before the MAS execution with the args informed in .mas2j */
  @Override
  public void init(String[] args) {
    super.init(new String[] { String.valueOf(DELAY*5) }); // set step timeout

    setOverActionsPolicy(OverActionsPolicy.ignoreSecond);
    setSleep(DELAY);

    N = Integer.parseInt(args[0]);
    float blockPerc = Float.parseFloat(args[1]); 
    int K = Integer.parseInt(args[2]);
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

    updateAgsPercept();    
    addPercept(Literal.parseLiteral("matriz(" + N + "," + N + ")"));	
  }

  public void initializeVariables(){

    OPEN.clear();
    CLOSED.clear();
    DELETED.clear();
    OTHERS.clear();
    visited = new int[N][N];

    for(int i = 0; i < N; i++){
      for(int j = 0; j < N; j++){

        OTHERS.add(new State(i, j, Math.abs(i - j) + Math.abs(i - j)));
      }
    }

    Location agLoc = model.getAgPos(START);
    Location taLoc = model.getAgPos(GOAL);
    start = new State(agLoc.x, agLoc.y, Math.abs(agLoc.x - taLoc.x) + Math.abs(agLoc.y - taLoc.y));
    start.setG(0);
    goal = new State(taLoc.x, taLoc.y, 0);

    OPEN.add(start);
    removeFromOthers(start.getX(), start.getY());
  }

  public void removeFromOthers(int x, int y){

    for(int i = 0; i < OTHERS.size(); i++){

      if(OTHERS.get(i).getX() == x){

        if(OTHERS.get(i).getY() == y){

          OTHERS.remove(i);
          break;
        }
      }
    }
  }

  public boolean isOnOthers(int x, int y){

    for(int i = 0; i < OTHERS.size(); i++){

      if(OTHERS.get(i).getX() == x){

        if(OTHERS.get(i).getY() == y){

          return true;
        }
      }
    }
    return false;
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
        model.moveTo(agId, x-1, y-1);
        moves++;
      } else if (action.getFunctor().equals("search")) {
        path = aStar();
      } else if (action.getFunctor().equals("nextMov")) {
        model.nextMov(agId);
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


  private List aStar(){

    initializeVariables();

    State smallestF = OPEN.get(0);
    visited[smallestF.getX()][smallestF.getY()] = 1;
    while((!smallestF.equals(goal)) && (!OPEN.isEmpty())){

      model.remove(MovingTargetModel.OPEN, smallestF.getX(), smallestF.getY());
      model.add(MovingTargetModel.CLOSED, smallestF.getX(), smallestF.getY());

      OPEN.remove(0);
      CLOSED.add(smallestF);  
      expand(smallestF);
      if (!OPEN.isEmpty())
        smallestF = OPEN.get(0);
    }

    List newPath = new ArrayList();		
    if(smallestF.equals(goal)) {
      State cur = smallestF;
      while (cur != start) {
        newPath.add(cur);
        cur = cur.getParent();
      }
    }

    return (List) newPath;
  }

  public void addSorted(State s){
    int x = 0;
    while(x < OPEN.size() && (s.getF() > OPEN.get(x).getF())){
      x++;
    }

    if(x == OPEN.size()){
      OPEN.add(s);
    } else {
      OPEN.add(x, s);
    }

    expandedStates++;
    model.add(MovingTargetModel.OPEN, s.getX(), s.getY());
  }

  public void expand(State s){
    int x = s.getX();
    int y = s.getY();
    if(x-1 >= 0 && isOnOthers(x-1, y) && model.isFreeOfObstacle(x-1, y)){
      int h = Math.abs(x-1 - goal.getX()) + Math.abs(y - goal.getY());
      //int cost = model.isFreeOfObstacle(x-1, y) ? 1 : OO;
      int cost = 1;
      State newS = new State(x-1, y, h);
      newS.setParent(s);
      newS.setG(s.getG() + cost);
      addSorted(newS);
      visited[x-1][y] = 1;
      removeFromOthers(x-1, y);
    }

    if(x+1 < N  && isOnOthers(x+1, y) && model.isFreeOfObstacle(x+1, y)){
      int h = Math.abs(x+1 - goal.getX()) + Math.abs(y - goal.getY());
      //int cost = model.isFreeOfObstacle(x+1, y) ? 1 : OO;
      int cost = 1;
      State newS = new State(x+1, y, h);
      newS.setParent(s);
      newS.setG(s.getG() + cost);
      addSorted(newS);
      visited[x+1][y] = 1;
      removeFromOthers(x+1, y);
    }

    if(y-1 >= 0 && isOnOthers(x, y-1) && model.isFreeOfObstacle(x, y-1)){
      int h = Math.abs(x - goal.getX()) + Math.abs(y-1 - goal.getY());
      //int cost = model.isFreeOfObstacle(x, y-1) ? 1 : OO;
      int cost = 1;
      State newS = new State(x, y-1, h);
      newS.setParent(s);
      newS.setG(s.getG() + cost);
      addSorted(newS);
      visited[x][y-1] = 1;
      removeFromOthers(x, y-1);
    }

    if(y+1 < N  && isOnOthers(x, y+1) && model.isFreeOfObstacle(x, y+1)){
      int h = Math.abs(x - goal.getX()) + Math.abs(y+1 - goal.getY());
      //int cost = model.isFreeOfObstacle(x, y+1) ? 1 : OO;
      int cost = 1;
      State newS = new State(x, y+1, h);
      newS.setParent(s);
      newS.setG(s.getG() + cost);
      addSorted(newS);
      visited[x][y+1] = 1;
      removeFromOthers(x, y+1);
    }
  }

  /** Called before the end of MAS execution */
  @Override
  public void stop() {
    super.stop();
  }

  @Override
  public void updateAgsPercept() {
    model.changeWorld();
    for (int i = 0; i < model.getNbOfAgs(); i++) {
      updateAgPercept(i);
    }
  }

  public void updateAgPercept(int agId) {
    Location agLoc, tLoc;

    clearPercepts(agentsName[agId]);

    agLoc = model.getAgPos(agId);
    Literal posPerc = Literal.parseLiteral(
        "pos(" + (agLoc.x+1) + "," + (agLoc.y+1) + ")");
    addPercept(agentsName[agId], posPerc);

    if(agId == START)
      tLoc = model.getAgPos(GOAL);
    else
      tLoc = model.getAgPos(START);
    Literal targetPosPerc = Literal.parseLiteral(
        "target(" + (tLoc.x+1) + "," + (tLoc.y+1) + ")");
    addPercept(agentsName[agId], targetPosPerc);

    if (agId == START) {
      long tStart, tEnd;
      ListTermImpl agentPath = new ListTermImpl();

      removeLastStatesFromModel();
      removeLastPathFromModel();

      tStart = System.currentTimeMillis();
      path = aStar();
      tEnd = System.currentTimeMillis();	

      for(State s : path) {
        Term step = (Term) ASSyntax.createLiteral("moveTo", 
            ASSyntax.createNumber(s.getX()+1),
            ASSyntax.createNumber(s.getY()+1)
            );
        agentPath.add(step);
        model.add(MovingTargetModel.PATH, s.getX(), s.getY());	
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

  public int getCost(int x, int y) {
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



  class State {
    int x;
    int y;
    private int h; // heuristica
    private int g = OO; // custo para se chegar até aqui do estado inicial
    private State parent = null;
    private int rhs = 0;

    public State(int x, int y, int h){
      this.x = x;
      this.y = y;
      this.h = h;
    }

    public int getX(){
      return x;
    }

    public int getY(){
      return y;
    }

    public int getH(){
      return h;
    }

    public int getG(){
      return g;
    }

    public State getParent(){
      return parent;
    }

    public void setG(int g){
      this.g = g;
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

    public boolean equals(State s) {
      return (x == s.getX() && y == s.getY());
    }

  }

}


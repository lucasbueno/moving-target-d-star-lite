

import jason.environment.grid.*;
import java.util.*;


class MovingTargetModel extends GridWorldModel {


	public static final int OPEN  = 8;
	public static final int CLOSED  = 16;
  public static final int PATH 	= 32;
  public static final int TEXT  = 64;

	private int K;	
	
	
	public MovingTargetModel(int idAgent, int idTarget, int N, float blockPerc, int K) {
		super(N, N, 2);

		this.K = K;

		//random.setSeed(123456); // gera o mesmo labirinto a cada execuçao
		generateNewWorld(blockPerc);

		// posicao inicial aleatoria
		setAgPos(idAgent, getFreePos());
		setAgPos(idTarget, getFreePos());
	}
	
	@Override
	public void setAgPos(int ag, Location l) {
		super.setAgPos(ag, l);
	}

	public void generateNewWorld(float blockPerc) {
		
		/*
		// mundo labirinto
    int wSize = (width+1)/2;
		int hSize = (height+1)/2;
    MazeGenerator m = new MazeGenerator(wSize, hSize);
    m.displayBinary();
    int[][] maze = m.getBinaryMaze();
	
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				add(TEXT, x, y);
				if(maze[x][y] == 1) {
					add(OBSTACLE, x, y);
				}
			}
		}
		*/
		
		// mundo aleatorio
		int numBlocks = (int)(width*height * blockPerc);
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
				int x = random.nextInt(width);
				int y = random.nextInt(height);
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
		if (agLoc.x < width-1 && isFreeOfObstacle(agLoc.x+1, agLoc.y)) {
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
		if (agLoc.y < height-1 && isFreeOfObstacle(agLoc.x, agLoc.y+1)) {
			agLoc.y++;
			setAgPos(ag, agLoc);
			return true;
		}
		return false;
	}

	boolean nextMov(int ag) throws Exception {
		boolean ok = false;
		while(!ok) { 
			int nextPos = random.nextInt(4);    
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
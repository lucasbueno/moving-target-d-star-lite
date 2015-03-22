 
import java.util.Collections;
import java.util.Arrays;
 
/*
 * recursive backtracking algorithm
 * shamelessly borrowed from the ruby at
 * http://weblog.jamisbuck.org/2010/12/27/maze-generation-recursive-backtracking
 */
public class MazeGenerator {
	private final int x;
	private final int y;
	private final int[][] maze;
	private final int[][] binaryMaze;
 
	public MazeGenerator(int x, int y) {
		this.x = x;
		this.y = y;
		maze = new int[this.x][this.y];
		binaryMaze = new int[this.x*2-1][this.y*2-1];
		generateMaze(0, 0);
		generateBinaryMaze();
	}
	
	public int[][] getMaze() {
		return maze;
	}
	
	public int[][] getBinaryMaze() {
		return binaryMaze;
	}
 
	public void displayBinary() {
		int nx = x*2-1;
		int ny = y*2-1;
		// build the binary maze
		for (int i = 0; i < ny; i++) {
			for (int j = 0; j < nx; j++) {
				binaryMaze[j][i] = isObstacle(j,i) ? 1 : 0;
			}
		}
		// draw the binary maze
		for (int i = 0; i < ny; i++) {
			for (int j = 0; j < nx; j++) {
				System.out.print(binaryMaze[j][i]);
			}
			System.out.println();
		}
	}
 
	public void display() {
		for (int i = 0; i < y; i++) {
			// draw the north edge
			for (int j = 0; j < x; j++) {
				System.out.print((maze[j][i] & 1) == 0 ? "+---" : "+   ");
			}
			System.out.println("+");
			// draw the west edge
			for (int j = 0; j < x; j++) {
				System.out.print((maze[j][i] & 8) == 0 ? "|   " : "    ");
			}
			System.out.println("|");
		}
		// draw the bottom line
		for (int j = 0; j < x; j++) {
			System.out.print("+---");
		}
		System.out.println("+");
	}
	
	
	public void displayDIR() {
		for (int i = 0; i < y; i++) {
			for (int j = 0; j < x; j++) {
				System.out.print((maze[j][i] & 1) == 0 ? "" : "N");
				System.out.print((maze[j][i] & 2) == 0 ? "" : "S");
				System.out.print((maze[j][i] & 4) == 0 ? "" : "E");
				System.out.print((maze[j][i] & 8) == 0 ? "" : "W");
				System.out.print(",");
			}
			System.out.println();
		}
	}
	
	private void generateBinaryMaze() {		
		int nx = x*2-1;
		int ny = y*2-1;
		// build the binary maze
		for (int i = 0; i < ny; i++) {
			for (int j = 0; j < nx; j++) {
				binaryMaze[j][i] = isObstacle(j,i) ? 1 : 0;
			}
		}
	}
	
	private boolean isObstacle(int bx, int by) {
		int cx = bx*10/2;
		int cy = by*10/2;
		boolean obstacle = true;
		
		if ((cx % 10) == 0 && (cy % 10) == 0) {
			return false;
		}
		else {
			if ((cx % 10) == 0) {
				int ny = (cy-5)/10;
				int sy = (cy+5)/10;
				cx /= 10;
				cy /= 10;
				//System.out.println("("+by+","+bx+") -> N("+ny+","+cx+"); S("+sy+","+cx+")");
				if(ny >= 0 && (maze[cx][ny] & 2) == 2)
					obstacle = false;
				if(sy < y && (maze[cx][sy] & 1) == 1)
					obstacle = false;			
			} else if ((cy % 10) == 0) {
				int wx = (cx-5)/10;
				int ex = (cx+5)/10;
				cx /= 10;
				cy /= 10;
				//System.out.println("("+by+","+bx+") -> E("+cy+","+ex+"); W("+cy+","+wx+")");		
				if(wx >= 0 && (maze[wx][cy] & 4) == 4)
					obstacle = false;
				if(ex < x && (maze[ex][cy] & 8) == 8)
					obstacle = false;
			} else {
				int ny = by-1;
				int sy = by+1;
				int wx = bx-1;
				int ex = bx+1;
				//System.out.println("("+by+","+bx+") -> N("+ny+","+bx+"); S("+sy+","+bx+")"; E("+by+","+ex+"); W("+by+","+wx+")");
				obstacle = false;
				if(binaryMaze[bx][ny] == 1 || isObstacle(bx,sy))
					obstacle = true;
				if(binaryMaze[wx][by] == 1 || isObstacle(ex,by))
					obstacle = true;
			}
			return obstacle;
		}
	}
 
	private void generateMaze(int cx, int cy) {
		DIR[] dirs = DIR.values();
		Collections.shuffle(Arrays.asList(dirs));
		for (DIR dir : dirs) {
			int nx = cx + dir.dx;
			int ny = cy + dir.dy;
			if (between(nx, x) && between(ny, y)
					&& (maze[nx][ny] == 0)) {
				maze[cx][cy] |= dir.bit;
				maze[nx][ny] |= dir.opposite.bit;
				generateMaze(nx, ny);
			}
		}
	}
 
	private static boolean between(int v, int upper) {
		return (v >= 0) && (v < upper);
	}
 
	private enum DIR {
		N(1, 0, -1), S(2, 0, 1), E(4, 1, 0), W(8, -1, 0);
		private final int bit;
		private final int dx;
		private final int dy;
		private DIR opposite;
 
		// use the static initializer to resolve forward references
		static {
			N.opposite = S;
			S.opposite = N;
			E.opposite = W;
			W.opposite = E;
		}
 
		private DIR(int bit, int dx, int dy) {
			this.bit = bit;
			this.dx = dx;
			this.dy = dy;
		}
	};
  
	
	public static int[][] createMaze(int x, int y) {
		return new MazeGenerator(x,y).getMaze();
	}
	
	public static int[][] createBinaryMaze(int x, int y) {
		return new MazeGenerator(x,y).getBinaryMaze();
	}
 
	public static void main(String[] args) {
		int x = args.length >= 1 ? (Integer.parseInt(args[0])) : 8;
		int y = args.length >= 2 ? (Integer.parseInt(args[1])) : 8;
		MazeGenerator maze = new MazeGenerator(x, y);
		maze.displayDIR();
		maze.display();
		maze.displayBinary();
	}
 
}

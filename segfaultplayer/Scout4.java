
package segfaultplayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import battlecode.common.*;


public strictfp class Scout4 extends RobotBase
{
	
	public String alphabet = "abcdefghijklmnopqrstuvwxyz";
	
	private int[][] grid;
	int xoffset;		// x-offset of grid
	int yoffset;		// y-offset of grid
	int resolution = 4; // grid resolution
	Set<Integer> uniqueTrees;
	Set<Integer> uniquePoints;
	
	public Scout4(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		//createGrid();
		uniqueTrees = new HashSet<Integer>();
		
		MapLocation dank = rc.getInitialArchonLocations(enemy)[0];
		System.out.println(dank);
		getpath(dank);
	}
	
	//== This Move class is used for pathfinding
	class Move implements Comparable<Move> {
		private float distanceToEnd;
		private float totalCost; // heuristic
		private float stride;
		private String moves;
		private MapLocation location;
		// Constructor 1: When you have no moves
		private Move(MapLocation thisLoc, MapLocation endLoc, float myStride) {
			location = thisLoc;
			distanceToEnd = thisLoc.distanceTo(endLoc);
			moves = "";
			stride = myStride;
			totalCost = (moves.length() * stride) + (distanceToEnd*1);//2);
		}
		
		// Constructor 2: When you have previous moves
		private Move(MapLocation thisLoc, MapLocation endLoc, String myMoves, float myStride) {
			location = thisLoc;
			distanceToEnd = thisLoc.distanceTo(endLoc);
			stride = myStride;
			moves = myMoves;
			totalCost = (moves.length() * stride) + (distanceToEnd*1);//2);
		}
		
		private void updateList(String s) {
			moves += s;
			updateCost();
		}
		
		private float updateCost() {
			totalCost = (moves.length() * stride) + (distanceToEnd*1);//2);
			return totalCost;
		}
		
		public int compareTo(Move other) {
			return Float.compare(totalCost,other.totalCost);
		}	
		
		public String toString() {
			int k = moves.length();
			return (Float.toString(totalCost)+ "  " 
			+ Float.toString(distanceToEnd) + "  " 
			+ Float.toString((moves.length() * stride)) + " " 
			+ Float.toString(stride) + " " 
			+ Integer.toString(k));
		}

	}
	
	public void getpath(MapLocation el) throws GameActionException {
		int steps = 0;
		float sR = 4f;
		float stride = 1f;
		float theta = 30f;

		
		System.out.println("dank Memes");
		System.out.println(rc.getLocation().distanceTo(el));
		
		
		while (rc.getLocation().distanceTo(el) > 5.0f) {
			boolean hasMoved = false;
			MapLocation myLoc = rc.getLocation();
			Direction startDir = myLoc.directionTo(el);
			float distance = myLoc.distanceTo(el);
			TreeInfo[] myTrees = rc.senseNearbyTrees(sR);
			//pathfinding
			uniquePoints = new HashSet<Integer>();
			Move bestMove = pathMeme(myLoc, el, myTrees, theta, stride, sR); 
			
			if(bestMove!=null) {
				String strdir = bestMove.moves.substring(0,1);
				Direction dir = startDir.rotateLeftDegrees(theta*alphabet.indexOf(strdir));
				if(rc.canMove(dir, stride)) {
					rc.move(dir, stride);
					steps+=1;
					rc.broadcast(150, steps);
					Clock.yield();
				} else {
					Clock.yield();
				}
			} else {
				System.out.println("Theres NO PATH");
				Clock.yield();
			}
		}
	}
	
	public Move pathMeme(MapLocation startLoc, MapLocation endLoc, TreeInfo[] trees, float theta, float stride, float rad) throws GameActionException {
		
		PriorityQueue<Move> myMoves = new PriorityQueue<Move>();
		Move firstMove = new Move(startLoc, endLoc, stride);
		myMoves.add(firstMove);
		System.out.println("This is danker than my memes");
		float width = 360f/theta;
		String possibleDirections = alphabet.substring(0, (int)width);
		//updateAllTrees(trees);

		while(!myMoves.isEmpty()) {
			System.out.println("im dank");
			Move lastMove = myMoves.remove();
			
			MapLocation myLoc = lastMove.location;
			//createGrid(myLoc,4f);
			//convert location --> int coordinate --> unique int
			int[] scaledMyLoc = scaledLocation(myLoc);
			int newScl = scaledMyLoc[0]*10000 + scaledMyLoc[1]; // since both x and y are 3 digits this should create a unique int for each grid square
			if(uniquePoints.add(newScl)) {
				if((startLoc.distanceTo(myLoc) + stride + 1) > rad) {
					// end case -> we've made it this far and we can't sense much further
					System.out.println("IM SO DANK");
					return lastMove;
				}
				
				// ==== add end case that returns no moves here =================
				Direction toEnd = myLoc.directionTo(endLoc);
				for(int i=0; i<possibleDirections.length(); i++) {
					MapLocation newLoc = myLoc.add(toEnd, stride);
					toEnd = toEnd.rotateLeftDegrees(theta);
					if(isClear(newLoc, trees)) {
						rc.setIndicatorLine(startLoc, newLoc, 0, 0, 255);
						rc.setIndicatorLine(myLoc, newLoc, 0, 255, 0);
						String temp = lastMove.moves+possibleDirections.substring(i, i+1);
						Move tempMove = new Move(newLoc, endLoc, temp, stride);
						//System.out.println(tempMove);
						myMoves.add(tempMove);
					} else {
						rc.setIndicatorLine(myLoc, newLoc, 255, 0, 0);
					}
				}	
			} else {
				System.out.println(newScl);
				System.out.println("failed");
				//System.out.println(uniquePoints);

				// we've already determined a better way to get to this grid square
			}
		}
		return null;
		
	}
	
	
	
	public void createGrid(MapLocation myLocation, float radius) throws GameActionException {
		float[] center = locateApproxCenter();
		int xcenter = (int)(myLocation.x*resolution);
		int ycenter = (int)(myLocation.x*resolution);
		xoffset = xcenter-((int)radius*resolution);
		yoffset = ycenter-((int)radius*resolution);
		grid = new int[2*(int)radius*resolution][2*(int)radius*resolution];
		//for(int i=0; i<grid.length; i++) {
		//	for (int j=0; j<grid[0].length; j++) {
		//		grid[i][j]=-1;
		//	}
		//}
	}
	
	public void updateGrid(TreeInfo myTree) {
		float x = myTree.location.x*resolution;
		float y = myTree.location.y*resolution;
		float res = myTree.radius*resolution;
		x = x-xoffset;
		y = y-yoffset;
		for(int xmin=(int)(x-res); xmin<(x+res); xmin++) {
			for(int ymin=(int)(y-res); ymin<(y+res); ymin++) {
				if(Math.sqrt(Math.pow(x-xmin, 2) + Math.pow(y-ymin, 2)) < res) {
					grid[xmin][ymin] = myTree.containedBullets+1;
				}
			}
		}
		
	}
	
	public boolean checkEmpty(float x, float y) {
		float xscl = x*resolution;
		float yscl = y*resolution;
		xscl = xscl-xoffset;
		yscl = yscl-yoffset;
		if(grid[(int)xscl][(int)yscl]==0) {
			return true;
		}
		return false;
	}
	
	public int[] scaledLocation(MapLocation m) {
		float x = m.x*resolution;
		float y = m.y*resolution;
		x = x-xoffset;
		y = y-yoffset;
		int[] xy = {(int)x,(int)y};
		return xy;
	}

	public void updateAllTrees(TreeInfo[] myTrees) {
		for (TreeInfo k : myTrees) {
			if(uniqueTrees.add(k.ID)) { // uses hashing to only add the new ones to the grid
				updateGrid(k);
			}
		}
	}
	
	
    public float[] locateApproxCenter() throws GameActionException {
    	MapLocation[] initArchonLocsA = rc.getInitialArchonLocations(Team.A);
    	MapLocation[] initArchonLocsB = rc.getInitialArchonLocations(Team.B);
    	MapLocation[] initArchonLocs = new MapLocation[2 * initArchonLocsA.length];
    	int t = 0;
    	for(MapLocation ml : initArchonLocsA)
    		initArchonLocs[t++] = ml;
    	for(MapLocation ml : initArchonLocsB)
    		initArchonLocs[t++] = ml;
    	float netX = 0.0f;
    	float netY = 0.0f;
    	for(MapLocation ml : initArchonLocs) {
    		netX += ml.x;
    		netY += ml.y;
    	}
    	
    	return new float[] {netX/initArchonLocs.length, netY/initArchonLocs.length};
    }
	
	
	
	
	private boolean[][] aroundMe = null;
	
	// offset should be my location
	public void processTrees(MapLocation offset, TreeInfo[] trees) throws GameActionException {
		System.out.println("Starting tree processing");
		aroundMe = new boolean[70][70];
		for(TreeInfo t : trees) {
			MapLocation ml = t.location;
			float relX = ml.x - offset.x;
			float relY = ml.y - offset.y;
			// relX, relY should be in range [-14,+14]
			relX = (60.0f/28.0f)*relX + 30.0f;
			relY = (60.0f/28.0f)*relY + 30.0f;
			// now they're in range [0,60]
			float effR = t.radius + 1.0f;
			// block of effR around center at relX,relY
			float minX = relX - effR;
			float minY = relY - effR;
			float maxX = relX + effR;
			float maxY = relY + effR;
			for(int a = (int)(minX+0.5); a < maxX; a++) {
				for(int b = (int)(minY+0.5); b < maxY; b++) {
					aroundMe[5+a][5+b] = true;
				}
			}
		}
		System.out.println("Finished base processing");
		for(int x = 0; x < 70; x ++) {
			for(int y = 0; y < 70; y ++) {
				if(aroundMe[x][y]) {
					rc.setIndicatorDot(new MapLocation(offset.x + (x - 35)*28.0f/60.0f, offset.y + (y - 35)*28.0f/60.0f),
							255, 255, 0);
				}
			}
		}
		System.out.println("Finished tree processing");
	}
	
	// checks for nearby trees
	
	public boolean isClear(MapLocation offset, MapLocation thisLoc) {
		float relX = thisLoc.x - offset.x;
		float relY = thisLoc.y - offset.y;
		int indexX = (int)(relX*(60.0f/28.0f)+35.5f);
		int indexY = (int)(relY*(60.0f/28.0f)+35.5f);
		if(indexX < 0 || indexY < 0 || indexX > 69 || indexY > 69)
			return false;
		return !aroundMe[indexX][indexY];
	}
	
	public boolean isClear(MapLocation thisLoc, TreeInfo[] trees) {
		
		for (TreeInfo t : trees) {
			float distance = thisLoc.distanceTo(t.location);
			if(distance<(t.radius+1.0f)) {
				return false;
			}
		}
		return true;
		
	}
	
	
    public static Direction[] getDirections(MapLocation myLoc, MapLocation otherLoc, float theta) throws GameActionException {
    	Direction[] dirs = new Direction [(int)(360.0f/theta)];
    	Direction bestdir = myLoc.directionTo(otherLoc);
    	dirs[0] = bestdir;
    	for (int j=1; j<dirs.length; j++) {
    		bestdir = bestdir.rotateLeftDegrees(theta);
    		dirs[j] = bestdir;
    	}
    	return dirs;
	}
    // directions towards end goal
}



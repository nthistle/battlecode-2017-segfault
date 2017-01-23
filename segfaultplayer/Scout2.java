package segfaultplayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import battlecode.common.*;


public strictfp class Scout2 extends RobotBase
{
	
	public String alphabet = "abcdefghijklmnopqrstuvwxyz";
	
	public Scout2(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		Clock.yield();//DEBUGYIELD
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
		Set<Integer> uniqueTrees = new HashSet<Integer>();
		float scaledNumTrees = 0;
		int steps = 0;
		float sR = rc.getType().sensorRadius;
		float stride = 1.25f;

		System.out.println("dank Memes");
		System.out.println(rc.getLocation().distanceTo(el));
		
		
		while (rc.getLocation().distanceTo(el) > 5.0f) {
			boolean hasMoved = false;
			MapLocation myLoc = rc.getLocation();
			float distance = myLoc.distanceTo(el);
			TreeInfo[] myTrees = rc.senseNearbyTrees(sR);
			
			//pathfinding
			Move bestMove = pathMeme(myLoc, el, myTrees, 30f, stride, sR); 
			
			if(bestMove!=null) {
				String strdir = bestMove.moves.substring(0,1);
				Direction dir = stringToDir(strdir);
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
	
	
	// my attempt at A*
	public Move neilAStar(MapLocation startLoc, MapLocation endLoc, TreeInfo[] trees) throws GameActionException {

		Clock.yield();
		PriorityQueue<Move> myMoves = new PriorityQueue<Move>();
		
		System.out.println("Testing bytecodes used for this");
		Clock.yield();
		//Move[] firstMoves = getNextMoves(new Move(startLoc, endLoc, 1.25f), endLoc);
		
		Clock.yield();
		for(Move m : firstMoves) {
			myMoves.add(m);
		}
		
		Move cur;
		
		processTrees(startLoc, trees);
		
		while(!myMoves.isEmpty()) {
			cur = myMoves.poll();
			// currently considering
			rc.setIndicatorDot(cur.location, 255, 0, 0);  // RED DOT FOR CURRENTLY CONSIDERING
			//Move[] nextSteps = getNextMoves(cur, endLoc);
			
			for(Move possNext : nextSteps) {
				if(isClear(startLoc, possNext.location)) {
					// color that crap green, so we know it's going in the PQ
					rc.setIndicatorLine(cur.location, possNext.location, 0, 255, 0);
					myMoves.add(possNext);
				} else {
					// color it blue so we know it's not going in PQ
					rc.setIndicatorLine(cur.location, possNext.location, 0, 0, 255);
				}
			}
			
			Clock.yield(); // kind of temp just so I can watch it work its magic
		}
			//Move lastMove = myMoves.remove();
			//MapLocation myLoc = lastMove.location;
			//
			//// ===== END CASE =====
			//if((startLoc.distanceTo(myLoc) + stride + 1) > rad) {
			//	// end case -> we've made it this far and we can't sense much further
			//	System.out.println("IM SO DANK");
			//	return lastMove;
			//}
		return null;
		
	}

	// A* PathFinding
	public Move pathMeme(MapLocation startLoc, MapLocation endLoc, TreeInfo[] trees, float theta, float stride, float rad) throws GameActionException {
		
		PriorityQueue<Move> myMoves = new PriorityQueue<Move>();
		Move firstMove = new Move(startLoc, endLoc, stride);
		myMoves.add(firstMove);
		System.out.println("This is danker than my memes");
		while(!myMoves.isEmpty()) {
			Move lastMove = myMoves.remove();

			MapLocation myLoc = lastMove.location;
			
			// ===== END CASE =====
			if((startLoc.distanceTo(myLoc) + stride + 1) > rad) {
				// end case -> we've made it this far and we can't sense much further
				System.out.println("IM SO DANK");
				return lastMove;
			}
			// ============================================================
			// ==== add end case that returns no moves here =================
			// ==== you don't have to wait for the entire queue to be over because it will never be ========
			// ================================================================
			
			Direction[] possibleDirections = getDirections(myLoc, endLoc, theta);
			for(Direction k : possibleDirections) {
				MapLocation newLoc = myLoc.add(k, stride);
				if(isClear(startLoc, trees)) {
					//rc.setIndicatorLine(startLoc, newLoc, 0, 0, 255);
					rc.setIndicatorLine(myLoc, newLoc, 0, 255, 0);
					ArrayList<Direction> newList = new ArrayList<Direction>(lastMove.moves);
					newList.add(k);
					Move tempMove = new Move(newLoc, endLoc, newList, stride);
					myMoves.add(tempMove);
				} else {
					rc.setIndicatorLine(myLoc, newLoc, 255, 0, 0);
				}
			}					

		}
		return null;
		
	}
	
//	// dfs pathfinding method
//	public Direction pathFind(MapLocation startLoc, MapLocation myLoc, MapLocation endLoc, TreeInfo[] trees, float theta, float stride, float rad) throws GameActionException {
//		if((startLoc.distanceTo(myLoc) + stride + 1) > rad) {
//			// end case -> we've made it this far and we can't sense much further
//			System.out.println("IM SO DANK");
//			return startLoc.directionTo(myLoc);
//		}
//		Direction[] moves = getBestDirections(myLoc, endLoc, theta);
//		for (int i=0; i<moves.length; i++) {
//			MapLocation temploc = myLoc.add(moves[i], stride);
//			rc.setIndicatorLine(myLoc, temploc, 20*i, 0, 0);
//			if(isClear(temploc, trees)) {
//				Direction bestDir = pathFind(startLoc, temploc, endLoc, trees, theta, stride, rad);
//				if(bestDir!=null) {
//					System.out.println("DANKER THAN U");
//					if(startLoc!=myLoc) {
//						return startLoc.directionTo(myLoc);
//					} else {
//						return bestDir;
//					}
//				}
//			}
//		}
//		return null;
//		
//	}
	
	
	
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
	

	
	// directions rotating around end goal
	public String stringDirections(float theta) {
		int len = (int)(360.0f/theta);
		String s = alphabet.substring(0, len);
		return s;
	}
	
	public Direction stringToDirection(String s, String dirs) {
		int len = dirs.length();
		float one = (360f/(float)len);
		int index = alphabet.indexOf(s);
		float dir = one*
		String k = stringDirections(theta);
		
		
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

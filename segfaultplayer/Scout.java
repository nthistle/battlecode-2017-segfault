package segfaultplayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import battlecode.common.*;


public strictfp class Scout extends RobotBase
{
	
	public Scout(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		Clock.yield();//DEBUGYIELD
		MapLocation dank = rc.getInitialArchonLocations(enemy)[0];
		System.out.println(dank);
		getpath(dank);
		//float dist = pathscout(dank);
		//harass();
		//circleEnemy(dank, dist);
	}
	
	//== This Move class is used for pathfinding
	class Move implements Comparable<Move> {
		private float distanceToEnd;
		private float totalCost; // heuristic
		// the heuristic takes in the total number of moves (moves.size()) * the stride length) == the cost to reach here
		// and the distanceToEnd*2 == the cost to goal (I multiplied by 2 so it works faster in straight line situations but tbh this is unnecessary)
		private float stride;
		private MapLocation location;
		private ArrayList<Move> moves;//<Direction> moves;
		
		// Constructor 1: When you have no moves
		private Move(MapLocation thisLoc, MapLocation endLoc, float myStride) {
			location = thisLoc;
			distanceToEnd = thisLoc.distanceTo(endLoc);
			moves = new ArrayList<Move>();//<Direction>();
			stride = myStride;
			totalCost = (moves.size() * stride) + (distanceToEnd*1);//2);
		}
		
		// Constructor 2: When you have previous moves
		private Move(MapLocation thisLoc, MapLocation endLoc, ArrayList<Move> myMoves, float myStride) {
			location = thisLoc;
			distanceToEnd = thisLoc.distanceTo(endLoc);
			moves = myMoves;
			stride = myStride;
			totalCost = (moves.size() * stride) + (distanceToEnd*1);//2);
		}
		
		/*private void updateList(Direction d) {
			moves.add(d);
			updateCost();
		}*/
		
		private float updateCost() {
			totalCost = (moves.size() * stride) + (distanceToEnd*1);//2);
			return totalCost;
		}
		
		private float getCost() {
			return totalCost;
		}
		
		public int compareTo(Move other) {
			return Float.compare(totalCost,other.totalCost);
		}	
		
		public String toString() {
			int k = moves.size();
			return (Float.toString(totalCost)+ "  " 
			+ Float.toString(distanceToEnd) + "  " 
			+ Float.toString((moves.size() * stride)) + " " 
			+ Float.toString(stride) + " " 
			+ Integer.toString(k));
		}

	}
	
	public void getpath(MapLocation el) throws GameActionException {
		Set<Integer> uniqueTrees = new HashSet<Integer>();
		float scaledNumTrees = 0;
		int steps = 0;
		//float sR = rc.getType().sensorRadius;
		float stride = 1.25f;

		System.out.println("dank Memes");
		System.out.println(rc.getLocation().distanceTo(el));
		
		Clock.yield();//DEBUGYIELD
		
		while (rc.getLocation().distanceTo(el) > 5.0f) {
			boolean hasMoved = false;
			MapLocation myLoc = rc.getLocation();
			float distance = myLoc.distanceTo(el);
			TreeInfo[] myTrees = rc.senseNearbyTrees(9.0f);//rc.senseNearbyTrees();
			// add all the new trees
			/*for (TreeInfo k : myTrees) {
				if(uniqueTrees.add(k.ID)) {
					scaledNumTrees+=(k.getRadius()*k.getRadius());
				}
			}*/
			
			Clock.yield();//DEBUGYIELD
			
			//broadcast tree data
			//rc.broadcast(151, uniqueTrees.size());
			//rc.broadcast(152, (int)scaledNumTrees);

			Clock.yield();//DEBUGYIELD
			
			//pathfinding
			Move bestMove = neilAStar(myLoc, el, myTrees);//pathMeme(myLoc, el, myTrees, 30f, stride, sR); // A*
			//Direction myDir = pathFind(myLoc, myLoc, el, myTrees, 30f, stride, sR); // old pathfinding
			if(bestMove!=null) {
				Direction theDir = myLoc.directionTo(bestMove.location);
				if(rc.canMove(theDir, stride)) {
					rc.move(theDir, stride);
				//if(rc.canMove(bestMove.moves.get(0), stride)) {
				//	rc.move(bestMove.moves.get(0), stride);
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
		Move[] firstMoves = getNextMoves(new Move(startLoc, endLoc, 1.25f), endLoc);
		
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
			Move[] nextSteps = getNextMoves(cur, endLoc);
			
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
	
	private Move[] getNextMoves(Move cur, MapLocation target) {
		// stride is 1.25
		Move[] nextMoves = new Move[5];
		Direction straightL = cur.location.directionTo(target);
		
		
		//System.out.println("Number 1");
		ArrayList<Move> newList = new ArrayList<Move>();
		for(Move m : cur.moves)
			newList.add(m);
		newList.add(cur);
		

		ArrayList<Move> tmpList = new ArrayList<Move>();
		tmpList.addAll(newList);
		
		nextMoves[0] = new Move(cur.location.add(straightL,1.25f),
				target, tmpList, 1.25f);


		//System.out.println("Number 2&3");
		tmpList = new ArrayList<Move>();
		tmpList.addAll(newList);
		nextMoves[1] = new Move(cur.location.add(straightL.rotateRightDegrees(30.0f),1.25f),
				target, tmpList, 1.25f);
		tmpList = new ArrayList<Move>();
		tmpList.addAll(newList);
		nextMoves[2] = new Move(cur.location.add(straightL.rotateLeftDegrees(30.0f),1.25f),
				target, tmpList, 1.25f);

		//System.out.println("Number 4&5");
		tmpList = new ArrayList<Move>();
		tmpList.addAll(newList);
		nextMoves[3] = new Move(cur.location.add(straightL.rotateRightDegrees(60.0f),1.25f),
				target, tmpList, 1.25f);
		tmpList = new ArrayList<Move>();
		tmpList.addAll(newList);
		nextMoves[4] = new Move(cur.location.add(straightL.rotateLeftDegrees(60.0f),1.25f),
				target, tmpList, 1.25f);
		
		return nextMoves;
	}
	/*
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
		
	}*/
	
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
	
	
	/*public boolean isClear(MapLocation thisLoc, TreeInfo[] trees) {
		for (TreeInfo t : trees) {
			float distance = thisLoc.distanceTo(t.location);
			if(distance<(t.radius+1.0f)) {
				return false;
			}
		}
		return true;
	}*/
	
	// directions rotating around end goal
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
    public static Direction[] getBestDirections(MapLocation myLoc, MapLocation otherLoc, float theta) throws GameActionException {
    	float initialtheta = theta;
    	Direction[] dirs = new Direction [(int)(360.0f/theta)];
    	Direction bestdir = myLoc.directionTo(otherLoc);
    	dirs[0] = bestdir;
    	for (int j=1; j<dirs.length; j++) {
    		bestdir = bestdir.rotateLeftDegrees(theta);
    		dirs[j] = bestdir;
    		if (theta>0f) {
    			theta = theta*-1f;
    		} else {
    			theta = theta*-1f + initialtheta;
    		}
    	}
    	return dirs;
	}
	
	// the harass method shoots at the best enemy
	// caveat: if another enemy is in the way of the "best enemy", 
	// it wont move towards the best enemy but will still shoot
	public void harass() throws GameActionException {
		// priority order of robots
		RobotType[] orderedBots = {RobotType.SCOUT, RobotType.GARDENER, RobotType.LUMBERJACK, RobotType.SOLDIER, RobotType.ARCHON};
		while(true) {
			RobotInfo[] iKillYou = pickRobot(orderedBots);	//orders all robot by type, then by distance
			
			if(orderedBots.length>=1) { 					// if any robots in range
				RobotInfo bestRobot = iKillYou[0];			// the best enemy
				int id = bestRobot.ID;			//the id of the best enemy
				
				System.out.println("Going to harrass a target!!");
				
				MapLocation myLoc = rc.getLocation();
				Direction bestDir = myLoc.directionTo(bestRobot.location); // direction to best enemy
				
				// shoot at enemy if no friendly fire
				if(isSingleShotClearScout(bestDir, true)) {		
					if(rc.canFireSingleShot()) {
						rc.fireSingleShot(bestDir);
					}
				} 
				// move in the direction of the best enemy if you can
				if(rc.canMove(bestDir)) {
					rc.move(bestDir);
				}
				Clock.yield();
			} else {
				return; // there are no enemies, go back to enemy archon
			}
		}
	}
	public RobotInfo[] pickRobot(RobotType[] mybots) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		// all nearby enemies
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
		
		// compare robots by type according to array taken in & distance is tiebreaker
	    Arrays.sort(nearbyRobots, new Comparator<RobotInfo>() {
	        public int compare(RobotInfo r1, RobotInfo r2) {
	        	int type1 = 0;
	        	int type2 = 0;
	        	for (int i=0; i<mybots.length; i++) {
	        		if (r1.type==mybots[i]) {
	        			type1 = i;
	        		}
	        		if(r2.type==mybots[i]) {
	        			type2 = i;
	        		}
	        	}
	        	if (type1==type2) { // the type of robot is the same
	        		return (Float.compare(myLoc.distanceTo(r1.location), myLoc.distanceTo(r2.location)));
	        	} else { // return the robot with the better type
	        		return Integer.compare(type1, type2);
	        	}	
	        }
	    });
	    for (int i=0; i<nearbyRobots.length; i++) {
	    	System.out.println(nearbyRobots[i].type);
	    	System.out.println(myLoc.distanceTo(nearbyRobots[i].location));
	    }
	    return nearbyRobots; // returns the array, best enemies at array[0], worst at array[array.length-1]
	}
	
	public void circleEnemy(MapLocation el, float circlerad) throws GameActionException {
		float theta = 2.5f/circlerad; // 48 * 7.5 = 360 degrees
		MapLocation myLocation = rc.getLocation();
		System.out.println("MOVING BRUH");
		while(true) {
			myLocation = rc.getLocation();
			Direction enemyToScout = el.directionTo(myLocation);
			enemyToScout = enemyToScout.rotateRightRads(theta);
			MapLocation newLocation = el.add(enemyToScout, circlerad);
			//make sure this is less than 2.5, but it should be
			if (rc.canMove(newLocation)) {
				System.out.println("MOVING BRUH");
				System.out.println(newLocation);
				rc.move(newLocation);
			} else {
				theta = -theta;
				System.out.println("RIP CANT MOVE THIS SHOULD NEVER HAPPEN UNLESS WE REACH THE EDGE");
			}
            Clock.yield();
		}
	}
	public float pathscout(MapLocation el) throws GameActionException {
		Set<Integer> uniqueTrees = new HashSet<Integer>();
		float scaledNumTrees = 0;
		int steps = 0;
		System.out.println("dank Memes");
		System.out.println(rc.getLocation().distanceTo(el));
		while (rc.getLocation().distanceTo(el) > 5.0f) {
			boolean hasMoved = false;
			MapLocation myLoc = rc.getLocation();
			float distance = myLoc.distanceTo(el);
			TreeInfo[] myTrees = rc.senseNearbyTrees();
			// add all the new trees
			for (TreeInfo k : myTrees) {
				if(uniqueTrees.add(k.ID)) {
					scaledNumTrees+=(k.getRadius()*k.getRadius());
				}
			}
			//broadcast tree data
			rc.broadcast(151, uniqueTrees.size());
			rc.broadcast(152, (int)scaledNumTrees);
			
			// finds and moves to closest tree with bullets in the direction of the enemy
			for (TreeInfo k : myTrees) {
				MapLocation treeLocation = k.getLocation();
				if(k.containedBullets!=0) {
					// COLLECT BULLETS
					if(rc.canShake(k.ID)) {
						rc.shake(k.ID);
					} else {
						if(treeLocation.distanceTo(el) < distance) {
							if(rc.canMove(treeLocation)) {
								rc.move(treeLocation);
								hasMoved = true;
								steps+=1;
								rc.broadcast(150, steps);
								break;
							}
						}
					}
				}
			}
			if (!hasMoved) {
				Direction toEnemy = myLoc.directionTo(el);
				if (rc.canMove(toEnemy)) {
					rc.move(toEnemy);
					steps+=1;
					rc.broadcast(150, steps);
				} else {
					return rc.getLocation().distanceTo(el);
				}
			}
			Clock.yield();
		}
		return rc.getLocation().distanceTo(el);
	}

	//scout linear move
	//copy paste standard move in robot base except deviates a certain amount of degrees while collecting bullets from trees
	//collects a crap ton of data
}
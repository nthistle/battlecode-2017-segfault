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
		private ArrayList<Direction> moves;
		// Constructor 1: When you have no moves
		private Move(MapLocation thisLoc, MapLocation endLoc, float myStride) {
			location = thisLoc;
			distanceToEnd = thisLoc.distanceTo(endLoc);
			moves = new ArrayList<Direction>();
			stride = myStride;
			totalCost = (moves.size() * stride) + (distanceToEnd*2);
		}
		// Constructor 2: When you have previous moves
		private Move(MapLocation thisLoc, MapLocation endLoc, ArrayList<Direction> myMoves, float myStride) {
			location = thisLoc;
			distanceToEnd = thisLoc.distanceTo(endLoc);
			moves = myMoves;
			stride = myStride;
			totalCost = (moves.size() * stride) + (distanceToEnd*2);
		}
		
		private void updateList(Direction d) {
			moves.add(d);
			updateCost();
		}
		private float updateCost() {
			totalCost = (moves.size() * stride) + (distanceToEnd*2);
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
		float sR = rc.getType().sensorRadius;
		float stride = 1.0f;

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
			
			//pathfinding
			Move bestMove = pathMeme(myLoc, el, myTrees, 30f, stride, sR); // A*
			//Direction myDir = pathFind(myLoc, myLoc, el, myTrees, 30f, stride, sR); // old pathfinding
			if(bestMove!=null) {
				if(rc.canMove(bestMove.moves.get(0), stride)) {
					rc.move(bestMove.moves.get(0), stride);
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
				if(isClear(newLoc, trees)) {
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
	// dfs pathfinding method
	public Direction pathFind(MapLocation startLoc, MapLocation myLoc, MapLocation endLoc, TreeInfo[] trees, float theta, float stride, float rad) throws GameActionException {
		if((startLoc.distanceTo(myLoc) + stride + 1) > rad) {
			// end case -> we've made it this far and we can't sense much further
			System.out.println("IM SO DANK");
			return startLoc.directionTo(myLoc);
		}
		Direction[] moves = getBestDirections(myLoc, endLoc, theta);
		for (int i=0; i<moves.length; i++) {
			MapLocation temploc = myLoc.add(moves[i], stride);
			rc.setIndicatorLine(myLoc, temploc, 20*i, 0, 0);
			if(isClear(temploc, trees)) {
				Direction bestDir = pathFind(startLoc, temploc, endLoc, trees, theta, stride, rad);
				if(bestDir!=null) {
					System.out.println("DANKER THAN U");
					if(startLoc!=myLoc) {
						return startLoc.directionTo(myLoc);
					} else {
						return bestDir;
					}
				}
			}
		}
		return null;
		
	}
	
	// checks for nearby trees
	public boolean isClear(MapLocation thisLoc, TreeInfo[] trees) {
		for (TreeInfo t : trees) {
			float distance = thisLoc.distanceTo(t.location);
			if(distance<(t.radius+1.0f)) {
				return false;
			}
		}
		return true;
	}
	
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
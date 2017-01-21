package segfaultplayer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

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
	
	public void getpath(MapLocation el) throws GameActionException {
		Set<Integer> uniqueTrees = new HashSet<Integer>();
		float scaledNumTrees = 0;
		int steps = 0;
		float sR = rc.getType().sensorRadius;
		float stride = 2.0f;

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
			Direction myDir = pathFind(myLoc, myLoc, el, myTrees, 30f, stride, sR);
			if(myDir!=null) {
				if(rc.canMove(myDir, stride)) {
					rc.move(myDir, stride);
					steps+=1;
					rc.broadcast(150, steps);
				}
			} else {
				System.out.println("Theres NO PATH");
				return;
			}
			Clock.yield();
		}
	}
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
	public boolean isClear(MapLocation thisLoc, TreeInfo[] trees) {
		for (TreeInfo t : trees) {
			float distance = thisLoc.distanceTo(t.location);
			if(distance<(t.radius+1.0f)) {
				return false;
			}
		}
		return true;
	}
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
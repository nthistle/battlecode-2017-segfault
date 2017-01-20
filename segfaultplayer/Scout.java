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
		float dist = pathscout(dank);
		harass();
		//circleEnemy(dank, dist);
	}
	public void harass() throws GameActionException {
		RobotType[] orderedBots = {RobotType.SCOUT, RobotType.GARDENER, RobotType.LUMBERJACK, RobotType.SOLDIER, RobotType.ARCHON};
		while(true) {
			RobotInfo[] iKillYou = pickRobot(orderedBots);
			RobotInfo bestRobot = iKillYou[0];
			int id = bestRobot.ID;
			while(bestRobot.health!=0) {
				MapLocation myLoc = rc.getLocation();
				Direction bestDir = myLoc.directionTo(bestRobot.location);
				// shoot at enemy
				if(isSingleShotClear(bestDir, true)) {
					if(rc.canFireSingleShot()) {
						rc.fireSingleShot(bestDir);
					}
				} 
				if(rc.canMove(bestDir)) {
					rc.move(bestDir); //move towards enemy
				}
				Clock.yield();
				// re-sense enemy info
				if(rc.canSenseRobot(id)) { 
					bestRobot = rc.senseRobot(id);
				}
			}
		}
	}
	public RobotInfo[] pickRobot(RobotType[] mybots) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
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
	        	if (type1==type2) {
	        		return (Float.compare(myLoc.distanceTo(r1.location), myLoc.distanceTo(r2.location)));
	        	} else {
	        		return Integer.compare(type1, type2);
	        	}	
	        }
	    });
	    for (int i=0; i<nearbyRobots.length; i++) {
	    	System.out.println(nearbyRobots[i].type);
	    	System.out.println(myLoc.distanceTo(nearbyRobots[i].location));
	    }
	    return nearbyRobots;
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
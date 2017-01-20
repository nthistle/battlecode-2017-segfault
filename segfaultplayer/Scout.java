package segfaultplayer;
import java.util.HashSet;
import java.util.Set;

import battlecode.common.*;


public strictfp class Scout extends RobotBase
{
	
	public Scout(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		MapLocation el = rc.getInitialArchonLocations(enemy)[0];
		pathscout(el);
	}
	
	public void pathscout(MapLocation el) throws GameActionException {
		Set<TreeInfo> uniqueTrees = new HashSet<TreeInfo>();
		float scaledNumTrees = 0;
		int steps = 0;
		
		while (rc.getLocation().distanceTo(el) < 1.0) {
			boolean hasMoved = false;
			MapLocation myLoc = rc.getLocation();
			float distance = myLoc.distanceTo(el);
			TreeInfo[] myTrees = rc.senseNearbyTrees();
			// add all the new trees
			for (TreeInfo k : myTrees) {
				if(uniqueTrees.add(k)) {
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
			if (!hasMoved) {
				Direction toEnemy = myLoc.directionTo(el);
				if (rc.canMove(toEnemy)) {
					rc.move(toEnemy);
					steps+=1;
					rc.broadcast(150, steps);
				}
			}
			Clock.yield();
		}
	}

	//scout linear move
	//copy paste standard move in robot base except deviates a certain amount of degrees while collecting bullets from trees
	//collects a crap ton of data
}
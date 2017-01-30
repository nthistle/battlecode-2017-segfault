package segfaultplayer;
import java.util.HashSet;
import java.util.Set;

import battlecode.common.*;


public strictfp class Scout3 extends RobotBase
{

	public Scout3(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}

	public void run() throws GameActionException {
		MapLocation dank = rc.getInitialArchonLocations(enemy)[0];
		System.out.println(dank);
		bulletPath(dank);
	}

	public void bulletPath(MapLocation el) throws GameActionException {
		Set<Integer> uniqueTrees = new HashSet<Integer>();
		float scaledNumTrees = 0;
		int steps = 0;
		float sR = rc.getType().sensorRadius; // D Y N A M I C
		float stride = rc.getType().strideRadius; // C O D E
		System.out.println(rc.getLocation().distanceTo(el));
		boolean isAtEnemy = false;
		int counter = 0;
		Direction dir = rc.getLocation().directionTo(el);
		while (true) {
			counter++;
			if(rc.senseNearbyBullets(5f).length>0) {
				moveWithDodging(el);
				Clock.yield();
			} else {
				MapLocation myLoc = rc.getLocation();
				boolean hasShaken = false;
				TreeInfo[] myTrees = rc.senseNearbyTrees(sR);
				// going towards enemy archon when it doesn't have any bullets
				/*
				if(!isAtEnemy) {
					isAtEnemy = rc.getLocation().distanceTo(el) < 10.0f;
					float distance = myLoc.distanceTo(el);
					//dir = myLoc.directionTo(el);
					// go in random directions after reaching enemy archon
				} else {
					isAtEnemy=true;
					if(counter%15==0) { // change direction
						dir = randomDirection();
						while(!rc.canMove(dir)) {
							dir = randomDirection();
						}
					}
				}
				*/
				if(counter%15==0) { // change direction
					dir = randomDirection();
					while(!rc.canMove(dir)) {
						dir = randomDirection();
					}
				}
				for (TreeInfo k : myTrees) {
					float q = k.radius;
					if(uniqueTrees.add(k.ID)) {
						scaledNumTrees += (q*q);
					}
					if(k.containedBullets!=0) {
						if(!hasShaken && rc.canShake(k.ID)){
							rc.shake(k.ID);
							hasShaken = true;
						} else {
							dir = myLoc.directionTo(k.location);
							break;
						}
					}
				}

				//broadcast tree data
				rc.broadcast(151, uniqueTrees.size());
				rc.broadcast(152, (int)scaledNumTrees);
				//move
				if(rc.canMove(dir, stride)) {
					rc.move(dir, stride);
					steps+=1;
					rc.broadcast(150, steps);
					Clock.yield();
				} else {
					Clock.yield();
				}
			}

		}
	}

}
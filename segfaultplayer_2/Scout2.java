package segfaultplayer_2;
import java.util.HashSet;
import java.util.Set;

import battlecode.common.*;

//
public strictfp class Scout2 extends RobotBase
{

	public String alphabet = "abcdefghijklmnopqrstuvwxyz";
	public float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
	public float curdirection = (float) Math.random() * 2 * (float) Math.PI;

	public Scout2(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}

	public void run() throws GameActionException {
	//	MapLocation dank = rc.getInitialArchonLocations(enemy)[0];
	//	System.out.println(dank);
	//	bulletPath(dank);
		while(true) {
			boolean hasShaked = shake();
			move();
			if (!hasShaked)
				shake();
			Clock.yield();
		}
	}

	public boolean shake() throws GameActionException {
		TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
		for(TreeInfo tree: nearbyTrees) {
			if(rc.canShake(tree.getID())) {
				rc.shake(tree.getID());
				return true;
			}
		}
		return false;
	}
	public void move() throws GameActionException {
		TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
		for(int i=0; i<nearbyTrees.length; i++) {
			TreeInfo tree = nearbyTrees[i];
			if(tree.getContainedBullets()>0) {
				if (rc.canMove(rc.getLocation().directionTo(tree.getLocation()))) {
					rc.move(rc.getLocation().directionTo(tree.getLocation()));
					rc.setIndicatorDot(tree.getLocation(),255,0,0);
					return;
				}
			}
		}
		if (Math.random() < 0.05)
			curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
		curdirection += curdiff + 2 * (float) Math.PI;
		while (curdirection > 2 * (float) Math.PI)
			curdirection -= 2 * (float) Math.PI;
		if(rc.canMove(new Direction(curdirection)))
			rc.move(new Direction(curdirection));
	}

	public void bulletPath(MapLocation el) throws GameActionException {
		Set<Integer> uniqueTrees = new HashSet<Integer>();
		float scaledNumTrees = 0;
		int steps = 0;
		float sR = rc.getType().sensorRadius;
		float stride = rc.getType().strideRadius;
		int broadcast = 3000;


		//while (rc.getLocation().distanceTo(el) > 5.0f) {
		while(true) {
			boolean hasMoved = false;
			boolean hasShaken = false;
			MapLocation myLoc = rc.getLocation();
			float distance = myLoc.distanceTo(el);
			TreeInfo[] myTrees = rc.senseNearbyTrees(sR);
			Direction dir = myLoc.directionTo(el);
			for (TreeInfo k : myTrees) {
				float q = k.radius;
				if(uniqueTrees.add(k.ID)) {
					if(k.containedRobot!=null) {
						rc.broadcast(broadcast, CommunicationsHandler.pack(k.location.x, k.location.y));
						broadcast++;
					}
					scaledNumTrees += (q*q);
				}
				if(!hasShaken && k.containedBullets!=0) {
					if(rc.canShake(k.ID)){
						rc.shake(k.ID);
						//System.out.println("Got a bullet lolololol");
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
			//pathfinding
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
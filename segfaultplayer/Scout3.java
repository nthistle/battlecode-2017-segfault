package segfaultplayer;

import battlecode.common.*;

import java.util.HashSet;
import java.util.Set;


public strictfp class Scout3 extends RobotBase
{

	public Scout3(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}

	public void run() throws GameActionException {
		while (true) {
			boolean hasShaken = shake();
			move();
			if(!hasShaken)
				shake();
			Clock.yield();
		}
	}

	public void move() throws GameActionException {
		BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
		int ctr=0;
		for(int i=0; i<nearbyBullets.length; i++) {
			if(rc.getLocation().directionTo(nearbyBullets[i].getLocation()).equals(nearbyBullets[i].getDir(),(float)(Math.PI/2.0)))
				nearbyBullets[i] = null;
			else
				ctr++;
		}
		if(ctr>0)
			moveWithDodging(enemyArchons[0]);
		else {
			TreeInfo[] trees = rc.senseNearbyTrees();
			for(int i=0; i<trees.length; i++) {
				if(trees[i].getContainedBullets()>0) {
					if(rc.canMove(rc.getLocation().directionTo(trees[i].getLocation())))
						rc.move(rc.getLocation().directionTo(trees[i].getLocation()));
					else if(rc.canMove(rc.getLocation().directionTo(trees[i].getLocation()),rc.getType().strideRadius*.5f))
						rc.move(rc.getLocation().directionTo(trees[i].getLocation()),rc.getType().strideRadius*.5f);
					else
						pathFind(trees[i].getLocation());
				}
			}
		}
	}

	public boolean shake() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees();
		for(TreeInfo tree: trees) {
			if(rc.canShake(tree.getID())) {
				rc.shake(tree.getID());
				return true;
			}
		}
		return false;
	}

}
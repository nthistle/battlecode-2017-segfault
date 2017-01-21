package segfaultplayer;
import battlecode.common.*;

public strictfp class Lumberjack extends RobotBase
{

	public Lumberjack(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}

	public void run() throws GameActionException {
		System.out.println("I'm a lumberjack!");

		float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
		float curdirection = (float) Math.random() * 2 * (float) Math.PI;

		while(true) {
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
			for(int i=0; i<nearbyTrees.length; i++) {
				if(rc.canShake(nearbyTrees[i].getID()))
					rc.shake(nearbyTrees[i].getID());
			}
			MapLocation alpha = allyArchons[0];
			for(int i=1; i<allyArchons.length; i++) {
				if(rc.getLocation().distanceTo(alpha)>rc.getLocation().distanceTo(allyArchons[i]))
					alpha = allyArchons[i];
			}
			TreeInfo nearest = null;
			for(int i=0; i<nearbyTrees.length; i++) {
				//				//if(nearest==null || alpha.distanceTo(nearest.getLocation())+enemyAlpha.distanceTo(nearest.getLocation())>alpha.distanceTo(nearbyTrees[i].getLocation())+enemyAlpha.distanceTo(nearest.getLocation()) && nearest.getContainedRobot()==null && nearbyTrees[i].getContainedRobot()==null || nearest.getContainedRobot()==null && nearbyTrees[i].getContainedRobot()!=null)

				if(nearbyTrees[i].getTeam()!=ally && (nearest==null || alpha.distanceTo(nearest.getLocation())>alpha.distanceTo(nearbyTrees[i].getLocation())))
					nearest = nearbyTrees[i];
			}
			if(nearest!=null) {
				if (rc.canChop(nearest.getID()))
					rc.chop(nearest.getID());
				else {
					Direction toTree = rc.getLocation().directionTo(nearest.getLocation());
					if (rc.canMove(toTree)) { //replace with proper move lgoic
						rc.move(toTree);
					} else if (rc.canMove(toTree.rotateRightDegrees(30.0f))) { // try to move perpendicularly, to get around obstacles
						rc.move(toTree.rotateRightDegrees(30.0f));
					} else if (rc.canMove(toTree.rotateLeftDegrees(30.0f))) {
						rc.move(toTree.rotateLeftDegrees(30.0f));
					}
					if (rc.canChop(nearest.getID()))
						rc.chop(nearest.getID());
					else {
						for(int i=0; i<nearbyTrees.length; i++) {
							if(nearbyTrees[i].getTeam()!=ally && rc.canChop(nearbyTrees[i].getID())) {
								rc.chop(nearbyTrees[i].getID());
								break;
							}
						}
					}
				}
			}
			else {
				if (Math.random() < 0.05) {
					curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
				}
				curdirection += curdiff + 2 * (float) Math.PI;
				while (curdirection > 2 * (float) Math.PI) {
					curdirection -= 2 * (float) Math.PI;
				}
				Direction d = new Direction(curdirection);
				if (rc.canMove(d)) {
					rc.move(d);
				} else {
					curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
					curdirection = (float) Math.random() * 2 * (float) Math.PI;
				}
			}

			Clock.yield();
		}
	}
}
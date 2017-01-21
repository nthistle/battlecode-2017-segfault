package segfaultplayer;
import battlecode.common.*;

import java.awt.*;


public strictfp class Soldier extends RobotBase
{
	
	public Soldier(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		while(true) {

			shoot();

			//Direction dir = randomDirection();
			//if(rc.canMove(dir))
			//	rc.move(dir);

			Clock.yield();
		}
	}

	//Does fire action
	public void shoot() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
		if(robots.length==0)
			return;
		RobotType[] priority = {RobotType.ARCHON, RobotType.SCOUT, RobotType.TANK, RobotType.SOLDIER, RobotType.GARDENER, RobotType.LUMBERJACK};
		RobotInfo target = null;
		int z = 0;
		while(target==null) {
			for (int i = 0; i < robots.length; i++) {
				if (robots[i].getType() == priority[z] && isSingleShotClear(rc.getLocation().directionTo(robots[i].getLocation()))) {
					target = robots[i];
					break;
				}
			}
			z++;
			if(z>priority.length-1)
				break;
		}
		if(target!=null) {
			Direction tDir = rc.getLocation().directionTo(target.getLocation());
			double[] vPentad = isPentadShotClear(tDir);
			double[] vTriad = isTriadShotClear(tDir);
			if (rc.canFirePentadShot() && vPentad[1]>vPentad[0] && checkPenta(target))
				rc.firePentadShot(tDir);
			else if (rc.canFireTriadShot() && vTriad[0]==0)
				rc.fireTriadShot(tDir);
			else if (rc.canFireSingleShot() && isSingleShotClear(tDir))
				rc.fireSingleShot(tDir);
		}
	}
}
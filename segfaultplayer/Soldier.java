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
		RobotInfo[] robots = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadius, enemy);
		RobotInfo target = null;
		for(int i=0; i<robots.length; i++) {
			if (robots[i].getType() == RobotType.SCOUT && isSingleShotClear(rc.getLocation().directionTo(robots[i].getLocation()))) {
				target = robots[i];
				break;
			}
		}
		if(target!=null) {
			Direction tDir = rc.getLocation().directionTo(target.getLocation());

			if (rc.canFirePentadShot() && isPentadShotClear(tDir) && checkPenta(target))
				rc.firePentadShot(tDir);
			else if (rc.canFireTriadShot() && isTriadShotClear(tDir))
				rc.fireTriadShot(tDir);
			else if (rc.canFireSingleShot() && isSingleShotClear(tDir))
				rc.fireSingleShot(tDir);
		}
	}

	public boolean checkPenta(RobotInfo target) {
		double distance = rc.getLocation().distanceTo(target.getLocation());
		boolean ret = false;
		if(target.getType()==RobotType.SCOUT && distance<4.0)
			ret = true;
		else if(target.getType()==RobotType.ARCHON && distance<5.0)
			ret = true;
		else if(target.getType()==RobotType.GARDENER && distance<5.0)
			ret = true;
		else if(target.getType()==RobotType.LUMBERJACK && distance<5.0)
			ret = true;
		else if(target.getType()==RobotType.SOLDIER && distance<5.0)
			ret = true;
		else if(target.getType()==RobotType.TANK && distance<5.0)
			ret = true;
		return ret;
	}
}
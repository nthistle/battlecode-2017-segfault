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

			Direction dir = randomDirection();
			if(rc.canMove(dir))
				rc.move(dir);

			Clock.yield();
		}
	}

	//Does fire action
	public void shoot() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadius, enemy);
		RobotInfo target = null;
		for(int i=0; i<robots.length; i++) {
			if (robots[i].getType() == RobotType.SCOUT && isWayClear(rc.getLocation().directionTo(robots[i].getLocation()))) {
				target = robots[i];
				break;
			}
		}
		if(target!=null && rc.canFireSingleShot())
			rc.fireSingleShot(rc.getLocation().directionTo(target.getLocation()));
	}
}
package segfaultplayer;
import battlecode.common.*;

import java.awt.*;


public strictfp class Gardener extends RobotBase
{
	
	public Gardener(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		boolean xd = true;
		while(true) {
			Direction dir = randomDirection();
			if(rc.canBuildRobot(RobotType.SCOUT,dir)==true && 1==0)
				rc.buildRobot(RobotType.SCOUT,dir);
			else if(rc.canBuildRobot(RobotType.SCOUT,dir) && xd) {
				rc.buildRobot(RobotType.SCOUT, dir);
				xd = false;
			}

			dir = randomDirection();
			if(rc.canMove(dir))
				rc.move(dir);

			Clock.yield();
		}
	}
}
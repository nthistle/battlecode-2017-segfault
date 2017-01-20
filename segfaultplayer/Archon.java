package segfaultplayer;
import battlecode.common.*;


public strictfp class Archon extends RobotBase
{
	
	public Archon(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		while(true) {
			Direction dir = randomDirection();
			if(rc.canBuildRobot(RobotType.GARDENER,dir) && rc.getTeamBullets()>200)
				rc.buildRobot(RobotType.GARDENER,dir);
			Clock.yield();
		}
	}
}
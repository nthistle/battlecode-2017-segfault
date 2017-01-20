package segfaultplayer;
import battlecode.common.*;


public strictfp class Soldier extends RobotBase
{
	
	public Soldier(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {

	}

	//Does fire action
	public void shoot() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots();
	}
}
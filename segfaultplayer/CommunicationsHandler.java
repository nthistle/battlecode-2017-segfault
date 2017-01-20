package segfaultplayer;
import battlecode.common.*;


/*
 * Handles advanced communications things, such as placing order queues for robots to be built,
 * as well as more complicated logging / recording / communicating
 */
public final strictfp class CommunicationsHandler
{
	private CommunicationsHandler() {
	}
	
	// add some communications stuff
	
	public int assignAlphaArchonProtocol(Archon a) throws GameActionException {
		return assignAlphaArchonProtocol(a, false);
	}
	
	public int assignAlphaArchonProtocol(Archon a, boolean debugPrint) throws GameActionException {
		// turn one, calculate distance to nearest archon
		MapLocation[] enemyStartingArchons = a.rc.getInitialArchonLocations(a.enemy);
		float myDist = RobotBase.findClosest(a.rc.getLocation(), enemyStartingArchons).distanceTo(a.rc.getLocation());
		if(debugPrint)
			System.out.println("[ID" + a.getID() + "] found my distance as " + myDist); 
		a.rc.broadcast(200 + a.getID(), (int)(100*myDist));
		Clock.yield();
		
		return 1;
	}
	
	
	public int getNumMade(RobotController rc, RobotType rt) throws GameActionException {
		int slot = RobotBase.typeToNum(rt);
		return rc.readBroadcast(100+slot);
	}
}
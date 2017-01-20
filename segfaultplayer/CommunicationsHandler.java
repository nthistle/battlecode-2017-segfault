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
		// turn one, calculate distance to nearest archon
		MapLocation enemyStartingArchons = a.rc.getInitialArchonLocations();
		
		return 1;
	}
}
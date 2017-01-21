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
	
	public static int assignAlphaArchonProtocol(Archon a) throws GameActionException {
		return assignAlphaArchonProtocol(a, false);
	}
	
	public static int assignAlphaArchonProtocol(Archon a, boolean debugPrint) throws GameActionException {
		// turn one, calculate distance to nearest archon
		MapLocation[] enemyStartingArchons = a.rc.getInitialArchonLocations(a.enemy);
		float myDist = RobotBase.findClosest(a.rc.getLocation(), enemyStartingArchons).distanceTo(a.rc.getLocation());
		if(debugPrint)
			System.out.println("[ID" + a.getID() + "] found my distance as " + myDist); 
		a.rc.broadcast(200 + a.getID(), (int)(100*myDist));
		Clock.yield();
		int numAlphas = getNumMade(a.rc, RobotType.ARCHON);
		int rank = 0;
		for(int i = 0; i < numAlphas; i ++) {
			if(i == a.getID())
				continue;
			if((100*myDist) > a.rc.readBroadcast(200 + i))
				rank ++;
		}
		if(debugPrint)
			System.out.println("[ID" + a.getID() + "] self assigned as rank " + rank);
		return rank;
	}
	
	
	public static int getNumMade(RobotController rc, RobotType rt) throws GameActionException {
		int slot = RobotBase.typeToNum(rt);
		return rc.readBroadcast(100+slot);
	}
	
	
	
	// Ordering a unit protocols
	
    public static void queueOrder(RobotController rc, RobotType rt) throws GameActionException {
    	int orderSlot = rc.readBroadcast(600); // number of orders in queue
    	int val = RobotBase.typeToNum(rt);
    	rc.broadcast(601 + orderSlot, val);
    	rc.broadcast(600, orderSlot + 1);
    	//////System.out.println("Queueing Order for " + val + ", in slot " + orderSlot);
    }
    
    public static RobotType peekOrder(RobotController rc) throws GameActionException {
    	int numOrders = rc.readBroadcast(600);
    	if(numOrders == 0)
    		return null;
    	return RobotBase.numToType(rc.readBroadcast(601));
    }
    
    public static RobotType popOrder(RobotController rc) throws GameActionException {
    	int numOrders = rc.readBroadcast(600);
    	if(numOrders == 0)
    		return null;
    	RobotType order = RobotBase.numToType(rc.readBroadcast(601));
    	// move everything over!
    	int i = 1;
    	int nextThing = rc.readBroadcast(601+i);
    	while(nextThing != 0) {
    		rc.broadcast(600+i, nextThing);
    		i++;
    		nextThing = rc.readBroadcast(601+i);
    	}
    	rc.broadcast(600, numOrders - 1);
    	//////System.out.println("Popping order for " + typeToNum(order) + ", now " + numOrders + " left");
    	return order;
    }
}
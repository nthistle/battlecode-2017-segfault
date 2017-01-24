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
		int myBlocked = a.getBlockedFactor();
		a.rc.broadcast(210 + a.getID(), myBlocked);
		a.rc.broadcast(220 + a.getID(), a.rc.getID());
		Clock.yield();
		int numAlphas = getNumMade(a.rc, RobotType.ARCHON);
		int rank = 0;
		// our rank is lower (+1) than a given other archon iff
		//  - our blocked factor is higher
		//  - OR our blocked factors are the same AND
		//        my distance is higher
		for(int i = 0; i < numAlphas; i ++) {
			if(i == a.getID())
				continue;
			if(myBlocked > a.rc.readBroadcast(210 + i)) {
				rank ++; // we're more boxed in than him, we can't be better alpha
			} else if(myBlocked == a.rc.readBroadcast(210 + i)) {
				if((100*myDist) > a.rc.readBroadcast(200 + i))
					rank ++; // we're as boxed in, but he's closer	
				else if((int)(100*myDist) == a.rc.readBroadcast(200+i)) {
					// we're really tied
					if(a.rc.getID() > a.rc.readBroadcast(220 + i))
						rank ++;
				}
			}
		}
		if(debugPrint)
			System.out.println("[ID" + a.getID() + "] self assigned as rank " + rank);
		return rank;
	}
	
	
	public static int getNumMade(RobotController rc, RobotType rt) throws GameActionException {
		int slot = RobotBase.typeToNum(rt);
		return rc.readBroadcast(100+slot);
	}
	
	
	// ========== Ordering a unit protocols ==========
	
	// 42 == tree (arbitrarily)
	private static final int TREE_VALUE = 42;
	
    public static void queueOrder(RobotController rc, Order o) throws GameActionException {
    	int orderSlot = rc.readBroadcast(600); // number of orders in queue	
    	if(o.type == OrderType.TREE) {
    		// queue a tree order
    		rc.broadcast(601 + orderSlot, TREE_VALUE);
    		rc.broadcast(600, orderSlot + 1);
    	} else {
    		// queue a robot order
    		int val = RobotBase.typeToNum(o.rt);
    		rc.broadcast(601 + orderSlot, val);
    		rc.broadcast(600, orderSlot + 1);
    	}
    	//////System.out.println("Queueing Order for " + val + ", in slot " + orderSlot);
    }
    
    public static Order peekOrder(RobotController rc) throws GameActionException {
    	int numOrders = rc.readBroadcast(600);
    	if(numOrders == 0)
    		return null;
    	int orderCode = rc.readBroadcast(601); 
    	if(orderCode == TREE_VALUE)
    		return new Order(OrderType.TREE); // tree
    	else
    		return new Order(OrderType.ROBOT, RobotBase.numToType(orderCode));
    }
    
    public static Order popOrder(RobotController rc) throws GameActionException {
    	int numOrders = rc.readBroadcast(600);
    	if(numOrders == 0)
    		return null;
    	int orderCode = rc.readBroadcast(601);
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
    	if(orderCode == TREE_VALUE)
    		return new Order(OrderType.TREE);
    	else
    		return new Order(OrderType.ROBOT, RobotBase.numToType(orderCode));
    }
    
    //
    // Brick Tree Setup Gardener Helpers
    
    public static void addTree(RobotController rc, float x, float y) throws GameActionException {
    	int curNum = rc.readBroadcast(2000);
    	// we're putting it in 2001+curNum
    	rc.broadcast(2001+curNum, pack(x,y));
    	rc.broadcast(2000, curNum + 1);
    }
    
    public static int getNumTrees(RobotController rc) throws GameActionException {
    	return rc.readBroadcast(2000);
    }
    
    public static float[][] getTreeLocations(RobotController rc) throws GameActionException {
    	int curNum = rc.readBroadcast(2000);
    	float[][] treeLocs = new float[curNum][2];
    	for(int i = 0; i < curNum; i ++) {
    		treeLocs[i] = unpack(rc.readBroadcast(2001+i));
    	}
    	return treeLocs;
    }
    
    public static int getSoldierStrategy(RobotController rc) throws GameActionException {
    	return rc.readBroadcast(2);
    }
    
    public static void setSoldierStrategy(RobotController rc, int newStrat) throws GameActionException {
    	rc.broadcast(2, newStrat);
    }
    
    // technically pack and unpack are public, but they really could (should?) be private
    
    public static int pack(float x, float y) {
		return (int)((((int)(x*10))/10.0*100000) + (int)(y*10));
	}
    
	public static float[] unpack(int p) {
		float[] ret = new float[2];
		ret[0] = ((p / 10000) / 10.0f);
		ret[1] = ((p % 10000) / 10.0f);
		return ret;
	}
}
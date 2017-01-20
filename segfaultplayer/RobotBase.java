package segfaultplayer;
import battlecode.common.*;


public strictfp abstract class RobotBase
{
	protected final RobotController rc;
	private int myID;
	final Team enemy;
	
	public RobotBase(RobotController rc) throws GameActionException {
		this.rc = rc;
		myID = -1;
		enemy = rc.getTeam().opponent();
	}
	
	public RobotBase(RobotController rc, int id) throws GameActionException {
		this.rc = rc;
		myID = id;
		enemy = rc.getTeam().opponent();
	}
	
	public abstract void run() throws GameActionException; // implemented by subclass robots

	//Srinidi: Add move with dodge.
	//Parameter: Destination
	//Moves 1 move without getting hit (dodge) towards destination as best as possible
	
	public int getID() {
		return myID;
	}
	

	// =====================================================================================
	//                                     HELPER   METHODS
	// =====================================================================================
	
	// consider moving to a static class later
    
	public static int getAndAssignNextID(RobotController rc) throws GameActionException {
		int num = typeToNum(rc.getType());
		int nextID = rc.readBroadcast(100+num);
		rc.broadcast(100+num, nextID+1);
		return nextID;
	}
	
	
    public static int typeToNum(RobotType rt) {
    	if(rt == RobotType.ARCHON)
    		return 0;
    	if(rt == RobotType.GARDENER)
    		return 1;
    	if(rt == RobotType.SCOUT)
    		return 2;
    	if(rt == RobotType.SOLDIER)
    		return 3;
    	if(rt == RobotType.LUMBERJACK)
    		return 4;
    	if(rt == RobotType.TANK)
    		return 5;
    	return -1;
    }
    
    public static RobotType numToType(int t) {
    	if(t == 0)
    		return RobotType.ARCHON;
    	if(t == 1)
    		return RobotType.GARDENER;
    	if(t == 2)
    		return RobotType.SCOUT;
    	if(t == 3)
    		return RobotType.SOLDIER;
    	if(t == 4)
    		return RobotType.LUMBERJACK;
    	if(t == 5)
    		return RobotType.TANK;
    	return null;
    }
}
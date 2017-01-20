package segfaultplayer;
import battlecode.common.*;


public strictfp abstract class RobotBase
{
	protected final RobotController rc;
	private int myID;
	public final Team enemy;
	public final Team ally;
	MapLocation[] enemyArchons;
	MapLocation[] allyArchons;

	public RobotBase(RobotController rc) throws GameActionException {
		this.rc = rc;
		myID = -1;
		enemy = rc.getTeam().opponent();
		ally = rc.getTeam();
		enemyArchons = rc.getInitialArchonLocations(enemy);
		allyArchons = rc.getInitialArchonLocations(ally);
	}
	
	public RobotBase(RobotController rc, int id) throws GameActionException {
		this.rc = rc;
		myID = id;
		enemy = rc.getTeam().opponent();
		ally = rc.getTeam();
		enemyArchons = rc.getInitialArchonLocations(enemy);
		allyArchons = rc.getInitialArchonLocations(ally);
	}
	
	public abstract void run() throws GameActionException; // implemented by subclass robots


	//Srinidi: Add move with dodge.
	//Parameter: Destination
	//Moves 1 move without getting hit (dodge) towards destination as best as possible

	//Parameters: Target direction
	//Returns true if way is clear, else false
	public boolean isSingleShotClear(Direction tDir) {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
		TreeInfo[] trees = rc.senseNearbyTrees();
		rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),0,255,255);
		for(int i=0; i<robots.length; i++) {
			Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
			Double length = (double)rc.getLocation().distanceTo(robots[i].getLocation());
			Double dist = Math.sqrt(2*length*length - 2*length*length*Math.cos(tDir.radiansBetween(fDir)));
			if(dist<robots[i].getRadius()+.1)
				return false;
		}
		for(int i=0; i<allyArchons.length; i++) {
			Direction fDir = rc.getLocation().directionTo(allyArchons[i]);
			Double length = (double)rc.getLocation().distanceTo(allyArchons[i]);
			Double dist = Math.sqrt(2*length*length - 2*length*length*Math.cos(tDir.radiansBetween(fDir)));
			if(dist<2.0+.1) //archon radius
				return false;
		}
		for(int i=0; i<trees.length; i++) {
			if(trees[i].getTeam()==ally) {
				Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
				Double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
				Double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < trees[i].getRadius()+.1)
					return false;
			}
		}
		return true;
	}

	//Parameters: Target direction
	//Returns true if triad is clear, else false
	public boolean isTriadShotClear(Direction tDir) {
		tDir = tDir.rotateRightDegrees(40.0f);
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
		TreeInfo[] trees = rc.senseNearbyTrees();
		for(int z=0; z<3; z++) {
			tDir = tDir.rotateLeftDegrees(20.0f);
			rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),255,0,255);
			for (int i = 0; i < robots.length; i++) {
				Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
				Double length = (double) rc.getLocation().distanceTo(robots[i].getLocation());
				Double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < robots[i].getRadius()+.1)
					return false;
			}
			for(int i=0; i<allyArchons.length; i++) {
				Direction fDir = rc.getLocation().directionTo(allyArchons[i]);
				Double length = (double)rc.getLocation().distanceTo(allyArchons[i]);
				Double dist = Math.sqrt(2*length*length - 2*length*length*Math.cos(tDir.radiansBetween(fDir)));
				if(dist<2.0+.1) //archon radius
					return false;
			}
			for(int i=0; i<trees.length; i++) {
				if(trees[i].getTeam()==ally) {
					Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
					Double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
					Double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
					if (dist < trees[i].getRadius()+.1)
						return false;
				}
			}
		}
		return true;
	}

	//Parameters: Target direction
	//Returns true if pentad is clear, else false
	public boolean isPentadShotClear(Direction tDir) {
		tDir = tDir.rotateRightDegrees(45.0f);
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
		TreeInfo[] trees = rc.senseNearbyTrees();
		for(int z=0; z<5; z++) {
			tDir = tDir.rotateLeftDegrees(15.0f);
			rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),255,255,0);
			for (int i = 0; i < robots.length; i++) {
					rc.setIndicatorDot(robots[i].getLocation(), 255,0,0);
				Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
				Double length = (double) rc.getLocation().distanceTo(robots[i].getLocation());
				Double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < robots[i].getRadius()+.1)
					return false;
			}
			for(int i=0; i<allyArchons.length; i++) {
					rc.setIndicatorDot(allyArchons[i], 0,0,255);
				Direction fDir = rc.getLocation().directionTo(allyArchons[i]);
				Double length = (double)rc.getLocation().distanceTo(allyArchons[i]);
				Double dist = Math.sqrt(2*length*length - 2*length*length*Math.cos(tDir.radiansBetween(fDir)));
				if(dist<2.0+.1) //archon radius
					return false;
			}
			for(int i=0; i<trees.length; i++) {
				if(trees[i].getTeam()==ally) {
						rc.setIndicatorDot(trees[i].getLocation(), 0,0,255);
					Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
					Double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
					Double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
					if (dist < trees[i].getRadius()+.1)
						return false;
				}
			}
		}
		return true;
	}
	
	
	//
	//
	//
	public RobotController getRC() {
		return rc;
	}
	
	public int getID() {
		return myID;
	}
	

	// =====================================================================================
	//                                     HELPER   METHODS
	// =====================================================================================
	
	// consider moving to a static class later

	/**
	 * Returns a random Direction
	 * @return a random Direction
	 */
	static Direction randomDirection() {
		return new Direction((float)Math.random() * 2 * (float)Math.PI);
	}
    
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
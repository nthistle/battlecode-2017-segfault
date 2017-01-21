package segfaultplayer;
import java.util.Random;

import battlecode.common.*;


public strictfp abstract class RobotBase
{
	protected final RobotController rc;
	private int myID;
	private Random rand;
	private int randSeed = 10382;
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
		rand = new Random(randSeed + rc.getID());
	}
	
	public RobotBase(RobotController rc, int id) throws GameActionException {
		this.rc = rc;
		myID = id;
		enemy = rc.getTeam().opponent();
		ally = rc.getTeam();
		enemyArchons = rc.getInitialArchonLocations(enemy);
		allyArchons = rc.getInitialArchonLocations(ally);
		rand = new Random(randSeed + rc.getID());
	}
	
	public abstract void run() throws GameActionException; // implemented by subclass robots


	//Srinidi: Add move with dodge.
	//Parameter: Destination
	//Moves 1 move without getting hit (dodge) towards destination as best as possible

	public RobotController getRC() {
		return rc;
	}
	
	public int getID() {
		return myID;
	}
	

	// =====================================================================================
	//                              INSTANCE  HELPER  METHODS
	// =====================================================================================
	
	
	// NOTE: if we ever need to cut down bytecodes, they changed the senseNearby
	// methods to return things in order of nearest, could just take the first one
	// that matches appropriate type
	
	public RobotInfo getNearest(RobotType rt, Team t) {
		RobotInfo nearest = null;
		RobotInfo[] sensed = rc.senseNearbyRobots(rc.getType().sensorRadius, t);
		for(RobotInfo ri : sensed) {
			if(ri.getType() == rt) {
				if(nearest == null)
					nearest = ri;
				else {
					if(ri.getLocation().distanceTo(rc.getLocation()) <
							nearest.getLocation().distanceTo(rc.getLocation())) {
						nearest = ri;
					}
				}
			}
		}
		return nearest;
	}
	
	
	// TODO: (big one)
	// rework checking if a shot is clear to instead assess amount of collateral damage (cost)
	// (and possibly also add one to check the possible benefit)

	public boolean isSingleShotClearScout(Direction tDir) throws GameActionException {
		return isSingleShotClearScout(tDir, false);
	}

	//Parameters: Target direction
	//Returns true if way is clear, else false
	public boolean isSingleShotClearScout(Direction tDir, boolean drawIndicators) throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
		TreeInfo[] trees = rc.senseNearbyTrees();

		if(drawIndicators)
			rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),0,255,255);

		for(int i=0; i<robots.length; i++) {
			Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
			double length = (double)rc.getLocation().distanceTo(robots[i].getLocation());
			double dist = Math.sqrt(2*length*length - 2*length*length*Math.cos(tDir.radiansBetween(fDir)));
			if(dist<robots[i].getRadius()+.1) {
				return false;
			}
		}
		for(int i=0; i<trees.length; i++) {
			Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
			double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
			double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
			if (dist < trees[i].getRadius()+.1) {
				System.out.println("thetrees");
				return false;
			}
			}
		return true;
	}

	public boolean isSingleShotClear(Direction tDir) throws GameActionException {
		return isSingleShotClear(tDir, false);
	}

	//Parameters: Target direction
	//Returns true if way is clear, else false
	public boolean isSingleShotClear(Direction tDir, boolean drawIndicators) throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
		TreeInfo[] trees = rc.senseNearbyTrees();
		
		if(drawIndicators)
			rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),0,255,255);
		
		for(int i=0; i<robots.length; i++) {
			Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
			double length = (double)rc.getLocation().distanceTo(robots[i].getLocation());
			double dist = Math.sqrt(2*length*length - 2*length*length*Math.cos(tDir.radiansBetween(fDir)));
			if(dist<robots[i].getRadius()+.1) {
				return false;
			}
		}
		for(int i=0; i<trees.length; i++) {
			if(trees[i].getTeam()==ally) {
				Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
				double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
				double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < trees[i].getRadius()+.1) {
					System.out.println("thetrees");
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean isTriadShotClear(Direction tDir) throws GameActionException {
		return isTriadShotClear(tDir, false);
	}

	//Parameters: Target direction
	//Returns true if triad is clear, else false
	public boolean isTriadShotClear(Direction tDir, boolean drawIndicators) throws GameActionException {
		tDir = tDir.rotateRightDegrees(40.0f);
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
		TreeInfo[] trees = rc.senseNearbyTrees();
		for(int z=0; z<3; z++) {
			tDir = tDir.rotateLeftDegrees(20.0f);
			
			if(drawIndicators) 
				rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),255,0,255);
			
			for (int i = 0; i < robots.length; i++) {
				Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
				double length = (double) rc.getLocation().distanceTo(robots[i].getLocation());
				double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < robots[i].getRadius()+.1)
					return false;
			}
			for(int i=0; i<allyArchons.length; i++) {
				Direction fDir = rc.getLocation().directionTo(allyArchons[i]);
				double length = (double)rc.getLocation().distanceTo(allyArchons[i]);
				double dist = Math.sqrt(2*length*length - 2*length*length*Math.cos(tDir.radiansBetween(fDir)));
				if(dist<2.0+.1) //archon radius
					System.out.println("ARCHON RIPRIPRIPR");
					return false;
			}
			for(int i=0; i<trees.length; i++) {
				if(trees[i].getTeam()==ally) {
					Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
					double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
					double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
					if (dist < trees[i].getRadius()+.1)
						return false;
				}
			}
		}
		return true;
	}
	

	public boolean isPentadShotClear(Direction tDir) throws GameActionException {
		return isPentadShotClear(tDir, false);
	}

	//Parameters: Target direction
	//Returns true if pentad is clear, else false
	public boolean isPentadShotClear(Direction tDir, boolean drawIndicators) throws GameActionException {
		tDir = tDir.rotateRightDegrees(45.0f);
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
		TreeInfo[] trees = rc.senseNearbyTrees();
		for(int z=0; z<5; z++) {
			tDir = tDir.rotateLeftDegrees(15.0f);
			
			if(drawIndicators)
				rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),255,255,0);
			
			for (int i = 0; i < robots.length; i++) {
				
				if(drawIndicators)
					rc.setIndicatorDot(robots[i].getLocation(), 255,0,0);
				
				Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
				double length = (double) rc.getLocation().distanceTo(robots[i].getLocation());
				double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < robots[i].getRadius()+.1)
					return false;
			}
			for(int i=0; i<allyArchons.length; i++) {
				if(drawIndicators)
					rc.setIndicatorDot(allyArchons[i], 0,0,255);
				Direction fDir = rc.getLocation().directionTo(allyArchons[i]);
				double length = (double)rc.getLocation().distanceTo(allyArchons[i]);
				double dist = Math.sqrt(2*length*length - 2*length*length*Math.cos(tDir.radiansBetween(fDir)));
				if(dist<2.0+.1) //archon radius
					return false;
			}
			for(int i=0; i<trees.length; i++) {
				if(trees[i].getTeam()==ally) {
					
					if(drawIndicators)
						rc.setIndicatorDot(trees[i].getLocation(), 0,0,255);
					
					Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
					double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
					double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
					if (dist < trees[i].getRadius()+.1)
						return false;
				}
			}
		}
		return true;
	}

	//checks if close enough for pentad shot
	public boolean checkPenta(RobotInfo target) {
		double distance = rc.getLocation().distanceTo(target.getLocation());
		boolean ret = false;
		if(target.getType()==RobotType.SCOUT && distance<6.0)
			ret = true;
		else if(target.getType()==RobotType.ARCHON && distance<7.0)
			ret = true;
		else if(target.getType()==RobotType.GARDENER && distance<7.0)
			ret = true;
		else if(target.getType()==RobotType.LUMBERJACK && distance<7.0)
			ret = true;
		else if(target.getType()==RobotType.SOLDIER && distance<7.0)
			ret = true;
		else if(target.getType()==RobotType.TANK && distance<7.0)
			ret = true;
		return ret;
	}

	

	// =====================================================================================
	//                              STATIC  HELPER  METHODS
	// =====================================================================================
	
	// consider moving to a static class later
	
	public static MapLocation findClosest(MapLocation to, MapLocation[] poss) {
		if(poss.length == 0)
			return null;
		MapLocation closest = poss[0];
		for(int i = 1; i < poss.length; i ++) {
			if(poss[i].distanceSquaredTo(to) < closest.distanceSquaredTo(closest))
				closest = poss[i];
		}
		return closest;
	}
	
	public static Direction randomDirection() {
		return new Direction(rand.nextFloat() * 2 * (float)Math.PI);
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
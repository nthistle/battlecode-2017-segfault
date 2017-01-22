package segfaultplayer;
import java.util.Random;

import battlecode.common.*;


public strictfp abstract class RobotBase
{
	protected final RobotController rc;
	private int myID;
	private static final int randSeed = 10383;
	public static Random rand = new Random(randSeed);
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


	public void checkVPWin() throws GameActionException {
		int vpNeeded = GameConstants.VICTORY_POINTS_TO_WIN - rc.getTeamVictoryPoints();
		if(vpNeeded*rc.getVictoryPointCost() < rc.getTeamBullets()) {
			System.out.println("Hey guys I think we can win");
			System.out.println("Watch this");
			rc.donate(rc.getTeamBullets());
		}
	}
	
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

	
	public void setIndicatorPlus(MapLocation ml, int r, int g, int b) throws GameActionException {
		rc.setIndicatorLine(ml.add(new Direction(0),1.0f),
				ml.add(new Direction((float)Math.PI),1.0f), r, g, b);
		rc.setIndicatorLine(ml.add(new Direction((float)Math.PI/2.0f),1.0f),
				ml.add(new Direction(3.0f*(float)Math.PI/2.0f),1.0f), r, g, b);
	}
	
	public void setIndicatorX(MapLocation ml, int r, int g, int b) throws GameActionException {
		rc.setIndicatorLine(ml.add(new Direction((float)Math.PI/4.0f),1.0f),
				ml.add(new Direction(5.0f*(float)Math.PI/4.0f),1.0f), r, g, b);
		rc.setIndicatorLine(ml.add(new Direction(3.0f*(float)Math.PI/4.0f),1.0f),
				ml.add(new Direction(7.0f*(float)Math.PI/4.0f),1.0f), r, g, b);
	}
	
	public float getDist(float x1, float y1, float x2, float y2) {
		float dx = x2-x1;
		float dy = y2-y1;
		return (float)Math.sqrt(dx*dx+dy*dy); 
	}
	
	
	public boolean moveTowards(MapLocation cur, MapLocation goal) throws GameActionException {
		return moveTowards(cur, goal, (float)Math.PI/16.0f, 8);
	}
	
	public Direction moveInDir(Direction ideal) throws GameActionException {
		return moveInDir(ideal, (float)Math.PI/16.0f, 8);
	}
	
	/**
	 * Trys to move from the current location towards the given goal location (just used for direction)
	 * while slowly tweaking angle in either direction, according to offset and max
	 * 
	 * @param cur current location you're at
	 * @param goal goal location you want to get to
	 * @param offset the increment to attempt directions in, maximum offset used is offset*max
	 * @param max maximum number of "offset"s away you are willing to try to move
	 * @return whether or not this was successfully able to move
	 * @throws GameActionException
	 */
	public boolean moveTowards(MapLocation cur, MapLocation goal, float offset, int max) throws GameActionException {
		Direction ideal = cur.directionTo(goal);
		if(rc.canMove(ideal)) {
			rc.move(ideal);
			return true;
		} else {
			return moveInDir(ideal, offset, max) != null;
		}
	}
	
	public Direction moveInDir(Direction ideal, float offset, int max) throws GameActionException {
		Direction dir;
		for(int i = 1; i < max; i ++) {
			dir = ideal.rotateRightRads(offset * i);
			if(rc.canMove(dir)) {
				rc.move(dir);
				return dir;
			}
			dir = ideal.rotateLeftRads(offset * i);
			if(rc.canMove(dir)) {
				rc.move(dir);
				return dir;
			}
		}
		return null;
		// unable to move
	}
	
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

	public double[] isTriadShotClear(Direction tDir) throws GameActionException {
		return isTriadShotClear(tDir, false);
	}

	//Parameters: Target direction
	//Returns length 2 array of doubles, first being friendly damage and second being enemy damage
	public double[] isTriadShotClear(Direction tDir, boolean drawIndicators) throws GameActionException {
		double[] ret = {0.0,0.0};
		tDir = tDir.rotateRightDegrees(40.0f);
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius);
		TreeInfo[] trees = rc.senseNearbyTrees();
		for(int z=0; z<3; z++) {
			tDir = tDir.rotateLeftDegrees(20.0f);

			if(drawIndicators)
				rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),255,255,0);

			for (int i = 0; i < robots.length; i++) {
				if(drawIndicators)
					rc.setIndicatorDot(robots[i].getLocation(), 255,0,0);
				Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
				double length = (double) rc.getLocation().distanceTo(robots[i].getLocation());
				double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < robots[i].getRadius()+.1) {
					double score = 2.0;
					if(robots[i].getType()==RobotType.ARCHON)
						score+=4.0;
					if(robots[i].getTeam()==rc.getTeam())
						ret[0]+=score;
					else
						ret[1]+=score;
				}
			}
			for(int i=0; i<trees.length; i++) {
				if(drawIndicators)
					rc.setIndicatorDot(trees[i].getLocation(), 0,0,255);
				Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
				double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
				double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < trees[i].getRadius()+.1) {
					double score=1.0;
					if(trees[i].getTeam()==rc.getTeam())
						ret[0]+=score;
					else
						ret[1]+=score;
				}
			}
		}
		return ret;
	}


	public double[] isPentadShotClear(Direction tDir) throws GameActionException {
		return isPentadShotClear(tDir, false);
	}

	//Parameters: Target direction
	//Returns length 2 array of doubles, first being friendly damage and second being enemy damage
	public double[] isPentadShotClear(Direction tDir, boolean drawIndicators) throws GameActionException {
		double[] ret = {0.0,0.0};
		tDir = tDir.rotateRightDegrees(45.0f);
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius);
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
				if (dist < robots[i].getRadius()+.1) {
					double score = 2.0;
					if(robots[i].getType()==RobotType.ARCHON)
						score+=4.0;
					if(robots[i].getTeam()==rc.getTeam())
						ret[0]+=score;
					else
						ret[1]+=score;
				}
			}
			for(int i=0; i<trees.length; i++) {
				if(drawIndicators)
					rc.setIndicatorDot(trees[i].getLocation(), 0,0,255);
				Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
				double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
				double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < trees[i].getRadius()+.1) {
					double score=1.0;
					if(trees[i].getTeam()==rc.getTeam())
						ret[0]+=score;
					else
						ret[1]+=score;
				}
			}
		}
		return ret;
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
	
	public static Direction averageDirection(Direction a, Direction b) {
		float adeg = a.radians;
		float bdeg = b.radians;
		if(bdeg > adeg) {
			float tmp = adeg;
			adeg = bdeg;
			bdeg = tmp;
		}
		if((adeg - bdeg) > (float)Math.PI) {
			bdeg += (float)Math.PI;
		}
		// just return the average
		float avg = (adeg + bdeg)/2.0f;
		if(avg > 2.0f*(float)Math.PI)
			avg -= 2.0f*(float)Math.PI;
		return new Direction(avg);
	}
	
    public static Direction[] getDirections(Direction startDir, float theta) throws GameActionException {
    	float initialtheta = theta;
    	Direction[] dirs = new Direction [(int)(360.0f/theta)];
    	Direction bestdir = startDir;
    	dirs[0] = bestdir;
    	for (int j=1; j<dirs.length; j++) {
    		bestdir = bestdir.rotateLeftDegrees(theta);
    		dirs[j] = bestdir;
    	}
    	return dirs;
	}
	
	public static MapLocation findClosest(MapLocation to, MapLocation[] poss) {
		if(poss.length == 0)
			return null;
		MapLocation closest = poss[0];
		for(int i = 1; i < poss.length; i ++) {
			if(poss[i].distanceSquaredTo(to) < closest.distanceSquaredTo(to))
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
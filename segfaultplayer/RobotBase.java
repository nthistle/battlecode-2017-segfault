package segfaultplayer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import battlecode.common.*;


public strictfp abstract class RobotBase
{
	private static final int randSeed = 10385;
	public static Random rand = new Random(randSeed);
	
	protected final RobotController rc;
	
	public final Team enemy;
	public final Team ally;
	
	public MapLocation[] enemyArchons;
	public MapLocation[] allyArchons;
	
	public MapLocation marker  = null;
	public MapLocation marker2 = null; // what are these?
	
	public int firstTurn;
	
	private int myID;

	public RobotBase(RobotController rc) throws GameActionException {
		this.rc = rc;
		myID = -1;
		enemy = rc.getTeam().opponent();
		ally = rc.getTeam();
		firstTurn = rc.getRoundNum();
		setAndSortArchons();
	}
	
	public RobotBase(RobotController rc, int id) throws GameActionException {
		this.rc = rc;
		myID = id;
		enemy = rc.getTeam().opponent();
		ally = rc.getTeam();
		firstTurn = rc.getRoundNum();
		setAndSortArchons();
	}
	
	/**
	 * Sets enemyArchons and allyArchons correspondingly, and sorts them according to this robot's
	 * current location (starting location, since called once upon spawn)
	 * Actually uses bubble sort
	 */
	private void setAndSortArchons() {
		enemyArchons = rc.getInitialArchonLocations(enemy);
		for(int i=0; i<enemyArchons.length; i++) {
			for(int z=0; z<enemyArchons.length; z++) {
				if(rc.getLocation().distanceTo(enemyArchons[i])<rc.getLocation().distanceTo(enemyArchons[z])) {
					MapLocation temp = enemyArchons[i];
					enemyArchons[i] = enemyArchons[z];
					enemyArchons[z] = temp;
				}
			}
		}
		allyArchons = rc.getInitialArchonLocations(ally);
		for(int i=0; i<allyArchons.length; i++) {
			for(int z=0; z<allyArchons.length; z++) {
				if(rc.getLocation().distanceTo(allyArchons[i])<rc.getLocation().distanceTo(allyArchons[z])) {
					MapLocation temp = allyArchons[i];
					allyArchons[i] = allyArchons[z];
					allyArchons[z] = temp;
				}
			}
		}
	}
	
	
	/**
	 * Implemented by subclass robots, is called by handler methods in RobotPlayer.java
	 * @throws GameActionException
	 */
	public abstract void run() throws GameActionException;

	
	/**
	 * Checks if our current bullet stockpile is enough to win the game on victory points, and
	 * if it is, attempts to cash in and win.
	 * @throws GameActionException
	 */
	public void checkVPWin() throws GameActionException {
		int vpNeeded = GameConstants.VICTORY_POINTS_TO_WIN - rc.getTeamVictoryPoints();
		if(vpNeeded*rc.getVictoryPointCost() < rc.getTeamBullets()) {
			rc.donate(rc.getTeamBullets());
		}
		if(rc.getRoundNum()>rc.getRoundLimit()-5)
			rc.donate(rc.getTeamBullets());
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

	/**
	 * Gets number of turns this robot has been alive
	 * @return number of turns this robot has been alive
	 */
	public int getLifespan() {
		return rc.getRoundNum()-firstTurn;
	}
	
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
				if(trees[i].getTeam()==ally) {
					if (drawIndicators)
						rc.setIndicatorDot(trees[i].getLocation(), 0, 0, 255);
					Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
					double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
					double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
					if (dist < trees[i].getRadius() + .1) {
						double score = 1.0;
						if (trees[i].getTeam() == rc.getTeam())
							ret[0] += score;
						else
							ret[1] += score;
					}
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
				if(trees[i].getTeam()==ally) {
					if (drawIndicators)
						rc.setIndicatorDot(trees[i].getLocation(), 0, 0, 255);
					Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
					double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
					double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
					if (dist < trees[i].getRadius() + .1) {
						double score = 1.0;
						if (trees[i].getTeam() == rc.getTeam())
							ret[0] += score;
						else
							ret[1] += score;
					}
				}
			}
		}
		return ret;
	}

	//very important! rc.canMove(dir) is always true for tanks bc they roll trees over. This sees if the tree in question is friendly so it can move around them and not steamroll
	public boolean canTankMove(MapLocation ml) {
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius,ally);
		for(int i=0; i<trees.length; i++) {
			if(ml.distanceTo(trees[i].getLocation())<trees[i].getRadius()+rc.getType().bodyRadius)
				return false;
			//System.out.println(ml.distanceTo(trees[i].getLocation())+" "+trees[i].getRadius());
		}
		return true;
	}

	public float degreesToRadians(double angle) {
		return (float)(angle/180.0*Math.PI);
	}

	// =========================================================
	// ======================= DODGING =========================
	// =========================================================
	
	public void moveWithDodging(MapLocation ml) throws GameActionException{
		moveWithDodging(ml, false);
	}

	//moves arbitrarily with dodging, if all bullets are non-threatening moves using without dodging
	public void moveWithDodging(MapLocation ml, boolean debug) throws GameActionException {
		Direction goal = rc.getLocation().directionTo(ml);
		BulletInfo[] nearbyBullets = rc.senseNearbyBullets(5.0f);
		int ctr=0;
		for(int i=0; i<nearbyBullets.length; i++) {
			if(rc.getLocation().directionTo(nearbyBullets[i].getLocation()).equals(nearbyBullets[i].getDir(),(float)(Math.PI/2.0)))
				nearbyBullets[i] = null;
			else
				ctr++;
		}
		if(ctr==0) {
			pathFind(ml);
			return;
		}
		BulletInfo[] bi = new BulletInfo[ctr];
		ctr=0;
		for(int i=0; i<nearbyBullets.length; i++) {
			if(nearbyBullets[i]!=null) {
				bi[ctr] = nearbyBullets[i];
				ctr++;
			}
		}
		for(int i=0; i<25; i++) {
			MapLocation mapLocation = rc.getLocation().add(randomDirection(),(float)(.5+(Math.random()*.5)*rc.getType().strideRadius));
			boolean clear = true;
			for(BulletInfo bullet: bi) {
				if(mapLocation.distanceTo(bullet.getLocation().add(bullet.getDir(),bullet.getSpeed()))<rc.getType().bodyRadius+.05
						&& mapLocation.distanceTo(bullet.getLocation().add(bullet.getDir(),(float)(bullet.getSpeed()*.5)))<rc.getType().bodyRadius+.05) {
					clear = false;
					break;
				}
			}
			if(clear) {
				if (rc.canMove(mapLocation)) {
					rc.move(mapLocation);
					System.out.println("i: "+i);
					break;
				}
			}
		}
	}

	public void moveWithDodgingScout(MapLocation ml) throws GameActionException{
		moveWithDodging(ml, false);
	}

	//moves arbitrarily with dodging, if all bullets are non-threatening moves using without dodging
	public void moveWithDodgingScout(MapLocation ml, boolean debug) throws GameActionException {
		Direction goal = rc.getLocation().directionTo(ml);
		BulletInfo[] nearbyBullets = rc.senseNearbyBullets(5.0f);
		int ctr=0;
		for(int i=0; i<nearbyBullets.length; i++) {
			if(rc.getLocation().directionTo(nearbyBullets[i].getLocation()).equals(nearbyBullets[i].getDir(),(float)(Math.PI/2.0)))
				nearbyBullets[i] = null;
			else
				ctr++;
		}
		if(ctr==0) {
			pathFind(ml);
			return;
		}
		BulletInfo[] bi = new BulletInfo[ctr];
		ctr=0;
		for(int i=0; i<nearbyBullets.length; i++) {
			if(nearbyBullets[i]!=null) {
				bi[ctr] = nearbyBullets[i];
				ctr++;
			}
		}
		MapLocation[] moves = new MapLocation[25];
		float dir = 0.0f;
		for(int i=0; i<25; i++) {
			MapLocation mapLocation = rc.getLocation().add(randomDirection(),(float)(.5+(Math.random()*.5)*rc.getType().strideRadius));
			boolean clear = true;
			for(BulletInfo bullet: bi) {
				if(mapLocation.distanceTo(bullet.getLocation().add(bullet.getDir(),bullet.getSpeed()))<rc.getType().bodyRadius+.05
						&& mapLocation.distanceTo(bullet.getLocation().add(bullet.getDir(),(float)(bullet.getSpeed()*.5)))<rc.getType().bodyRadius+.05) {
					clear = false;
					break;
				}
			}
			if(clear)
				moves[i] = mapLocation;
			else
				moves[i] = null;
		}
		for(BulletInfo bullet: bi) {
			dir+=bullet.getDir().radians;
		}
		dir = dir / bi.length*1.0f;
		Direction dodgeDir = new Direction(dir);
		MapLocation move = moves[0];
		for(int i=1; i<25; i++) {
			if(move.distanceTo(rc.getLocation().add(dodgeDir,rc.getType().strideRadius)) > move.distanceTo(rc.getLocation().add(dodgeDir,rc.getType().strideRadius)))
				move = moves[i];
		}
		if(rc.canMove(move))
			rc.move(move);
	}
	

	// =========================================================
	// ==================== PATHFINDING ========================
	// =========================================================
	public void pathFind(MapLocation endLoc) throws GameActionException {
		float stride = (float)(rc.getType().strideRadius);
		MapLocation myLoc = rc.getLocation();
		updateRatio(myLoc);
		Direction toEnd = myLoc.directionTo(endLoc);
		Direction[] myDirs = getDirections(Direction.getNorth(), 30f); // not pointed towards enemy archon so it can move straight up and down
		float[][] hyperMeme = new float[myDirs.length][2];
		
		for(int i=0; i<myDirs.length; i++) {
			if(rc.canMove(myDirs[i], stride)) {
				hyperMeme[i][0] = i;
				MapLocation newLoc = myLoc.add(myDirs[i], stride);
				hyperMeme[i][1] = newLoc.distanceTo(endLoc);
				//System.out.print(hyperMeme[i][1] + " ");
				if(marker!=null) {
					hyperMeme[i][1] -= newLoc.distanceTo(marker)*1.6;//(1.6+2*getLifespan()/rc.getRoundLimit()); //*1.6;
					rc.setIndicatorLine(myLoc, marker, 255, 0, 0);
					//System.out.print(newLoc.distanceTo(marker) + " ");
				}
				if(marker2!=null) {
					hyperMeme[i][1] -= newLoc.distanceTo(marker2)*.4;//(.4+2*getLifespan()/rc.getRoundLimit()); //*.4;
					rc.setIndicatorLine(myLoc, marker, 255, 0, 0);
					//System.out.println(newLoc.distanceTo(marker2) + " ");
				}
				rc.setIndicatorLine(myLoc,newLoc, 0, (int)hyperMeme[i][1]*5, 0);
			} else {
				// theres something in the way
				hyperMeme[i][0] = i;
				hyperMeme[i][1] = Float.MAX_VALUE;
			}
		}
		System.out.println("Boost: "+6*getLifespan()/(1.0*rc.getRoundLimit()));
		// sort the array
		java.util.Arrays.sort(hyperMeme, new java.util.Comparator<float[]>() {
			public int compare(float[] a, float[] b) {
				return Float.compare(a[1], b[1]); // sort by heuristic if damage is equal (lower = closer to goal)
			}
		});
		//update markers
		marker2 = marker;
		marker = myLoc;
		
		//actually move
		for (int j=0; j<myDirs.length; j++) {
			if(rc.canMove(myDirs[(int)hyperMeme[j][0]])) {
				rc.move(myDirs[(int)hyperMeme[j][0]]);
				break;
			}
		}
	}
	
	/**
	 * Determines gardener unit vs. tree production ratio (for phase 2 of hex gardener)
	 * based on the closest we've reached to an enemy archon.
	 * Specifically, ratio is [# of trees]:[# of units]
	 * When low, we want lots of units, when high, we want to prioritize finishing pods
	 * @return ratio of number of trees planted to number of units built, ideally
	 * @throws GameActionException
	 */
	public float getFloatRatio() throws GameActionException {
		return 2.1f; // some weird stuff was happening, so for debugging
		// and because I'm not sure how well this method works, I just made
		// this return 2.1
		
		/*int realRatio = rc.readBroadcast(11);
		return (float)realRatio / 1000.0f;*/
	}
	
	/**
	 * Updates the ratio for gardener unit vs. tree production as a function of how close
	 * to enemy archon we've made it.
	 * If the furthest we've reached is mid-way between enemy archon and our archon, there's
	 * most likely a path due to symmetry, and we want more soldiers to rush down that path.
	 * @param myLocation
	 * @throws GameActionException
	 */
	public void updateRatio(MapLocation myLocation) throws GameActionException {
		//large number = lots of trees, small number = lots of troops
		//# of Archons * (Distance between Archons / 100f) * (1 / (P(x) + .1))
		//P(x) is 1 when at enemy, 0 at friendly robot
		//Big x means P(x) is 1, Small X is means P(x) is 0
		double distance = allyArchons[0].distanceTo(enemyArchons[0]);
		double myScore = allyArchons.length * distance / 100.0 * ( 1.0 / ( 1 + Math.exp( -rc.getLocation().distanceTo(allyArchons[0]) + distance/2.0) ) + .1 );

		int myRatio = (int)(myScore*1000);
		int realRatio = rc.readBroadcast(11);

		if(myRatio>realRatio) {
			rc.broadcast(11,myRatio ); //myRatio
		}
	}
	// =====================================================================================
	//                              OLD METHODS (Useless?)
	// =====================================================================================

	public void moveWithoutDodging(Direction goal) throws GameActionException {
		moveWithoutDodging(goal, false);
	}

	//Parameters: Intended movement direction, 15 degree intervals
	//Moves robot as best possible
	public void moveWithoutDodging(Direction goal, boolean debug) throws GameActionException {
		if(rc.canMove(goal) && canTankMove(rc.getLocation().add(goal,rc.getType().strideRadius)) ) { //ideal move
			rc.move(goal);
			if(debug)
				System.out.println("Move straight");
			return;
		}
		for(int i=1; i<13; i++) { //fans out, adding fixed degrees right and left on 7.5 intervals. Neal's code performs better!!!! idk why, look at it. This gets stuck
			Direction copyRight = (new Direction(degreesToRadians(goal.getAngleDegrees()))).rotateRightDegrees((float)(i*15));
			Direction copyLeft = (new Direction(degreesToRadians(goal.getAngleDegrees()))).rotateLeftDegrees((float)(i*15));
			if(rc.canMove(copyRight) && canTankMove(rc.getLocation().add(copyRight,rc.getType().strideRadius)) ) {
				rc.move(copyRight);
				if(debug)
					System.out.println("Move Right");
				return;
			}
			else if(rc.canMove(copyLeft) && canTankMove(rc.getLocation().add(copyLeft,rc.getType().strideRadius))) {
				rc.move(copyLeft);
				if(debug)
					System.out.println("Move Right");
				return;
			}
		}
		if(debug)
			System.out.println("NO move");
	}

	// =====================================================================================
	//                              STATIC  HELPER  METHODS
	// =====================================================================================
	
	// consider moving to a static class later
	
	//fans out directions from initial direction by delta theta
	// returns array of directions best arr[0] to worst arr[arr.len-1]
    public static Direction[] getBestDirections(Direction bestDir, float theta) throws GameActionException {
    	float initialtheta = theta;
    	Direction[] dirs = new Direction [(int)(360.0f/theta)];
    	dirs[0] = bestDir;
    	for (int j=1; j<dirs.length; j++) {
    		bestDir = bestDir.rotateLeftDegrees(theta);
    		dirs[j] = bestDir;
    		if (theta>0f) {
    			theta = theta*-1f;
    		} else {
    			theta = theta*-1f + initialtheta;
    		}
    	}
    	return dirs;
	}
    

    public static Direction[] getBestDirections2(Direction bestDir, float theta) throws GameActionException {
    	float initialtheta = theta;
    	Direction[] dirs = new Direction [(int)(360.0f/theta)];
    	dirs[0] = bestDir;
    	for (int j=1; j<dirs.length; j++) {
    		bestDir = bestDir.rotateLeftDegrees(theta);
    		dirs[j] = bestDir;
    	}
    	return dirs;
	}

	public static Direction[] getBestDirectionsMihir(Direction bestDir, float theta) throws GameActionException {
		float initialtheta = theta;
		Direction[] dirs = new Direction [(int)(360.0f/theta)];
		dirs[0] = bestDir;
		for (int j=1; j<dirs.length; j++) {
			dirs[j] = bestDir.rotateLeftDegrees(theta);
			if(j!=dirs.length-1) {
				dirs[j] = bestDir.rotateRightDegrees(theta);
				theta += initialtheta;
			}
		}
		return dirs;
	}
    
	
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

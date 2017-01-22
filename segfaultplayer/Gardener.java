package segfaultplayer;
import battlecode.common.*;

import java.awt.*;


public strictfp class Gardener extends RobotBase
{
	public static final float MAJOR_AXIS_CRAD = 76.5f; // eventually int, or do millirads 
	public static final float SPACING_DISTANCE = 4.5f;
	
	public Gardener(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		
		
		/*
		addToGrid();
		addToGrid();
		while(true) {
			//if(rc.getRoundNum() % 50 == 0)
				addToGrid();
			//else
			//	stepCircleRoutine();
			Clock.yield();
		}
		*/


		//TESTING CODE: Comment in for testing stuff
		while(true) {
			TreeInfo[] trees = rc.senseNearbyTrees(2.0f,rc.getTeam());
			Direction dir = randomDirection();
			if(rc.canBuildRobot(RobotType.LUMBERJACK,dir)) // was tank
				rc.buildRobot(RobotType.LUMBERJACK,dir);
			else if(rc.canPlantTree(dir) && trees.length<2)
				rc.plantTree(dir);
			dir = randomDirection();
			TreeInfo tree = null;
			for(int i=0; i<trees.length; i++)
				if(tree==null || tree.getHealth()>trees[i].getHealth())
					tree = trees[i];
			if(tree!=null && rc.canWater(tree.getID()))
				rc.water(tree.getID());
			Clock.yield();
		}

		/* OLD MEME
		else {
			RobotInfo nearestArchon = getNearest(RobotType.ARCHON, ally);
			Direction dir = nearestArchon.getLocation().directionTo(rc.getLocation());
			for (int i = 0; i < 20 && !rc.canMove(dir); i++)
				dir = randomDirection();
			rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(dir, 5.0f), 255, 0, 0);
			for (int i = 0; i < 25; i++) {
				if (rc.canMove(dir))
					rc.move(dir);
				Clock.yield();
			}
			makeHexPod();
			lifetimeWaterLowest();
		}*/
	}
	
	public void addToGrid() throws GameActionException {
		int ntrees = CommunicationsHandler.getNumTrees(rc);
		if(ntrees == 0) {
			// this is a whole nother case bud
			return; // TODO: make it plant the initial tree
			
		} else {
			float[][] trees = CommunicationsHandler.getTreeLocations(rc);
			float withinDist = SPACING_DISTANCE * (float)Math.sqrt(2.0);
			float[] current = trees[0];
			MapLocation myLoc = rc.getLocation();
			
			// draw blue + where we think are trees, for visualization purposes
			for(float[] t : trees) {
				//setIndicatorPlus(new MapLocation(t[0],t[1]), 0, 0, 255);
				//rc.setIndicatorDot(new MapLocation(t[0],t[1]), 0, 0, 255);
			}
			
			for(int i = 1; i < trees.length; i ++) {
				if(getDist(trees[i][0], trees[i][1], myLoc.x, myLoc.y) <
						getDist(current[0], current[1], myLoc.x, myLoc.y)) {
					current = trees[i];
				}
			}
			rc.setIndicatorDot(new MapLocation(current[0],current[1]), 255, 0, 0);
			// red means the nearest planted tree, now we extrapolate closer
			MapLocation[] neighbors;
			MapLocation best;
			

			neighbors = getNeighborTreeLocs(new MapLocation(current[0],current[1]));
			// this is basically findClosest here, but with a stipulation that it has to
			// satisfy cellHelperIsValid, which checks to make sure there's not already a tree
			// there (more efficient than going through float[][] trees), and that it's on
			// the map (for now)
			best = null;
			for(int i = 0; i < neighbors.length; i ++) {
				if(best == null) {
					if(cellHelperIsValid(neighbors[i]))
						best = neighbors[i];
				}
				else if(neighbors[i].distanceSquaredTo(myLoc) < best.distanceSquaredTo(myLoc)) {
					if(cellHelperIsValid(neighbors[i]))
						best = neighbors[i];
				}
			}
			
			if(best == null) {
				best = new MapLocation(current[0],current[1]);
			}
			
			current[0] = best.x;
			current[1] = best.y;

			//rc.setIndicatorDot(best, 0, 255, 0); // green means extrapolation point
			
			//System.out.println("Current dist is " + getDist(current[0],current[1],myLoc.x,myLoc.y));
			while(getDist(current[0],current[1],myLoc.x,myLoc.y) > withinDist) {
				//System.out.println(" -> Current dist is " + getDist(current[0],current[1],myLoc.x,myLoc.y));
				
				
				neighbors = getNeighborTreeLocs(new MapLocation(current[0],current[1]));
				

				// this is basically findClosest here, but with a stipulation that it has to
				// satisfy cellHelperIsValid, which checks to make sure there's not already a tree
				// there (more efficient than going through float[][] trees), and that it's on
				// the map (for now)
				best = null;
				for(int i = 0; i < neighbors.length; i ++) {
					if(best == null) {
						if(cellHelperIsValid(neighbors[i]))
							best = neighbors[i];
					}
					else if(neighbors[i].distanceSquaredTo(myLoc) < best.distanceSquaredTo(myLoc)) {
						if(cellHelperIsValid(neighbors[i]))
							best = neighbors[i];
					}
				}

				//best = RobotBase.findClosest(myLoc, neighbors);
				
				
				rc.setIndicatorDot(best, 0, 255, 0); // green means extrapolation point
				current[0] = best.x;
				current[1] = best.y;
			}

			if(best == null) {
				best = new MapLocation(current[0],current[1]);
			}


			rc.setIndicatorLine(myLoc, best, 255, 255, 0);
			
			// alright, now let's move towards this point, capped at 20 turns of movement (give up)
			for(int mt = 0; mt < 20 && myLoc.distanceTo(best) > 2.5f; mt ++) {
				//rc.setIndicatorDot(best, 0, 0, 0);
				myLoc = rc.getLocation();
				rc.setIndicatorLine(myLoc, best, 255, 255, 0);
				if(!moveTowards(myLoc, best))
					moveTowards(best, myLoc); // attempt to reverse 1 step
				waterLowest();
				Clock.yield();
			}
			
			//setIndicatorX(best, 0, 0, 0);
			
			myLoc = rc.getLocation();
			
			// hopefully we're within 2.5 units of best now
			// we want to be exactly 2.0 units away
			if(myLoc.distanceTo(best) > 2.0f) {
				if(rc.canMove(myLoc.directionTo(best), myLoc.distanceTo(best)-2.0f))
					rc.move(myLoc.directionTo(best), myLoc.distanceTo(best)-2.0f);
			}
			Clock.yield();
			// hopefully now we're exactly 2.0 units away
			myLoc = rc.getLocation();
			if(myLoc.distanceTo(best) < 2.2f) { // 0.2 is tolerable
				for(int mt = 0; mt < 30 && rc.getTeamBullets() < 50.0f; mt ++) { // wait up to 30 rounds
					waterLowest();
					Clock.yield();
					// waterLowest might not hit anything, but better than completely stalling
				}
				myLoc = rc.getLocation();
				if(rc.getTeamBullets() >= 50.0f) {
					// whoopdiedoo, build
					if(rc.canPlantTree(myLoc.directionTo(best))) {
						rc.plantTree(myLoc.directionTo(best));
						System.out.println("Building a tree, extending stuff");
						// even though 0.2 is fine, we don't want it to propagate, so we're broadcasting
						// the ideal coords of this tree
						CommunicationsHandler.addTree(rc, best.x, best.y);
					} else {
						// nice waste of time, TODO: blacklist this point or something,
						// so we don't try to build here again (for a while?)
					}
				} else return;
			}
			
		}
	}
	
	private boolean cellHelperIsValid(MapLocation ml) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		if(ml.distanceTo(myLoc) < rc.getType().sensorRadius) {
			return rc.onTheMap(ml) && rc.senseNearbyTrees(ml,1.0f,null).length == 0 && rc.senseNearbyRobots(ml, 1.0f, null).length == 0;
		} else {
			return true;
		}
	}
	
	
	public MapLocation[] getNeighborTreeLocs(MapLocation m) {
		
		Direction[] dirs = new Direction[4];
		dirs[0] = new Direction(MAJOR_AXIS_CRAD/100.0f);
		dirs[1] = new Direction(((float)Math.PI/2.0f) + MAJOR_AXIS_CRAD/100.0f);
		dirs[2] = new Direction(((float)Math.PI) + MAJOR_AXIS_CRAD/100.0f);
		dirs[3] = new Direction((3.0f*(float)Math.PI/2.0f) + MAJOR_AXIS_CRAD/100.0f);
		
		MapLocation[] neighbors = new MapLocation[8];
		// 1 off neighbors
		neighbors[0] = m.add(dirs[0], SPACING_DISTANCE);
		neighbors[1] = m.add(dirs[1], SPACING_DISTANCE);
		neighbors[2] = m.add(dirs[2], SPACING_DISTANCE);
		neighbors[3] = m.add(dirs[3], SPACING_DISTANCE);

		// in-between neighbors (SPACING_DISTANCE * sqrt(2) away)
		neighbors[4] = neighbors[0].add(dirs[1], SPACING_DISTANCE);
		neighbors[5] = neighbors[1].add(dirs[2], SPACING_DISTANCE);
		neighbors[6] = neighbors[2].add(dirs[3], SPACING_DISTANCE);
		neighbors[7] = neighbors[3].add(dirs[0], SPACING_DISTANCE);
		
		return neighbors;
	}
	
	/**
	 * Waters the lowest until the end of time
	 * At some point, this should be replaced (or wherever it's used) with more
	 * dynamic code that enables a gardener to run away if it's in danger etc.
	 */
	public void lifetimeWaterLowest() throws GameActionException {
		while(true) {
			waterLowest();
			Clock.yield();
		}
	}
	
	/**
	 * Creates a full hex pod wherever it is when called, blocking in all 6 sides,
	 * uses up bullets as soon as it can get them (50+)
	 */
	public void makeHexPod() throws GameActionException {
		float smallAngleInc = (float)Math.PI/16.0f;
		float treeOffsetAngle = (float)Math.PI/3.0f;
		while(rc.getTeamBullets() < RobotType.GARDENER.bulletCost) {
			waterLowest();
			Clock.yield();
		}
		// now we can afford a gardener
		float cAngle = 0.0f;
		while(cAngle < 2.0*(float)Math.PI && !rc.canPlantTree(new Direction(cAngle)))
			cAngle += smallAngleInc;
		if(!rc.canPlantTree(new Direction(cAngle)))
			return; // hex pod failed
		boolean[] planted = new boolean[] {true, false, false, false, false, false};
		rc.plantTree(new Direction(cAngle));
		waterLowest();
		Clock.yield();
		for(int i = 0; i < 5; i ++) {
			System.out.println("Trying " + i);
			for(int j = 0; j < 10 || rc.getTeamBullets() < 50.0f; j ++) {
				waterLowest();
				Clock.yield();
			}
			rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(new Direction(cAngle + (i+1) * treeOffsetAngle), 2.0f), 0, 255, 0);
			System.out.println("Now we have enough bullets!");
			if(rc.canPlantTree(new Direction(cAngle + (i+1) * treeOffsetAngle))) {
				planted[i+1] = true;
				rc.plantTree(new Direction(cAngle + (i+1) * treeOffsetAngle));
				waterLowest();
				Clock.yield();
			} else {System.out.println("I can't plant though"); }
		}
	}
	
	
    private void waterLowest() throws GameActionException {
		TreeInfo[] myTrees = rc.senseNearbyTrees(2.0f, rc.getTeam());

		if (myTrees.length > 0) { // Waters lowest
			double hp = 100.0;
			int water = 0;
			for (int i = 0; i < myTrees.length; i++) {
				if ((double) myTrees[i].getHealth() < hp) {
					hp = (double) myTrees[i].getHealth();
					water = i;
				}
			}
			rc.water(myTrees[water].getID());
		}
    }
    
    
}

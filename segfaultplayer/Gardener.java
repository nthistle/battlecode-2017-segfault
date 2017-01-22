package segfaultplayer;
import battlecode.common.*;

import java.awt.*;


public strictfp class Gardener extends RobotBase
{
	public static final float MAJOR_AXIS_CRAD = 76.5f; // eventually int, or do millirads 
	public static final float SPACING_DISTANCE = 3.5f;
	
	public Gardener(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {

		boolean testOtherStuff = true;

		if(testOtherStuff) {
			while(true) {
				Direction dir = randomDirection();
				if(rc.canBuildRobot(RobotType.LUMBERJACK,dir))
					rc.buildRobot(RobotType.LUMBERJACK,dir);
				if(rc.canBuildRobot(RobotType.SCOUT,dir))
					rc.buildRobot(RobotType.SCOUT,dir);
				Clock.yield();
			}
		}
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
		}
	}
	
	public void addToGrid() throws GameActionException {
		int ntrees = CommunicationsHandler.getNumTrees(rc);
		if(ntrees == 0) {
			// this is a whole nother case bud
		} else {
			float[][] trees = CommunicationsHandler.getTreeLocations(rc);
			float withinDist = SPACING_DISTANCE * (float)Math.sqrt(2.0);
			float[] current = trees[0];
			MapLocation myLoc = rc.getLocation();
			for(int i = 1; i < trees.length; i ++) {
				if(getDist(trees[i][0], trees[i][1], myLoc.x, myLoc.y) <
						getDist(current[0], current[1], myLoc.x, myLoc.y)) {
					current = trees[i];
				}
			}
			rc.setIndicatorDot(new MapLocation(current[0],current[1]), 255, 0, 0);
		}
	}
	
	
	public MapLocation[] getNeighborTreeLocs(MapLocation m) {
		MapLocation[] neighbors = new MapLocation[8];
		// 1 off neighbors
		neighbors[0] = m.add(new Direction(1*MAJOR_AXIS_CRAD/100.0f), SPACING_DISTANCE);
		neighbors[1] = m.add(new Direction(2*MAJOR_AXIS_CRAD/100.0f), SPACING_DISTANCE);
		neighbors[2] = m.add(new Direction(3*MAJOR_AXIS_CRAD/100.0f), SPACING_DISTANCE);
		neighbors[3] = m.add(new Direction(4*MAJOR_AXIS_CRAD/100.0f), SPACING_DISTANCE);

		// in-between neighbors (SPACING_DISTANCE * sqrt(2) away)
		neighbors[4] = neighbors[0].add(new Direction(2*MAJOR_AXIS_CRAD/100.0f), SPACING_DISTANCE);
		neighbors[5] = neighbors[1].add(new Direction(3*MAJOR_AXIS_CRAD/100.0f), SPACING_DISTANCE);
		neighbors[6] = neighbors[2].add(new Direction(4*MAJOR_AXIS_CRAD/100.0f), SPACING_DISTANCE);
		neighbors[7] = neighbors[3].add(new Direction(1*MAJOR_AXIS_CRAD/100.0f), SPACING_DISTANCE);
		
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
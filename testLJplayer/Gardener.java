package testLJplayer;
import battlecode.common.*;

import java.awt.*;


public strictfp class Gardener extends RobotBase
{
	
	public Gardener(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		
		boolean xd = true;
		while(true) {
			Direction dir = randomDirection();
			if(rc.canBuildRobot(RobotType.SCOUT,dir)==true && 1==0)
				rc.buildRobot(RobotType.SCOUT,dir);
			else if(rc.canBuildRobot(RobotType.SCOUT,dir) && xd && 1==0) {
				rc.buildRobot(RobotType.SCOUT, dir);
				xd = false;
			}
			else if(rc.canBuildRobot(RobotType.TANK,dir)) {
				rc.buildRobot(RobotType.TANK,dir);
			}
			else if(rc.canPlantTree(dir)) {
				rc.plantTree(dir);
			}

			TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
			for(int i=0; i<nearbyTrees.length; i++) {
				if(nearbyTrees[i].getTeam()==rc.getTeam()) {
					if (rc.canWater(nearbyTrees[i].getID())) {
						rc.water(nearbyTrees[i].getID());
						break;
					}
				}
			}

			dir = randomDirection();
			if(rc.canMove(dir))
				rc.move(dir);

			Clock.yield();
		}
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
package segfaultplayer;
import battlecode.common.*;

import java.awt.*;


public strictfp class Gardener extends RobotBase
{
	
	public Gardener(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		/*boolean xd = true;
		while(true) {
			Direction dir = randomDirection();
			if(rc.canBuildRobot(RobotType.SCOUT,dir)==true && 1==0)
				rc.buildRobot(RobotType.SCOUT,dir);
			else if(rc.canBuildRobot(RobotType.SCOUT,dir) && xd && 1==0) {
				rc.buildRobot(RobotType.SCOUT, dir);
				xd = false;
			}
			else if(rc.canBuildRobot(RobotType.LUMBERJACK,dir)) {
				rc.buildRobot(RobotType.LUMBERJACK,dir);
			}

			dir = randomDirection();
			if(rc.canMove(dir))
				rc.move(dir);

			Clock.yield();
		}*/
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
			while(rc.getTeamBullets() < RobotType.GARDENER.bulletCost) {
				waterLowest();
				Clock.yield();
			}
			if(rc.canPlantTree(new Direction(cAngle + (i+1) * treeOffsetAngle))) {
				planted[i+1] = true;
				rc.plantTree(new Direction(cAngle + (i+1) * treeOffsetAngle));
				waterLowest();
				Clock.yield();
			}
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
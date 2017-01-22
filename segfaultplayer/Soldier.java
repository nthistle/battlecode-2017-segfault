package segfaultplayer;
import battlecode.common.*;

import java.awt.*;


public strictfp class Soldier extends RobotBase
{
	
	public Soldier(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		while(true) {

			shoot();

			//Direction dir = randomDirection();
			//if(rc.canMove(dir))
			//	rc.move(dir);

			Clock.yield();
		}
	}

	//Does fire action
	public void shoot() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
		if(robots.length==0)
			return;
		RobotType[] priority = {RobotType.ARCHON, RobotType.SCOUT, RobotType.TANK, RobotType.SOLDIER, RobotType.GARDENER, RobotType.LUMBERJACK};
		RobotInfo target = null;
		int z = 0;
		while(target==null) {
			for (int i = 0; i < robots.length; i++) {
				if (robots[i].getType() == priority[z] && isSingleShotClear(rc.getLocation().directionTo(robots[i].getLocation()))) {
					target = robots[i];
					break;
				}
			}
			z++;
			if(z>priority.length-1)
				break;
		}
		if(target!=null) {
			Direction tDir = rc.getLocation().directionTo(target.getLocation());
			double[] vPentad = isPentadShotClear(tDir);
			double[] vTriad = isTriadShotClear(tDir);
			if (rc.canFirePentadShot() && vPentad[1]>vPentad[0] && checkPenta(target))
				rc.firePentadShot(tDir);
			else if (rc.canFireTriadShot() && vTriad[0]==0)
				rc.fireTriadShot(tDir);
			else if (rc.canFireSingleShot() && isSingleShotClear(tDir))
				rc.fireSingleShot(tDir);
		}
	}

	public void move(Direction goal) throws GameActionException {
		double[] scores = new double[359];
		Direction[] moves = new Direction[359];

		if(rc.canMove(goal))
			scores[0] = predict(rc.getLocation().add(goal,rc.getType().strideRadius));
		else
			scores[0] = Double.MAX_VALUE;
		moves[0] = goal;

		for(int i=1; i<180; i++) {
			Direction copyRight = (new Direction(degreesToRadians(goal.getAngleDegrees()))).rotateRightDegrees((float)i);
			Direction copyLeft = (new Direction(degreesToRadians(goal.getAngleDegrees()))).rotateLeftDegrees((float)i);
			moves[i*2-1] = copyRight;
			if(rc.canMove(copyRight))
				scores[i*2-1] = predict(rc.getLocation().add(copyRight,rc.getType().strideRadius));
			else
				scores[i*2-1] = Double.MAX_VALUE;
			moves[i*2] = copyLeft;
			if(rc.canMove(copyLeft))
				scores[i*2] = predict(rc.getLocation().add(copyLeft,rc.getType().strideRadius));
			else
				scores[i*2] =  Double.MAX_VALUE;
		}
		int ideal = 0;
		for(int i=0; i<scores.length; i++)
			if(scores[ideal]>scores[i])
				ideal = i;
		if(scores[ideal] != Double.MAX_VALUE)
			if(rc.canMove(moves[ideal]))
				rc.move(moves[ideal]);
	}

	public float degreesToRadians(double angle) {
		return (float)(angle/180.0*Math.PI);
	}

	public double predict(MapLocation ml) {
		return -1.0;
	}
}
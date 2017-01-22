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

	public void move(Direction goal) {
		double[] scores = new double[360];
		double degrees = goal.getAngleDegrees();
		if(rc.canMove(goal))
			scores[0] = predict(rc.getLocation().add(goal,rc.getType().strideRadius));
		else
			scores[0] = 999999.0;
		for(int i=0; i<359; i++) {
			if(i%2==1)
				degrees*=-1;
			else
				degrees=Math.abs(degrees)+1;
			if(rc.canMove(new Direction(degreesToRadians(degrees))))
				scores[i] = predict(rc.getLocation().add(new Direction(degreesToRadians(degrees)),rc.getType().strideRadius));
			else
				scores[i] = -1.0;
		}
		int ideal = 0;
		for(int i=0; i<scores.length; i++)
			if(scores[ideal]>scores[i])
				ideal = i;
	}

	public float degreesToRadians(double angle) {
		return (float)(angle/180.0*Math.PI);
	}

	public double predict(MapLocation ml) {
		return -1.0;
	}
}
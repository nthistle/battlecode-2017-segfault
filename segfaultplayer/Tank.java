package segfaultplayer;
import battlecode.common.*;

public strictfp class Tank extends RobotBase
{
	
	public Tank(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}

	public void run() throws GameActionException {
		int ctr = 0;
		while(true) {
			boolean attack = true;
			if(attack) {
				Direction goal = rc.getLocation().directionTo(enemyArchons[ctr]);
				moveWithoutDodging(goal);
			}
			if(rc.getLocation().distanceTo(enemyArchons[ctr])<7 && isArchonDead())
				ctr++;
			shoot();
			Clock.yield();
		}
	}

	public boolean isArchonDead() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy);
		for(int i=0; i<robots.length; i++)
			if(robots[i].getType() == RobotType.ARCHON)
				return true;
		return false;
	}

	//Does fire action
	public void shoot() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
			TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius);
		if(robots.length==0) {
			if(trees.length>0 && trees[0].getTeam()!=ally) {
				Direction tDir = rc.getLocation().directionTo(trees[0].getLocation());
				if (rc.canFirePentadShot()) {
					rc.firePentadShot(tDir);
				}
				else if (rc.canFireTriadShot())
					rc.fireTriadShot(tDir);
				else if (rc.canFireSingleShot())
					rc.fireSingleShot(tDir);
			}
			return;
		}
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
}
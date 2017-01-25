package segfaultplayer;
import battlecode.common.*;

public strictfp class Soldier extends RobotBase
{
	
	public Soldier(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}

	public void runAlt() throws GameActionException {
		while(true) {
			moveWithDodging(rc.getLocation().directionTo(enemyArchons[0]), true);
			if(rc.hasMoved()==false && rc.canMove(rc.getLocation().directionTo(enemyArchons[0])))
				rc.move(rc.getLocation().directionTo(enemyArchons[0]));
			RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
			if(rc.canFireSingleShot() && robots.length>0)
				rc.fireSingleShot(rc.getLocation().directionTo(robots[0].getLocation()));
			Clock.yield();
		}
	}
	
	public void run() throws GameActionException {
		int ctr = 0;
		int steps = 0;
		float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
		float curdirection = (float) Math.random() * 2 * (float) Math.PI;
		int turnsAlive = 0;
		
		float[] alph = CommunicationsHandler.unpack(rc.readBroadcast(1));
		MapLocation alphaLoc = new MapLocation(alph[0], alph[1]);
		
		while(true) {
			checkVPWin();
			turnsAlive++;
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
			for(int i=0; i<nearbyTrees.length; i++) {
				if(nearbyTrees[i].getContainedBullets() > 0 && rc.canShake(nearbyTrees[i].getID())) {
					rc.shake(nearbyTrees[i].getID());
					break;
				}
			}
			
			
			// SOLDIER ADDITION I'M TRYING SO THEY DON'T CLOG MUH TREES
			boolean attack = (CommunicationsHandler.getSoldierStrategy(rc) == SoldierStrategy.BLITZ);
			if(!attack && turnsAlive < 30) {
				// if we're at the beginning of a "sentry/patrol" lifestyle,
				// MOVE THE HELL AWAY A LITTLE BIT WHY DON'T YOU
				//Direction goalDir = alphaLoc.directionTo(rc.getLocation());
				moveTowards(alphaLoc, rc.getLocation(), (float)Math.PI/8, 5);
			}
			
			BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
			if(attack) {// || steps<15) {    // temporarily removing this condition because I want my own anti-clog
				if(ctr>=enemyArchons.length) {
					if (Math.random() < 0.05) {
						curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
					}
					curdirection += curdiff + 2 * (float) Math.PI;
					while (curdirection > 2 * (float) Math.PI) {
						curdirection -= 2 * (float) Math.PI;
					}
					Direction d = new Direction(curdirection);
					RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy);
					if(robots.length>0)
						d = rc.getLocation().directionTo(robots[0].getLocation());
					if(nearbyBullets.length>0)
						moveWithDodging(d); //movewWithoutDodging
					else
						moveWithoutDodging(d);
				}
				else {
					Direction goal = rc.getLocation().directionTo(enemyArchons[ctr]);
					if(nearbyBullets.length>0)
						moveWithDodging(goal); //movewWithoutDodging
					else
						moveWithoutDodging(goal);
				}
				steps++;
			}
			if(ctr<enemyArchons.length && rc.getLocation().distanceTo(enemyArchons[ctr])<4 && isArchonDead())
				ctr++;
			if(ctr>=enemyArchons.length)
				shoot(null);
			else
				shoot(rc.getLocation().directionTo(enemyArchons[ctr]));
			Clock.yield();
		}
	}

	public boolean isArchonDead() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy);
		for(int i=0; i<robots.length; i++)
			if(robots[i].getType() == RobotType.ARCHON)
				return false;
		return true;
	}

	//Does fire action
	public void shoot(Direction goal) throws GameActionException {
		try {
			RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
			TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius);
			if (robots.length == 0) {
				if(trees.length>0 && trees[0].getTeam()!=ally) {
					Direction tDir = rc.getLocation().directionTo(trees[0].getLocation());
					if(goal!=null && tDir.equals(goal,(float)(Math.PI/4.0)) && rc.getLocation().distanceTo(trees[0].getLocation())<3.0) {
						if (rc.canFireTriadShot())
							rc.fireTriadShot(tDir);
						else if (rc.canFireSingleShot())
							rc.fireSingleShot(tDir);
					}
				}
				return;
			}
			RobotType[] priority = {RobotType.SOLDIER, RobotType.TANK, RobotType.GARDENER, RobotType.LUMBERJACK, RobotType.ARCHON, RobotType.SCOUT};
			RobotInfo target = null;
			int z = 0;
			while (target == null) {
				for (int i = 0; i < robots.length; i++) {
					if (robots[i].getType() == priority[z] && isSingleShotClear(rc.getLocation().directionTo(robots[i].getLocation()))) {
						target = robots[i];
						break;
					}
				}
				z++;
				if (z > priority.length - 1)
					break;
			}
			if (target != null) {
				Direction tDir = rc.getLocation().directionTo(target.getLocation());
				double[] vPentad = isPentadShotClear(tDir);
				double[] vTriad = isTriadShotClear(tDir);
				if (rc.canFirePentadShot() && vPentad[1] > vPentad[0])
					rc.firePentadShot(tDir);
				else if (rc.canFireTriadShot() && vTriad[0] == 0)
					rc.fireTriadShot(tDir);
				else if (rc.canFireSingleShot() && isSingleShotClear(tDir))
					rc.fireSingleShot(tDir);
			}
		} catch(Exception e) {
			System.out.println("Shooting error");
			setIndicatorPlus(rc.getLocation(),255,0,0);
		}
	}
}
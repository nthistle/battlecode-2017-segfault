package segfaultplayer;
import battlecode.common.*;

public strictfp class Lumberjack extends RobotBase
{

	public Lumberjack(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}

	public void run() throws GameActionException {
		System.out.println("I'm a lumberjack!");

		int scoutTempt = 0;
		
		int turnsSinceLastChop = 0;

		float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
		float curdirection = (float) Math.random() * 2 * (float) Math.PI;

		while(true) {
			boolean hasMoved = false;
			checkVPWin();
			RobotInfo[] nearby = rc.senseNearbyRobots(4.0f, enemy);
			RobotInfo closestEnemyScout = null;
			for(int i = 0; i < nearby.length; i ++) {
				if(nearby[i].getType() == RobotType.SCOUT) {
					closestEnemyScout = nearby[i];
					break;
				}
			}
			if(closestEnemyScout != null) {
				if(scoutTempt < 25) {
					moveTowards(closestEnemyScout.getLocation(), rc.getLocation());
					hasMoved = true;
					scoutTempt ++;
				}
			} else {
				scoutTempt = 0;
			}
			
			
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
			for(int i=0; i<nearbyTrees.length; i++) {
				if(rc.canShake(nearbyTrees[i].getID())) {
					rc.shake(nearbyTrees[i].getID());
					break;
				}
			}
			MapLocation alpha = allyArchons[0];
			for(int i=1; i<allyArchons.length; i++) {
				if(rc.getLocation().distanceTo(alpha)>rc.getLocation().distanceTo(allyArchons[i]))
					alpha = allyArchons[i];
			}
			MapLocation enemyAlpha = enemyArchons[0];
			for(int i=1; i<enemyArchons.length; i++) {
				if(rc.getLocation().distanceTo(enemyAlpha)>rc.getLocation().distanceTo(enemyArchons[i]))
					enemyAlpha = enemyArchons[i];
			}
			TreeInfo nearest = null;
			for(int i=0; i<nearbyTrees.length; i++) {
				//TODO: Prioritze contained units?
				//if(nearbyTrees[i].getTeam()!=ally && (nearest==null || alpha.distanceTo(nearest.getLocation())>alpha.distanceTo(nearbyTrees[i].getLocation())))
				////if(nearest!=null)
				////	System.out.println(alpha.distanceTo(nearest.getLocation())+" "+enemyAlpha.distanceTo(nearest.getLocation())+" "+alpha.distanceTo(nearbyTrees[i].getLocation())+" "+enemyAlpha.distanceTo(nearbyTrees[i].getLocation()));
				if(nearbyTrees[i].getTeam()!=ally && (nearest==null ||
						1.5*alpha.distanceTo(nearest.getLocation())+enemyAlpha.distanceTo(nearest.getLocation()) > 
				1.5*alpha.distanceTo(nearbyTrees[i].getLocation())+enemyAlpha.distanceTo(nearbyTrees[i].getLocation())))
					nearest = nearbyTrees[i];
			}
			if(rc.senseNearbyRobots(2.5f, enemy).length > 0) {
				if(rc.canStrike())
					rc.strike();
			} else if(nearest!=null) {
				if (rc.canChop(nearest.getID())) {
					rc.chop(nearest.getID());
					turnsSinceLastChop = 0;
				}
				else {
					Direction toTree = rc.getLocation().directionTo(nearest.getLocation());
					if(!hasMoved)
						moveWithoutDodging(toTree);
					if (rc.canChop(nearest.getID())) {
						rc.chop(nearest.getID());
						turnsSinceLastChop = 0;
					}
					else {
						for(int i=0; i<nearbyTrees.length; i++) {
							if(nearbyTrees[i].getTeam()!=ally && rc.canChop(nearbyTrees[i].getID())) {
								rc.chop(nearbyTrees[i].getID());
								turnsSinceLastChop = 0;
								break;
							}
						}
						if(rc.canStrike()) {
							strike();
							turnsSinceLastChop++;
						}
					}
				}
			}
			else {
				if (Math.random() < 0.05) {
					curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
				}
				curdirection += curdiff + 2 * (float) Math.PI;
				while (curdirection > 2 * (float) Math.PI) {
					curdirection -= 2 * (float) Math.PI;
				}
				Direction d = new Direction(curdirection);
				if(!hasMoved)
					moveWithoutDodging(d);
//				if (rc.canMove(d)) {
//					rc.move(d);
//				} else {
//					curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
//					curdirection = (float) Math.random() * 2 * (float) Math.PI;
//				}
				if(rc.canStrike()) {
					strike();
					turnsSinceLastChop++;
				}
			}

			Clock.yield();
		}
	}

	public void strike() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS);
		TreeInfo[] trees = rc.senseNearbyTrees(GameConstants.LUMBERJACK_STRIKE_RADIUS);
		double[] score = {0.0,0.0};
		for(int i=0; i<robots.length; i++) {
			double value = 2.0;
			if(robots[i].getType()==RobotType.ARCHON)
				value+=4.0;
			if(robots[i].getTeam()==ally)
				score[0]+=value;
			else
				score[1]+=value;
		}
		for(int i=0; i<trees.length; i++) {
			double value = 2.0;
			if(trees[i].getTeam()==ally)
				score[0]+=value;
			else
				score[1]+=value;
		}
		if(score[0]<score[1])
			if(rc.canStrike())
				rc.strike();
	}
	
	public void setOutOfWork() throws GameActionException {
		// do nothing yet
	}
	
	public void setBackToWork() throws GameActionException {
		// do nothing yet
	}
}
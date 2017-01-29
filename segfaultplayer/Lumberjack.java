package segfaultplayer;
import battlecode.common.*;

public strictfp class Lumberjack extends RobotBase
{
	public float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
	public float curdirection = (float) Math.random() * 2 * (float) Math.PI;

	public Lumberjack(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}

	public void run() throws GameActionException {
		while (true) {
			try {
				dailyTasks();
				TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
				if(!strike(true)) { //tries attacking first
					TreeInfo target = getTarget(nearbyTrees,true); //finds best tree
					if (target == null) //if no trees, random move
						randomMove();
					else {
						boolean struckTarget = tryChoppingTarget(target); //tries chopping best tree
						if (!struckTarget) {
							moveWithoutDodging(rc.getLocation().directionTo(target.getLocation())); //moves if couldnt chop best tree
							struckTarget = tryChoppingTarget(target); //tries chopping best tree post-mvoe
							if (!struckTarget) //chops any nearby trees if still hasn't chopped
								chopAnyTree(nearbyTrees,true);
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
				System.out.println("Lumberjack Error");
			}
			Clock.yield();
		}
	}

	//daily non-movement/shooting tasks
	public void dailyTasks() throws  GameActionException {
		checkVPWin(); //check if can win game on VPs
		TreeInfo[] nearbyTrees = rc.senseNearbyTrees(); //shake nearby bullet trees
		for(int i=0; i<nearbyTrees.length; i++) {
			if(nearbyTrees[i].getContainedBullets() > 0 && rc.canShake(nearbyTrees[i].getID())) {
				rc.shake(nearbyTrees[i].getID());
				break;
			}
		}
	}

	//random movement in case of no trees nearby
	public void randomMove() throws GameActionException {
		if (Math.random() < 0.05)
			curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
		curdirection += curdiff + 2 * (float) Math.PI;
		while (curdirection > 2 * (float) Math.PI)
			curdirection -= 2 * (float) Math.PI;
		if(rc.canMove(new Direction(curdirection)))
			rc.move(new Direction(curdirection));
	}

	public void chopAnyTree(TreeInfo[] nearbyTrees) throws GameActionException {
		chopAnyTree(nearbyTrees,false);
	}

	//chops any nearby tree (closest)
	public void chopAnyTree(TreeInfo[] nearbyTrees, boolean debug) throws GameActionException {
		for (int i = 0; i < nearbyTrees.length; i++) {
			if (rc.canChop(nearbyTrees[i].getLocation()) && nearbyTrees[i].getTeam()!=ally) {
				rc.setIndicatorDot(nearbyTrees[i].getLocation(),0,180,0);
				rc.chop(nearbyTrees[i].getLocation());
				return;
			}
		}
	}

	//chops tree in question if possible
	//returns true if chopped, else false
	public boolean tryChoppingTarget(TreeInfo target) throws GameActionException {
		if(rc.canChop(target.getLocation())) {
			rc.chop(target.getLocation());
			return true;
		}
		return false;
	}

	public TreeInfo getTarget(TreeInfo[] nearbyTrees) throws GameActionException {
		return getTarget(nearbyTrees, false);
	}

	//finds ideal tree
	public TreeInfo getTarget(TreeInfo[] nearbyTrees, boolean debug) throws GameActionException {
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
		TreeInfo target = null;
		for(int i=0; i<nearbyTrees.length; i++) {
			if(nearbyTrees[i].getTeam()!=ally && (target==null || treeHeuristic(target,nearbyTrees[i],alpha,enemyAlpha)))
				target = nearbyTrees[i];
		}
		if(debug && target!=null)
			rc.setIndicatorDot(target.getLocation(),0,255,0);
		return target;
	}

	//prioritizes ideal tree using radius, distance, and contained units
	public boolean treeHeuristic(TreeInfo target, TreeInfo test, MapLocation alpha, MapLocation enemyAlpha) {
		TreeInfo[] input = {target,test};
		double[] scores = new double[2];
		for(int i=0; i<2; i++) {
			scores[i] = 1.5*alpha.distanceTo(input[i].getLocation())+enemyAlpha.distanceTo(input[i].getLocation())+rc.getLocation().distanceTo(input[i].getLocation());
			scores[i] *= (input[i].getRadius()+8.0)/(4.0+8.0);
			if(input[i].getContainedRobot()!=null) {
				switch (input[i].getContainedRobot()) {
					case TANK:
						scores[i] *= .75;
						break;
					case SOLDIER:
						scores[i] *= .775;
						break;
					case GARDENER:
						scores[i] *= .8;
						break;
					case LUMBERJACK:
						scores[i] *= .85;
						break;
					case SCOUT:
						scores[i] *= .875;
						break;
					default:
						break;
				}
			}

		}
		return scores[1]<scores[0];
	}


	public boolean strike() throws GameActionException {
		return strike(false);
	}

	//strikes if it does more damage to enemy than friendly
	//returns true if it it attacked, else false
	public boolean strike(boolean debug) throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS);
		TreeInfo[] trees = rc.senseNearbyTrees(GameConstants.LUMBERJACK_STRIKE_RADIUS);
		double[] score = {0.0,0.0};
		for(int i=0; i<robots.length; i++) {
			double value = 8.0;
			if(robots[i].getType()==RobotType.ARCHON)
				value+=10.0;
			if(robots[i].getTeam()==ally)
				score[0]+=value;
			else
				score[1]+=value;
		}
		for(int i=0; i<trees.length; i++) {
			double value = 2.0;
			if(trees[i].getTeam()==ally)
				score[0]+=value;
			else if(trees[i].getTeam()==enemy)
				score[1]+=value;
			else
				score[1]+=value*.5;
		}
		if(rc.canStrike() && score[1]>score[0] && score[1]>3.0) {
			if(debug) {
				Direction start = new Direction(0.0f);
				for (int i = 0; i < 8; i++) {
					rc.setIndicatorDot(rc.getLocation().add(start,GameConstants.LUMBERJACK_STRIKE_RADIUS),255,0,0);
					start = start.rotateRightRads((float)(Math.PI/4.0));
				}
			}
			rc.strike();
			return true;
		}
		return false;
	}
}
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
		try {
			while (true) {
				dailyTasks();
				TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
				if(!strike()) { //tries attacking first
					TreeInfo target = getTarget(nearbyTrees); //finds best tree
					if (target == null) //if no trees, random move
						randomMove();
					else {
						boolean struckTarget = tryChoppingTarget(target); //tries chopping best tree
						if (!struckTarget) {
							moveWithoutDodging(rc.getLocation().directionTo(target.getLocation())); //moves if couldnt chop best tree
							struckTarget = tryChoppingTarget(target); //tries chopping best tree post-mvoe
							if (!struckTarget) //chops any nearby trees if still hasn't chopped
								chopAnyTree(nearbyTrees);
						}
					}
				}
				Clock.yield();
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Lumberjack Error");
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

	//chops any nearby tree (closest)
	public void chopAnyTree(TreeInfo[] nearbyTrees) throws GameActionException {
		for(int i=0; i<nearbyTrees.length; i++) {
			if(rc.canChop(nearbyTrees[i].getLocation())) {
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

	//prioritizes ideal tree
	public TreeInfo getTarget(TreeInfo[] nearbyTrees) throws GameActionException {
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
			//TODO: Prioritze contained units? Prioritize nearest to me?
			if(nearbyTrees[i].getTeam()!=ally &&
					(target==null || 1.5*alpha.distanceTo(target.getLocation())+enemyAlpha.distanceTo(target.getLocation())
							> 1.5*alpha.distanceTo(nearbyTrees[i].getLocation())+enemyAlpha.distanceTo(nearbyTrees[i].getLocation())))
				target = nearbyTrees[i];
		}
		return target;
	}

	//strikes if it does more damage to enemy than friendly
	//returns true if it it attacked, else false
	public boolean strike() throws GameActionException {
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
		if(score[1]>score[0] && rc.canStrike()) {
			rc.strike();
			return true;
		}
		return false;
	}
}
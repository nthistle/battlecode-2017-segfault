package segfaultplayer;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import battlecode.common.*;


public strictfp abstract class RobotBase
{
	protected final RobotController rc;
	private int myID;
	private static final int randSeed = 10385;
	public static Random rand = new Random(randSeed);
	public final Team enemy;
	public final Team ally;
	MapLocation[] enemyArchons;
	MapLocation[] allyArchons;

	public RobotBase(RobotController rc) throws GameActionException {
		this.rc = rc;
		myID = -1;
		enemy = rc.getTeam().opponent();
		ally = rc.getTeam();
		enemyArchons = rc.getInitialArchonLocations(enemy);
		allyArchons = rc.getInitialArchonLocations(ally);
	}
	
	public RobotBase(RobotController rc, int id) throws GameActionException {
		this.rc = rc;
		myID = id;
		enemy = rc.getTeam().opponent();
		ally = rc.getTeam();
		enemyArchons = rc.getInitialArchonLocations(enemy);
		allyArchons = rc.getInitialArchonLocations(ally);
	}
	
	public abstract void run() throws GameActionException; // implemented by subclass robots


	public void checkVPWin() throws GameActionException {
		int vpNeeded = GameConstants.VICTORY_POINTS_TO_WIN - rc.getTeamVictoryPoints();
		if(vpNeeded*rc.getVictoryPointCost() < rc.getTeamBullets()) {
			System.out.println("Hey guys I think we can win");
			System.out.println("Watch this");
			rc.donate(rc.getTeamBullets());
		}
	}
	
	//Srinidi: Add move with dodge.
	//Parameter: Destination
	//Moves 1 move without getting hit (dodge) towards destination as best as possible

	public RobotController getRC() {
		return rc;
	}
	
	public int getID() {
		return myID;
	}
	

	// =====================================================================================
	//                              INSTANCE  HELPER  METHODS
	// =====================================================================================

	
	public void setIndicatorPlus(MapLocation ml, int r, int g, int b) throws GameActionException {
		rc.setIndicatorLine(ml.add(new Direction(0),1.0f),
				ml.add(new Direction((float)Math.PI),1.0f), r, g, b);
		rc.setIndicatorLine(ml.add(new Direction((float)Math.PI/2.0f),1.0f),
				ml.add(new Direction(3.0f*(float)Math.PI/2.0f),1.0f), r, g, b);
	}
	
	public void setIndicatorX(MapLocation ml, int r, int g, int b) throws GameActionException {
		rc.setIndicatorLine(ml.add(new Direction((float)Math.PI/4.0f),1.0f),
				ml.add(new Direction(5.0f*(float)Math.PI/4.0f),1.0f), r, g, b);
		rc.setIndicatorLine(ml.add(new Direction(3.0f*(float)Math.PI/4.0f),1.0f),
				ml.add(new Direction(7.0f*(float)Math.PI/4.0f),1.0f), r, g, b);
	}
	
	public float getDist(float x1, float y1, float x2, float y2) {
		float dx = x2-x1;
		float dy = y2-y1;
		return (float)Math.sqrt(dx*dx+dy*dy); 
	}
	
	
	public boolean moveTowards(MapLocation cur, MapLocation goal) throws GameActionException {
		return moveTowards(cur, goal, (float)Math.PI/16.0f, 8);
	}
	
	public Direction moveInDir(Direction ideal) throws GameActionException {
		return moveInDir(ideal, (float)Math.PI/16.0f, 8);
	}
	
	/**
	 * Trys to move from the current location towards the given goal location (just used for direction)
	 * while slowly tweaking angle in either direction, according to offset and max
	 * 
	 * @param cur current location you're at
	 * @param goal goal location you want to get to
	 * @param offset the increment to attempt directions in, maximum offset used is offset*max
	 * @param max maximum number of "offset"s away you are willing to try to move
	 * @return whether or not this was successfully able to move
	 * @throws GameActionException
	 */
	public boolean moveTowards(MapLocation cur, MapLocation goal, float offset, int max) throws GameActionException {
		Direction ideal = cur.directionTo(goal);
		if(rc.canMove(ideal)) {
			rc.move(ideal);
			return true;
		} else {
			return moveInDir(ideal, offset, max) != null;
		}
	}
	
	public Direction moveInDir(Direction ideal, float offset, int max) throws GameActionException {
		Direction dir;
		for(int i = 1; i < max; i ++) {
			dir = ideal.rotateRightRads(offset * i);
			if(rc.canMove(dir)) {
				rc.move(dir);
				return dir;
			}
			dir = ideal.rotateLeftRads(offset * i);
			if(rc.canMove(dir)) {
				rc.move(dir);
				return dir;
			}
		}
		return null;
		// unable to move
	}
	
	// NOTE: if we ever need to cut down bytecodes, they changed the senseNearby
	// methods to return things in order of nearest, could just take the first one
	// that matches appropriate type
	
	public RobotInfo getNearest(RobotType rt, Team t) {
		RobotInfo nearest = null;
		RobotInfo[] sensed = rc.senseNearbyRobots(rc.getType().sensorRadius, t);
		for(RobotInfo ri : sensed) {
			if(ri.getType() == rt) {
				if(nearest == null)
					nearest = ri;
				else {
					if(ri.getLocation().distanceTo(rc.getLocation()) <
							nearest.getLocation().distanceTo(rc.getLocation())) {
						nearest = ri;
					}
				}
			}
		}
		return nearest;
	}

	public boolean isSingleShotClearScout(Direction tDir) throws GameActionException {
		return isSingleShotClearScout(tDir, false);
	}

	//Parameters: Target direction
	//Returns true if way is clear, else false
	public boolean isSingleShotClearScout(Direction tDir, boolean drawIndicators) throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
		TreeInfo[] trees = rc.senseNearbyTrees();

		if(drawIndicators)
			rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),0,255,255);

		for(int i=0; i<robots.length; i++) {
			Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
			double length = (double)rc.getLocation().distanceTo(robots[i].getLocation());
			double dist = Math.sqrt(2*length*length - 2*length*length*Math.cos(tDir.radiansBetween(fDir)));
			if(dist<robots[i].getRadius()+.1) {
				return false;
			}
		}
		for(int i=0; i<trees.length; i++) {
			Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
			double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
			double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
			if (dist < trees[i].getRadius()+.1) {
				System.out.println("thetrees");
				return false;
			}
			}
		return true;
	}

	public boolean isSingleShotClear(Direction tDir) throws GameActionException {
		return isSingleShotClear(tDir, false);
	}

	//Parameters: Target direction
	//Returns true if way is clear, else false
	public boolean isSingleShotClear(Direction tDir, boolean drawIndicators) throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
		TreeInfo[] trees = rc.senseNearbyTrees();
		
		if(drawIndicators)
			rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),0,255,255);
		
		for(int i=0; i<robots.length; i++) {
			Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
			double length = (double)rc.getLocation().distanceTo(robots[i].getLocation());
			double dist = Math.sqrt(2*length*length - 2*length*length*Math.cos(tDir.radiansBetween(fDir)));
			if(dist<robots[i].getRadius()+.1) {
				return false;
			}
		}
		for(int i=0; i<trees.length; i++) {
			if(trees[i].getTeam()==ally) {
				Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
				double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
				double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < trees[i].getRadius()+.1) {
					System.out.println("thetrees");
					return false;
				}
			}
		}
		return true;
	}

	public double[] isTriadShotClear(Direction tDir) throws GameActionException {
		return isTriadShotClear(tDir, false);
	}

	//Parameters: Target direction
	//Returns length 2 array of doubles, first being friendly damage and second being enemy damage
	public double[] isTriadShotClear(Direction tDir, boolean drawIndicators) throws GameActionException {
		double[] ret = {0.0,0.0};
		tDir = tDir.rotateRightDegrees(40.0f);
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius);
		TreeInfo[] trees = rc.senseNearbyTrees();
		for(int z=0; z<3; z++) {
			tDir = tDir.rotateLeftDegrees(20.0f);

			if(drawIndicators)
				rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),255,255,0);

			for (int i = 0; i < robots.length; i++) {
				if(drawIndicators)
					rc.setIndicatorDot(robots[i].getLocation(), 255,0,0);
				Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
				double length = (double) rc.getLocation().distanceTo(robots[i].getLocation());
				double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < robots[i].getRadius()+.1) {
					double score = 2.0;
					if(robots[i].getType()==RobotType.ARCHON)
						score+=4.0;
					if(robots[i].getTeam()==rc.getTeam())
						ret[0]+=score;
					else
						ret[1]+=score;
				}
			}
			for(int i=0; i<trees.length; i++) {
				if(drawIndicators)
					rc.setIndicatorDot(trees[i].getLocation(), 0,0,255);
				Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
				double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
				double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < trees[i].getRadius()+.1) {
					double score=1.0;
					if(trees[i].getTeam()==rc.getTeam())
						ret[0]+=score;
					else
						ret[1]+=score;
				}
			}
		}
		return ret;
	}


	public double[] isPentadShotClear(Direction tDir) throws GameActionException {
		return isPentadShotClear(tDir, false);
	}

	//Parameters: Target direction
	//Returns length 2 array of doubles, first being friendly damage and second being enemy damage
	public double[] isPentadShotClear(Direction tDir, boolean drawIndicators) throws GameActionException {
		double[] ret = {0.0,0.0};
		tDir = tDir.rotateRightDegrees(45.0f);
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius);
		TreeInfo[] trees = rc.senseNearbyTrees();
		for(int z=0; z<5; z++) {
			tDir = tDir.rotateLeftDegrees(15.0f);

			if(drawIndicators)
				rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(tDir,Float.valueOf(100.0+"")),255,255,0);

			for (int i = 0; i < robots.length; i++) {
				if(drawIndicators)
					rc.setIndicatorDot(robots[i].getLocation(), 255,0,0);
				Direction fDir = rc.getLocation().directionTo(robots[i].getLocation());
				double length = (double) rc.getLocation().distanceTo(robots[i].getLocation());
				double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < robots[i].getRadius()+.1) {
					double score = 2.0;
					if(robots[i].getType()==RobotType.ARCHON)
						score+=4.0;
					if(robots[i].getTeam()==rc.getTeam())
						ret[0]+=score;
					else
						ret[1]+=score;
				}
			}
			for(int i=0; i<trees.length; i++) {
				if(drawIndicators)
					rc.setIndicatorDot(trees[i].getLocation(), 0,0,255);
				Direction fDir = rc.getLocation().directionTo(trees[i].getLocation());
				double length = (double) rc.getLocation().distanceTo(trees[i].getLocation());
				double dist = Math.sqrt(2 * length * length - 2 * length * length * Math.cos(tDir.radiansBetween(fDir)));
				if (dist < trees[i].getRadius()+.1) {
					double score=1.0;
					if(trees[i].getTeam()==rc.getTeam())
						ret[0]+=score;
					else
						ret[1]+=score;
				}
			}
		}
		return ret;
	}

	public void moveWithoutDodging(Direction goal) throws GameActionException {
		moveWithoutDodging(goal, false);
	}

	//Parameters: Intended movement direction, 15 degree intervals
	//Moves robot as best possible
	public void moveWithoutDodging(Direction goal, boolean debug) throws GameActionException {
		if(rc.canMove(goal) && canTankMove(rc.getLocation().add(goal,rc.getType().strideRadius)) ) { //ideal move
			rc.move(goal);
			if(debug)
				System.out.println("Move straight");
			return;
		}
		for(int i=1; i<25; i++) { //fans out, adding fixed degrees right and left on 7.5 intervals. Neal's code performs better!!!! idk why, look at it. This gets stuck
			Direction copyRight = (new Direction(degreesToRadians(goal.getAngleDegrees()))).rotateRightDegrees((float)(i*7.5));
			Direction copyLeft = (new Direction(degreesToRadians(goal.getAngleDegrees()))).rotateLeftDegrees((float)(i*7.5));
			if(rc.canMove(copyRight) && canTankMove(rc.getLocation().add(copyRight,rc.getType().strideRadius)) ) {
				rc.move(copyRight);
				if(debug)
					System.out.println("Move Right");
				return;
			}
			else if(rc.canMove(copyLeft) && canTankMove(rc.getLocation().add(copyLeft,rc.getType().strideRadius))) {
				rc.move(copyLeft);
				if(debug)
					System.out.println("Move Right");
				return;
			}
		}
		if(debug)
			System.out.println("NO move");
	}

	//very important! rc.canMove(dir) is always true for tanks bc they roll trees over. This sees if the tree in question is friendly so it can move around them and not steamroll
	public boolean canTankMove(MapLocation ml) {
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius,ally);
		for(int i=0; i<trees.length; i++) {
			if(ml.distanceTo(trees[i].getLocation())<trees[i].getRadius()+rc.getType().bodyRadius)
				return false;
			//System.out.println(ml.distanceTo(trees[i].getLocation())+" "+trees[i].getRadius());
		}
		return true;
	}

	public float degreesToRadians(double angle) {
		return (float)(angle/180.0*Math.PI);
	}

	public void moveWithDodging() throws GameActionException{
		moveWithDodging(false);
	}

	public void moveWithDodging(boolean debug) throws GameActionException {
		BulletInfo[] nearbyBullets = rc.senseNearbyBullets(5.0f);
		int ctr=0;
		for(int i=0; i<nearbyBullets.length; i++) {
			if(rc.getLocation().directionTo(nearbyBullets[i].getLocation()).equals(nearbyBullets[i].getDir(),(float)(Math.PI/2.0)))
				nearbyBullets[i] = null;
			else
				ctr++;
		}
		BulletInfo[] bi = new BulletInfo[ctr];
		ctr=0;
		for(int i=0; i<nearbyBullets.length; i++) {
			if(nearbyBullets[i]!=null) {
				bi[ctr] = nearbyBullets[i];
				ctr++;
			}
		}
		for(int i=0; i<25; i++) {
			MapLocation ml = rc.getLocation().add(randomDirection(),(float)(.5+(Math.random()*.5)*rc.getType().strideRadius));
			boolean clear = true;
			for(BulletInfo bullet: bi) {
				if(ml.distanceTo(bullet.getLocation().add(bullet.getDir(),bullet.getSpeed()))<rc.getType().bodyRadius+.05
						&& ml.distanceTo(bullet.getLocation().add(bullet.getDir(),(float)(bullet.getSpeed()*.5)))<rc.getType().bodyRadius+.05) {
					clear = false;
					break;
				}
			}
			if(clear) {
				if (rc.canMove(ml)) {
					rc.move(ml);
					System.out.println("i: "+i);
					break;
				}
			}
		}
	}

    static Direction[] getBestDirections(Direction bestDir, float theta) throws GameActionException {
    	float initialtheta = theta;
    	Direction[] dirs = new Direction [(int)(360.0f/theta)];
    	dirs[0] = bestDir;
    	for (int j=1; j<dirs.length; j++) {
    		bestDir = bestDir.rotateLeftDegrees(theta);
    		dirs[j] = bestDir;
    		if (theta>0f) {
    			theta = theta*-1f;
    		} else {
    			theta = theta*-1f + initialtheta;
    		}
    	}
    	return dirs;
	}
    
    public float doCalculations(MapLocation myLoc, BulletInfo b) {
    	MapLocation updatedLoc = b.getLocation().add(b.getDir(), b.getSpeed());
		float A = myLoc.x - b.location.x;
		float B = myLoc.y - b.location.y;
		float C = updatedLoc.x - b.location.x;
		float D = updatedLoc.y - b.location.y;

		float dot = A * C + B * D;
		float len_sq = C * C + D * D;
		float param = -1f;
		if (len_sq != 0)  {//in case of 0 length line
		      param = dot / len_sq;
		}
		float xx, yy;
		if (param < 0) {
		    xx = b.location.x;
		    yy = b.location.y;
		}
		else if (param > 1) {
		  xx = updatedLoc.x;
		  yy = updatedLoc.y;
		}
		else {
		    xx = b.location.x + param * C;
		    yy = b.location.y + param * D;
		 }
		float dx = myLoc.x - xx;
		float dy = myLoc.y - yy;
		double distance =  Math.sqrt(dx * dx + dy * dy);
		return (float)distance;
    }
    public float getDamage(MapLocation myLoc, BulletInfo b) {
    	float distance = doCalculations(myLoc, b);
    	if (distance > 1.0f) {
    		return b.damage;
    	}
    	return 0f;
    }
    
    public void eliminateBullets(MapLocation myLoc, BulletInfo[] myB) {
    	for(BulletInfo b : myB) {
    		if(Math.abs(myLoc.directionTo(b.location).degreesBetween(b.dir)) > 90f) {
    			b = null;
    		}
    	}
    }


 /*   public void moveWithDodging(Direction goal, boolean debug) throws GameActionException {
		float theta = 45.0f;
		MapLocation myLoc = rc.getLocation();

		int[][] damage = new int[(int)(360f/theta)][2];
		// damage is a matrix because damage[i][0] = i, so that once damage is sorted we still know
		// the original direction index. damage[i][1] is the damage itself

		float stride = rc.getType().strideRadius;
		BulletInfo fuckyou[] = rc.senseNearbyBullets(rc.getLocation(), 3); //bullets (3 is fastest for soldier)
		Direction[] myDirs = getBestDirections(goal, theta); 			   //method finds dirs fanning out from specified startDir

		System.out.println(Clock.getBytecodeNum());
		eliminateBullets(myLoc, fuckyou); // gets rid of bullets outside of strideRadius
		System.out.println(Clock.getBytecodeNum());

		int i=0;
		for (Direction k : myDirs) {
			damage[i][0] = i;
			// if i can't move to a location, it gets a high damage so its prioritized last
			if(!rc.canMove(k) || !canTankMove(myLoc.add(k, stride))) {
				damage[i][1] = Integer.MAX_VALUE;
			} else {
				MapLocation newLoc = myLoc.add(k, stride);
				for (BulletInfo b : fuckyou) {
					// calculate damage
					System.out.println(Clock.getBytecodeNum());
					if(b!=null) {
						float thisDamage = getDamage(newLoc, b);
						damage[i][1] += thisDamage;
					} // when b==null, damage[i][1]+=0
					System.out.println(Clock.getBytecodeNum());
				}
				rc.setIndicatorLine(myLoc, newLoc, 0, 20*damage[i][1], 0 );
			}
			i+=1;
		}
		System.out.println("Sorting array");
		System.out.println(Clock.getBytecodeNum());
		java.util.Arrays.sort(damage, new java.util.Comparator<int[]>() {
			public int compare(int[] a, int[] b) {
				if(a[1]==b[1]) {
					return Integer.compare(a[0], b[0]); // sort by direction if damage is equal (lower = closer to goal)
				}
				return Integer.compare(a[1], b[1]);		// sort by damage if they are unequal (lower = less damage)
		    }
		});
		System.out.println(Clock.getBytecodeNum());
		// because the array has been sorted, the first legal move is the best legal move
		for(int j=0; j<damage.length; j++) {
			if(rc.canMove(myDirs[damage[j][0]]) && canTankMove(myLoc.add(myDirs[damage[j][0]],stride))) {
				moveInDir(myDirs[damage[j][0]]);
				break;
			}
		}
	} */
    
    /*
    
     Ok mihir.  NOTEES. What follows is Srinidhi code (broken)
     =====================================================
     The idea for this is that you take the max and min danger values [the float] from the hashmap returned by the heatmap function (denoted by max and min)
     You also get the danger value for the closest angle to "goal" that is in the hashmap (call the danger value d) (you need to find the angle urself)
     So then u have:
     
     min<---------|x|---->max
     
     and you are given a threshold
     
     you find (x - min) / (max - min)
     and see if its greater than the threshold.
     if greater:moveInDir(for that direction)
     else: moveInDir(safest one) [min danger value]
     
     
     I sorta tried to do it. But it became a meme.
     
    public void moveWithDodging(Direction goal, float off, int dept, float dete, double tolerance) throws GameActionException{
        // find the closest direction
        HashMap map = heatmap(rc.getLocation(), off, dept, dete);
        float curgoel = goal.radians;
        HashMap<Float, Direction[]> surf = new HashMap<Float, Direction[]>();
        
        for(Direction entry : map.keySet()) {
            float r1 = entry.radians;
            surf.put(new Float(Math.abs(r1 - curgoel)), new Direction[]{entry, goal});
        }
        
        Map.Entry<Float, Direction[]> min = null;
        for (Map.Entry<Float, Direction[]> entry : surf.entrySet()) {
            if (min == null || min.getKey() > entry.getKey()) {
                min = entry;
            }
        }
        
        Direction imtryna = min.getValue()[0];
        float res_danger = map.get(imtryna).floatValue();
        
        Map.Entry<Direction, Float> min_danger = null;
        Map.Entry<Direction, Float> max_danger = null;

        for (Map.Entry<Direction, Float> entry : map.entrySet()) {
            if (min_danger == null || min_danger.getKey().radians > entry.getKey().radians) {
                min_danger = entry;
            }
            if (max_danger == null || max_danger.getKey().radians < entry.getKey().radians) {
                max_danger = entry;
            }
        }
        
        float isOkay = (res_danger - min_danger.getValue())/ (max_danger.getValue() - min_danger.getValue());
        if(isOkay > tolerance){
            moveInDir(imtryna);
        } else {
            moveInDir(min_danger.getKey());
        }
    }*/

	/**
	 * Computes the BulletInfos for a given set of bullets in the range of current bullet sight radius
	 *
	 * @param depth depth of how many turns of lookahead
	 * @param dt duration of each non-continuous step (the smaller the more accurate)
	 * @return locations of all the bullets after a given time dt; ex output[bulletnumber][depth] is a given bullet at a given depth
	 */
	public BulletInfo[][] muchDoge(int depth, float dt) throws GameActionException{
		BulletInfo[] bi = rc.senseNearbyBullets(rc.getLocation(), -1); // scan full loc; sorted by distance
		BulletInfo[][] res = new BulletInfo[bi.length][depth];
		for(int i = 0; i < res.length; i++){
			res[i][0] = bi[i];
		}
		for(int i = 0; i < res.length; i++){
			BulletInfo cur = bi[i];
			for(int j = 1; j < res[0].length; j++){
				MapLocation np = new MapLocation(cur.location.x, cur.location.y);
				float deex = res[i][j-1].getSpeed() * dt;
				np = np.add(cur.dir, deex);
				if(rc.isLocationOccupied(np)){
					BulletInfo add = new BulletInfo(cur.getID(), np, cur.getDir(), cur.getSpeed(), cur.getDamage());
				} else{
					BulletInfo add = new BulletInfo(cur.getID(), np, cur.getDir(), -1, -1);
				}
			}
		}
		return res;
	}

	/**
	 * Computes an approx sense of danger for a given location based on speed and
	 * power of nearby bullets
	 *
	 * @param ml location in question
	 * @return relative danger; closer to zero the less danger you are in
	 */
	public float dangerHeuristic(BulletInfo[] bi, MapLocation ml){
		// scan full loc; sorted by distance
		if(bi.length == 0){
			bi = rc.senseNearbyBullets(rc.getType().sensorRadius);
			if(bi.length == 0){
				return 0;
			}
		}

		// compute max and min damages within radius to create a scale and instill a
		// notion of "how deadly" a given bullet is
		BulletInfo max_damage = bi[0];
		BulletInfo min_damage = bi[0];
		BulletInfo max_speed = bi[0];
		BulletInfo min_speed = bi[0];
		BulletInfo max_loc = bi[0];
		BulletInfo min_loc = bi[0];
		for(int i = 0; i < bi.length; i++){
			if(bi[i].damage > max_damage.damage){
				max_damage = bi[i];
			}
			if(bi[i].damage < min_damage.damage){
				min_damage = bi[i];
			}
			if(bi[i].speed > max_speed.speed){
				max_speed = bi[i];
			}
			if(bi[i].speed < min_speed.speed){
				min_speed = bi[i];
			}
			if(bi[i].location.distanceTo(ml) > max_loc.location.distanceTo(ml)){
				max_loc = bi[i];
			}
			if(bi[i].location.distanceTo(ml) < min_loc.location.distanceTo(ml)){
				min_loc = bi[i];
			}
		}

		// If you want to mess around with more ratios you can use this
		float dam_diff = max_damage.damage - min_damage.damage;
		float spd_diff = max_speed.speed - min_speed.speed;
		float loc_diff = max_loc.location.distanceTo(ml) - min_loc.location.distanceTo(ml);
		// generate how deadly a bullet is on a scale of  based on speed and damage
		float[] res = new float[bi.length];
		for(int i = 0; i < bi.length; i++){
			res[i] = (bi[i].damage - min_damage.damage) / dam_diff;
			res[i] += (bi[i].speed - min_speed.speed) / spd_diff;
			res[i] += 1 / ((bi[i].location.distanceTo(ml) - min_loc.location.distanceTo(ml)) / loc_diff);
		}

		float sum = 0.0f;
		for(int i = 0; i < res.length; i++){
			System.out.println("SCARY : "+i+" "+sum+" "+res[i]);
			sum += res[i];
		}
		System.out.println("SCARY MEME: "+sum);
		return sum;
	}


	/** OLD
	 * Generates an array of floats with the relative danger of each direction with a
	 * given offset
	 *
	 * @param ml location of interest
	 * @param offset the increment to attempt directions in in degrees
	 * @return array with relative danger potentials of each position with stride of 1
	 */
	public HashMap<Direction, Float> heatmap(MapLocation ml, float offset, int depth, float deetee) throws GameActionException{
		float[] res = new float[(int)(360f/offset)];
		HashMap hm = new HashMap<Direction, Float>();
		MapLocation orig = new MapLocation(ml.x, ml.y);
		float pie = (float)Math.PI;
		float cur = 0.0f;
		BulletInfo[][] doge = muchDoge(depth, deetee);
		for(int g = 0; g < doge.length; g++){
			for(int i = 0; i < res.length; i++){
				Direction d = new Direction(cur * pie / 180.0f);
				hm.put(d, dangerHeuristic(doge[g], ml.add(d)));
				ml = new MapLocation(orig.x, orig.y);
				cur += offset;
			}
		}
		return hm;
	}

	//old code, ignore
	public HashMap<Direction, Float> heatmapTwo(MapLocation ml, float offset, int depth, float deetee) throws GameActionException{
		float[] res = new float[(int)(360f/offset)];
		HashMap hm = new HashMap<Direction, Float>();
		MapLocation orig = new MapLocation(ml.x, ml.y);
		float pie = (float)Math.PI;
		float cur = 0.0f;
		BulletInfo[][] doge = muchDoge(depth, deetee);

		for(int i = 0; i < res.length; i++) {
			Direction d = new Direction(cur * pie / 180.0f);
			MapLocation target = ml.add(d,rc.getType().strideRadius);
			float sum = 0.0f;
			for(int g = 0; g < doge.length; g++) {
				sum+=dangerHeuristic(doge[g], target);
			}
			hm.put(d,sum);
			ml = new MapLocation(orig.x, orig.y);
			cur += offset;
		}
		return hm;
	}
	

	// =====================================================================================
	//                              STATIC  HELPER  METHODS
	// =====================================================================================
	
	// consider moving to a static class later
	
	public static Direction averageDirection(Direction a, Direction b) {
		float adeg = a.radians;
		float bdeg = b.radians;
		if(bdeg > adeg) {
			float tmp = adeg;
			adeg = bdeg;
			bdeg = tmp;
		}
		if((adeg - bdeg) > (float)Math.PI) {
			bdeg += (float)Math.PI;
		}
		// just return the average
		float avg = (adeg + bdeg)/2.0f;
		if(avg > 2.0f*(float)Math.PI)
			avg -= 2.0f*(float)Math.PI;
		return new Direction(avg);
	}
	
    public static Direction[] getDirections(Direction startDir, float theta) throws GameActionException {
    	float initialtheta = theta;
    	Direction[] dirs = new Direction [(int)(360.0f/theta)];
    	Direction bestdir = startDir;
    	dirs[0] = bestdir;
    	for (int j=1; j<dirs.length; j++) {
    		bestdir = bestdir.rotateLeftDegrees(theta);
    		dirs[j] = bestdir;
    	}
    	return dirs;
	}
	
	public static MapLocation findClosest(MapLocation to, MapLocation[] poss) {
		if(poss.length == 0)
			return null;
		MapLocation closest = poss[0];
		for(int i = 1; i < poss.length; i ++) {
			if(poss[i].distanceSquaredTo(to) < closest.distanceSquaredTo(to))
				closest = poss[i];
		}
		return closest;
	}
	
	public static Direction randomDirection() {
		return new Direction(rand.nextFloat() * 2 * (float)Math.PI);
	}
    
	public static int getAndAssignNextID(RobotController rc) throws GameActionException {
		int num = typeToNum(rc.getType());
		int nextID = rc.readBroadcast(100+num);
		rc.broadcast(100+num, nextID+1);
		return nextID;
	}
	
	
    public static int typeToNum(RobotType rt) {
    	if(rt == RobotType.ARCHON)
    		return 0;
    	if(rt == RobotType.GARDENER)
    		return 1;
    	if(rt == RobotType.SCOUT)
    		return 2;
    	if(rt == RobotType.SOLDIER)
    		return 3;
    	if(rt == RobotType.LUMBERJACK)
    		return 4;
    	if(rt == RobotType.TANK)
    		return 5;
    	return -1;
    }
    
    public static RobotType numToType(int t) {
    	if(t == 0)
    		return RobotType.ARCHON;
    	if(t == 1)
    		return RobotType.GARDENER;
    	if(t == 2)
    		return RobotType.SCOUT;
    	if(t == 3)
    		return RobotType.SOLDIER;
    	if(t == 4)
    		return RobotType.LUMBERJACK;
    	if(t == 5)
    		return RobotType.TANK;
    	return null;
    }
}

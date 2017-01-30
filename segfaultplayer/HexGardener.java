package segfaultplayer;
import java.util.ArrayList;

import battlecode.common.*;


public strictfp class HexGardener extends RobotBase
{
	protected float[] aArch;
	protected MapLocation alphaLoc;
	
	protected int[] myPodStatus;
	
	protected MapLocation[] myPodLocations;
	
	public static final Direction[] podDirs = new Direction[] {
			new Direction(0),
			new Direction((float)Math.PI/3.0f),
			new Direction(2.0f*(float)Math.PI/3.0f),
			new Direction(3.0f*(float)Math.PI/3.0f),
			new Direction(4.0f*(float)Math.PI/3.0f),
			new Direction(5.0f*(float)Math.PI/3.0f)
	};
	
	public static final float halfDirection = (float)Math.PI/6.0f;

	// tree blocked is considered more "permanently" blocked than unit blocked,
	// and so takes precedence
	public static final int TREE_BLOCKED_SPOT = 1;
	public static final int UNIT_BLOCKED_SPOT = 2;
	public static final int FREE_SPOT = 3;
	public static final int PLANTED_SPOT = 4;
	
	public static final int PHASE_2_MAX_TREES = 4; // so that tanks can happen
	
	public static final int TURNS_WITHOUT_TREES_TIMEOUT = 100;
	
	protected int numPodTrees = 0; // how many trees our pod currently has planted ((ignores destroyed))
	protected int openDirection = -1; // which direction we open in 
	protected int buildCooldown = 0; // ticks until we build again
	
	public static final int BUILD_COOLDOWN = 15;
	

	public HexGardener(RobotController rc, int id) throws GameActionException {
		super(rc, id);
		myPodStatus = new int[6];
	}

	public void run() throws GameActionException {

		aArch = CommunicationsHandler.unpack(rc.readBroadcast(1));
		alphaLoc = new MapLocation(aArch[0],aArch[1]);
		
		// first thing we have to do is fine a location for our pod

		findPodLocation();
		setPodLocations();
		updatePodStatus();
		drawPodStatus();
		setPodOpenDir();
		drawPodOpenDir();
		
		if(rc.senseNearbyTrees(3.0f,Team.NEUTRAL).length > 0) {
			// attempt to build a lumberjack
			// if we can't, just give up, TODO: make it try again later
			for(float rads = 0.0f; rads < (float)Math.PI*2.0f; rads += (float)Math.PI/8) {
				if(rc.canBuildRobot(RobotType.LUMBERJACK, new Direction(rads))) {
					rc.buildRobot(RobotType.LUMBERJACK, new Direction(rads));
					Clock.yield();
					break;
				}
			}
		}
		
		
		// phase 1
		while(rc.getRoundNum() < 100) {
			try {
				// standard stuff we should do every turn
				checkVPWin();
				updatePodStatus();
				drawPodStatus();
				
				// building stuff during phase 1 is done entirely through order queue
				Order nextOrder = CommunicationsHandler.peekOrder(rc);
				if(nextOrder == null) break;
				if(nextOrder.type == OrderType.TREE) {
					if(isAbleToBuildTree()) {
						if(addTreeToPod()) {
							System.out.println("Filled a tree order");
							CommunicationsHandler.popOrder(rc);
						}
					}
				} else {
					if(buildCooldown <= 0) {
						if(rc.getTeamBullets() > nextOrder.rt.bulletCost) {
							Direction buildDir = getBuildDirection(nextOrder.rt);
							if(buildDir == null) {
								// can't build this, ohwell
							} else {
								System.out.println("Filled an order for " + nextOrder.rt.toString());
								rc.buildRobot(nextOrder.rt, buildDir);
								CommunicationsHandler.popOrder(rc);
							}
						}
					}
				}
				
				// more standard stuff
				drawPodOpenDir();
				waterLowest();
				if(buildCooldown>0) buildCooldown--;
				
			} catch(Exception e) {
				// should never trigger except in weird instances of buildRobot throwing GameActionException,
				// here for security more than anything
				e.printStackTrace();
			}
			Clock.yield();
		}
		// alright, now round num has exceeded 100
		
		// phase 2
		int unitsBuilt = 0;
		int treesPlanted = 0;
		int numTurnsNoTree = 0;
		boolean hasBroadcastedGardenerNecessity = false;
		while(true) {
			try {
				// standard stuff
				checkVPWin();
				updatePodStatus();
				drawPodStatus();
				
				if(buildCooldown <= 0) {
					//System.out.println("Ready to build");
					// build stuff for phase 2 is done based on ratio
					//System.out.println("Ratio: "+getFloatRatio());
					// build a unit to make up for it
					if(numPodTrees >= PHASE_2_MAX_TREES || treesPlanted > getFloatRatio() * (1+unitsBuilt)) {
						//System.out.println("Trying for a unit, T:" + treesPlanted + ",U:" + unitsBuilt);
						RobotType nextType = getNextRobotBuildType();

						Direction buildDir = getBuildDirection(nextType);
						if(buildDir == null) {
							// can't build this, ohwell
						} else {
							rc.buildRobot(nextType, buildDir);
							hasBuiltType(nextType);
							buildCooldown = 15; //replace with actual constant
							unitsBuilt ++;
						}
					} else {
						//System.out.println("Trying for a tree");
						// plant a tree to make up for it
						if(isAbleToBuildTree()) {
							if(addTreeToPod()) {
								treesPlanted ++;
								numTurnsNoTree = 0;
							}
						}
					}
					if(numTurnsNoTree >= TURNS_WITHOUT_TREES_TIMEOUT && !hasBroadcastedGardenerNecessity) {
						weNeedAnotherGardener();
						hasBroadcastedGardenerNecessity = true;
					}
					if(numPodTrees >= PHASE_2_MAX_TREES && !hasBroadcastedGardenerNecessity) {
						weNeedAnotherGardener();
						hasBroadcastedGardenerNecessity = true;
					}
				}
				
				// more standard stuff
				waterLowest();
				if(buildCooldown>0) buildCooldown--;
				numTurnsNoTree ++;
			} catch(Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}
	
	public boolean isAbleToBuildTree() {
		if(buildCooldown > 0)
			return false;
		int numFree = 0;
		for(int i : myPodStatus) {
			if(i == FREE_SPOT) numFree ++;
		}
		return numFree > 0;
	}
	
	/**
	 * based on trees / progress towards attacking enemy, returns what type of
	 * robot we should build, TODO: have local counter variables to keep track so
	 * it makes stuff in good ratios / w/e
	 * also TODO: modify to return array of preferred types
	 * right now returns soldier at round 300 and before, and 50/50 tank or soldier
	 * after that
	 * @return
	 */
	public RobotType getNextRobotBuildType() {
		if(rc.senseNearbyRobots(5.0f, enemy).length > 0) {
			return RobotType.SOLDIER;
		}
		if(rc.senseNearbyTrees(4.5f, Team.NEUTRAL).length > 0) {
			return RobotType.LUMBERJACK;
		}
		if(rc.getRoundNum() < 300) {
			return RobotType.SOLDIER;
		} else {
			if(rand.nextFloat() < 0.05f)
				return RobotType.LUMBERJACK;
			// make it 50/50 after round 300 on tanks vs soldiers
			if(rand.nextBoolean()) {
				return RobotType.TANK;
			} else {
				return RobotType.SOLDIER;
			}
		}
	}
	
	/**
	 * used in conjunction with getNextRobotBuildType so we know what to make next,
	 * TODO: modify local variables accordingly.
	 * Right now does nothing. 
	 */
	public void hasBuiltType(RobotType rt) {
		// do nothing lol
	}
	
	
	public Direction getBuildDirection(RobotType rt) {
		Direction idealDirection = podDirs[openDirection];
		
		if(rc.canBuildRobot(rt, idealDirection))
			return idealDirection;

		Direction poss;
		for(int i = 1; i < 5; i ++) {
			poss = idealDirection.rotateRightRads(halfDirection * i);
			if(rc.canBuildRobot(rt, poss)) {
				return poss;
			}
			poss = idealDirection.rotateLeftRads(halfDirection * i);
			if(rc.canBuildRobot(rt, poss)) {
				return poss;
			}
		}
		return null;
	}
	
	/**
	 * attempts to add a tree to our pod, based on information on spots being open or not (yk)
	 * @return whether or not we were successfully able to add a tree to the pod
	 * @throws GameActionException
	 */
	public boolean addTreeToPod() throws GameActionException {
		if(rc.getTeamBullets() < GameConstants.BULLET_TREE_COST)
			return false;
		MapLocation myLoc = rc.getLocation();
		MapLocation[] archLocs = rc.getInitialArchonLocations(enemy);
		MapLocation closest = null;
		for(MapLocation ml : archLocs) {
			if(closest == null) closest = ml;
			else if(ml.distanceTo(myLoc) < closest.distanceTo(myLoc)) {
				closest = ml;
			}
		}
		if(closest != null) {
			Direction goalDir = myLoc.directionTo(closest);
			int bestIndex = -1;
			for(int i = 0; i < 6; i ++) {
				if(i == openDirection) continue;
				if(bestIndex == -1 || Math.abs(podDirs[i].degreesBetween(goalDir)) < Math.abs(podDirs[bestIndex].degreesBetween(goalDir))) {
					if(myPodStatus[i] == FREE_SPOT) {
						if(rc.canPlantTree(podDirs[i])) {
							bestIndex = i;
						}
					}
				}
			}
			if(bestIndex != -1) {
				rc.plantTree(podDirs[bestIndex]);
				myPodStatus[bestIndex] = PLANTED_SPOT;
				numPodTrees ++;
				buildCooldown = BUILD_COOLDOWN;
				return true;
			}
		}
		return false;
	}
	
	
	public void findPodLocation() throws GameActionException {
		float currentValue = 1000000.0f;
		float newValue = currentValue - 1;
    	Direction[] myDirs = getDirections(Direction.NORTH, 60.0f);
    	int t = 0;
    	while(newValue < currentValue && t < 60) {
    		currentValue = newValue;
    		t++;
			float[][] intendedThing = findOptimalPlace(60.0f);
			newValue = intendedThing[0][1];
			Direction intendedDirection = myDirs[(int)intendedThing[0][0]];
			moveInDir(intendedDirection);
			
			rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(intendedDirection, 3.0f),
					0, 255, 0);
			for(int i = 0; i < intendedThing.length; i ++) {
				int tval = (int)(10.0f*intendedThing[i][1]);
				//System.out.println("Value is " + tval);
				rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(myDirs[(int)intendedThing[i][0]]),
						tval, 0, 0);
			}
			Clock.yield();
		}
	}
	
	
	/**
	 * by the termination of this method, the gardener will be in an optimal (decent)
	 * location to build a hex pod
	 * @throws GameActionException
	 */
	public void findPodLocationOld() throws GameActionException {
		// for now, simplistic move 15 steps away from archon that spawned us
		Direction dir = alphaLoc.directionTo(rc.getLocation());
		for(int i = 0; i < 15; i ++) {
			if(rc.canMove(dir))
				rc.move(dir);
			Clock.yield();
		}
	}
	
	/**
	 * sets podLocations[] according to podDirs and current location
	 * does not call Clock.yield()
	 * @throws GameActionException
	 */
	public void setPodOpenDir() throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		MapLocation[] archLocs = rc.getInitialArchonLocations(enemy);
		MapLocation closest = null;
		for(MapLocation ml : archLocs) {
			if(closest == null) closest = ml;
			else if(ml.distanceTo(myLoc) < closest.distanceTo(myLoc)) {
				closest = ml;
			}
		}
		if(closest != null) {
			Direction goalDir = myLoc.directionTo(closest);
			System.out.println("goaldir is " + goalDir.radians);
			int closestIndex = -1;
			for(int i = 0; i < 6; i ++) {
				System.out.println("Direction #" + i + ", degrees between is " + podDirs[i].degreesBetween(goalDir));
				if(closestIndex == -1 || Math.abs(podDirs[i].degreesBetween(goalDir)) < Math.abs(podDirs[closestIndex].degreesBetween(goalDir))) {
					if(myPodStatus[i] == FREE_SPOT || myPodStatus[i] == UNIT_BLOCKED_SPOT) // could remove unit blocked, but eh
						closestIndex = i;
				}
			}
			if(closestIndex != -1) {
				openDirection = closestIndex;
				System.out.println("Pod open direction is " + openDirection);
				// open direction is the closest one towards direction pointing to enemy archon 
				// that is free or blocked by a unit (no tree blocked)
			} else {
				System.out.println("HUHHHH nothing works!");
				openDirection = rand.nextInt(6);
			}
		} else {
			System.out.println("No initial enemy archons? Using random initial open direction");
			openDirection = rand.nextInt(6);
		}
	}
	
	/**
	 * sets podLocations[] according to podDirs and current location
	 * does not call Clock.yield()
	 * @throws GameActionException
	 */
	public void setPodLocations() throws GameActionException {
		myPodLocations = new MapLocation[6];
		for(int i = 0; i < podDirs.length; i ++) {
			myPodLocations[i] = rc.getLocation().add(podDirs[i],2.05f); // 0.05f buffer
		}
	}
	
	/**
	 * updates myPodStatus according to which potential pod locations are open
	 * does not call Clock.yield()
	 * 1 = occupied by tree
	 * 2 = occupied by unit
	 * 3 = free
	 * @throws GameActionException
	 */
	public void updatePodStatus() throws GameActionException {
		TreeInfo[] obstructingTrees = rc.senseNearbyTrees(3.5f);
		RobotInfo[] obstructingRobots = rc.senseNearbyRobots(3.5f);
		for(int i = 0; i < 6; i ++) {
			if(myPodStatus[i] == PLANTED_SPOT) continue;
			myPodStatus[i] = FREE_SPOT;
		}
		// I really should do this with angles, but if this runs under
		// bytecode limit, I don't really care
		outerloop:
		for(int i = 0; i < 6; i ++) {
			if(myPodStatus[i] == PLANTED_SPOT) continue; // we don't want to override this
			
			MapLocation pod = myPodLocations[i];
			for(TreeInfo ti : obstructingTrees) {
				if(ti.getLocation().distanceTo(pod) < (1.00f + ti.getRadius())) {
					myPodStatus[i] = TREE_BLOCKED_SPOT;
					continue outerloop;
				}
			}
			for(RobotInfo ri : obstructingRobots) {
				if(ri.getLocation().distanceTo(pod) < (1.00f + ri.getType().bodyRadius)) {
					myPodStatus[i] = UNIT_BLOCKED_SPOT;
				}
			}
		}
	}
	
	// first index is the direction, second index is the value
    public float[][] findOptimalPlace(float theta) throws GameActionException {
    	float targetDistAwayFromArchons = 7.0f;
    	if(getID() < 2)
    		targetDistAwayFromArchons += 3.0f;
    	TreeInfo[] trees = rc.senseNearbyTrees();
    	RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, ally);
    	Direction[] myDirs = getDirections(Direction.NORTH, theta);
    	float[][] heuristic = new float[myDirs.length][2];
    	MapLocation myLoc = rc.getLocation();
    	for (int i=0; i<myDirs.length; i++) {
    		heuristic[i][0] = i+0.01f;
    		MapLocation newLoc = myLoc.add(myDirs[i]);
    		for (TreeInfo t : trees) {
    			float myDistance = newLoc.distanceTo(t.location);
    			if(myDistance - t.radius < 3.0f) {
    				heuristic[i][1] += (3.0 - (myDistance - t.radius));
    			} else if (myDistance - t.radius > 5.0f) {
    				heuristic[i][1] += 0.1 * ((myDistance - t.radius) - 5.0f);
    			}
    		}
    		for (MapLocation mL : allyArchons) {
    			float myDistance = newLoc.distanceTo(mL);
    			if (myDistance < targetDistAwayFromArchons) {
    				rc.setIndicatorLine(newLoc, mL, 255, 255, 255);
    				heuristic[i][1] += 3.0f * (targetDistAwayFromArchons - myDistance);
    			}
    		}
    		for(RobotInfo ri : nearbyRobots) {
    			if(ri.getType() == RobotType.GARDENER) {
    				float mdist = newLoc.distanceTo(ri.getLocation());
    				if(mdist < 6.0) {
    					heuristic[i][1] += 6.0 * (6.5 - mdist); // intentionally not 6, little bonus falloff here
    				}
    			}
    		}
    	}
		java.util.Arrays.sort(heuristic, new java.util.Comparator<float[]>() {
			public int compare(float[] a, float[] b) {
				return Float.compare(a[1], b[1]); // lower = more optimal
			}
		});
		
		return heuristic;
    }
    // ========================================================
    // ================OLD CODE DOESNT WORK====================
    // ========================================================
    /*
    public float[] findTheOptimalShits() throws GameActionException{
        TreeInfo[] trees = rc.senseNearbyTrees();
        
        ArrayList<TreeInfo> dicked = new ArrayList<TreeInfo>();
        ArrayList<TreeInfo> less_dicked = new ArrayList<TreeInfo>();
        
        for(int i = 0; i < trees.length; i++){
            if(rc.getLocation().distanceTo(trees[i].location) - trees[i].radius < 3.0f){
                dicked.add(trees[i]);
            }
            if(rc.getLocation().distanceTo(trees[i]) - trees[i].radius > 5.0f){
                less_dicked.add(trees[i]);
            }
        }
        
        ArrayList<MapLocation> dicked_archons = new ArrayList<MapLocation>();
        for(int i = 0; i < allyArchons.length; i++){
            if(rc.getLocation().distanceTo[allyArchons[i]] < 5.0f){
                dicked_archons.add(allyArchons[i]);
            }
        }
        
        MapLocation[] toCheck = new MapLocation[8];
        float cur_rad = 0;
        for(int i = 0; i < toCheck.length; i++) {
            toCheck[i] = rc.getLocation().add(new Direction(cur_rad)); // 
            cur_rad += ((2 * Math.PI) / toCheck.length);
        }
        
        float[] evaluatedLocs = new float[8];
        for(int i = 0; i < evaluatedLocs.length; i++){
            evaluatedLocs[i] = evaluation_function(toCheck[i], dicked, less_dicked, dicked_archons);
        }
        return evaluatedLocs;
    }
    
    public float evaluation_function(MapLocation check_this, ArrayList<TreeInfo> dickedTrees, ArrayList<TreeInfo> lessDickedTrees, ArrayList<MapLocation> dicked_archons) throws GameActionException{
        float heuristicValue = 0f;
    	for(int i = 0; i < dicked_archons.size(); i++){
    		float distanceToArchon = check_this.distanceTo(dicked_archons.get(i));
    		if(distanceToArchon < 5f) { // archons have
    			heuristicValue += (5f-distanceToArchon);
    		}
        }
    	heuristicValue += dickedTrees.size()*2f; // trees that are really close to you
    	heuristicValue += lessDickedTrees.size()*1f; // trees that aren't really close to you
    	return heuristicValue;
    }
    */
    // ========================================================
    // =============END OLD CODE DOESNT WORK===================
    // ========================================================


    
	public void weNeedAnotherGardener() throws GameActionException {
		rc.broadcast(21, 1);
	}
	
	
	/**
	 * uses indicator dots to illustrate the status of each potential pod location 
	 * red = occupied by tree 
	 * purple = occupied by unit 
	 * blue = F R E E 
	 */
	public void drawPodStatus() throws GameActionException {
		for(int i = 0; i < 6; i ++) {
			MapLocation pod = myPodLocations[i];
			int status = myPodStatus[i];
			switch(status) {
			  case TREE_BLOCKED_SPOT:
				rc.setIndicatorDot(pod, 255, 0, 0);
				break;
			  case UNIT_BLOCKED_SPOT:
				rc.setIndicatorDot(pod, 255, 0, 255);
				break;
			  case FREE_SPOT:
				rc.setIndicatorDot(pod, 0, 0, 255);
				break;
			  default:
				break;
			}
		}
	}
	
	public void drawPodOpenDir() throws GameActionException {
		rc.setIndicatorLine(rc.getLocation(),rc.getLocation().add(podDirs[openDirection], 2.0f),
				255,255,255);
	}
	
	
	protected void waterLowest() throws GameActionException {
		TreeInfo[] myTrees = rc.senseNearbyTrees(2.1f, rc.getTeam());

		if (myTrees.length > 0) { // Waters lowest
			double hp = 100.0;
			int water = 0;
			for (int i = 0; i < myTrees.length; i++) {
				if ((double) myTrees[i].getHealth() < hp) {
					hp = (double) myTrees[i].getHealth();
					water = i;
				}
			}
			if(rc.canWater(myTrees[water].getID()))
				rc.water(myTrees[water].getID());
		}
	}
}

package segfaultplayer;
import battlecode.common.*;


public strictfp class HexGardener extends RobotBase
{
	private float[] aArch;
	private MapLocation alphaLoc;
	
	private int[] myPodStatus;
	
	private MapLocation[] myPodLocations;
	
	private Direction[] podDirs = new Direction[] {
			new Direction(0),
			new Direction((float)Math.PI/3.0f),
			new Direction(2.0f*(float)Math.PI/3.0f),
			new Direction(3.0f*(float)Math.PI/3.0f),
			new Direction(4.0f*(float)Math.PI/3.0f),
			new Direction(5.0f*(float)Math.PI/3.0f)
	};
	

	// tree blocked is considered more "permanently" blocked than unit blocked,
	// and so takes precedence
	public static final int TREE_BLOCKED_SPOT = 1;
	public static final int UNIT_BLOCKED_SPOT = 2;
	public static final int FREE_SPOT = 3;
	public static final int PLANTED_SPOT = 4;
	
	private int openDirection = -1;
	
	private int buildCooldown = 0;
	
	public static final int BUILD_COOLDOWN = 15;
	

	public HexGardener(RobotController rc, int id) throws GameActionException {
		super(rc, id);
		myPodStatus = new int[6];
	}

	public void run() throws GameActionException {

		aArch = CommunicationsHandler.unpack(rc.readBroadcast(1));
		alphaLoc = new MapLocation(aArch[0],aArch[1]);
		
		// first thing we have to do is fine a location for our pod

		//findPodLocation();
		setPodLocations();
		updatePodStatus();
		drawPodStatus();
		setPodOpenDir();
		drawPodOpenDir();
		
		while(true) {
			checkVPWin();
			updatePodStatus();
			drawPodStatus();
			Order nextOrder = CommunicationsHandler.peekOrder(rc);
			if(nextOrder.type == OrderType.TREE) {
				if(isAbleToBuild()) {
					if(addTreeToPod()) {
						CommunicationsHandler.popOrder(rc);
					}
				}
			//} else {
				
			//}
			drawPodOpenDir();
			waterLowest();
			if(buildCooldown>0) buildCooldown--;
			Clock.yield();
		}
	}
	
	public boolean isAbleToBuild() {
		if(buildCooldown > 0)
			return false;
		int numFree = 0;
		for(int i : myPodStatus) {
			if(i == FREE_SPOT) numFree ++;
		}
		return numFree > 0;
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
				buildCooldown = BUILD_COOLDOWN;
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * by the termination of this method, the gardener will be in an optimal (decent)
	 * location to build a hex pod
	 * @throws GameActionException
	 */
	public void findPodLocation() throws GameActionException {
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
			openDirection = closestIndex;
			System.out.println("Pod open direction is " + openDirection);
			// open direction is the closest one towards direction pointing to enemy archon 
			// that is free or blocked by a unit (no tree blocked)
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
	
	
	private void waterLowest() throws GameActionException {
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
			rc.water(myTrees[water].getID());
		}
	}
}

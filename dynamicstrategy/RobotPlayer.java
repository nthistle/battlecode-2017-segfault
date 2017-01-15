package dynamicstrategy;
import java.util.Random;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Random rand;
    static int otherID;
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        rand = new Random();
        int timeSeed = rand.nextInt();
        rand = new Random(timeSeed + rc.getID());
        
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SCOUT:
            	runScout();
            	break;
            default:
            	break;
        }
	}
    
    static void runArchon() throws GameActionException {
    	// T=1
    	// determine center
    	if(rc.readBroadcast(0) == 0) {
    		System.out.println("Approx center has not been located, locating it...");
    		float[] center = locateApproxCenter();
    		System.out.println("roughly @ (" + center[0] + "," + center[1] + ")");
    		rc.broadcast(0, pack(center[0], center[1]));
    	}
    	// determine alpha
    	int preID = rc.readBroadcast(500) + 1;
    	rc.broadcast(500, preID);
    	float[] center = unpack(rc.readBroadcast(0));
    	System.out.println("Read center as " + center[0] + ", " + center[1]);
    	float myDist = rc.getLocation().distanceSquaredTo(new MapLocation(center[0],center[1]));
    	System.out.println("My coords: " + rc.getLocation().x + ", " + rc.getLocation().y);
    	System.out.println("PreID: " + preID + ", My dist to center^2: " + myDist);
    	rc.broadcast(500+preID, (int)(myDist));
    	Clock.yield();
    	// T=2
    	int rank = 0;
    	int numArchons = rc.readBroadcast(500);
        for(int i = 0; i < numArchons; i ++) {
    		if((int)(myDist) > rc.readBroadcast(501+i))
    			rank ++;
    	}
    	System.out.println("Self-identified as rank #" + rank);
    	boolean isAlpha = (rank == 0);
    	if(isAlpha) {
    		System.out.println("I am the Alpha, broadcasting my coords");
    		rc.broadcast(1, pack(rc.getLocation().x,rc.getLocation().y));
    	}
    	
    	Clock.yield();
    	if(isAlpha) {
    		Direction dir = randomDirection();
    		for(int i = 0; i < 10 && !rc.canBuildRobot(RobotType.GARDENER, dir); i++) {
    			dir = randomDirection();
    		}
    		if(rc.canBuildRobot(RobotType.GARDENER, dir)) {
    			rc.buildRobot(RobotType.GARDENER, dir);
    		}
    		Clock.yield();
    		while(true)
    			Clock.yield();
    	}
    }
    
    /*
     * Returns approximate center in following format:
     * float[] {x, y}
     */
    static float[] locateApproxCenter() throws GameActionException {
    	MapLocation[] initArchonLocsA = rc.getInitialArchonLocations(Team.A);
    	MapLocation[] initArchonLocsB = rc.getInitialArchonLocations(Team.B);
    	MapLocation[] initArchonLocs = new MapLocation[2 * initArchonLocsA.length];
    	int t = 0;
    	for(MapLocation ml : initArchonLocsA)
    		initArchonLocs[t++] = ml;
    	for(MapLocation ml : initArchonLocsB)
    		initArchonLocs[t++] = ml;
    	float netX = 0.0f;
    	float netY = 0.0f;
    	for(MapLocation ml : initArchonLocs) {
    		netX += ml.x;
    		netY += ml.y;
    	}
    	
    	return new float[] {netX/initArchonLocs.length, netY/initArchonLocs.length};
    }
    
    static void runGardener() throws GameActionException {
    	while(true) {
    		Direction dir = randomDirection();
    		for(int i = 0; i < 10 && !rc.canBuildRobot(RobotType.SCOUT, dir); i++) {
    			dir = randomDirection();
    		}
    		if(rc.canBuildRobot(RobotType.SCOUT, dir)) {
    			rc.buildRobot(RobotType.SCOUT, dir);
        		while(true)
        			Clock.yield();
    		}
    		Clock.yield();
    	}
    }
    
    static void runScout() throws GameActionException {
    	//circleScout(); // todo: only if "alpha"/initial scout
    }
    
    
    
    // TODO: repurpose circle scout code
    
    /*
    
    static void circleScout() throws GameActionException {
    	float[] alphalocation = unpack(rc.readBroadcast(1));
    	MapLocation al = new MapLocation(alphalocation[0], alphalocation[1]);
		//get sufficient distance away from archon
    	float[] centerlocation = unpack(rc.readBroadcast(0));
    	MapLocation cl = new MapLocation(centerlocation[0], centerlocation[1]);
    	MapLocation myLoc = rc.getLocation();
    	Direction dir = myLoc.directionTo(cl);
    	float mdist = myLoc.distanceTo(al);
    	System.out.println("Circlescout, about to move away");
    	while(mdist < 10) { // circle radius @17
    		System.out.println("Still in loop");
    		if(mdist > 14.5) {
    			if(rc.canMove(dir, 17.0f-mdist)) {
    				rc.move(dir, 17.0f-mdist);
    			}
    		}
    		else if(rc.canMove(dir)) {
    			rc.move(dir);
    		}
    		
    		Clock.yield();
    		myLoc = rc.getLocation();
    		mdist = myLoc.distanceTo(al);
    	}
    	
    	
    	for(int i = 0; i < 43; i ++) {
    		myLoc = rc.getLocation();
    		dir = myLoc.directionTo(al);
    		dir = dir.rotateRightDegrees(85.78f); // with stride radius 2.5, this keeps in circle
    		if(rc.canMove(dir)) {
    			rc.move(dir);
    		}
    		else {
    			System.out.println("Cannot move (circle) in dir " + dir);
    		}
    		Clock.yield();
    	}
    	// guaranteed circle radius @ almost exactly 17.0f
    	
    	
    	// for now, perpetually circle
    	
		//circle around archon
		float theta = 7.5f; // 48 * 7.5 = 360 degrees
		Direction alphaToScout = al.directionTo(rc.getLocation());
		MapLocation newLoc = rc.getLocation();
		int[] totaltrees = new int[48];
		int sumtrees = 0;
		for (int i=0; i<48; i++) {
			//sense trees (a simplistic approach)
			if (rc.onTheMap(rc.getLocation(), 10.0f)) {
    			TreeInfo[] mytrees = rc.senseNearbyTrees();
    			sumtrees += mytrees.length;
    			totaltrees[i] = mytrees.length;
    			System.out.println(mytrees.length);
			}
			// move scout
			alphaToScout = alphaToScout.rotateRightDegrees(theta);
			System.out.println(alphaToScout);
			newLoc = al.add(alphaToScout, 19.0f);
			//make sure this is less than 2.5, but it should be
			if (rc.canMove(newLoc)) {
				System.out.println("MOVING BRUH");
				System.out.println(newLoc);
				rc.move(newLoc);
			} else {
				System.out.println("RIP CANT MOVE THIS SHOULD NEVER HAPPEN UNLESS WE REACH THE EDGE");
			}
            Clock.yield();
		}
		rc.broadcast(151, (int)sumtrees);
    }
    */
    
    
    // ====================== HELPER =======================
    
    
    public static int pack(float x, float y) {
		return (int)((((int)(x*10))/10.0*100000) + (int)(y*10));
	}
    
	public static float[] unpack(int p) {
		float[] ret = new float[2];
		ret[0] = ((p / 10000) / 10.0f);
		ret[1] = ((p % 10000) / 10.0f);
		return ret;
	}

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction(rand.nextFloat() * 2 * (float)Math.PI);
    }
}

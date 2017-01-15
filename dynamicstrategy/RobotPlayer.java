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
    	// determine bounds
    	if(rc.readBroadcast(0) == 0 && rc.readBroadcast(1) == 0) {
    		System.out.println("Bounds have not been located, locating them...");
    		float[] bounds = locateBounds();
    		System.out.println("MaxX: " + bounds[0]);
    		System.out.println("MinX: " + bounds[1]);
    		System.out.println("MaxY: " + bounds[2]);
    		System.out.println("MinY: " + bounds[3]);
    	}
    }
    
    /*
     * Returns bounds in following format:
     * float[] {maxX, minX, maxY, minY}
     */
    static float[] locateBounds() throws GameActionException {
    	MapLocation[] initArchonLocsA = rc.getInitialArchonLocations(Team.A);
    	MapLocation[] initArchonLocsB = rc.getInitialArchonLocations(Team.A);
    	MapLocation[] initArchonLocs = new MapLocation[2 * initArchonLocsA.length];
    	float maxX, maxY, minX, minY
    	float upperXBound, lowerXBound, upperYBound, lowerYBound, upper, lower, mid;
    	maxX = initArchonLocs[0].x;
    	minX = initArchonLocs[0].x;
    	maxY = initArchonLocs[0].y;
    	minY = initArchonLocs[0].y;
    	for(MapLocation ml : initArchonLocs) {
    		if(ml.x > maxX)	maxX = ml.x;
    		if(ml.x < minX) minX = ml.x;
    		if(ml.y > maxY) maxY = ml.y;
    		if(ml.y < minY) minY = ml.y;
    	}
    	
    	float midX = (maxX+minX)/2.0f;
    	float midY = (maxY+minY)/2.0f;
    	// (midX, midY) guaranteed to be on map
    	
    	upper = maxX + 200f;
    	lower = maxX;
    	for(int i = 0; i < 8; i ++) {
    		mid = (upper+lower)/2.0f;
    		if(rc.onTheMap(new MapLocation(mid, midY)))
    			lower = mid;
    		else
    			upper = mid;
    	}
    	upperXBound = (upper+lower)/2.0f;
    	
    	upper = minX;
    	lower = minX - 200f;
    	for(int i = 0; i < 8; i ++) {
    		mid = (upper+lower)/2.0f;
    		if(rc.onTheMap(new MapLocation(mid, midY)))
    			upper = mid;
    		else
    			lower = mid;
    	}
    	lowerXBound = (upper+lower)/2.0f;
    	
    	
    	upper = maxY + 200f;
    	lower = maxY;
    	for(int i = 0; i < 8; i ++) {
    		mid = (upper+lower)/2.0f;
    		if(rc.onTheMap(new MapLocation(midX, mid)))
    			lower = mid;
    		else
    			upper = mid;
    	}
    	upperYBound = (upper+lower)/2.0f;
    	
    	upper = minY;
    	lower = minY - 200f;
    	for(int i = 0; i < 8; i ++) {
    		mid = (upper+lower)/2.0f;
    		if(rc.onTheMap(new MapLocation(midX, mid)))
    			upper = mid;
    		else
    			lower = mid;
    	}
    	lowerYBound = (upper+lower)/2.0f;
    	
    	return new float[] {upperXBound, lowerXBound, upperYBound, lowerYBound};
    }
    
    static void runGardener() {
    	// pass
    }
    
    static void runScout() {
    	// pass
    }
    
    

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction(rand.nextFloat() * 2 * (float)Math.PI);
    }
}

package scoutplayer;
import java.util.Arrays;
import java.util.Random;


import com.sun.jdi.Location;


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
    // =================== ARCHON ===========================


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
    	float myDist = rc.getLocation().distanceSquaredTo(new MapLocation(center[0],center[1]))*100;
    	System.out.println("My dist to center: " + myDist);
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
    		System.out.println("I am the Alpha");
    		rc.broadcast(1, pack(rc.getLocation().x,rc.getLocation().y));

    		// Build a gardener in the best direction
			if (rc.hasRobotBuildRequirements(RobotType.GARDENER)) {
				Direction[] buildDirection = bestArchonDirection(rc.getLocation());
				for (Direction k : buildDirection) {
	    			if (rc.canHireGardener(k)) {
	    				System.out.println("Getting a gardener");
	    	    		rc.broadcast(100, 0);
	    				rc.hireGardener(k);
	    				break;
	    			}
				}
			}

    	}
    	Clock.yield();
    	// The rest of the game
    	while(true) {
    		Clock.yield();
    	}
    }
    // Returns an array of directions sorted from the least number of trees to the most
    static Direction[] bestArchonDirection(MapLocation myLocation) throws GameActionException {
    	float x = myLocation.x;
    	float y = myLocation.y;
    	float diff = 2.5f*(float)Math.sqrt(2);
    	float xcen = 0;
    	float ycen = 0;
    	int numtrees[][] = new int[8][2];
    	//Creates and checks tree numbers for 8 different circles, each with radius 5 rotated 45degrees from each other
    	for (int i=0; i<8; i++) {
    		if (i==0) {
    			xcen = x+5; ycen = y;
    		}
    		if (i==1) {
    			xcen = x+diff; ycen = y+diff;
    		}
    		if (i==2) {
    			xcen = x; ycen = y+5;
    		}
    		if (i==3) {
    			xcen = x-diff; ycen = y+diff;
    		}
    		if (i==4) {
    			xcen = x-5; ycen = y;
    		}
    		if (i==5) {
    			xcen = x-diff; ycen = y-diff;
    		}
    		if (i==6) {
    			xcen = x; ycen = y-5;
    		}

    		if (i==7) {
    			xcen = x+diff; ycen = y-diff;
    		}
    		MapLocation myloc = new MapLocation(xcen, ycen);
    		if(rc.onTheMap(myloc, 5.0f)) {
    			TreeInfo[] myT = rc.senseNearbyTrees(new MapLocation(xcen, ycen), 5.0f, Team.NEUTRAL);
    			numtrees[i][0] = myT.length;
    			numtrees[i][1] = i;
    			System.out.println(myT.length);
    		} else {
    			numtrees[i][1] = i;
    			numtrees[i][0] = 10000000; // arbitrarily large number
    		}
    	}
    	// Sort the array[][] of [trees][location] by number of trees
    	java.util.Arrays.sort(numtrees, new java.util.Comparator<int[]>() {
    	    public int compare(int[] a, int[] b) {
    	        return Integer.compare(a[0], b[0]);
    	    }
    	});
    	//Translate circle number (0-8) to direction (0-7pi/8)
    	Direction[] ranked = new Direction[8];
    	for (int j=0; j<8; j++) {
    		ranked[j] = new Direction((float)numtrees[j][1]*(float)Math.PI/4.0f);
    	}

    	return ranked;
    }


    /*
     * Returns approximate center in following format:
     * float[] {x, y}
     */
    static float[] locateApproxCenter() throws GameActionException {
    	MapLocation[] initArchonLocsA = rc.getInitialArchonLocations(Team.A);
    	MapLocation[] initArchonLocsB = rc.getInitialArchonLocations(Team.B);
    	if(rc.getTeam()==Team.A) {
    		for (int k=0; k<initArchonLocsB.length; k++) {
    			rc.broadcast(k+4, pack(initArchonLocsB[k].x, initArchonLocsB[k].y));
    		}
    	} else {
    		for (int k=0; k<initArchonLocsA.length; k++) {
    			rc.broadcast(k+4, pack(initArchonLocsA[k].x, initArchonLocsA[k].y));
    		}
    	}
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
    // =========================END ARCHON===================================
    // ========================= GARDENER ===================================
	static void runGardener() throws GameActionException {
		System.out.println("I am a gardener");
    	if (rc.readBroadcast(100)==0) {
    		if (rc.hasRobotBuildRequirements(RobotType.SCOUT)) {
	    		for (Direction k : getBestDirections(rc.getLocation())) {
	    			if(rc.canBuildRobot(RobotType.SCOUT, k)) {
	    				rc.broadcast(100, 1);
	    				rc.broadcast(150, 1);
	    				rc.buildRobot(RobotType.SCOUT, k);
	    				break;
	    			}
	    		}
    		}
    	}
    	gardenerTreeStrategy();
    }
    static void gardenerTreeStrategy() throws GameActionException {
    	while(true) {
    		Clock.yield();
    	}
    }
    // =====================END GARDENER=================================
    // ======================== SCOUT ===================================

    static void runScout() throws GameActionException {
    	System.out.println("I am a scout.");
    	int scouttype = rc.readBroadcast(150);
    	if (scouttype==0) {
    		rc.broadcast(150, 1);
    		circleScout();
    		return;
    	} else if (scouttype==1) {
    		rc.broadcast(150, 2);
    		pathScout();
    	}
    }

    static void pathScout() throws GameActionException {
    	System.out.println("PathScout");
    	System.out.println(rc.getLocation());
    	float[] enemylocation = unpack(rc.readBroadcast(4));
    	MapLocation el = new MapLocation(enemylocation[0], enemylocation[1]);
    	// runs until it hits the enemy archon
    	while(rc.getLocation().distanceTo(el) > 1) {
    		boolean moved = false;
    		Direction[] dankMemes = getBestDirections(rc.getLocation(), el, 15f);
    		for (int i=0; i<dankMemes.length; i++) {
    			MapLocation mymeme = rc.getLocation().add(dankMemes[i], 2.5f);
    			System.out.println(mymeme);
    		}
    		float[][] possibleMoves = minimizeDistance(rc.getLocation(), el, 15f, 2.5f);
			MapLocation dankloc = rc.getLocation();
    		for (int i=0; i< possibleMoves.length; i++) {
    			dankloc = new MapLocation(possibleMoves[i][0], possibleMoves[i][1]);
    			if(rc.senseNearbyTrees(dankloc, 1f, Team.NEUTRAL).length == 0) {
    				if(rc.canMove(dankloc)) {
    					rc.move(dankloc);
    					moved = true;
    					break;
    				}
    			}
    		}
    		if (!moved) {
    			if(rc.canMove(el)) {
    				rc.move(el);
    				moved = true;
    			}
    		}
			Clock.yield();
    	}
    }
    static float[][] minimizeDistance(MapLocation myLoc, MapLocation otherLoc, float theta, float step) throws GameActionException {
		Direction[] dankMemes = getBestDirections(myLoc, otherLoc, theta);
		float[][] myMoves = new float[dankMemes.length][3];
		for (int i=0; i<dankMemes.length; i++) {
			MapLocation mymeme = rc.getLocation().add(dankMemes[i], 2.5f);
			myMoves[i][0] = mymeme.x;
			myMoves[i][1] = mymeme.y;
			myMoves[i][2] = mymeme.distanceTo(otherLoc);
		}
        java.util.Arrays.sort(myMoves,
                new java.util.Comparator<float[]>(){
                    public int compare(float[] a,float[] b){
                        return Float.compare(a[2], b[2]);
                    }
            });
        System.out.println("dankmemes");
        for (int i=0; i<myMoves.length; i++) {
        	for (int q=0; q<myMoves[0].length; q++) {
        		System.out.print(myMoves[i][q]);
        		System.out.print("  ");
        	}
        	System.out.println();
        }
    	return myMoves;
    	
    }

    static int dankdir() throws GameActionException {
    	TreeInfo[] nearbyTrees  = rc.senseNearbyTrees();
    	float[] treeMassByDirection = new float[16];
    	// tree mass by direction represents roughly area of a tree in a given direction,
    	// giving additional weight to closer trees (think inverse of a moment of inertia)
    	Direction dir;
    	MapLocation myLocation = rc.getLocation();
    	float inDeg, dist;
    	int realDir;
    	for(TreeInfo ti : nearbyTrees) {
    		dir = myLocation.directionTo(ti.getLocation());
    		inDeg = dir.getAngleDegrees() + 11.25f;
    		while(inDeg < 360f) inDeg += 360f;
    		while(inDeg > 360f) inDeg -= 360f;
    		realDir = (int)(inDeg/22.5f);
    		dist = myLocation.distanceTo(ti.getLocation());
    		treeMassByDirection[realDir] += (ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
    	}
    	for(int i = 0; i < 16; i ++) {
    		dir = new Direction(i*(float)Math.PI/8.0f);
    		for(dist = 2.49f; dist < 10.0f; dist += 2.49f) {
    			if(!rc.onTheMap(myLocation.add(dir,dist))) {
    				// boundary in this direction, pretend it's a huge tree
    				treeMassByDirection[i] += 35 * (10.0f-dist)*(10.0f-dist);
    			}
    		}
    	}
    	for(int i = 0; i < 16; i ++) {
    		System.out.println("In direction " + i + ", " + treeMassByDirection[i]);
    	}
    	float[] smoothedTreeMassByDirection = new float[16];
    	for(int i = 0; i < 16; i ++) {
    		smoothedTreeMassByDirection[i] += 4.0f * treeMassByDirection[i];
    		smoothedTreeMassByDirection[i] += 2.0f * treeMassByDirection[(15+i)%16];
    		smoothedTreeMassByDirection[i] += 2.0f * treeMassByDirection[(17+i)%16];
    		smoothedTreeMassByDirection[i] += 1.0f * treeMassByDirection[(14+i)%16];
    		smoothedTreeMassByDirection[i] += 1.0f * treeMassByDirection[(18+i)%16];
    		smoothedTreeMassByDirection[i] += 0.5f * treeMassByDirection[(13+i)%16];
    		smoothedTreeMassByDirection[i] += 0.5f * treeMassByDirection[(19+i)%16];
    		smoothedTreeMassByDirection[i] += 0.25f * treeMassByDirection[(12+i)%16];
    		smoothedTreeMassByDirection[i] += 0.25f * treeMassByDirection[(20+i)%16];
    		smoothedTreeMassByDirection[i] += 0.125f * treeMassByDirection[(11+i)%16];
    		smoothedTreeMassByDirection[i] += 0.125f * treeMassByDirection[(21+i)%16];
    		smoothedTreeMassByDirection[i] += 0.0625f * treeMassByDirection[(10+i)%16];
    		smoothedTreeMassByDirection[i] += 0.0625f * treeMassByDirection[(22+i)%16];
    		smoothedTreeMassByDirection[i] += 0.03125f * treeMassByDirection[(9+i)%16];
    		smoothedTreeMassByDirection[i] += 0.03125f * treeMassByDirection[(23+i)%16];
    	}

    	int bestDirection = 0;
    	for(int i = 1; i < 16; i ++) {
    		if(smoothedTreeMassByDirection[i] < smoothedTreeMassByDirection[bestDirection]) {
    			bestDirection = i;
    		}
    	}
    	int secondBestDirection = 0;
    	for(int i = 1; i < 16; i ++) {
    		if(i == bestDirection) continue;
    		if(smoothedTreeMassByDirection[i] < smoothedTreeMassByDirection[secondBestDirection]) {
    			secondBestDirection = i;
    		}
    	}

    	return bestDirection;
    }

    static void circleScout() throws GameActionException {
    	float[] alphalocation = unpack(rc.readBroadcast(1));
    	MapLocation al = new MapLocation(alphalocation[0], alphalocation[1]);
		//get sufficient distance away from archon
    	while(rc.getLocation().distanceTo(al) < 19.0f) {
			Direction[] mydir = getBestDirections(rc.getLocation());
			for (Direction k : mydir) {
				if(rc.canMove(k)) {
					rc.move(k);
					break;
				}
			}
            Clock.yield();
		}
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
    // =====================END SCOUT=======================
    // ====================== HELPER =======================


    public static int pack(float x, float y) {
		return (int)((((int)(x*100))/100.0*10000000) + (int)(((int)(y*100))/100.0*100));
	}

	public static float[] unpack(int p) {
		float[] ret = new float[2];
		ret[0] = ((p / 100000) / 100.0f);
		ret[1] = ((p % 100000) / 100.0f);
		return ret;
	}


    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction(rand.nextFloat() * 2 * (float)Math.PI);
    }

    // gets the perpendicular directions in order of closest to the center
    static Direction[] getBestDirections(MapLocation myLoc) throws GameActionException {
    	float[] center = locateApproxCenter();
    	Direction bestdir = myLoc.directionTo(new MapLocation(center[0], center[1]));
    	Direction nextdir = bestdir.rotateLeftDegrees(90f);
    	Direction threedir = bestdir.rotateLeftDegrees(-90f);
    	Direction[] bestDirections = new Direction[] {bestdir, nextdir, threedir, bestdir.opposite()};
    	return bestDirections;
	}

    // gets the directions in order of closest to specified location at intervals of theta (degrees)
    static Direction[] getBestDirections(MapLocation myLoc, MapLocation otherLoc, float theta) throws GameActionException {
    	float initialtheta = theta;
    	Direction[] dirs = new Direction [(int)(360.0f/theta)];
    	Direction bestdir = myLoc.directionTo(otherLoc);
    	dirs[0] = bestdir;
    	for (int j=1; j<dirs.length; j++) {
    		bestdir = bestdir.rotateLeftDegrees(theta);
    		dirs[j] = bestdir;
    		if (theta>0f) {
    			theta = theta*-1f;
    		} else {
    			theta = theta*-1f + initialtheta;
    		}
    	}
    	return dirs;
	}
}

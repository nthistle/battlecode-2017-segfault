package offensiveplayer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Random rand;
    static int myID;
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
    	// all archons assess their surroundings
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

    	if(isAlpha) {
    		Direction buildDir = findBuildDirection(bestDirection*(float)Math.PI/8.0f,RobotType.GARDENER);
    		System.out.println("bestDirection is " + bestDirection);
    		if(buildDir != null) {
    			rc.buildRobot(RobotType.GARDENER, buildDir);
    			rc.broadcast(400+rank, 1);
    		}
    		else
    			System.out.println("BIG PROBLEM!!! EDGE CASE!!! DIRECTION IS BADDDDD");
    	}

    	Clock.yield();
    	// T=3
    	// gardener starts building a scout

    	Clock.yield();
    	// T=4+

    	// now we have non-alpha ones build gardeners
    	if(!isAlpha) {
    		Direction buildDir = findBuildDirection(bestDirection*(float)Math.PI/8.0f,RobotType.GARDENER);
    		System.out.println("bestDirection is " + bestDirection);
    		if(buildDir != null) {
    			rc.buildRobot(RobotType.GARDENER, buildDir);
    		}
    		else
    			System.out.println("BIG PROBLEM!!! EDGE CASE!!! DIRECTION IS BADDDDD");
    	}

    	Clock.yield();
    	for(int t = 0; true; t++) {
    		if(t%50 == rank) {
    			if(rand.nextDouble() < 25.0f/t) { // lower chance to spawn new ones as time goes on
    				float tdir = rand.nextFloat()*2.0f*(float)Math.PI;
    				for(float j = 0.0f; j < 2.0f*(float)Math.PI; j+=(float)Math.PI/16.0) {
    					if(rc.canBuildRobot(RobotType.GARDENER, new Direction(tdir + j))) {
    						rc.buildRobot(RobotType.GARDENER, new Direction(tdir+j));
    						break;
    					}
    				}
    			}
    		}
    		Clock.yield();
    		// other than occasionally spawn gardeners, do nothing
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
    	myID = rc.readBroadcast(101);
    	rc.broadcast(101, myID + 1);
    	System.out.println("Gardener #" + myID + " spawned");

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

    static boolean goesThroughTrees(MapLocation a, MapLocation b) throws GameActionException {
    	MapLocation midpoint = new MapLocation(a.x + b.x / 2, a.y + b.y / 2);
    	float radius = a.distanceTo(midpoint);
    	return rc.isCircleOccupied(midpoint, radius);
	}

	static void runScout() throws GameActionException {
    	try {
            myID = rc.readBroadcast(102);
            rc.broadcast(102, myID+1);
            System.out.println("Scout #" + myID + " spawned");
            Team enemy = rc.getTeam().opponent();
            MapLocation myLocation;
            if(myID == 0) {
                System.out.println("I am initial scout");
				myLocation = rc.getLocation();

				// Pathfinding Logic Begins

				MapLocation[] initArchLocs = rc.getInitialArchonLocations(enemy);
				MapLocation minArchonLoc = initArchLocs[0];

				// for cases with more than one enemy Archon
				for(int g = 0; g < initArchLocs.length; g++) {
					if (initArchLocs[g].distanceTo(myLocation) < minArchonLoc.distanceTo(myLocation)) {
						minArchonLoc = initArchLocs[g];
					}
				}

				// generate all possible directions of travel
				while(rc.getLocation().distanceTo(minArchonLoc) > 10.0f) {
					float dt = 2.0f;
					Direction[] fullcirc = new Direction[(int) (360.0f / dt)];
					Direction direct = rc.getLocation().directionTo(minArchonLoc);
					fullcirc[0] = direct;
					for (int j = 1; j < fullcirc.length; j++) {
						direct = direct.rotateRightDegrees(dt);
						fullcirc[j] = direct;
						if (dt > 0.0f) {
							dt = -1.0f * dt ;
						} else {
							dt = -1.0f * dt + dt;
						}
					}

					MapLocation possibleNewLocation;
					//ArrayList<MapLocation> no_trees = new ArrayList<MapLocation>();

					for (Direction k: fullcirc) {
						possibleNewLocation = rc.getLocation().add(k, 0.50f);
						/*
						if(!goesThroughTrees(possibleNewLocation, rc.getLocation())){
							no_trees.add(possibleNewLocation);
						}
						*/
						// Thinking out loud:
						//
						// go through all directons and see which loc is best.
						// add all the good locs to a list and pick the one that minimizes the dist to trees.
						// set the strides to be small for greater accuracy
						// but when you move, move 90% of the way

						// cast rays
						// pick the closest one to the enemy archon that doesnt go through trees
						// ----> make theta small for more accuracy
						//
						if(!rc.isLocationOccupiedByTree(possibleNewLocation)) {
							if(rc.canMove(possibleNewLocation)) {
								rc.move(k);
								break;
							}
						}
					}

					// now find the one in no_trees that has the min distance to archon.
					// if there are no elements in no trees, pick a random direction to move slightly in.
					/*
					if(no_trees.size() != 0) {
						MapLocation minStep = no_trees.get(0);
						for (MapLocation ml : no_trees) {
							if (ml.distanceTo(rc.getLocation()) < minStep.distanceTo(rc.getLocation())) {
								minStep = ml;
							}
						}
						if (rc.canMove(minStep)) {
							rc.move(minStep);
						}
					} else{
						rc.move(randomDirection(), 0.1f);
					}
					Clock.yield();
					*/
				}

				//Path Finding Ends

				// dont use canInteractWithTree now; use that for the TODO: bulletshaking on the way
				// now within 6.0 of starting enemy archon location

                RobotInfo[] nearbyRobots;
                Direction mdir = randomDirection();
                while(true) {

                    myLocation = rc.getLocation();
                    nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);

                    // priorities for scout harassment:
                    // closest enemy gardener
                    // closest enemy archon
                    if(nearbyRobots.length > 0) {
                        RobotInfo closestGardener = null;
                        RobotInfo closestArchon = null;
                        for(RobotInfo ri : nearbyRobots) {
                            if(ri.getType() == RobotType.GARDENER) {
                                if(closestGardener == null) {
                                    closestGardener = ri;
                                } else if(ri.getLocation().distanceTo(myLocation) < closestGardener.getLocation().distanceTo(myLocation)) {
                                    closestGardener = ri;
                                }
                            } else if(ri.getType() == RobotType.ARCHON) {
                                if(closestArchon == null) {
                                    closestArchon = ri;
                                } else if(ri.getLocation().distanceTo(myLocation) < closestArchon.getLocation().distanceTo(myLocation)) {
                                    closestArchon = ri;
                                }
                            }
                        }
                        if(closestGardener != null) {
                            // attack closestGardener
                            float distTo = closestGardener.getLocation().distanceTo(myLocation);
                            Direction towardsTarget = myLocation.directionTo(closestGardener.getLocation());
                            if(distTo < 3.0f) { // 2 is used up by radius
                                if(rc.canFireSingleShot()) {
                                    rc.fireSingleShot(towardsTarget);
                                }
                            } else {
                                // move closer
                                if(distTo < 4.5f) { // 2.0f (radius) + 2.5f (stride dist)
                                    if(rc.canMove(towardsTarget, distTo-2.5f)) {
                                        rc.move(towardsTarget, distTo-2.5f);
                                    }
                                    else {
                                        if(rc.canFireSingleShot()) {
                                            rc.fireSingleShot(towardsTarget);
                                        }
                                    }
                                } else {
                                    if(rc.canMove(towardsTarget)) {
                                        rc.move(towardsTarget);
                                    }
                                    else if(rc.canMove(towardsTarget.rotateRightDegrees(90.0f))) {
                                        rc.move(towardsTarget.rotateRightDegrees(90.0f));
                                    }
                                    else if(rc.canMove(towardsTarget.rotateLeftDegrees(90.0f))) {
                                        rc.move(towardsTarget.rotateLeftDegrees(90.0f));
                                    }
                                }
                            }
                        }
                        else {
                            // attack closestArchon
                            float distTo = closestArchon.getLocation().distanceTo(myLocation);
                            Direction towardsTarget = myLocation.directionTo(closestArchon.getLocation());
                            if(distTo < 4.0f) { // 3 is used up by radius
                                if(rc.canFireSingleShot()) {
                                    rc.fireSingleShot(towardsTarget);
                                }
                            } else {
                                // move closer
                                if(distTo < 5.5f) { // 3.0f (radius) + 2.5f (stride dist)
                                    if(rc.canMove(towardsTarget, distTo-2.5f)) {
                                        rc.move(towardsTarget, distTo-2.5f);
                                    }
                                    else {
                                        if(rc.canFireSingleShot()) {
                                            rc.fireSingleShot(towardsTarget);
                                        }
                                    }
                                } else {
                                    if(rc.canMove(towardsTarget)) {
                                        rc.move(towardsTarget);
                                    }
                                    else if(rc.canMove(towardsTarget.rotateRightDegrees(90.0f))) {
                                        rc.move(towardsTarget.rotateRightDegrees(90.0f));
                                    }
                                    else if(rc.canMove(towardsTarget.rotateLeftDegrees(90.0f))) {
                                        rc.move(towardsTarget.rotateLeftDegrees(90.0f));
                                    }
                                }
                            }
                        }
                        Clock.yield();
                    }
                    else {
                        // wander around until this is no longer the case
                        while(nearbyRobots.length == 0) {
                            if(rc.canMove(mdir)) {
                                rc.move(mdir);
                            }
                            else {
                                for(int i = 0; !rc.canMove(mdir) && i < 25; i ++) {
                                    mdir = randomDirection();
                                }
                                if(rc.canMove(mdir)) {
                                    rc.move(mdir);
                                }
                            }
                            Clock.yield();
                            nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);;
                        }
                    }
                }
    	    }


    	}catch(Exception e){
    		e.printStackTrace();
    	}
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


	public static boolean canAfford(RobotType rt) {
		return rc.getTeamBullets() > rt.bulletCost;
	}


	// checks canBuild in up to pi/8 in either direction (increments of pi/64)
	public static Direction findBuildDirection(float angle, RobotType rt) {
		Direction dir = new Direction(angle);
		if(rc.canBuildRobot(rt, new Direction(angle)))
			return new Direction(angle); // cue "that was easy"
		for(int i = 1; i <= 8; i ++) {
			dir = new Direction(angle + (i*(float)Math.PI/64.0f));
			if(rc.canBuildRobot(rt,dir))
				return dir;
			dir = new Direction(angle - (i*(float)Math.PI/64.0f));
			if(rc.canBuildRobot(rt,dir))
				return dir;
		}
		return null; // ruh roh
	}

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction(rand.nextFloat() * 2 * (float)Math.PI);
    }
}

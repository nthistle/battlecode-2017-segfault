package simpleplayer;
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

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        rand = new Random();
        int timeSeed = rand.nextInt();
        rand = new Random(timeSeed + rc.getID());
        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
            case SCOUT:
            	runScout();
            	break;
            case TANK:
            	runTank();
            default:
            	break;
        }
	}

    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");

        // The code you want your robot to perform every round should be in this loop
        boolean onFirst = true;
        MapLocation myLoc = rc.getLocation();
        Direction dir;
        
        Direction[] baseDirections = new Direction[] {Direction.getNorth(), Direction.getWest(), Direction.getSouth(), Direction.getEast()};
        boolean[] validDirections = new boolean[] {false, false, false, false};
        boolean regularMode = false;
        
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

            	if(onFirst) {
            		for(int i = 0; i < 4; i ++) {
            			validDirections[i] = rc.onTheMap(myLoc.add(baseDirections[i],5.0f));
            			if(validDirections[i]) 
            				System.out.println("direction " + i + " is good!");
            		}
            		onFirst = false;
            	}
            	regularMode = true;
                
            	for(int i = 0; i < 4; i ++) {
            		if(validDirections[i]) {
            			regularMode = false;
            			dir = baseDirections[i];
            			if(rc.canHireGardener(dir)) {
            				rc.hireGardener(dir);
            				validDirections[i] = false;
            				System.out.println("put something in direction " + i + ", no longer good");
            			}
            		}
            	}
            	

                // Randomly attempt to build a gardener in this direction
                /*if (rc.canHireGardener(dir) && Math.random() < .01) {
                    rc.hireGardener(dir);
                }*/
            	if(regularMode) {
            		dir = randomDirection();
            		if(rc.canHireGardener(dir) && rand.nextDouble() < 0.05) {
            			rc.hireGardener(dir);
            		}
            	}
                // Move randomly
                //tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                //MapLocation myLocation = rc.getLocation();
                //rc.broadcast(0,(int)myLocation.x);
                //rc.broadcast(1,(int)myLocation.y);
            	
            	// TEMPORARY FOR VICTORY POINTS WIN TEST
            	if(rc.getTeamBullets() > 100) {
            		rc.donate(10*(int)((rc.getTeamBullets()-100.0f)/10.0f));
            	}
            	
            	
            	
            	
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

	static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");
        System.out.println(rc.getID());
        System.exit(0);
        int strategy = -1;
        // add some communications in here
        //if(rc.getTeam() == Team.A)
        //	strategy = 2;
        //else
        otherID = rc.readBroadcast(5);
        strategy = otherID%2;
        rc.broadcast(5, otherID+1);
        
        if(strategy == 0) {
        	gardenerBaseStrategy();
        }
        else if(strategy == 1) {
        	gardenerBlockStrategy();
        }
	}
	
	static boolean gardenerWaterLowestTree() throws GameActionException {
		TreeInfo[] myCircle = rc.senseNearbyTrees(2.1f, rc.getTeam());
		if(myCircle.length > 0) {
			TreeInfo lowestTree = myCircle[0];
			for(int i = 1; i < myCircle.length; i ++) {
				if(myCircle[i].getHealth() < lowestTree.getHealth()) {
					lowestTree = myCircle[i];
				}
			}
			if(rc.canWater(lowestTree.getID())) {
				rc.water(lowestTree.getID());
				return true;
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}
	
	static void gardenerBlockStrategy() throws GameActionException {
		// move away from the archon a little bit, then BLOCK UP
		
		int cap = 8; // if we're that tightly boxed in, just give up
		int stepsSinceLastCollision = 0;
		Direction dir = randomDirection();
		int mincap = 5;
		while(!rc.canMove(dir) && mincap > 0) {
			dir = randomDirection();
			mincap --;
		}
		while(cap > 0 && stepsSinceLastCollision < 8) { // 8 steps to get away
			if(rc.canMove(dir)) {
				rc.move(dir);
				stepsSinceLastCollision ++;
				Clock.yield();
			}
			else {
				cap --;
				stepsSinceLastCollision = 0;
				mincap = 5;
				while(!rc.canMove(dir) && mincap > 0) {
					dir = randomDirection();
					mincap --;
				}
			}
		}
		
		boolean stillPlanting = true;
		boolean hasPlanted;
		while(true) {
			if(stillPlanting) {
				if(rc.getTeamBullets() < 55) {
					gardenerWaterLowestTree();
					Clock.yield();
					continue;
				} // won't tell us anything if we try to plant rn
				hasPlanted = false;
				for(float tdir = 0.0f; tdir <= 2*Math.PI; tdir += 0.1*Math.PI) {
					if(rc.canPlantTree(new Direction(tdir))) {
						rc.plantTree(new Direction(tdir));
						hasPlanted = true;
						break;
					}
				}
				gardenerWaterLowestTree(); // not sure if you can plant and water
				// but we might as well try
				
				if(!hasPlanted)
					stillPlanting = false; // switch to WATER MODE
			}
			if(!stillPlanting) {
				if(rc.getRoundNum()%100 == 0) {
					stillPlanting = true; // just do a quick check to see if any vacancies
				}
				gardenerWaterLowestTree();
			}
			Clock.yield();
			// no redundancy worry since the continue above would skip this
			// anyways
		}
	}
	
	static void gardenerBaseStrategy() throws GameActionException {
        MapLocation myLocation;
        boolean hasMadeLumberjack = false;
        int treesPlanted = 0;
        // The code you want your robot to perform every round should be in this loop
        while (true) {
        	
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	myLocation = rc.getLocation();
                // Listen for home archon's location
                //int xPos = rc.readBroadcast(0);
                //int yPos = rc.readBroadcast(1);
                //MapLocation archonLoc = new MapLocation(xPos,yPos);

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a soldier or lumberjack in this direction
                if(treesPlanted < 3) {
                	if(rc.canPlantTree(dir)) {
                		rc.plantTree(dir);
                		treesPlanted += 1;
                	}
                }
                else {
                	if(rand.nextDouble() < 0.7 && hasMadeLumberjack) {
                		if(rc.canBuildRobot(RobotType.SOLDIER, dir) && rand.nextDouble() < 0.4) {
                			rc.buildRobot(RobotType.SOLDIER, dir);
                		}
                	}
                	else {
                		if(rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
                			rc.buildRobot(RobotType.LUMBERJACK, dir);
                			hasMadeLumberjack = true;
                		}
                	}
                }
                TreeInfo[] nearbyTrees = rc.senseNearbyTrees(7.0f, rc.getTeam());
                for(TreeInfo ti : nearbyTrees) {
                	if(rc.canWater(ti.location)) {
                		rc.water(ti.location);
                	}
                }
                if(nearbyTrees.length > 0) {
                	TreeInfo lowestHPTree = nearbyTrees[0];
                	for(int i = 1; i < nearbyTrees.length; i ++) {
                		if(nearbyTrees[i].getHealth() < lowestHPTree.getHealth()) {
                			lowestHPTree = nearbyTrees[i];
                		}
                	}
                	// want to move towards lowestHPTree, if we can
                	Direction towardsTree = myLocation.directionTo(lowestHPTree.getLocation());
                	if(rc.canMove(towardsTree)) {
                		rc.move(towardsTree);
                	}
                	else if(rc.canMove(towardsTree.rotateRightDegrees(90.0f))) { // try to move perpendicularly, to get around obstacles
                		rc.move(towardsTree.rotateRightDegrees(90.0f));
                	}
                	else if(rc.canMove(towardsTree.rotateLeftDegrees(90.0f))) {
                		rc.move(towardsTree.rotateLeftDegrees(90.0f));
                	}
                }
                
                /*if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .01) {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                } else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .01 && rc.isBuildReady()) {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                }*/

                // Move randomly

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();

        float curdirection = (float)Math.random() * 2 * (float)Math.PI;
        float curdiff = (float) ((float)(Math.random()-0.5) * 0.1 * (float)Math.PI);
        
        boolean hasMoved;
        MapLocation myLocation;
        
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	myLocation = rc.getLocation();
            	hasMoved = false;

                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                if(robots.length > 0) {
                    RobotInfo closestEnemy = robots[0];
                    float closestDist = myLocation.distanceSquaredTo(robots[0].getLocation());
                    float dist;
                    for(int i = 1; i < robots.length; i ++) {
                    	dist = myLocation.distanceSquaredTo(robots[i].getLocation());
                    	if(dist < closestDist) {
                    		closestDist = dist;
                    		closestEnemy = robots[i];
                    	}
                    }
                    Direction enemyDir = myLocation.directionTo(closestEnemy.getLocation());
                    if(rc.canMove(enemyDir)) {
                    	rc.move(enemyDir); 
                    	hasMoved = true;
                    }
                	else if(rc.canMove(enemyDir.rotateRightDegrees(90.0f))) { // try to move perpendicularly, to get around obstacles
                		rc.move(enemyDir.rotateRightDegrees(90.0f));
                    	hasMoved = true;
                	}
                	else if(rc.canMove(enemyDir.rotateLeftDegrees(90.0f))) {
                		rc.move(enemyDir.rotateLeftDegrees(90.0f));
                    	hasMoved = true;
                	}
                	curdirection = 2*(float)Math.PI*enemyDir.getAngleDegrees()/360.0f;
                    // And we have enough bullets, and haven't attacked yet this turn...
                	if(myLocation.distanceSquaredTo(closestEnemy.getLocation()) < 18.0f) {
                        if (rc.canFirePentadShot()) {//SingleShot()) {
                            // ...Then fire a bullet in the direction of the enemy.
                            rc.firePentadShot(enemyDir);
                        }
                        else if(rc.canFireSingleShot()) {
                			rc.fireSingleShot(enemyDir);
                		}
                	}
                	else {
                		if(rc.canFireSingleShot()) {
                			rc.fireSingleShot(enemyDir);
                		}
                	}

                }
                
            	if(rand.nextDouble() < 0.05) {
                    curdiff = (float) ((float)(Math.random()-0.5) * 0.1 * (float)Math.PI);
            	}
            	curdirection += curdiff + 2 * (float)Math.PI;
            	while(curdirection > 2 * (float)Math.PI) {
            		curdirection -= 2 * (float)Math.PI;
            	}
            	Direction d = new Direction(curdirection);
            	if(!hasMoved && rc.canMove(d)) {
            		rc.move(d);
            	}
            	else {
                    curdiff = (float) ((float)(Math.random()-0.5) * 0.1 * (float)Math.PI);
                    curdirection = (float)Math.random() * 2 * (float)Math.PI;
            	}
                // Move randomly
                
                
                // See if there are any nearby enemy robots
                
                //MapLocation[] broadcasters = rc.senseBroadcastingRobotLocations();


                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();
        
        MapLocation myLocation;

        float curdirection = (float)Math.random() * 2 * (float)Math.PI;
        float curdiff = (float) ((float)(Math.random()-0.5) * 0.1 * (float)Math.PI);
        // The code you want your robot to perform every round should be in this loop
        while (true) {
        	myLocation = rc.getLocation();
        	
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
            	int numTargetTrees = 0;
            	for(TreeInfo ti : nearbyTrees) {
            		if(ti.getTeam() == enemy || ti.getTeam() == Team.NEUTRAL)
            			numTargetTrees ++;
            	}
            	TreeInfo[] targetTrees = new TreeInfo[numTargetTrees];
            	int j = 0;
            	for(TreeInfo ti : nearbyTrees) {
            		if(ti.getTeam() == enemy || ti.getTeam() == Team.NEUTRAL)
            			targetTrees[j++] = ti;
            	}
            	nearbyTrees = targetTrees; // temporary, eventually just replace instances
            	if(nearbyTrees.length > 0) {
                	TreeInfo closestTree = nearbyTrees[0];
                	/*float closestDist = closestTree.getLocation().distanceTo(myLocation);
            		for(int i = 1; i < nearbyTrees.length; i ++) {
            			if(nearbyTrees[i].getLocation().distanceTo(myLocation) < closestDist) {
            				closestDist = nearbyTrees[i].getLocation().distanceTo(myLocation);
            				closestTree = nearbyTrees[i];
            			}
            		}*/

            		for(int i = 1; i < nearbyTrees.length; i ++) {
            			if(nearbyTrees[i].getHealth() < closestTree.getHealth()) {
            				closestTree = nearbyTrees[i];
            			}
            		}
            		
            		
            		if(rc.canChop(closestTree.getID())) {
            			rc.chop(closestTree.getID());
            		}
            		else {
            			Direction toTree = myLocation.directionTo(closestTree.getLocation());
            			if(rc.canMove(toTree)) {
            				rc.move(toTree);
            			}
            		}
            	}
            	else {
                	if(Math.random() < 0.05) {
                        curdiff = (float) ((float)(Math.random()-0.5) * 0.1 * (float)Math.PI);
                	}
                	curdirection += curdiff + 2 * (float)Math.PI;
                	while(curdirection > 2 * (float)Math.PI) {
                		curdirection -= 2 * (float)Math.PI;
                	}
                	Direction d = new Direction(curdirection);
                	if(rc.canMove(d)) {
                		rc.move(d);
                	}
                	else {
                        curdiff = (float) ((float)(Math.random()-0.5) * 0.1 * (float)Math.PI);
                        curdirection = (float)Math.random() * 2 * (float)Math.PI;
                	}
                	
                	RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                    if(robots.length > 0 && !rc.hasAttacked()) {
                        // Use strike() to hit all nearby robots!
                        rc.strike();
                    }
            	}
                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                /* else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                    } else {
                        // Move Randomly
                        tryMove(randomDirection());
                    }
                }*/

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }
    


    static void runScout() throws GameActionException {
    	runSoldier();
    }
    


    static void runTank() throws GameActionException {
    	runSoldier();
    }
    
    

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction(rand.nextFloat() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        //boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}

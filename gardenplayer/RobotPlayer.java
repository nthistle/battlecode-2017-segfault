package gardenplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

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
        }
	}

    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");

        boolean garden = true;
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction
                if(rc.getTeamBullets()>10000)
                    rc.donate(10000f);
                if (rc.canHireGardener(dir) &&  (garden==true || rc.getTeamBullets()>200)) {
                    rc.hireGardener(dir);
                    garden = false;
                }

                // Move randomly
                //tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

	static void runGardener() throws GameActionException {
        //UPDATE TO ACCOUNT FOR UNIT COLLISION

        System.out.println("I'm a gardener!");
        int DONOTBUILDHERE = 0;
        int z=0;
        boolean clear = false;
        boolean corners = false;
        boolean[] cs = new boolean[4];
        for(int i=0; i<4; i++)
            cs[i] = false;
        int status = 0;
        TreeInfo[] myTrees;
        float rad = 3.5f;
        int withoutSpace = 0;
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Listen for home archon's location
                int xPos = rc.readBroadcast(0);
                int yPos = rc.readBroadcast(1);
                MapLocation archonLoc = new MapLocation(xPos,yPos);

                // Generate a random direction
                Direction dir = randomDirection();

//                // Randomly attempt to build a soldier or lumberjack in this direction
//                if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .01) {
//                    rc.buildRobot(RobotType.SOLDIER, dir);
//                } else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .01 && rc.isBuildReady()) {
//                    rc.buildRobot(RobotType.LUMBERJACK, dir);
//                }

                
                if(clear == false) {    //is there space
                    TreeInfo[] nearbyTrees = rc.senseNearbyTrees(3.5f);
                    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(3.5f);
                    if(nearbyTrees.length==0 && nearbyRobots.length==0 && rc.onTheMap(rc.getLocation(),3.5f)==true) {
                        clear = true;
                    } else {
                    	withoutSpace +=1;
                    	if (withoutSpace>10) {
                    		withoutSpace = 0;
                    		if (rad>2.0f) {
                    			rad-=1f;
                    		} else {
                    			if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && rc.isBuildReady()) {
                    				rc.buildRobot(RobotType.LUMBERJACK, dir);
                    			}
                    		}
                    	}
                    }
                }
                if(clear == false) { //move to space
                    Direction didr = randomDirection();
                    while(!rc.canMove(didr))
                        didr = randomDirection();
                    tryMove(didr);
                }
                else { //main algo
                    myTrees = rc.senseNearbyTrees(2.0f);

                        if(myTrees.length>0) { //Waters lowest
                            double hp = 100.0;
                            int water = 0;
                            for(int i=0; i<myTrees.length; i++) {
                                if((double)myTrees[i].getHealth()<hp) {
                                    hp = (double)myTrees[i].getHealth();
                                    water = i;
                                }
                        }
                            rc.water(myTrees[water].getID());
                        }
                        if(!corners) { //build diagnols first, left-top, left-bottom, right-top, right-bottom
                            if(cs[0]==false && rc.hasTreeBuildRequirements()) {
                                if(status==0) {
                                    tryMove(new Direction((float) (Math.PI)));
                                    Clock.yield();
                                    tryMove(new Direction((float) (Math.PI)));
                                    status = 1;
                                }
                                if(rc.canPlantTree(new Direction((float)(Math.PI/2.0 + .1)))) {
                                    rc.plantTree(new Direction((float) (Math.PI / 2.0 + .1)));
                                    cs[0] = true;
                                }
                            }
                            else if(cs[2]==false && rc.hasTreeBuildRequirements()) {
                                //tryMove(new Direction((float)(Math.PI)));
                                if(rc.canPlantTree(new Direction((float)(Math.PI*3/2.0 - .1)))) {
                                    rc.plantTree(new Direction((float) (Math.PI * 3 / 2.0-.1)));
                                    tryMove(new Direction((float) (0.0)));
                                    Clock.yield();
                                    tryMove(new Direction((float) (0.0)));
                                    cs[2] = true;
                                }
                            }
                            else if(cs[1]==false && rc.hasTreeBuildRequirements()) {
                                if(status==1) {
                                    tryMove(new Direction((float) (0.0)));
                                    Clock.yield();
                                    tryMove(new Direction((float) (0.0)));
                                    status=2;
                                }
                                if(rc.canPlantTree(new Direction((float)(Math.PI/2.0 - .1)))) {
                                    rc.plantTree(new Direction((float) (Math.PI / 2.0-.1)));
                                    cs[1] = true;
                                }
                            }
                            else if(cs[3]==false && rc.hasTreeBuildRequirements()) {
                                //tryMove(new Direction((float)(0.0)));
                                if(rc.canPlantTree(new Direction((float)(Math.PI*3/2.0 + .1)))) {
                                    rc.plantTree(new Direction((float) (Math.PI * 3 / 2.0+.1)));
                                    tryMove(new Direction((float) (Math.PI)));
                                    Clock.yield();
                                    tryMove(new Direction((float) (Math.PI)));
                                    cs[3] = true;
                                }
                            }
                            if(cs[0]==true && cs[1]==true && cs[2]==true && cs[3]==true)
                                corners = true;
                        }
                        else { //build cardinal directions, skip over hole to build units
                            for (int i = 0; i < 4; i++) {
                                if(DONOTBUILDHERE!=i && rc.canPlantTree(new Direction((float) (0.0 + Math.PI / 2.0 * i))))
                                    rc.plantTree(new Direction((float) (0.0 + Math.PI / 2.0 * i)));
                            }
                        }
                    }

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

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                // Move randomly
                tryMove(randomDirection());

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

		float curdirection = (float) Math.random() * 2 * (float) Math.PI;
		float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
		// The code you want your robot to perform every round should be in this loop
		while (true) {
			myLocation = rc.getLocation();

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				TreeInfo[] nearbyEnemyTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, rc.getTeam().opponent());
				TreeInfo[] nearbyNeutralTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
				
				TreeInfo[] nearbyTrees = new TreeInfo[nearbyEnemyTrees.length + nearbyNeutralTrees.length];
				int t = 0;
				for(TreeInfo ti : nearbyEnemyTrees)
					nearbyTrees[t++] = ti;
				for(TreeInfo ti : nearbyNeutralTrees)
					nearbyTrees[t++] = ti;
				// temporary, eventually just replace instances
				System.out.println(nearbyTrees.length);
				if (nearbyTrees.length > 0) {
					TreeInfo closestTree = nearbyTrees[0]; // has closest robot containing tree, if none, most bullets 
					TreeInfo dankestTree = nearbyTrees[0]; // actually the closest tree

					for (int i = 0; i < nearbyTrees.length; i++) { //gets tree with most bullets
						if (nearbyTrees[i].getContainedBullets() > closestTree.getContainedBullets()) {
							closestTree = nearbyTrees[i];
						}
					}
					if (closestTree.getContainedBullets() == 0) { //else gets tree w/ robot that is closest
						for (int i = 0; i < nearbyTrees.length; i++) {
							if (nearbyTrees[i].getContainedRobot()!=null && (closestTree.getContainedRobot()==null || rc.getLocation().distanceTo(nearbyTrees[i].getLocation()) < rc.getLocation().distanceTo(closestTree.getLocation()))) {
								closestTree = nearbyTrees[i];
							}
						}
					}
					/*if(closestTree.getContainedBullets() == 0 && closestTree.getContainedRobot() == null) { //gets tree that is closest enemy
						for(int i=0; i<nearbyTrees.length; i++)
							if(nearbyTrees[i].getTeam()==enemy && (closestTree.getTeam()!=enemy || rc.getLocation().distanceTo(nearbyTrees[i].getLocation()) < rc.getLocation().distanceTo(closestTree.getLocation()) ) ) {
								closestTree = nearbyTrees[i];
						}
					}*/
					if(closestTree.getContainedBullets() == 0 && closestTree.getContainedRobot() == null && closestTree.getTeam()!=enemy) { //gets closest tree
						for(int i=0; i<nearbyTrees.length; i++) {
							if(rc.getLocation().distanceTo(nearbyTrees[i].getLocation()) < rc.getLocation().distanceTo(closestTree.getLocation()))
								closestTree = nearbyTrees[i];
						}
					}
//					System.out.println("BEGIN TURN!");
//					for(int i=0; i<nearbyTrees.length; i++) {
//						System.out.println(i+". "+nearbyTrees[i].getID()+" "+nearbyTrees[i].getContainedBullets()+" "+nearbyTrees[i].getContainedRobot()+" "+nearbyTrees[i].getTeam()+" "+rc.getLocation().distanceTo(nearbyTrees[i].getLocation()));
//					}
//					System.out.println(closestTree.getID()+" "+rc.getLocation().distanceTo(closestTree.getLocation()));

					if(rc.canShake(closestTree.getID())) { //get bullets if applicable
						rc.shake(closestTree.getID());
					}
					if (rc.canChop(closestTree.getID())) { //chop if applicable
						rc.chop(closestTree.getID());
					} else { //move w/ funny dodge stuff
						if (rc.canChop(dankestTree.getLocation())) {
							rc.chop(dankestTree.getLocation());
						}
						Direction toTree = myLocation.directionTo(closestTree.getLocation());
						if (rc.canMove(toTree)) {
							rc.move(toTree);
						} else if (rc.canMove(toTree.rotateRightDegrees(30.0f))) { // try to move perpendicularly, to get around obstacles
							rc.move(toTree.rotateRightDegrees(30.0f));
						} else if (rc.canMove(toTree.rotateLeftDegrees(30.0f))) {
							rc.move(toTree.rotateLeftDegrees(30.0f));
						}
					}
				} else { //run around until you find trees
                    RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.sensorRadius, enemy);
                    RobotInfo[] robotsF = rc.senseNearbyRobots(RobotType.LUMBERJACK.sensorRadius);
                    //RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
                    System.out.println("MEMEMS");
                    System.out.println(enemy);
                    System.out.println(robotsF.length);
                    System.out.println(robots.length);
                    if (robots.length > 0 && !rc.hasAttacked()) {
                        // Use strike() to hit all nearby robots!
                        RobotInfo[] rs = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
//                        RobotInfo[] rs = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, );
                        if(rs.length>0 && !rc.hasAttacked())
                            rc.strike();
                        else {
                            Direction toEnemy = myLocation.directionTo(robots[0].getLocation());
                            if (rc.canMove(toEnemy))
                                rc.move(toEnemy);
                        }
                    }
                    else {
                        if (Math.random() < 0.05) {
                            curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
                        }
                        curdirection += curdiff + 2 * (float) Math.PI;
                        while (curdirection > 2 * (float) Math.PI) {
                            curdirection -= 2 * (float) Math.PI;
                        }
                        Direction d = new Direction(curdirection);
                        if (rc.canMove(d)) {
                            rc.move(d);
                        } else {
                            curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
                            curdirection = (float) Math.random() * 2 * (float) Math.PI;
                        }
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
    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
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
        boolean moved = false;
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

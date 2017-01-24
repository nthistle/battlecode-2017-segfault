package blitzplayer;

import battlecode.common.*;

public strictfp class RobotPlayer {
	
	static RobotController rc;

	public static void run(RobotController rc) throws GameActionException {
		
		RobotPlayer.rc = rc;
		
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
			default:
				break;
		}
	}

	static void runArchon() throws GameActionException {
		int gardenerCount = 0;
		int gardenerCooldown = 0;
		while(true) {
			if(gardenerCount < 2 && gardenerCooldown <= 0 &&
					rc.getTeamBullets() > RobotType.GARDENER.bulletCost) {
				Direction dir = randomDirection();
				for(int j = 0; j < 20 && !rc.canBuildRobot(RobotType.GARDENER, dir); j++)
					dir = randomDirection();
				if(rc.canBuildRobot(RobotType.GARDENER, dir)) {
					rc.buildRobot(RobotType.GARDENER, dir);
					gardenerCooldown = 80;
					gardenerCount ++;
				}
			} else if(gardenerCooldown > 0) {
				gardenerCooldown --;
			}
			Clock.yield();
		}
	}

	static void runGardener() throws GameActionException {
		// build soldier, then single tree, repeat soldiers for life
		
		int stage = 0;
		
		while(true) {
			TreeInfo[] myCircle = rc.senseNearbyTrees(2.1f, rc.getTeam());
			if(myCircle.length > 0) {
				if(rc.canWater(myCircle[0].getID()))
					rc.water(myCircle[0].getID());
			}
			if(stage == 0 || stage > 1) {
				if(rc.getTeamBullets() > RobotType.SOLDIER.bulletCost) {
					Direction dir = randomDirection();
					for(int j = 0; j < 20 && !rc.canBuildRobot(RobotType.GARDENER, dir); j++)
						dir = randomDirection();
					if(rc.canBuildRobot(RobotType.SOLDIER, dir)) {
						rc.buildRobot(RobotType.SOLDIER, dir);
						stage = 1;
					}
				}
			} else if(stage == 1) {
				if(rc.getTeamBullets() > 50.0f) {
					Direction dir = randomDirection();
					for(int j = 0; j < 20 && !rc.canPlantTree(dir); j++)
						dir = randomDirection();
					if(rc.canPlantTree(dir)) {
						rc.plantTree(dir);
						stage = 2;
					}
				}
			}
		}
	}

	static void runSoldier() throws GameActionException {
		boolean hasMoved;
		MapLocation myLocation;
		// The code you want your robot to perform every round should be in this loop
		while (true) {
			myLocation = rc.getLocation();
			hasMoved = false;
			RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
			if (robots.length > 0) {
				RobotInfo closestEnemy = robots[0];
				float closestDist = myLocation.distanceSquaredTo(robots[0].getLocation());
				float dist;
				for (int i = 1; i < robots.length; i++) {
					dist = myLocation.distanceSquaredTo(robots[i].getLocation());
					if (dist < closestDist) {
						closestDist = dist;
						closestEnemy = robots[i];
					}
				}
				Direction enemyDir = myLocation.directionTo(closestEnemy.getLocation());
				if (rc.canMove(enemyDir)) {
					rc.move(enemyDir);
					hasMoved = true;
				} else if (rc.canMove(enemyDir.rotateRightDegrees(90.0f))) { // try to move perpendicularly, to get around obstacles
					rc.move(enemyDir.rotateRightDegrees(90.0f));
					hasMoved = true;
				} else if (rc.canMove(enemyDir.rotateLeftDegrees(90.0f))) {
					rc.move(enemyDir.rotateLeftDegrees(90.0f));
					hasMoved = true;
				}
				// And we have enough bullets, and haven't attacked yet this turn...
				if (myLocation.distanceSquaredTo(closestEnemy.getLocation()) < 18.0f) {
					if (rc.canFirePentadShot()) {//SingleShot()) {
						// ...Then fire a bullet in the direction of the enemy.
						rc.firePentadShot(enemyDir);
					} else if (rc.canFireSingleShot()) {
						rc.fireSingleShot(enemyDir);
					}
				} else {
					if (rc.canFireSingleShot()) {
						rc.fireSingleShot(enemyDir);
					}
				}

			} else {
				MapLocation enemyArch = null;
				for(MapLocation ml : rc.getInitialArchonLocations(rc.getTeam().opponent())) {
					if(enemyArch == null ||
							ml.distanceTo(rc.getLocation()) < enemyArch.distanceTo(rc.getLocation()))
						enemyArch = ml;
				}
				Direction enemyDir = myLocation.directionTo(enemyArch);
				if (rc.canMove(enemyDir)) {
					rc.move(enemyDir);
					hasMoved = true;
				} else if (rc.canMove(enemyDir.rotateRightDegrees(90.0f))) { // try to move perpendicularly, to get around obstacles
					rc.move(enemyDir.rotateRightDegrees(90.0f));
					hasMoved = true;
				} else if (rc.canMove(enemyDir.rotateLeftDegrees(90.0f))) {
					rc.move(enemyDir.rotateLeftDegrees(90.0f));
					hasMoved = true;
				}
			}
			// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
			Clock.yield();

		}
	}
	
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }
}

package turtleplayer;
import java.util.Random;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Random rand;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
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
            default:
            	break;
        }
	}

    static void runArchon() throws GameActionException {

        boolean onFirst = true;
        MapLocation myLoc = rc.getLocation();
        Direction dir;
        
        Direction[] baseDirections = new Direction[] {Direction.getNorth(), Direction.getWest(), Direction.getSouth(), Direction.getEast()};
        boolean[] validDirections = new boolean[] {false, false, false, false};
        boolean regularMode = false;
        
        while (true) {
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
            	
            	if(regularMode) {
            		dir = randomDirection();
            		if(rc.canHireGardener(dir)) {
            			rc.hireGardener(dir);
            		}
            	}
            	
            	
            	// TEMPORARY FOR VICTORY POINTS 
            	if(rc.getTeamBullets() > 100) {
            		rc.donate(10*(int)((rc.getTeamBullets()-100.0f)/10.0f));
            	}
            	
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

	static void runGardener() throws GameActionException {
        gardenerTurtleStrategy();
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
	
	static void gardenerTurtleStrategy() throws GameActionException {
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

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction(rand.nextFloat() * 2 * (float)Math.PI);
    }
}

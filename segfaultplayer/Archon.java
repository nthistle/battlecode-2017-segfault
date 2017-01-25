package segfaultplayer;
import battlecode.common.*;


public strictfp class Archon extends RobotBase
{
	private int rank;
	private boolean alpha;
	
	private int[][] grid;

	private float[] closeTreeMassByDirection;
	private float[] farTreeMassSmoothed;
	private static final float TREE_BLOCKED_THRESHOLD = 10.0f;
	
	int xoffset;		// x-offset of grid
	int yoffset;		// y-offset of grid
	int resolution = 2; // grid resolution
	
	public Archon(RobotController rc, int id) throws GameActionException {
		super(rc, id);
		this.calculateDensityClose();
		this.rank = CommunicationsHandler.assignAlphaArchonProtocol(this, true);
		this.alpha = (this.rank == 0);
	}
	
	public void calculateDensityClose() throws GameActionException {
    	closeTreeMassByDirection = new float[16];
    	//TreeInfo[] nearbyTrees  = rc.senseNearbyTrees();
    	float lookDist = 4.5f;
    	TreeInfo[] nearbyTrees  = rc.senseNearbyTrees(lookDist, Team.NEUTRAL);
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
    		closeTreeMassByDirection[realDir] += (ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
    		if(ti.radius > (dist * (float)Math.PI/8.0f)) {
        		closeTreeMassByDirection[(realDir+1)%16] += (0.25f * ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
        		closeTreeMassByDirection[(realDir+15)%16] += (0.25f * ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
    		}
    	}
    	float totalTreeMassFactor = 0.0f;
    	for(int i = 0; i < 16; i ++) {
    		totalTreeMassFactor += closeTreeMassByDirection[i];
    	}
    	////System.out.println("Total Tree Mass Factor: " + totalTreeMassFactor);
    	for(int i = 0; i < 16; i ++) {
    		dir = new Direction(i*(float)Math.PI/8.0f);
    		for(dist = 2.01f; dist < lookDist; dist += (lookDist/4.0f)) {
    			if(!rc.onTheMap(myLocation.add(dir,dist))) {
    				// boundary in this direction, pretend it's a huge tree
    				closeTreeMassByDirection[i] += 35 * (10.0f-dist)*(10.0f-dist);
    			}
    		}
    	}
	}
	
	public void lightExpansionStrategy() throws GameActionException {
		System.out.println("Case: EXPANSION-LIGHT");
		// make enough lumberjacks to get some decent expansion, then start investing
		// in trees until 
		CommunicationsHandler.setSoldierStrategy(rc, SoldierStrategy.PATROL);
		
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SCOUT));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
		
		while(true) {
			if(rc.getRoundNum() == 450 || rc.getRoundNum() == 451) // in case we hit bytecodes, but still
				CommunicationsHandler.setSoldierStrategy(rc, SoldierStrategy.BLITZ);
			
			int gardenerCooldown = 0;
	
			if(gardenerCooldown <= 0 &&	rc.getTeamBullets() > RobotType.GARDENER.bulletCost
					&& ((2.5f * rc.readBroadcast(101)) < rc.readBroadcast(2000))) {
				Direction dir = randomDirection();
				for(int j = 0; j < 20 && !rc.canBuildRobot(RobotType.GARDENER, dir); j++)
					dir = randomDirection();
				if(rc.canBuildRobot(RobotType.GARDENER, dir)) {
					rc.buildRobot(RobotType.GARDENER, dir);
					gardenerCooldown = (rc.getRoundNum()>500?80:30); // later game knock down gardener production
				}
			} else if(gardenerCooldown > 0) {
				gardenerCooldown --;
			}
			if(CommunicationsHandler.peekOrder(rc) == null) {
				if(rc.getRoundNum() < 450) {
					// growth phase
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
				} else if(rc.getRoundNum() < 1500) {
					// full on  a t t a c k
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.TANK));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.TANK));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				} else { // something weird happened, hope we can win on VP
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				}
			}
			Clock.yield();
		}
	}
	

	
	public void heavyExpansionStrategy() throws GameActionException {
		System.out.println("Case: EXPANSION-HEAVY");
		// make enough lumberjacks to get some decent expansion, then start investing
		// in trees until 
		CommunicationsHandler.setSoldierStrategy(rc, SoldierStrategy.PATROL);
		
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SCOUT));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		
		while(true) {
			if(rc.getRoundNum() == 600 || rc.getRoundNum() == 601) // in case we hit bytecodes, but still
				CommunicationsHandler.setSoldierStrategy(rc, SoldierStrategy.BLITZ);
			// TODO: implement swarming for real
			
			int gardenerCooldown = 0;
	
			if(gardenerCooldown <= 0 &&	rc.getTeamBullets() > RobotType.GARDENER.bulletCost
					&& (rc.readBroadcast(101) < 2 || ((2.5f * rc.readBroadcast(101)) < rc.readBroadcast(2000)))) {
				System.out.println("G: " + rc.readBroadcast(101) + " T: " + rc.readBroadcast(2000));
				Direction dir = randomDirection();
				for(int j = 0; j < 20 && !rc.canBuildRobot(RobotType.GARDENER, dir); j++)
					dir = randomDirection();
				if(rc.canBuildRobot(RobotType.GARDENER, dir)) {
					rc.buildRobot(RobotType.GARDENER, dir);
					gardenerCooldown = (rc.getRoundNum()>500?60:30); // later game knock down gardener production
				}
			} else if(gardenerCooldown > 0) {
				gardenerCooldown --;
			}
			if(CommunicationsHandler.peekOrder(rc) == null) {
				if(rc.getRoundNum() < 600) {
					// growth phase
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
					//CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
					//CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
				} else if(rc.getRoundNum() < 1500) {
					// full on  a t t a c k
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.TANK));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.TANK));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
				} else { // something weird happened, hope we can win on VP
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				}
			}
			Clock.yield();
		}
	}
	
	public void blitzStrategy() throws GameActionException {
		System.out.println("Case: BLITZ");
		CommunicationsHandler.setSoldierStrategy(rc, SoldierStrategy.BLITZ);
		// we're really close, let's try to blitz

		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));

		// make two gardeners, then wait until order queue is empty
		int gardenerCount = 0;
		int gardenerCooldown = 0;
		// end "blitz" at 800 if it's "failed"
		while(CommunicationsHandler.peekOrder(rc) != null || rc.getRoundNum() < 800) {
			checkVPWin();
			if(gardenerCount < 2 && gardenerCooldown <= 0 &&
					rc.getTeamBullets() > RobotType.GARDENER.bulletCost) {
				Direction dir = randomDirection();
				for(int j = 0; j < 20 && !rc.canBuildRobot(RobotType.GARDENER, dir); j++)
					dir = randomDirection();
				if(rc.canBuildRobot(RobotType.GARDENER, dir)) {
					rc.buildRobot(RobotType.GARDENER, dir);
					gardenerCooldown = 30;
					gardenerCount ++;
				}
			} else if(gardenerCooldown > 0) {
				gardenerCooldown --;
			}
			if(CommunicationsHandler.peekOrder(rc) == null) {
				if(RobotBase.rand.nextBoolean())
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.TANK));
				else
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
			}
			Clock.yield();
		}
		// now it's turn 800, what are we going to do?
		// well blitz failed, so try lumberjacks and trees, I guess?
		while(true) {
			if(gardenerCooldown <= 0 &&	rc.getTeamBullets() > RobotType.GARDENER.bulletCost
					&& 2.5f * rc.readBroadcast(101) < rc.readBroadcast(2000)) {
				Direction dir = randomDirection();
				for(int j = 0; j < 20 && !rc.canBuildRobot(RobotType.GARDENER, dir); j++)
					dir = randomDirection();
				if(rc.canBuildRobot(RobotType.GARDENER, dir)) {
					rc.buildRobot(RobotType.GARDENER, dir);
					gardenerCooldown = 50;
					gardenerCount ++;
				}
			} else if(gardenerCooldown > 0) {
				gardenerCooldown --;
			}
			if(CommunicationsHandler.peekOrder(rc) == null) {
				CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
				CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
				CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
			}
			Clock.yield();
		}
	}
	
	
	public void calculateDensityFar() throws GameActionException {
		float[] farTreeMassByDirection = new float[16];
    	//TreeInfo[] nearbyTrees  = rc.senseNearbyTrees();
    	TreeInfo[] nearbyTrees  = rc.senseNearbyTrees();
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
    		farTreeMassByDirection[realDir] += (ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
    		if(ti.radius > (dist * (float)Math.PI/8.0f)) {
    			farTreeMassByDirection[(realDir+1)%16] += (0.25f * ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
    			farTreeMassByDirection[(realDir+15)%16] += (0.25f * ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
    		}
    	}
    	
    	for(int i = 0; i < 16; i ++) {
    		dir = new Direction(i*(float)Math.PI/8.0f);
    		for(dist = 2.01f; dist < 7.0f; dist += (1.25f)) {
    			if(!rc.onTheMap(myLocation.add(dir,dist))) {
    				// boundary in this direction, pretend it's a huge tree
    				farTreeMassByDirection[i] += 35 * (10.0f-dist)*(10.0f-dist);
    			}
    		}
    	}
    	farTreeMassSmoothed = new float[16];
    	for(int i = 0; i < 16; i ++) {
    		farTreeMassSmoothed[i] += 4.0f * farTreeMassByDirection[i];
    		farTreeMassSmoothed[i] += 2.0f * farTreeMassByDirection[(15+i)%16];
    		farTreeMassSmoothed[i] += 2.0f * farTreeMassByDirection[(17+i)%16];
    		farTreeMassSmoothed[i] += 1.0f * farTreeMassByDirection[(14+i)%16];
    		farTreeMassSmoothed[i] += 1.0f * farTreeMassByDirection[(18+i)%16];
    		farTreeMassSmoothed[i] += 0.5f * farTreeMassByDirection[(13+i)%16];
    		farTreeMassSmoothed[i] += 0.5f * farTreeMassByDirection[(19+i)%16];
    		farTreeMassSmoothed[i] += 0.25f * farTreeMassByDirection[(12+i)%16];
    		farTreeMassSmoothed[i] += 0.25f * farTreeMassByDirection[(20+i)%16];
    		farTreeMassSmoothed[i] += 0.125f * farTreeMassByDirection[(11+i)%16];
    		farTreeMassSmoothed[i] += 0.125f * farTreeMassByDirection[(21+i)%16];
    		farTreeMassSmoothed[i] += 0.0625f * farTreeMassByDirection[(10+i)%16];
    		farTreeMassSmoothed[i] += 0.0625f * farTreeMassByDirection[(22+i)%16];
    		farTreeMassSmoothed[i] += 0.03125f * farTreeMassByDirection[(9+i)%16];
    		farTreeMassSmoothed[i] += 0.03125f * farTreeMassByDirection[(23+i)%16];
    	}
    	// far tree mass smoothed is like 12.5x total (sum of above multipliers), so let's scale that down
    	for(int i = 0; i < 16; i ++) {
    		farTreeMassSmoothed[i] = farTreeMassSmoothed[i]/12.5f;
    	}
	}
	
	public int getBlockedFactor() {
		if(closeTreeMassByDirection != null) {
			// can either be 0, blocking pretty insignificant (< 8 blocked)
			// 1, we're fairly obstructed here (8-12 blocked)
			// 2, please don't pick me to build stuff unless you have to (12+ blocked)
			int nBlocked = 0;
			boolean[] blocked = new boolean[16]; // just for debugging/visualization
			for(int i = 0; i < 16; i ++) {
				if(closeTreeMassByDirection[i] > TREE_BLOCKED_THRESHOLD) {
					blocked[i] = true;
					nBlocked ++;
				}
			}
			System.out.println("Here's what I think is blocked:");
			
			// I know this is kind of bad, but it looks really nice I promise
			System.out.println("     " + (blocked[4]?"X":"O") + " " + (blocked[3]?"X":"O") + "     ");
			System.out.println("   " + (blocked[5]?"X":"O") + "     " + (blocked[2]?"X":"O") + "   ");
			System.out.println("  " + (blocked[6]?"X":"O") + "       " + (blocked[1]?"X":"O") + "  ");
			System.out.println("             ");
			System.out.println(" " + (blocked[7]?"X":"O") + "         " + (blocked[0]?"X":"O") + " ");
			System.out.println(" " + (blocked[8]?"X":"O") + "         " + (blocked[15]?"X":"O") + " ");
			System.out.println("             ");
			System.out.println("  " + (blocked[9]?"X":"O") + "       " + (blocked[14]?"X":"O") + "  ");
			System.out.println("   " + (blocked[10]?"X":"O") + "     " + (blocked[13]?"X":"O") + "   ");
			System.out.println("     " + (blocked[11]?"X":"O") + " " + (blocked[12]?"X":"O") + "     ");
			
			if(nBlocked < 8) return 0;
			if(nBlocked <= 12) return 1;
			return 2;
		} else return 0;
	}
	
	// =========================================================================================
	// ===================================== MAIN RUN THREAD ===================================
	// =========================================================================================
	
	public void run() throws GameActionException {
		
		if(alpha) {
			System.out.println("I am the alpha");
			calculateDensityFar();
			
			
			rc.broadcast(1, CommunicationsHandler.pack(rc.getLocation().x, rc.getLocation().y));

			MapLocation enemyArch = null;
			for(MapLocation ml : rc.getInitialArchonLocations(enemy)) {
				if(enemyArch == null ||
						ml.distanceTo(rc.getLocation()) < enemyArch.distanceTo(rc.getLocation()))
					enemyArch = ml;
			}

			CommunicationsHandler.addTree(rc, enemyArch.x, enemyArch.y);

			// testing for trees :))
			//for(int i = 0; i < 10; i ++) {
			//	CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
			//} // end testing for trees

			// testing some lumbejacks 8)

			CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));

			// alright



			float closestDist = rc.getLocation().distanceTo(enemyArch);
			int numArchons = rc.getInitialArchonLocations(enemy).length;
			
			// alright now we consider cases
			
			if(closestDist < 20.0f) {
				if(numArchons < 2) {
					blitzStrategy();
				} else {
					lightExpansionStrategy();
				}
			} else {
				float sumInner = 0.0f;
				float sumOuter = 0.0f;
				
				for(int i = 0; i < 16; i ++) {
					sumInner += closeTreeMassByDirection[i];
					sumOuter += farTreeMassSmoothed[i];
				}
				
				if(sumOuter < 1.25 * sumInner) {
					// most of trees are just really close, nothing outside of that
					lightExpansionStrategy();
					// okay for edge case where small ring of circles completely blocks,
					// because then we can't be picked as alpha anyways
				} else {
					heavyExpansionStrategy();
				}
			}
			
			
			/*
			if(closestDist < 20.0f && numArchons < 3) { // for now, we consider rushing if there's 2 enemy archons as well
				blitzStrategy();
			} else {
				// NOT VERY GOOD
				for(int i = 0; i < 5; i ++) {
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				}
				int t = 40;
				while(true) {
					checkVPWin();
					//if(rc.getRoundNum() > 200)
					//	rc.resign(); // temporary for testing to prevent 3000 long games
					Direction dir = randomDirection();
					if (rc.canBuildRobot(RobotType.GARDENER, dir) && t > 30) {// && rc.getTeamBullets()>200)
						// do we actually need another gardener though?
						if(2.5f * rc.readBroadcast(101) < rc.readBroadcast(2000)) {
							// only if we have more trees than 2.5 * [num gardeners made]
							// do we decide that we need more gardeners
							rc.buildRobot(RobotType.GARDENER, dir);
							t = 0;
						}
					}
					Clock.yield();
					t++;
					if(t%5 == 0) {
						CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
						//System.out.println("Queued a tree");
					} else if(t%5 == 1) {
						CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
					}
				}
			}*/





			//CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
			//CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
			//CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
			//CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
			//CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
			//System.out.println("Queued three trees");
		} else {
			while(true) {
				// draw some pretty lines
				Direction dir;
				for(int i = 0; i < 15; i ++) {
					dir = randomDirection();
					rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(dir, 4.0f),
							RobotBase.rand.nextInt(255), RobotBase.rand.nextInt(255), RobotBase.rand.nextInt(255));
				}
				Clock.yield();
			}
		}
	}
	

	public void runAlternate() throws GameActionException {
		
		boolean testOtherStuff = true;

		int t = 40;
		while(true) {
			checkVPWin();
			//if(rc.getRoundNum() > 200)
			//	rc.resign(); // temporary for testing to prevent 3000 long games
			if(testOtherStuff) {
				Direction dir = randomDirection();
				if (rc.canBuildRobot(RobotType.GARDENER, dir) && rc.getTeamBullets()>200)
					rc.buildRobot(RobotType.GARDENER, dir);
				Clock.yield();
			}
			else {
				Direction dir = randomDirection();
				if (rc.canBuildRobot(RobotType.GARDENER, dir) && t > 30) {// && rc.getTeamBullets()>200)
					// do we actually need another gardener though?
					if(2.5f * rc.readBroadcast(101) < rc.readBroadcast(2000)) {
						// only if we have more trees than 2.5 * [num gardeners made]
						// do we decide that we need more gardeners
						rc.buildRobot(RobotType.GARDENER, dir);
						t = 0;
					}
				}
				Clock.yield();
				t++;
			}
			if(t%5 == 0) {
				CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				System.out.println("Queued a tree");
			}// else if(t%19 == 1) {
			//	CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
			//}
		}
	}
	
	public void createGrid() throws GameActionException {
		float[] center = locateApproxCenter();
		int xcenter = (int)(center[0]*resolution);
		int ycenter = (int)(center[1]*resolution);
		xoffset = xcenter-(100*resolution);
		yoffset = ycenter-(100*resolution);
		grid = new int[200*resolution][200*resolution];
		for(int i=0; i<grid.length; i++) {
			for (int j=0; j<grid[0].length; j++) {
				grid[i][j]=-1;
			}
		}
	}
	
	public void updateGrid(TreeInfo myTree) {
		float x = myTree.location.x*resolution;
		float y = myTree.location.y*resolution;
		float res = myTree.radius*resolution;
		x = x-xoffset;
		y = y-yoffset;
		for(int xmin=(int)(x-res); xmin<(x+res); xmin++) {
			for(int ymin=(int)(y-res); ymin<(y+res); ymin++) {
				if(Math.sqrt(Math.pow(x-xmin, 2) + Math.pow(y-ymin, 2)) < res) {
					grid[xmin][ymin] = myTree.containedBullets;
				}
			}
		}
		
	}
	
    public float[] locateApproxCenter() throws GameActionException {
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
}








package segfaultplayer;
import battlecode.common.*;


public strictfp class Archon extends RobotBase
{
	private int rank;
	private boolean alpha;
	
	private int[][] grid;

	private float[] closeTreeMassByDirection;
	private float[] farTreeMassSmoothed;
	private float[] farTreeMassByDirection;
	private static final float TREE_BLOCKED_THRESHOLD = 10.0f;
	private static final int GARDENER_SPAWN_TIMEOUT = 100;
	private MapLocation enemyArch;
	
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
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
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
			//Mihir Code MEME
			if(rc.getRoundNum()%30==0) {
				Direction dir = randomDirection();
				if(rc.canBuildRobot(RobotType.GARDENER,dir)) {
					rc.buildRobot(RobotType.GARDENER,dir);
				}
			}

			Clock.yield();
		}
	}
	

	public void noneExpansionStrategy() throws GameActionException {
		System.out.println("Case: EXPANSION-NONE");
		// make enough lumberjacks to get some decent expansion, then start investing
		// in trees until 
		CommunicationsHandler.setSoldierStrategy(rc, SoldierStrategy.PATROL);
		
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SCOUT));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
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
			//Mihir Code MEME
			if(rc.getRoundNum()%30==0) {
				Direction dir = randomDirection();
				if(rc.canBuildRobot(RobotType.GARDENER,dir)) {
					rc.buildRobot(RobotType.GARDENER,dir);
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
		CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
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
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.TANK));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.SOLDIER));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				} else { // something weird happened, hope we can win on VP
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
					CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				}
			}
			//Mihir Code MEME
			if(rc.getRoundNum()%30==0) {
				Direction dir = randomDirection();
				if(rc.canBuildRobot(RobotType.GARDENER,dir)) {
					rc.buildRobot(RobotType.GARDENER,dir);
				}
			}
			Clock.yield();
		}
	}
	
	public void blitzStrategy() throws GameActionException {
		int moveTurnsLeft = 20;
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
			if(moveTurnsLeft > 0) {
				moveTowards(enemyArch, rc.getLocation());
				moveTurnsLeft --;
			}
			checkVPWin();
			if(gardenerCount < 2 && gardenerCooldown <= 0 &&
					rc.getTeamBullets() > RobotType.GARDENER.bulletCost) {
				Direction dir = randomDirection();
				for(int j = 0; j < 20 && !rc.canBuildRobot(RobotType.GARDENER, dir); j++)
					dir = randomDirection();
				if(rc.canBuildRobot(RobotType.GARDENER, dir)) {
					rc.buildRobot(RobotType.GARDENER, dir);
					gardenerCooldown = 70; // BOOSTED
					gardenerCount ++;
				}
			} else if(gardenerCooldown > 0) {
				gardenerCooldown --;
			}
			if(CommunicationsHandler.peekOrder(rc) == null) {
				if(rand.nextBoolean())
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
		farTreeMassByDirection = new float[16];
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
    	

		for(int i = 0; i < 16; i ++) {
			System.out.println("farTreeMassByDirection[" + i + "] = " + farTreeMassByDirection[i]);
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
		
		
		// uncomment if you want to see a doge fly towards enemy archon
		////drawDoge(rc.getLocation(), 0.65f);
		//float distTo = rc.getInitialArchonLocations(enemy)[0].distanceTo(rc.getLocation());
		//Direction dirr = rc.getLocation().directionTo(rc.getInitialArchonLocations(enemy)[0]);
		//for(int i = 0; i < 50; i ++) {
		//	drawDoge(rc.getLocation().add(dirr,i*distTo/50.0f), 0.65f);
		//	Clock.yield();
		//}
		boolean doge = false;
		int turnsSinceGardenerSpawn = 0;
		
		if(alpha) {
			int archonsClose = 2;
			for(int i=0; i<enemyArchons.length; i++)
				if(rc.getLocation().distanceTo(enemyArchons[i])<40.0)
					archonsClose=1;
			if(enemyArchons.length < 3 &&  (rc.getLocation().distanceTo(enemyArchons[0])<20.0 ||
					(enemyArchons.length == 2 && rc.getLocation().distanceTo(enemyArchons[1])<20.0)))
				rc.broadcast(2, 3);
			rc.broadcast(2,archonsClose);
			Direction[] buildDirections = getBestDirections(rc.getLocation().directionTo(enemyArchons[0]),1.0f);
			for(int i=0; i<buildDirections.length; i++) {
				if(rc.canBuildRobot(RobotType.GARDENER,buildDirections[i])) {
					rc.buildRobot(RobotType.GARDENER, buildDirections[i]);
					break;
				}
			}
			Direction curDir = randomDirection();
			MapLocation initLoc = rc.getLocation();
			while(true) {
				buildDirections = getBestDirectionsMihir(rc.getLocation().directionTo(enemyArchons[0]),1.0f);
				int minDir = rand.nextInt(50);
				if(rc.readBroadcast(21)==1 || turnsSinceGardenerSpawn > GARDENER_SPAWN_TIMEOUT) {
					System.out.println("I'm trying to make another gardener, buildDirection is length " + buildDirections.length);
					for(int i=0; i<buildDirections.length; i++) {
						if(i<minDir)continue;
						rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(buildDirections[i], 1.0f),
								i, i, i);
						if(rc.canBuildRobot(RobotType.GARDENER, buildDirections[i])) {
							rc.buildRobot(RobotType.GARDENER, buildDirections[i]);
							rc.broadcast(21,0);
							turnsSinceGardenerSpawn = 0;
							break;
						}
					}
				}
				if(rc.canMove(curDir) && rc.getLocation().distanceTo(initLoc) < 10.0f) {
					rc.move(curDir);
				} else {
					curDir = randomDirection();
					if(rc.canMove(curDir) && rc.getLocation().add(curDir,RobotType.ARCHON.strideRadius).distanceTo(initLoc) < 10.5f) {
						rc.move(curDir);
					}
				}
				
				if(Clock.getBytecodesLeft()>20000 && doge==true)
					drawDoge(rc.getLocation(),0.65f);
				
				turnsSinceGardenerSpawn++;
				Clock.yield();
			}

			/* System.out.println("I am the alpha");
			calculateDensityFar();
			
			
			rc.broadcast(1, CommunicationsHandler.pack(rc.getLocation().x, rc.getLocation().y));

			enemyArch = null;
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

			//CommunicationsHandler.queueOrder(rc, new Order(OrderType.ROBOT, RobotType.LUMBERJACK));

			// alright


			for(int i = 0; i < 16; i ++) {
				System.out.println("farSmoothed[" + i + "] = " + farTreeMassSmoothed[i]);
			}

			float closestDist = rc.getLocation().distanceTo(enemyArch);
			int numArchons = rc.getInitialArchonLocations(enemy).length;
			
			float effectiveDist = closestDist;
			// figure out which direction enemy arch is in
			Direction towardsEnemyArch = rc.getLocation().directionTo(enemyArch);
			float inDeg = towardsEnemyArch.getAngleDegrees() + 11.25f;
			System.out.println("Degrees of direction to enemy archon: " + inDeg);
			while(inDeg < 360f) inDeg += 360f;
			while(inDeg > 360f) inDeg -= 360f;
			int intDir = (int)(inDeg/22.5f);
			System.out.println("(int direction is " + intDir + ")");

			effectiveDist += farTreeMassByDirection[intDir%16];
			effectiveDist += 0.8 * farTreeMassByDirection[(intDir+15)%16];
			effectiveDist += 0.8 * farTreeMassByDirection[(intDir+1)%16];
			
			// alright now we consider cases
			
			System.out.println("Real distance is " + closestDist);
			System.out.println("Effective distance is " + effectiveDist);
			
			if(effectiveDist < 39.0f) { //closestDist < 20.0f) {
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
				
				if(sumOuter < 50.0f) { // sufficiently low
					noneExpansionStrategy();
				} else {
					if(sumOuter < 1.25 * sumInner) {
						// most of trees are just really close, nothing outside of that
						lightExpansionStrategy();
						// okay for edge case where small ring of circles completely blocks,
						// because then we can't be picked as alpha anyways
					} else {
						heavyExpansionStrategy();
					}
				}
			} */
			
			
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
			
			// as non alphas, wait 150 turns before doing anything
			for(int i = 0; i < 150; i ++) {
				drawDoge(rc.getLocation(),0.65f);
				Clock.yield();
			}
			
			boolean myFirstGardener = true;
			while(true) {
				Direction[] buildDirections = getBestDirections(rc.getLocation().directionTo(enemyArchons[0]),1.0f);
				if(myFirstGardener || rc.readBroadcast(21)==1) {
					for(int i=0; i<buildDirections.length; i++) {
						if(rc.canBuildRobot(RobotType.GARDENER,buildDirections[i])) {
							rc.buildRobot(RobotType.GARDENER, buildDirections[i]);
							if(myFirstGardener)
								myFirstGardener = false;
							else
								rc.broadcast(21,0);
							break;
						}
					}
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
    
    
    
    
    
    private int[][][] colorArr = new int[][][] {
    	{{207,185,86},{207,180,73},{232,213,119},{175,145,55},{165,137,72},{178,160,96},{204,199,167},{202,197,159},{204,196,147},{207,199,150},{217,207,158},{224,216,170},{227,219,173},{233,216,160},{223,208,153},{225,204,151},{210,190,137}},
    	{{206,186,91},{215,187,88},{229,208,115},{159,131,57},{176,152,82},{215,196,138},{233,227,191},{226,224,183},{225,221,184},{205,201,166},{212,201,155},{210,198,148},{219,199,146},{204,189,124},{208,185,117},{215,200,145},{214,203,157}},
    	{{208,188,93},{221,196,104},{229,207,122},{170,140,68},{220,189,124},{232,218,169},{220,204,145},{226,217,160},{17,18,12},{38,39,25},{29,30,14},{172,164,117},{201,186,129},{207,187,128},{199,176,106},{208,189,123},{208,189,133}},
    	{{217,189,79},{223,205,107},{224,199,109},{213,180,103},{211,183,109},{41,38,31},{77,66,36},{201,181,120},{9,11,8},{26,29,20},{91,94,87},{90,83,55},{200,182,120},{200,179,112},{198,179,110},{184,160,86},{191,170,107}},
    	{{232,203,109},{231,210,119},{201,171,83},{214,184,112},{213,192,127},{203,185,121},{206,168,93},{215,192,125},{128,130,106},{157,156,138},{138,133,95},{76,72,47},{194,175,109},{196,175,110},{191,172,103},{182,156,81},{182,163,94}},
    	{{191,145,59},{197,151,66},{216,178,95},{206,179,108},{198,167,87},{183,151,68},{214,183,116},{211,182,112},{213,198,141},{193,189,144},{159,143,84},{108,94,55},{184,166,104},{184,165,99},{186,164,89},{175,151,79},{177,155,80}},
    	{{207,187,134},{184,129,46},{204,170,83},{192,145,57},{183,150,71},{201,178,111},{179,146,79},{181,152,76},{200,176,112},{197,182,127},{166,150,91},{185,168,116},{183,165,103},{183,165,99},{183,163,92},{169,147,74},{189,170,104}},
    	{{222,217,179},{220,209,163},{198,153,62},{200,160,72},{190,166,106},{198,178,117},{192,196,181},{120,111,72},{217,202,145},{192,183,140},{196,180,129},{194,174,115},{188,170,108},{186,170,108},{181,160,93},{167,149,77},{191,179,127}},
    	{{221,216,176},{220,212,176},{216,169,79},{183,154,76},{189,165,95},{193,160,83},{11,7,0},{202,185,129},{192,180,128},{196,189,147},{187,170,114},{199,180,124},{193,174,118},{199,182,128},{186,173,120},{170,148,75},{195,184,128}},
    	{{221,216,176},{221,209,171},{198,161,72},{181,149,72},{196,164,79},{185,153,76},{186,158,85},{198,188,137},{198,190,141},{204,194,145},{199,183,131},{193,181,131},{192,183,128},{193,182,126},{200,187,134},{190,175,110},{181,168,113}},
    	{{221,216,176},{220,216,178},{208,159,66},{197,163,76},{182,149,70},{192,163,85},{201,175,100},{207,197,146},{212,204,155},{198,186,138},{197,185,133},{192,179,126},{192,178,129},{203,191,141},{196,180,120},{207,188,129},{193,172,117}},
    	{{218,215,182},{220,220,182},{183,135,53},{199,157,75},{191,158,77},{193,164,84},{200,169,89},{199,187,135},{199,189,136},{197,190,138},{199,183,131},{190,174,122},{194,174,123},{192,175,121},{191,173,111},{185,165,104},{193,177,117}},
    	{{225,218,176},{187,160,81},{169,125,60},{164,125,60},{189,151,70},{198,166,91},{194,161,80},{197,185,127},{193,183,130},{202,190,138},{189,171,109},{202,190,142},{200,181,125},{176,156,87},{197,174,104},{188,162,88},{207,188,129}},
    	{{185,166,98},{179,141,79},{135,98,56},{53,38,15},{107,73,36},{202,162,75},{199,168,86},{200,181,115},{195,183,133},{204,191,138},{190,173,117},{197,187,128},{185,160,93},{187,151,65},{195,159,73},{190,161,81},{214,197,145}},
    	{{225,216,177},{200,169,112},{163,115,43},{158,113,45},{171,134,66},{152,98,34},{183,138,55},{202,165,77},{207,180,113},{192,172,109},{189,163,89},{182,155,78},{186,151,67},{193,148,63},{179,132,52},{205,182,114},{199,189,136}},
    	{{220,213,171},{222,217,177},{219,212,170},{220,215,177},{223,216,174},{222,209,156},{207,178,102},{199,159,74},{205,170,86},{198,163,81},{191,161,75},{184,152,67},{182,142,57},{176,134,50},{191,158,77},{197,181,119},{197,185,133}},
    	{{223,216,174},{224,215,172},{223,218,178},{224,219,177},{221,213,174},{223,215,178},{224,217,175},{226,219,177},{223,214,173},{224,213,168},{222,197,140},{206,177,111},{203,168,86},{197,161,75},{202,176,102},{214,195,127},{213,185,122}}};
    
    private void drawDoge(MapLocation ml, float sizeD) throws GameActionException {
    	int width = colorArr.length;
    	int height = colorArr[0].length;
    	float newX,newY;
    	for(int i = 0; i < width; i ++) {
    		for(int j = 0; j < height; j ++) {
    			newX = ml.x - (i-(width/2))*sizeD;
    			newY = ml.y - (j-(height/2))*sizeD;
    			rc.setIndicatorDot(new MapLocation(newX, newY), colorArr[i][j][0], colorArr[i][j][1], colorArr[i][j][2]);
    		}
    	}
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
}








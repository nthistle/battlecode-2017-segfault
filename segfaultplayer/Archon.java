package segfaultplayer;
import battlecode.common.*;


public strictfp class Archon extends RobotBase
{
	private int rank;
	private boolean alpha;
	
	private int[][] grid;
	
	private float[] treeMassByDirection;
	private static final float TREE_BLOCKED_THRESHOLD = 10.0f;
	
	int xoffset;		// x-offset of grid
	int yoffset;		// y-offset of grid
	int resolution = 2; // grid resolution
	
	public Archon(RobotController rc, int id) throws GameActionException {
		super(rc, id);
		this.calculateDensity();
		this.rank = CommunicationsHandler.assignAlphaArchonProtocol(this, true);
		this.alpha = (this.rank == 0);
	}
	
	public void calculateDensity() throws GameActionException {
    	treeMassByDirection = new float[16];
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
    		treeMassByDirection[realDir] += (ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
    		if(ti.radius > (dist * (float)Math.PI/8.0f)) {
        		treeMassByDirection[(realDir+1)%16] += (0.25f * ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
        		treeMassByDirection[(realDir+15)%16] += (0.25f * ti.radius * ti.radius) * (10.0f-dist)*(10.0f-dist);
    		}
    	}
    	float totalTreeMassFactor = 0.0f;
    	for(int i = 0; i < 16; i ++) {
    		totalTreeMassFactor += treeMassByDirection[i];
    	}
    	////System.out.println("Total Tree Mass Factor: " + totalTreeMassFactor);
    	for(int i = 0; i < 16; i ++) {
    		dir = new Direction(i*(float)Math.PI/8.0f);
    		for(dist = 2.49f; dist < 10.0f; dist += 2.49f) {
    			if(!rc.onTheMap(myLocation.add(dir,dist))) {
    				// boundary in this direction, pretend it's a huge tree
    				treeMassByDirection[i] += 35 * (10.0f-dist)*(10.0f-dist);
    			}
    		}
    	}
	}
	
	public int getBlockedFactor() {
		if(treeMassByDirection != null) {
			// can either be 0, blocking pretty insignificant (< 8 blocked)
			// 1, we're fairly obstructed here (8-12 blocked)
			// 2, please don't pick me to build stuff unless you have to (12+ blocked)
			int nBlocked = 0;
			boolean[] blocked = new boolean[16]; // just for debugging/visualization
			for(int i = 0; i < 16; i ++) {
				if(treeMassByDirection[i] > TREE_BLOCKED_THRESHOLD) {
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
	
	public void run() throws GameActionException {
		
		if(alpha) {
			System.out.println("I am the alpha");
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
			
			// alright now we consider cases
			if(closestDist < 20.0f) {
				System.out.println("Case: BLITZ");
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
			}
			
			
			
			

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

		
		/*boolean testOtherStuff = false;

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
		}*/
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
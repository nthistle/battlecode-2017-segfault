package segfaultplayer;
import battlecode.common.*;


public strictfp class Archon extends RobotBase
{
	private int rank;
	private boolean alpha;
	
	private int[][] grid;
	int xoffset;		// x-offset of grid
	int yoffset;		// y-offset of grid
	int resolution = 2; // grid resolution
	
	public Archon(RobotController rc, int id) throws GameActionException {
		super(rc, id);
		this.rank = CommunicationsHandler.assignAlphaArchonProtocol(this, true);
		this.alpha = (this.rank == 0);
	}
	
	public void run() throws GameActionException {
		
		if(alpha) {
			System.out.println("I am the alpha");
			rc.broadcast(1, CommunicationsHandler.pack(rc.getLocation().x, rc.getLocation().y));
			
			MapLocation enemyArch = rc.getInitialArchonLocations(enemy)[0];
			CommunicationsHandler.addTree(rc, enemyArch.x, enemyArch.y);

			CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
			CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
			System.out.println("Queued two trees");
		}

		boolean testOtherStuff = false;

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
					rc.buildRobot(RobotType.GARDENER, dir);
					t = 0;
				}
				Clock.yield();
				t++;
			}
			if(t%17 == 0) {
				CommunicationsHandler.queueOrder(rc, new Order(OrderType.TREE));
				System.out.println("Queued a tree");
			}
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
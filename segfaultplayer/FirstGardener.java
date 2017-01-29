package segfaultplayer;
import java.util.ArrayList;

import battlecode.common.*;


public strictfp class FirstGardener extends HexGardener
{
	private float[] aArch;
	private MapLocation alphaLoc;
	
	private int[] myPodStatus;
	
	private MapLocation[] myPodLocations;
	
	private Direction[] podDirs = new Direction[] {
			new Direction(0),
			new Direction((float)Math.PI/3.0f),
			new Direction(2.0f*(float)Math.PI/3.0f),
			new Direction(3.0f*(float)Math.PI/3.0f),
			new Direction(4.0f*(float)Math.PI/3.0f),
			new Direction(5.0f*(float)Math.PI/3.0f)
	};
	
	public static final float halfDirection = (float)Math.PI/6.0f;

	// tree blocked is considered more "permanently" blocked than unit blocked,
	// and so takes precedence
	public static final int TREE_BLOCKED_SPOT = 1;
	public static final int UNIT_BLOCKED_SPOT = 2;
	public static final int FREE_SPOT = 3;
	public static final int PLANTED_SPOT = 4;
	
	public static final int PHASE_2_MAX_TREES = 4; // so that tanks can happen
	
	private int numPodTrees = 0; // how many trees our pod currently has planted ((ignores destroyed))
	private int openDirection = -1; // which direction we open in 
	private int buildCooldown = 0; // ticks until we build again
	
	public static final int BUILD_COOLDOWN = 15;
	

	public FirstGardener(RobotController rc, int id) throws GameActionException {
		super(rc, id);
		myPodStatus = new int[6];
	}

	@Override 
	public void run() throws GameActionException {

		aArch = CommunicationsHandler.unpack(rc.readBroadcast(1));
		alphaLoc = new MapLocation(aArch[0],aArch[1]);
		
		// first thing we have to do is fine a location for our pod

		findFirstPodLocation(); // TODO: cap this version to 5 turns
		
		
		setPodLocations();
		updatePodStatus();
		drawPodStatus();
		
		/*
		
		// case 1 is we have 
		
		setPodOpenDir();
		drawPodOpenDir();
		
		if(rc.senseNearbyTrees(3.0f,Team.NEUTRAL).length > 0) {
			// attempt to build a lumberjack
			// if we can't, just give up, TODO: make it try again later
			for(float rads = 0.0f; rads < (float)Math.PI*2.0f; rads += (float)Math.PI/8) {
				if(rc.canBuildRobot(RobotType.LUMBERJACK, new Direction(rads))) {
					rc.buildRobot(RobotType.LUMBERJACK, new Direction(rads));
					Clock.yield();
					break;
				}
			}
		}
		
		
		// phase 1
		while(rc.getRoundNum() < 100) {
			try {
				// standard stuff we should do every turn
				checkVPWin();
				updatePodStatus();
				drawPodStatus();
				
				// building stuff during phase 1 is done entirely through order queue
				Order nextOrder = CommunicationsHandler.peekOrder(rc);
				if(nextOrder.type == OrderType.TREE) {
					if(isAbleToBuildTree()) {
						if(addTreeToPod()) {
							System.out.println("Filled a tree order");
							CommunicationsHandler.popOrder(rc);
						}
					}
				} else {
					if(buildCooldown <= 0) {
						if(rc.getTeamBullets() > nextOrder.rt.bulletCost) {
							Direction buildDir = getBuildDirection(nextOrder.rt);
							if(buildDir == null) {
								// can't build this, ohwell
							} else {
								System.out.println("Filled an order for " + nextOrder.rt.toString());
								rc.buildRobot(nextOrder.rt, buildDir);
								CommunicationsHandler.popOrder(rc);
							}
						}
					}
				}
				
				// more standard stuff
				drawPodOpenDir();
				waterLowest();
				if(buildCooldown>0) buildCooldown--;
				
			} catch(Exception e) {
				// should never trigger except in weird instances of buildRobot throwing GameActionException,
				// here for security more than anything
				e.printStackTrace();
			}
			Clock.yield();
		}
		// alright, now round num has exceeded 100
		
		// phase 2
		int unitsBuilt = 0;
		int treesPlanted = 0;
		while(true) {
			try {
				// standard stuff
				checkVPWin();
				updatePodStatus();
				drawPodStatus();
				
				if(buildCooldown <= 0) {
					// build stuff for phase 2 is done based on ratio
					System.out.println("Ratio: "+getFloatRatio());
						// build a unit to make up for it
						if(numPodTrees >= PHASE_2_MAX_TREES || treesPlanted > getFloatRatio() * unitsBuilt) {

							RobotType nextType = getNextRobotBuildType();

						Direction buildDir = getBuildDirection(nextType);
						if(buildDir == null) {
							// can't build this, ohwell
						} else {
							rc.buildRobot(nextType, buildDir);
							hasBuiltType(nextType);
							buildCooldown = 15;
							unitsBuilt ++;
						}
					} else {
						// plant a tree to make up for it
						if(isAbleToBuildTree()) {
							if(addTreeToPod()) {
								treesPlanted ++;
							}
						}
					}
				}
				
				// more standard stuff
				waterLowest();
				if(buildCooldown>0) buildCooldown--;
				
			} catch(Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}*/
	}
	
	
	public void findFirstPodLocation() throws GameActionException {
		this.findPodLocation();
	}
}
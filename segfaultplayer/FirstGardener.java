package segfaultplayer;
import java.util.ArrayList;
import java.util.LinkedList;

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
		
		int numFree = 0;
		for(int i : myPodStatus) {
			if(i == FREE_SPOT) {
				numFree ++;
			}
		}
		
		if(numFree <= 1) {
			caseClosed();
		} else if(rc.readBroadcast(2) == 2) {
			caseFar();
		} else {
			caseNear();
		}
	}
	
	public void caseClosed() throws GameActionException {
		System.out.println("CASE: CLOSED");
		LinkedList<Order> myOrders = new LinkedList<Order>();
		myOrders.addLast(new Order(OrderType.ROBOT, RobotType.LUMBERJACK));
		myOrders.addLast(new Order(OrderType.ROBOT, RobotType.SOLDIER));
		myOrders.addLast(new Order(OrderType.TREE));
		myOrders.addLast(new Order(OrderType.ROBOT, RobotType.SCOUT));
		myOrders.addLast(new Order(OrderType.TREE));
		myOrders.addLast(new Order(OrderType.TREE));
		while(myOrders.size() > 0) {
			checkVPWin();
			updatePodStatus();
			drawPodStatus();
			
			Order nextOrder = myOrders.peekFirst();
			if(nextOrder.type == OrderType.TREE) {
				if(isAbleToBuildTree()) {
					if(addTreeToPod()) {
						System.out.println("We succesfully added our tree");
						myOrders.pollFirst();
					}
				}
			} else {
				if(buildCooldown <= 0) {
					if(rc.getTeamBullets() > nextOrder.rt.bulletCost) {
						Direction buildDir = getBuildDirection(nextOrder.rt);
						if(buildDir == null) {
							// can't build this, ohwell
						} else {
							rc.buildRobot(nextOrder.rt, buildDir);
							System.out.println("We succesfully built a " + nextOrder.rt.toString());
							buildCooldown = 15;
						}
					}
				}
			}
			
			waterLowest();
			if(buildCooldown>0) buildCooldown--;
			Clock.yield();
		}
		
		weNeedAnotherGardener();
		
		// TODO: make this main loop a little more sophisticated
		while(true) {
			checkVPWin();
			waterLowest();
			Clock.yield();
		}
		
	}
	
	public void caseFar() throws GameActionException {
		System.out.println("CASE: FAR");
		LinkedList<Order> myOrders = new LinkedList<Order>();
		myOrders.addLast(new Order(OrderType.ROBOT, RobotType.SOLDIER));
		myOrders.addLast(new Order(OrderType.ROBOT, RobotType.SCOUT));
		myOrders.addLast(new Order(OrderType.TREE));
		myOrders.addLast(new Order(OrderType.TREE));
		while(myOrders.size() > 0) {
			checkVPWin();
			updatePodStatus();
			drawPodStatus();
			
			Order nextOrder = myOrders.peekFirst();
			if(nextOrder.type == OrderType.TREE) {
				if(isAbleToBuildTree()) {
					if(addTreeToPod()) {
						System.out.println("We succesfully added our tree");
						myOrders.pollFirst();
					}
				}
			} else {
				if(buildCooldown <= 0) {
					if(rc.getTeamBullets() > nextOrder.rt.bulletCost) {
						Direction buildDir = getBuildDirection(nextOrder.rt);
						if(buildDir == null) {
							// can't build this, ohwell
						} else {
							rc.buildRobot(nextOrder.rt, buildDir);
							System.out.println("We succesfully built a " + nextOrder.rt.toString());
							buildCooldown = 15;
						}
					}
				}
			}
			
			waterLowest();
			if(buildCooldown>0) buildCooldown--;
			Clock.yield();
		}
		
		weNeedAnotherGardener();
		
		// TODO: make this main loop a little more sophisticated
		while(true) {
			checkVPWin();
			waterLowest();
			Clock.yield();
		}
	}
	
	public void caseNear() throws GameActionException {
		System.out.println("CASE: NEAR");
		LinkedList<Order> myOrders = new LinkedList<Order>();
		myOrders.addLast(new Order(OrderType.ROBOT, RobotType.SOLDIER));
		myOrders.addLast(new Order(OrderType.ROBOT, RobotType.SOLDIER));
		myOrders.addLast(new Order(OrderType.TREE));
		myOrders.addLast(new Order(OrderType.TREE));
		myOrders.addLast(new Order(OrderType.TREE));
		while(myOrders.size() > 0) {
			checkVPWin();
			updatePodStatus();
			drawPodStatus();
			
			Order nextOrder = myOrders.peekFirst();
			if(nextOrder.type == OrderType.TREE) {
				if(isAbleToBuildTree()) {
					if(addTreeToPod()) {
						System.out.println("We succesfully added our tree");
						myOrders.pollFirst();
					}
				}
			} else {
				if(buildCooldown <= 0) {
					if(rc.getTeamBullets() > nextOrder.rt.bulletCost) {
						Direction buildDir = getBuildDirection(nextOrder.rt);
						if(buildDir == null) {
							// can't build this, ohwell
						} else {
							rc.buildRobot(nextOrder.rt, buildDir);
							System.out.println("We succesfully built a " + nextOrder.rt.toString());
							buildCooldown = 15;
						}
					}
				}
			}
			
			waterLowest();
			if(buildCooldown>0) buildCooldown--;
			Clock.yield();
		}
		
		weNeedAnotherGardener();
		
		// TODO: make this main loop a little more sophisticated
		while(true) {
			checkVPWin();
			waterLowest();
			Clock.yield();
		}
	}

	public void weNeedAnotherGardener() throws GameActionException {
		rc.broadcast(21, 1);
	}
	
	public void findFirstPodLocation() throws GameActionException {
		this.findPodLocation();
	}
}
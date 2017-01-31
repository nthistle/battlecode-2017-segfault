package segfaultplayer_2;
import java.util.ArrayList;
import java.util.LinkedList;

import battlecode.common.*;


public strictfp class FirstGardener extends HexGardener
{
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
		setPodOpenDir();
		drawPodOpenDir();
		Clock.yield();
		
		if(rc.senseNearbyTrees(3.5f, Team.NEUTRAL).length > 0) {
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
							myOrders.pollFirst();
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
		normalBehavior();
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
							myOrders.pollFirst();
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
		normalBehavior();
	}
	
	public void caseNear() throws GameActionException {
		System.out.println("CASE: NEAR");
		LinkedList<Order> myOrders = new LinkedList<Order>();
		myOrders.addLast(new Order(OrderType.ROBOT, RobotType.SOLDIER));
		myOrders.addLast(new Order(OrderType.ROBOT, RobotType.SOLDIER));
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
							myOrders.pollFirst();
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
		
		normalBehavior();
	}
	
	// this is basically just phase 2 code
	public void normalBehavior() throws GameActionException {

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
					//System.out.println("Ratio: "+getFloatRatio());
						// build a unit to make up for it
					if(numPodTrees >= PHASE_2_MAX_TREES || treesPlanted > getFloatRatio() * unitsBuilt) {

						RobotType nextType = getNextRobotBuildType();

						Direction buildDir = getBuildDirection(nextType);
						if(buildDir == null) {
							// can't build this, ohwell
						} else {
							rc.buildRobot(nextType, buildDir);
							hasBuiltType(nextType);
							buildCooldown = 15; //replace with actual constant
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
		}
	}
	
	public void findFirstPodLocation() throws GameActionException {
		float currentValue = 1000000.0f;
		float newValue = currentValue - 1;
    	Direction[] myDirs = getDirections(Direction.NORTH, 60.0f);
    	int t = 0;
    	while(newValue < currentValue && t < 5) {
    		currentValue = newValue;
    		t++;
			float[][] intendedThing = findOptimalPlace(60.0f);
			newValue = intendedThing[0][1];
			Direction intendedDirection = myDirs[(int)intendedThing[0][0]];
			moveInDir(intendedDirection);
			
			rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(intendedDirection, 3.0f),
					0, 255, 0);
			for(int i = 0; i < intendedThing.length; i ++) {
				int tval = (int)(10.0f*intendedThing[i][1]);
				//System.out.println("Value is " + tval);
				rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(myDirs[(int)intendedThing[i][0]]),
						tval, 0, 0);
			}
			Clock.yield();
		}
	}
}
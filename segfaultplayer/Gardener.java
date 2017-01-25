package segfaultplayer;
import battlecode.common.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;


public strictfp class Gardener extends RobotBase
{
	public static final float MAJOR_AXIS_CRAD = 76.5f; // eventually int, or do millirads 
	public static final float SPACING_DISTANCE = 4.5f;
	public static final float VALID_ANGLE_THRESH = 15.0f; // in degrees
	public static final float BUFFER_DIST = 2.5f; // if you must know what this does, ask me

	public static final boolean DOES_BROADCAST_INFO = true;

	private float[] aArch;
	private MapLocation alphaLoc;

	public Gardener(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}

	public void runAlternate(RobotType rcs) throws GameActionException {
		int ctr = 0;
		while(true) {
			TreeInfo[] trees = rc.senseNearbyTrees(2.0f,rc.getTeam());
			Direction dir = randomDirection();
			if(rc.canBuildRobot(rcs,dir)&&ctr<1) {
				rc.buildRobot(rcs, dir);
				ctr++;
			}
			if(rc.canBuildRobot(RobotType.TANK,dir)) // was tank
				rc.buildRobot(RobotType.TANK,dir);
			else if(rc.canPlantTree(dir) && trees.length<2)
				rc.plantTree(dir);
			dir = randomDirection();
			TreeInfo tree = null;
			for(int i=0; i<trees.length; i++)
				if(tree==null || tree.getHealth()>trees[i].getHealth())
					tree = trees[i];
			if(tree!=null && rc.canWater(tree.getID()))
				rc.water(tree.getID());
			Clock.yield();
		}
	}

	public void run() throws GameActionException {

		aArch = CommunicationsHandler.unpack(rc.readBroadcast(1));
		alphaLoc = new MapLocation(aArch[0],aArch[1]);

		// MAIN GARDENER CODE
		int myBuildCooldown = 0;
		int timeSinceSeenFurther = 50;
		Order nextOrder;
		while(true) {
			try {
				checkVPWin(); // boilerplate
				if(areFurtherGardeners()) {
					setIndicatorX(rc.getLocation(), 0, 0, 0);
					timeSinceSeenFurther = 0;
				} else timeSinceSeenFurther ++;
				// only consider orders if we're close to fringe, i.e.,
				// one or fewer fellow gardeners further from alpha arch
				if(timeSinceSeenFurther > 15) {//numFurtherGardeners == 0) {//<= 1) {
					// this case we consider ourself a frontier gardener

					if(DOES_BROADCAST_INFO && (rc.getRoundNum() % 50 == 0)) {
						// count friendly lumberjacks and neutral trees
						RobotInfo[] friendlies = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
						int numFLJs = 0;
						for(RobotInfo ri : friendlies) {
							if(ri.getType() == RobotType.LUMBERJACK) {
								numFLJs ++;
							}
						}
						int numNTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL).length;

						if(rc.readBroadcast(400) != rc.getRoundNum()) {
							// nobody has reset yet
							rc.broadcast(400, rc.getRoundNum());
							rc.broadcast(401, numFLJs);
							rc.broadcast(402, numNTrees);
						} else {
							// okay we just add ours
							rc.broadcast(401, numFLJs + rc.readBroadcast(401));
							rc.broadcast(402, numNTrees + rc.readBroadcast(402));
						}
					}



					// check if we can build something and if we can
					nextOrder = CommunicationsHandler.peekOrder(rc);
					if(myBuildCooldown <= 0 && nextOrder != null) {
						//System.out.println("Trying order!");
						if(nextOrder.type == OrderType.TREE) {
							CommunicationsHandler.popOrder(rc);
							if(!addToGrid()) System.out.println("Problem adding a tree to grid, although received order");
							myBuildCooldown = 11;
						} else {
							if(rc.getTeamBullets() > nextOrder.rt.bulletCost) {
								Direction[] buildDirs = getGoodBuildDirections(alphaLoc.directionTo(rc.getLocation()));
								for(Direction d : buildDirs) {
									if(rc.canBuildRobot(nextOrder.rt, d)) {
										CommunicationsHandler.popOrder(rc);
										rc.buildRobot(nextOrder.rt, d);
										myBuildCooldown = 11;
										break;
									}
								}
								//Direction dir = randomDirection();
								//for(int attempt = 0; attempt < 20 && !rc.canBuildRobot(nextOrder.rt, dir); attempt ++)
								//	dir = randomDirection();
								//if(rc.canBuildRobot(nextOrder.rt, dir)) {
								//	CommunicationsHandler.popOrder(rc);
								//	rc.buildRobot(nextOrder.rt, dir);
								//	myBuildCooldown = 11;
								//}
							}
						}
					} else
						myBuildCooldown --;
				}
				// some kind of watering protocol here
				gridStepFunction();
			} catch(Exception e) {
				e.printStackTrace();
				setIndicatorPlus(rc.getLocation(), 255, 0, 0);
			}
			Clock.yield();
		}


		//addToGrid();
		//addToGrid();
		//while(true) {
		//if(rc.getRoundNum() % 50 == 0)
		//		addToGrid();
		//else
		//	stepCircleRoutine();
		//	Clock.yield();
		//}



		//TESTING CODE: Comment in for testing stuff
		/*
		int ctr = 0;
		while(true) {
			TreeInfo[] trees = rc.senseNearbyTrees(2.0f,rc.getTeam());
			Direction dir = randomDirection();
			if(rc.canBuildRobot(RobotType.LUMBERJACK,dir)&&ctr<1) {
				rc.buildRobot(RobotType.LUMBERJACK, dir);
				ctr++;
			}
			if(rc.canBuildRobot(RobotType.TANK,dir)) // was tank
				rc.buildRobot(RobotType.TANK,dir);
			else if(rc.canPlantTree(dir) && trees.length<2)
				rc.plantTree(dir);
			dir = randomDirection();
			TreeInfo tree = null;
			for(int i=0; i<trees.length; i++)
				if(tree==null || tree.getHealth()>trees[i].getHealth())
					tree = trees[i];
			if(tree!=null && rc.canWater(tree.getID()))
				rc.water(tree.getID());
			Clock.yield();
		} */

//		while(true) {
//			TreeInfo[] trees = rc.senseNearbyTrees(2.0f,rc.getTeam());
//			Direction dir = randomDirection();
//			if(rc.canBuildRobot(RobotType.LUMBERJACK,dir)) // was tank
//				rc.buildRobot(RobotType.LUMBERJACK,dir);
//			else if(rc.canPlantTree(dir) && trees.length<2)
//				rc.plantTree(dir);
//			dir = randomDirection();
//			TreeInfo tree = null;
//			for(int i=0; i<trees.length; i++)
//				if(tree==null || tree.getHealth()>trees[i].getHealth())
//					tree = trees[i];
//			if(tree!=null && rc.canWater(tree.getID()))
//				rc.water(tree.getID());
//			Clock.yield();
//		}

		/* OLD MEME
		else {
			RobotInfo nearestArchon = getNearest(RobotType.ARCHON, ally);
			Direction dir = nearestArchon.getLocation().directionTo(rc.getLocation());
			for (int i = 0; i < 20 && !rc.canMove(dir); i++)
				dir = randomDirection();
			rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(dir, 5.0f), 255, 0, 0);
			for (int i = 0; i < 25; i++) {
				if (rc.canMove(dir))
					rc.move(dir);
				Clock.yield();
			}
			makeHexPod();
			lifetimeWaterLowest();
		}*/
	}

	public Direction[] getGoodBuildDirections(Direction intended) {
		Direction[] buildDirs = new Direction[17];
		buildDirs[0] = intended;
		for(int i = 0; i < 8; i ++) {
			buildDirs[1+(2*i)] = intended.rotateRightDegrees(11.25f * (i+1));
			buildDirs[2+(2*i)] = intended.rotateLeftDegrees(11.25f * (i+1));
		}
		return buildDirs;
	}


	public boolean areFurtherGardeners() throws GameActionException {
		RobotInfo[] ri = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
		int numFurtherGardeners = 0;
		float myDist = rc.getLocation().distanceTo(alphaLoc);
		for(RobotInfo r : ri) {
			if(r.getType() == RobotType.GARDENER) {
				if(r.getLocation().distanceTo(alphaLoc) > (myDist + BUFFER_DIST)) {
					if(alphaLoc.directionTo(r.getLocation()).degreesBetween(
							alphaLoc.directionTo(rc.getLocation())) < VALID_ANGLE_THRESH) {
						numFurtherGardeners ++;
					}
				}
			}
		}
		return numFurtherGardeners > 0;
	}


	public void gridStepFunction() throws GameActionException {
		TreeInfo[] myTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, rc.getTeam());


		if (myTrees.length > 0) {

			RobotInfo[] nRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());

			ArrayList<TreeInfo> trees = new ArrayList<TreeInfo>();
			for(TreeInfo ti : myTrees)
				trees.add(ti);
			ArrayList<TreeInfo> lowerPriority = new ArrayList<TreeInfo>();

			for(RobotInfo ri : nRobots) {
				if(ri.getType() == RobotType.GARDENER &&
						ri.getLocation().distanceTo(alphaLoc) < rc.getLocation().distanceTo(alphaLoc)) {
					// pretend all trees within 4.5 of friendly gardeners don't exist,
					// so as to prevent us from bumping into them
					// but ONLY if they're closer to alpha than us
					Iterator<TreeInfo> it = trees.iterator();
					while(it.hasNext()) {
						TreeInfo t = it.next();
						if(t.getLocation().distanceTo(ri.getLocation()) < 4.5) {
							it.remove();
							lowerPriority.add(t);
							//
							rc.setIndicatorLine(t.getLocation(), ri.getLocation(), 150, 50, 0);
							//
						}
					}
				}
			}

			// now, among the trees that remain, find lowest

			if(trees.size() > 0) {
				TreeInfo lowest = trees.get(0);
				for(int i = 1; i < trees.size(); i ++) {
					if(trees.get(i).getHealth() < lowest.getHealth())
						lowest = trees.get(i);
				}
				rc.setIndicatorLine(rc.getLocation(), lowest.getLocation(), 255, 0, 150);
				moveTowards(rc.getLocation(), lowest.getLocation());
				waterLowest();
			} else if(lowerPriority.size() > 0) {
				TreeInfo lowest = lowerPriority.get(0);
				for(int i = 1; i < lowerPriority.size(); i ++) {
					if(lowerPriority.get(i).getHealth() < lowest.getHealth())
						lowest = lowerPriority.get(i);
				}
				rc.setIndicatorLine(rc.getLocation(), lowest.getLocation(), 255, 0, 150);
				moveTowards(rc.getLocation(), lowest.getLocation());
				waterLowest();
			} else {
				waterLowest();
			}

		} else {
			// we don't see any friendly trees? really?
			// okay just move towards our alpha archon then
			MapLocation target = new MapLocation(aArch[0], aArch[1]);
			// but only if we're further than 7 away, otherwise we're probably just 
			// supposed to wait for build orders (hopefully?)
			if(rc.getLocation().distanceTo(target) < 7.0f)
				if(!moveTowards(rc.getLocation(), target))
					if(rc.canMove(target.directionTo(rc.getLocation())))
						rc.move(target.directionTo(rc.getLocation()));
		}
	}

	public boolean addToGrid() throws GameActionException {
		int ntrees = CommunicationsHandler.getNumTrees(rc);
		if(ntrees == 0) {
			// this is a whole nother case bud
			return false; // TODO: make it plant the initial tree

		} else {
			for(int attempt = 0; attempt < 3; attempt ++) { // 3 attempts 
				float[][] trees = CommunicationsHandler.getTreeLocations(rc);
				float withinDist = SPACING_DISTANCE * (float)Math.sqrt(2.0);
				float[] current = trees[0];
				MapLocation myLoc = rc.getLocation();

				// draw blue + where we think are trees, for visualization purposes
				//for(float[] t : trees) {
				//setIndicatorPlus(new MapLocation(t[0],t[1]), 0, 0, 255);
				//rc.setIndicatorDot(new MapLocation(t[0],t[1]), 0, 0, 255);
				//}

				for(int i = 1; i < trees.length; i ++) {
					if(getDist(trees[i][0], trees[i][1], myLoc.x, myLoc.y) <
							getDist(current[0], current[1], myLoc.x, myLoc.y)) {
						current = trees[i];
					}
				}
				rc.setIndicatorDot(new MapLocation(current[0],current[1]), 255, 0, 0);
				// red means the nearest planted tree, now we extrapolate closer
				MapLocation[] neighbors;
				MapLocation best;


				neighbors = getNeighborTreeLocs(new MapLocation(current[0],current[1]));
				// this is basically findClosest here, but with a stipulation that it has to
				// satisfy cellHelperIsValid, which checks to make sure there's not already a tree
				// there (more efficient than going through float[][] trees), and that it's on
				// the map (for now)
				best = null;
				for(int i = 0; i < neighbors.length; i ++) {
					if(best == null) {
						if(cellHelperIsValid(neighbors[i]))
							best = neighbors[i];
					}
					else if(neighbors[i].distanceSquaredTo(myLoc) < best.distanceSquaredTo(myLoc)) {
						if(cellHelperIsValid(neighbors[i]))
							best = neighbors[i];
					}
				}

				if(best == null) {
					best = new MapLocation(current[0],current[1]);
				}

				current[0] = best.x;
				current[1] = best.y;

				//rc.setIndicatorDot(best, 0, 255, 0); // green means extrapolation point

				//System.out.println("Current dist is " + getDist(current[0],current[1],myLoc.x,myLoc.y));

				while(getDist(current[0],current[1],myLoc.x,myLoc.y) > withinDist) {
					// I'm not really sure what I was smoking when I made this while loop condition
					// TODO: make it so that as long as dist is decreasing we're good
					// for now we're relying on the break below in that case

					//System.out.println(" -> Current dist is " + getDist(current[0],current[1],myLoc.x,myLoc.y));


					neighbors = getNeighborTreeLocs(new MapLocation(current[0],current[1]));


					// this is basically findClosest here, but with a stipulation that it has to
					// satisfy cellHelperIsValid, which checks to make sure there's not already a tree
					// there (more efficient than going through float[][] trees), and that it's on
					// the map (for now)
					best = null;
					for(int i = 0; i < neighbors.length; i ++) {
						if(best == null) {
							if(cellHelperIsValid(neighbors[i]))
								best = neighbors[i];
						}
						else if(neighbors[i].distanceSquaredTo(myLoc) < best.distanceSquaredTo(myLoc)) {
							if(cellHelperIsValid(neighbors[i]))
								best = neighbors[i];
						}
					}

					if(getDist(current[0],current[1],myLoc.x,myLoc.y) < best.distanceTo(myLoc)) {
						// we're just oscillating, the "best" one we found is worse than current
						best = new MapLocation(current[0],current[1]);
						break;
					}

					//best = RobotBase.findClosest(myLoc, neighbors);

					rc.setIndicatorLine(myLoc, best, 255, 255, 255);
					rc.setIndicatorDot(best, 0, 255, 0); // green means extrapolation point
					current[0] = best.x;
					current[1] = best.y;
				}

				if(best == null) {
					best = new MapLocation(current[0],current[1]);
				}


				rc.setIndicatorLine(myLoc, best, 255, 255, 0);

				boolean tryAgain = false;

				// alright, now let's move towards this point, capped at 20 turns of movement (give up)
				for(int mt = 0; mt < 20 && myLoc.distanceTo(best) > 2.5f; mt ++) {
					//rc.setIndicatorDot(best, 0, 0, 0);
					if(!cellHelperIsValid(best)) {
						if(!rc.onTheMap(best))
							return false;
						tryAgain = true;
						break;
					}
					myLoc = rc.getLocation();
					rc.setIndicatorLine(myLoc, best, 255, 255, 0);
					if(!moveTowards(myLoc, best))
						moveTowards(best, myLoc); // attempt to reverse 1 step
					waterLowest();
					Clock.yield();
				}

				if(tryAgain) continue;

				//setIndicatorX(best, 0, 0, 0);

				myLoc = rc.getLocation();

				// hopefully we're within 2.5 units of best now
				// we want to be exactly 2.0 units away
				float tdist = myLoc.distanceTo(best);
				if(tdist > 2.0f) {
					if(rc.canMove(myLoc.directionTo(best), tdist-2.0f))
						rc.move(myLoc.directionTo(best), tdist-2.0f);
				} else {
					// that means we're actually probably too close
					if(rc.canMove(best.directionTo(myLoc), 2.0f-tdist))
						rc.move(best.directionTo(myLoc), 2.0f-tdist);
				}
				Clock.yield();
				// hopefully now we're exactly 2.0 units away
				myLoc = rc.getLocation();
				if(myLoc.distanceTo(best) < 2.15f && myLoc.distanceTo(best) > 1.85f) { // 0.15 is tolerable
					for(int mt = 0; mt < 30 && rc.getTeamBullets() < 50.0f; mt ++) { // wait up to 30 rounds
						waterLowest();
						Clock.yield();
						// waterLowest might not hit anything, but better than completely stalling
					}
					myLoc = rc.getLocation();
					if(rc.getTeamBullets() >= 50.0f) {
						// whoopdiedoo, build
						if(rc.canPlantTree(myLoc.directionTo(best))) {
							rc.plantTree(myLoc.directionTo(best));
							System.out.println("Building a tree, extending stuff");
							// even though 0.2 is fine, we don't want it to propagate, so we're broadcasting
							// the ideal coords of this tree
							CommunicationsHandler.addTree(rc, best.x, best.y);
							return true;
						} else {
							// nice waste of time, TODO: blacklist this point or something,
							// so we don't try to build here again (for a while?)
						}
					} else return false;
				}
			}

		}
		return false; // not sure why we'd ever get here though
	}

	private boolean cellHelperIsValid(MapLocation ml) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		if(ml.distanceTo(myLoc) < rc.getType().sensorRadius) {
			return rc.onTheMap(ml) && rc.senseNearbyTrees(ml,1.0f,null).length == 0 && rc.senseNearbyRobots(ml, 1.0f, null).length == 0;
		} else {
			return true;
		}
	}


	public MapLocation[] getNeighborTreeLocs(MapLocation m) {

		Direction[] dirs = new Direction[4];
		dirs[0] = new Direction(MAJOR_AXIS_CRAD/100.0f);
		dirs[1] = new Direction(((float)Math.PI/2.0f) + MAJOR_AXIS_CRAD/100.0f);
		dirs[2] = new Direction(((float)Math.PI) + MAJOR_AXIS_CRAD/100.0f);
		dirs[3] = new Direction((3.0f*(float)Math.PI/2.0f) + MAJOR_AXIS_CRAD/100.0f);

		MapLocation[] neighbors = new MapLocation[8];
		// 1 off neighbors
		neighbors[0] = m.add(dirs[0], SPACING_DISTANCE);
		neighbors[1] = m.add(dirs[1], SPACING_DISTANCE);
		neighbors[2] = m.add(dirs[2], SPACING_DISTANCE);
		neighbors[3] = m.add(dirs[3], SPACING_DISTANCE);

		// in-between neighbors (SPACING_DISTANCE * sqrt(2) away)
		neighbors[4] = neighbors[0].add(dirs[1], SPACING_DISTANCE);
		neighbors[5] = neighbors[1].add(dirs[2], SPACING_DISTANCE);
		neighbors[6] = neighbors[2].add(dirs[3], SPACING_DISTANCE);
		neighbors[7] = neighbors[3].add(dirs[0], SPACING_DISTANCE);

		return neighbors;
	}

	/**
	 * Waters the lowest until the end of time
	 * At some point, this should be replaced (or wherever it's used) with more
	 * dynamic code that enables a gardener to run away if it's in danger etc.
	 */
	public void lifetimeWaterLowest() throws GameActionException {
		while(true) {
			waterLowest();
			Clock.yield();
		}
	}

	/**
	 * Creates a full hex pod wherever it is when called, blocking in all 6 sides,
	 * uses up bullets as soon as it can get them (50+)
	 */
	public void makeHexPod() throws GameActionException {
		float smallAngleInc = (float)Math.PI/16.0f;
		float treeOffsetAngle = (float)Math.PI/3.0f;
		while(rc.getTeamBullets() < RobotType.GARDENER.bulletCost) {
			waterLowest();
			Clock.yield();
		}
		// now we can afford a gardener
		float cAngle = 0.0f;
		while(cAngle < 2.0*(float)Math.PI && !rc.canPlantTree(new Direction(cAngle)))
			cAngle += smallAngleInc;
		if(!rc.canPlantTree(new Direction(cAngle)))
			return; // hex pod failed
		boolean[] planted = new boolean[] {true, false, false, false, false, false};
		rc.plantTree(new Direction(cAngle));
		waterLowest();
		Clock.yield();
		for(int i = 0; i < 5; i ++) {
			System.out.println("Trying " + i);
			for(int j = 0; j < 10 || rc.getTeamBullets() < 50.0f; j ++) {
				waterLowest();
				Clock.yield();
			}
			rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(new Direction(cAngle + (i+1) * treeOffsetAngle), 2.0f), 0, 255, 0);
			System.out.println("Now we have enough bullets!");
			if(rc.canPlantTree(new Direction(cAngle + (i+1) * treeOffsetAngle))) {
				planted[i+1] = true;
				rc.plantTree(new Direction(cAngle + (i+1) * treeOffsetAngle));
				waterLowest();
				Clock.yield();
			} else {System.out.println("I can't plant though"); }
		}
	}


	private void waterLowest() throws GameActionException {
		TreeInfo[] myTrees = rc.senseNearbyTrees(2.0f, rc.getTeam());

		if (myTrees.length > 0) { // Waters lowest
			double hp = 100.0;
			int water = 0;
			for (int i = 0; i < myTrees.length; i++) {
				if ((double) myTrees[i].getHealth() < hp) {
					hp = (double) myTrees[i].getHealth();
					water = i;
				}
			}
			rc.water(myTrees[water].getID());
		}
	}


}

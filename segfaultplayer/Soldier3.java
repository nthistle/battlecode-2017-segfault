package segfaultplayer;
import battlecode.common.*;

public strictfp class Soldier3 extends RobotBase
{
    public static final int SWARM_ROUND_NUM = 800;

    public float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
    public float curdirection = (float) Math.random() * 2 * (float) Math.PI;
    public int ctr = 0;
    public int gardenerHarassCounter = 0;
    public float offset = 0f;
    public Soldier3(RobotController rc, int id) throws GameActionException {
        super(rc, id);
    }

    public void run() throws GameActionException {
        int combatCounter = 0;
        MapLocation target = null;
        try {
            while(true) {
                TreeInfo[] nearbyTrees = rc.senseNearbyTrees(); //shake nearby bullet trees
                dailyTasks(nearbyTrees); //checks VP win and shaking and if archon needs to be progressed
                BulletInfo[] nearbyBullets = getBullets();
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy); //TODO: Log coordinates into swarm
                if(nearbyBullets.length>0 || combatCounter>0) { //combat counter maintains 5 turn fire
                    if(nearbyBullets.length>0) //normal case
                        combatCounter=5;
                    else {//fire at enemy's last location case
                        combatCounter--;
                        if(rc.canFireTriadShot() && combatCounter%2==1) //odd counter fire at enemy
                            rc.fireSingleShot(rc.getLocation().directionTo(target));
                        else if(rc.canFireTriadShot()) //offset shot on even counters
                            rc.fireTriadShot(rc.getLocation().directionTo(target).rotateRightDegrees(10.0f));
                    }
                    //TODO: Combat case
                }
                else if(nearbyRobots.length>0) {
                    //TODO: Hunting case
                    hunting(nearbyRobots, nearbyTrees);

                }
                else { //default case
                    if(rc.readBroadcast(300)==1 && rc.readBroadcast(301)!=0) { //swarm is on
                        float[] maplocation = CommunicationsHandler.unpack(rc.readBroadcast(301));
                        pathFind(new MapLocation(maplocation[0], maplocation[1]));
                    }
                    else //find archon
                        pathFind(enemyArchons[ctr]);
                }
                if(target!=null)
                    rc.broadcast(301,CommunicationsHandler.pack(target.x,target.y));
                if(rc.getRoundNum() > SWARM_ROUND_NUM) { //turn on the swarm
                    if(rc.readBroadcast(300) == 0)
                        rc.broadcast(300, 1);
                }
                Clock.yield();
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Soldier Error");
        }
    }

    public MapLocation isClear(RobotInfo myRobot, TreeInfo[] trees) {
        // method returns null if robot can't be hit. returns maplocation of part of robot (middle, top, bottom) that can be hit if !null
        Direction backToMe = myRobot.location.directionTo(rc.getLocation());
        MapLocation middle = myRobot.location.add(backToMe, rc.getType().bodyRadius);
        MapLocation top = myRobot.location.add(backToMe.rotateLeftDegrees(90f), rc.getType().bodyRadius);
        MapLocation bottom = myRobot.location.add(backToMe.rotateRightDegrees(90f), rc.getType().bodyRadius);
        rc.setIndicatorLine(rc.getLocation(), middle, 0, 0, 255);
        rc.setIndicatorLine(rc.getLocation(), top, 255, 0, 0);
        rc.setIndicatorLine(rc.getLocation(), bottom, 0, 0, 255);

        boolean middleFlag = true; // i know boolean markers are bad but whatever
        boolean topFlag = true;
        boolean bottomFlag = true;
        for(TreeInfo t : trees) {
            rc.setIndicatorDot(t.location, 255, 0, 0);
            if(middleFlag) {
                if(distance(t.location, rc.getLocation(), middle) < t.radius) {
                    middleFlag = false;
                    rc.setIndicatorDot(t.location, 255, 255, 255);
                }
            }
            if(topFlag) {
                if(distance(t.location, rc.getLocation(), top) < t.radius) {
                    topFlag = false;
                    rc.setIndicatorDot(t.location, 255, 255, 255);
                }
            }
            if(bottomFlag) {
                if(distance(t.location, rc.getLocation(), bottom) < t.radius) {
                    bottomFlag = false;
                    rc.setIndicatorDot(t.location, 255, 255, 255);
                }
            }
        }
        if(middleFlag) {
            System.out.println("middle");
            return middle;
        }
        if(topFlag) {
            System.out.println("top");
            return top;
        }
        if(bottomFlag) {
            System.out.println("bottom");
            return bottom;
        }
        return null;
    }

    public void hunting(RobotInfo[] robots, TreeInfo[] trees) throws GameActionException {
        // Figure out which robot to shoot at
        RobotType[] priority = {RobotType.SOLDIER, RobotType.TANK, RobotType.GARDENER, RobotType.LUMBERJACK, RobotType.SCOUT, RobotType.ARCHON}; //priority of shooting
        RobotInfo target = null;
        MapLocation shootMe = null; //
        int z = 0;
        // TODO: Make more efficient Mihir
        while (target == null && (z < priority.length && rc.getRoundNum() > 300 || z < priority.length - 1)) {
            for (int i = 0; i < robots.length; i++) {
                if (robots[i].getType() == priority[z]) {
                    shootMe = isClear(robots[i], trees);
                    if(shootMe!=null) {
                        target=robots[i];
                        break;
                    }
                }
            }
            z++;
        }
        //shooting
        if (shootMe != null && target != null) {
            rc.setIndicatorLine(rc.getLocation(), shootMe, 0, 255, 0);
            Direction tDir = rc.getLocation().directionTo(shootMe);
            double[] vTriad = isTriadShotClear(tDir);
            double[] vSingle = isSingleShotClearValue(tDir);
            //can fire triad,  triad does more damage than single, triad only at max hits 1 friendly non-gardener/archon unit
            if (rc.canFireTriadShot() && vTriad[1] > vSingle[1] && vTriad[0] < 3) {
                rc.fireTriadShot(tDir);
            } else if (rc.canFireSingleShot() && isSingleShotClear(tDir)) {
                rc.fireSingleShot(tDir);
            }
            // alternate offset
        }
    }
    //===========================================
    //==========DISTANCE FROM POINT TO LINE======
    //===========================================
    private double sqr(double x) {
        return x*x;
    }
    private double dist2(MapLocation v, MapLocation w) {
        return (sqr(v.x - w.x) + sqr(v.y - w.y));
    }
    private double distToSegmentSquared(MapLocation p, MapLocation v, MapLocation w) {
        double l2 = dist2(v, w);
        if(l2==0) {
            return dist2(p, v);
        }
        double t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2;
        t = Math.max(0, Math.min(1, t));
        MapLocation newLoc = new MapLocation((float)(v.x + t * (w.x - v.x)), (float)(v.y + t * (w.y - v.y)));
        return dist2(p, newLoc);
    }
    private double distance(MapLocation p, MapLocation v, MapLocation w) {
        return Math.sqrt(distToSegmentSquared(p, v, w));
    }
    //===============================================
    //==========END DISTANCE FROM POINT TO LINE======
    //===============================================


    //Does fire action
    // =================================================================
    // ========================OLD CODE===============================
    // =================================================================
    /*
	public void shoot(Direction goal, boolean debug) throws GameActionException {
		try {
			RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
			TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius);
			BulletInfo[] nearbyBullets = rc.senseNearbyBullets(5.0f);
			if (robots.length > 0) { //there are nearby robots
				RobotType[] priority = {RobotType.SOLDIER, RobotType.TANK, RobotType.GARDENER, RobotType.LUMBERJACK, RobotType.SCOUT, RobotType.ARCHON}; //priority of shooting
				RobotInfo target = null;
				int z = 0;
				while (target == null && (z < priority.length && rc.getRoundNum() > 300 || z < priority.length - 1)) {
					for (int i = 0; i < robots.length; i++) {
						if (robots[i].getType() == priority[z] && isSingleShotClear(rc.getLocation().directionTo(robots[i].getLocation()))) {
							target = robots[i];
							break;
						}
					}
					z++;
				}
				if (target != null) { //shooting
					Direction tDir = rc.getLocation().directionTo(target.getLocation());
					double[] vPentad = isPentadShotClear(tDir);
					double[] vTriad = isTriadShotClear(tDir);
					double[] vSingle = isSingleShotClearValue(tDir);
					if (debug) {
						System.out.println("vPentad: " + vPentad[0] + " " + vPentad[1]);
						System.out.println("vTriad: " + vTriad[0] + " " + vTriad[1]);
						System.out.println("vSingle: " + vSingle[0] + " " + vSingle[1]);
					}
					if (debug) {
						System.out.println(rc.canFirePentadShot() + " " + (vPentad[1] > vPentad[0]) + " " + (target.getType() == RobotType.SOLDIER));
						System.out.println(rc.canFireTriadShot() + " " + (vTriad[1] > vSingle[1]));
						System.out.println((rc.canFireSingleShot() + " " + isSingleShotClear(tDir)));
					}
					if (rc.canFirePentadShot() && (vPentad[1] > vTriad[1] || target.getType() == RobotType.SOLDIER)) {
						rc.firePentadShot(tDir);
						if (debug)
							System.out.println("Fired Penta");
					}
					//can fire triad,  triad does more damage than single, triad only at max hits 1 friendly non-gardener/archon unit
					else if (rc.canFireTriadShot() && vTriad[1] > vSingle[1] && vTriad[0] < 3) {
						rc.fireTriadShot(tDir);
						if (debug)
							System.out.println("Fired triad");
					} else if (rc.canFireSingleShot() && isSingleShotClear(tDir)) {
						rc.fireSingleShot(tDir);
						if (debug)
							System.out.println("Fired single");
					}
				}
			} else if (rc.getRoundNum() > 600 && rc.getTeamBullets() > 350 && goal != null && trees.length > 0 && trees[0].getTeam() != ally) { //are there nearby non-ally trees
				Direction tDir = rc.getLocation().directionTo(trees[0].getLocation());
				if (tDir.equals(goal, (float) (Math.PI / 4.0)) && rc.getLocation().distanceTo(trees[0].getLocation()) < 3.0) { //are they in our way
					if (rc.canFireTriadShot()) //shoot em down
						rc.fireTriadShot(tDir);
					else if (rc.canFireSingleShot())
						rc.fireSingleShot(tDir);
				}
			}
			//TODO: Make soldiers not rape each other
//		else if(nearbyBullets.length>0 && rc.getTeamBullets()>50) {
//			int ctr=0;
//			for(int i=0; i<nearbyBullets.length; i++) {
//				if(rc.getLocation().directionTo(nearbyBullets[i].getLocation()).equals(nearbyBullets[i].getDir(),(float)(Math.PI/2.0)))
//					nearbyBullets[i] = null;
//				else
//					ctr++;
//			}
//			if(ctr!=0) {
//				for(int i=0; i<nearbyBullets.length; i++) {
//					if(nearbyBullets[i]!=null) {
//						Direction tDir = rc.getLocation().directionTo(nearbyBullets[i].getLocation());
//						double[] vTriad = isTriadShotClear(tDir);
//						if(rc.canFireTriadShot() && vTriad[0]==0)
//							rc.fireTriadShot(tDir);
//						else if(rc.canFireSingleShot() && isSingleShotClear(tDir))
//							rc.fireSingleShot(tDir);
//						break;
//					}
//				}
//			}
//		}
		} catch(Exception e){}
	}
	*/
    // =================================================================
    // ======================END OLD CODE===============================
    // =================================================================


    //daily non-movement/shooting tasks
    public void dailyTasks(TreeInfo[] nearbyTrees) throws  GameActionException {
        checkVPWin(); //check if can win game on VPs
        for(int i=0; i<nearbyTrees.length; i++) {
            if(nearbyTrees[i].getContainedBullets() > 0 && rc.canShake(nearbyTrees[i].getID())) {
                rc.shake(nearbyTrees[i].getID());
                break;
            }
        }
        if(ctr<enemyArchons.length && rc.getLocation().distanceTo(enemyArchons[ctr])<4 && isArchonDead()) //if archon is dead, move to next one
            ctr++;
    }

    //is the enemy archon here dead?
    public boolean isArchonDead() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy);
        for(int i=0; i<robots.length; i++)
            if(robots[i].getType() == RobotType.ARCHON)
                return false;
        return true;
    }



    public BulletInfo[] getBullets() throws GameActionException {
        BulletInfo[] nearbyBullets = rc.senseNearbyBullets(6.0f); //TODO: Change with bytecode limit
        int ctr=0;
        for(int i=0; i<nearbyBullets.length; i++) {
            if(rc.getLocation().directionTo(nearbyBullets[i].getLocation()).equals(nearbyBullets[i].getDir(),(float)(Math.PI/2.0)))
                nearbyBullets[i] = null;
            else
                ctr++;
        }
        BulletInfo[] bi = new BulletInfo[ctr];
        ctr=0;
        for(int i=0; i<nearbyBullets.length; i++) {
            if(nearbyBullets[i]!=null) {
                bi[ctr] = nearbyBullets[i];
                ctr++;
            }
        }
        return bi;
    }
}
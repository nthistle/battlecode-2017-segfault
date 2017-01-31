package segfaultplayer;
import battlecode.common.*;

import java.awt.*;

public strictfp class Soldier2 extends RobotBase
{
    public static final int SWARM_ROUND_NUM = 800;
    public static final int MAX_INT =  147483647;
    public float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
    public float curdirection = (float) Math.random() * 2 * (float) Math.PI;
    public int ctr = 0;
    public Direction huntDir;

    public Soldier2(RobotController rc, int id) throws GameActionException {
        super(rc, id);
    }

    public void run() throws GameActionException {
        int combatCounter = 0;
        int tankCounter = 0;
        MapLocation target = null;
        boolean altShot = false;
        try {
            while(true) {
                TreeInfo[] nearbyTrees = rc.senseNearbyTrees(); //shake nearby bullet trees
                dailyTasks(nearbyTrees); //checks VP win and shaking and if archon needs to be progressed
                BulletInfo[] nearbyBullets = getBullets();
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy);
                if(nearbyBullets.length>0 || combatCounter>0) { //COMBAT CASE. Combat counter maintains 5 turn fire
                    if(nearbyBullets.length>0 || tankCounter>0) {//normal case
                        RobotInfo targetRobot = combatTarget(nearbyRobots); //find target if applicable
                        if(targetRobot!=null)
                            target = targetRobot.getLocation();
                        //System.out.println(rc.getRoundNum()+" "+tankCounter);
                        Direction front = null;
                        if(nearbyBullets.length>0)
                            front = getFront(nearbyBullets);
                        if((targetRobot!=null && targetRobot.getType()==RobotType.TANK) || tankCounter>0) { // Run.

                            if(rc.canMove(rc.getLocation().subtract(rc.getLocation().directionTo(target))))
                                rc.move(rc.getLocation().subtract(rc.getLocation().directionTo(target)));
                            if(tankCounter>0)
                                tankCounter--;
                            else
                                tankCounter = 3;
                        }
                        else if(isSafe(rc.getLocation().add(front, rc.getType().strideRadius),nearbyBullets)==0) { //can I safely move backwards IF TARGET (length>0)
                            rc.move(rc.getLocation().add(front, rc.getType().strideRadius));
                            System.out.println("Backwards");
                        }
                        else if(isSafe(rc.getLocation(),nearbyBullets)==0) {//can I safely stay
                            System.out.println("Stay");
                        }
                        else {   // can I safely dodge sideways
                            dodge(front, nearbyBullets,true);
                            System.out.println("Dodge");
                        }

                        if(target!=null) {//shoot at target IF TARGET (port over nikhil's firing) TODO: Make sure u can hit it /  FRIENDLY FIRE (shouldnt occur)
                            if (rc.canFireTriadShot() && !altShot) {
                                rc.fireTriadShot(rc.getLocation().directionTo(target));
                                altShot = true;
                            }
                            else if(rc.canFireTriadShot() && altShot) {
                                rc.fireTriadShot(rc.getLocation().directionTo(target).rotateRightDegrees(10.0f));
                                altShot = false;
                            }
                        }
                        if(targetRobot!=null)
                            combatCounter = 5;
                    }
                    else {//fire at enemy's last location case //TODO ACCOUNT FOR FRIENDLY FIRE (shouldnt occur)
                        combatCounter--;
                        if(rc.canFireTriadShot() && combatCounter%2==1 && target!=null) //odd counter fire at enemy
                            rc.fireSingleShot(rc.getLocation().directionTo(target));
                        else if(rc.canFireTriadShot() && target!=null) //offset shot on even counters
                            rc.fireTriadShot(rc.getLocation().directionTo(target).rotateRightDegrees(10.0f));
                    }
                }
                else if(nearbyRobots.length>0) { //TODO: account for sht theres a tank/soldier but it hasnt fired yet (Fire at soldier and it will fire back = good, for tank set target and combatcounter then back up)
                    //TODO: Hunting case
                    MapLocation huntLoc = hunting(nearbyRobots, nearbyTrees, rc.getLocation());
                    if(huntLoc!=null) {
                        if(huntLoc==rc.getLocation()) {
                            if(rc.canFireSingleShot()) {
                                rc.fireSingleShot(huntDir);
                            }
                        } else {
                            pathFind(huntLoc);
                        }
                    }
                    else if(ctr<enemyArchons.length) //elif archons are alive, move towards them
                        pathFind(enemyArchons[ctr]);
                    else { //move randomly
                        if (Math.random() < 0.05)
                            curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
                        curdirection += curdiff + 2 * (float) Math.PI;
                        while (curdirection > 2 * (float) Math.PI)
                            curdirection -= 2 * (float) Math.PI;
                        pathFind(rc.getLocation().add(new Direction(curdirection),rc.getType().strideRadius));
                    }
                }
                else { //default case
                    if(rc.readBroadcast(300)==1 && rc.readBroadcast(301)!=0) { //swarm is on
                        float[] maplocation = CommunicationsHandler.unpack(rc.readBroadcast(301));
                        MapLocation goal = new MapLocation(maplocation[0], maplocation[1]);
                        pathFind(goal);
            			rc.setIndicatorLine(rc.getLocation(), goal, 115, 202, 226);
                    }
                    else if(ctr<enemyArchons.length) //elif archons are alive, move towards them
                        pathFind(enemyArchons[ctr]);
                    else { //move randomly
                        if (Math.random() < 0.05)
                            curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
                        curdirection += curdiff + 2 * (float) Math.PI;
                        while (curdirection > 2 * (float) Math.PI)
                            curdirection -= 2 * (float) Math.PI;
                        pathFind(rc.getLocation().add(new Direction(curdirection),rc.getType().strideRadius));
                    }

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

    public void dodge(Direction front, BulletInfo[] nearbyBullets) throws GameActionException {
        dodge(front, nearbyBullets,false);
    }

    //tries dodging to the side, failing that attempts to move backwards regardless of price
    public void dodge(Direction front, BulletInfo[] nearbyBullets, boolean debug) throws GameActionException {
        int imax = 1;
        int zmax = 8;
        MapLocation[] moves = new MapLocation[zmax*imax];
        int[] damage = new int[zmax*imax];
        for(int i=0; i<imax; i++) {
            for(int z=0; z<zmax; z++) {
                moves[i*zmax+z] = rc.getLocation().add( front.rotateRightDegrees(45.0f*z) ,(9 + i) / 9.0f * rc.getType().strideRadius );
                if(debug)
                    rc.setIndicatorLine(rc.getLocation(),moves[i*zmax+z],255,0,0);
                damage[i*zmax+z] = 1000*isSafe(moves[i*zmax+z],nearbyBullets) + (int)(moves[i*zmax+z].distanceTo(allyArchons[0]));//
                //Math.abs((int)(rc.getLocation().directionTo(moves[i*zmax+z]).degreesBetween(front))
            }
        }
        if(debug) {
            for (int i = 0; i < 8; i++) {
                System.out.println(damage[i]);
            }
        }
        int index =0;
        for(int i=0; i<damage.length; i++)
            if(damage[i]<damage[index])
                index = i;
        if(rc.canMove(moves[index]))
            rc.move(moves[index]);
    }

    //returns avg bullet direction
    public Direction getFront(BulletInfo[] nearbyBullets) {
        float rads = 0.0f;
        for(BulletInfo bullet: nearbyBullets)
            rads+=bullet.getDir().radians;
        return new Direction(rads / (nearbyBullets.length*1.0f));
    }

    //checks if space is safe from bullets and movable
    public int isSafe(MapLocation ml, BulletInfo[] nearbyBullets) {
        int score = 0;
        if(!rc.canMove(ml))
            return MAX_INT;
        for(BulletInfo bullet: nearbyBullets) {
            if(rc.getType().bodyRadius>willHitMe(ml,bullet.getLocation(),bullet.getLocation().add(bullet.getDir(),bullet.getSpeed()))) {
                score += bullet.getDamage();
                if(score>=8)
                    return MAX_INT;
            }
        }
        return score;
    }

    public RobotInfo combatTarget(RobotInfo[] nearbyRobots) {
        if(nearbyRobots.length==0)
            return null;
        int score = MAX_INT;
        int index = -1;
        for(int i=0; i<nearbyRobots.length; i++) {
            int myScore = i;
            switch (rc.getType()) {
                case TANK:
                    myScore+=100;
                    break;
                case SOLDIER:
                    score+=80;
                    break;
                case GARDENER:
                    score+=60;
                    break;
                case LUMBERJACK:
                    score+=40;
                    break;
                case SCOUT:
                    score+=20;
                    break;
                case ARCHON:
                    break;
            }
            if(myScore<score) {
                score = myScore;
                index = i;
            }
        }
        return nearbyRobots[index];
    }

    //returns smallest distance between point and line segment  from l1 to l2
    private double willHitMe(MapLocation p, MapLocation l1, MapLocation l2) {
        float x1 = p.x;
        float y1 = p.y;
        float x2 = l1.x;
        float y2 = l1.y;
        float x3 = l2.x;
        float y3 = l2.y;
        float px=x2-x1;
        float py=y2-y1;
        float temp=(px*px)+(py*py);
        float u=((x3 - x1) * px + (y3 - y1) * py) / (temp);
        if(u>1){
            u=1;
        }
        else if(u<0){
            u=0;
        }
        float x = x1 + u * px;
        float y = y1 + u * py;

        float dx = x - x3;
        float dy = y - y3;
        double dist = Math.sqrt(dx*dx + dy*dy);
        return dist;
    }

    //gets bullets being fired towards me
    public BulletInfo[] getBullets() throws GameActionException {
        BulletInfo[] nearbyBullets = rc.senseNearbyBullets(rc.getType().sensorRadius); //TODO: Change with bytecode limit
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

    //=================INTEGRATE======================
    //=================================================
    //===============================================
    public MapLocation isClear(RobotInfo myRobot, TreeInfo[] trees, RobotInfo[] team, MapLocation thisLoc) throws GameActionException {
        // method returns null if robot can't be hit. returns maplocation of part of robot (middle, top, bottom) that can be hit if !null
        Direction backToMe = myRobot.location.directionTo(thisLoc);
        MapLocation middle = myRobot.location.add(backToMe, rc.getType().bodyRadius);
        MapLocation top = myRobot.location.add(backToMe.rotateLeftDegrees(90f), rc.getType().bodyRadius);
        MapLocation bottom = myRobot.location.add(backToMe.rotateRightDegrees(90f), rc.getType().bodyRadius);
        //rc.setIndicatorLine(rc.getLocation(), middle, 0, 0, 255);
        //rc.setIndicatorLine(rc.getLocation(), top, 255, 0, 0);
        //rc.setIndicatorLine(rc.getLocation(), bottom, 0, 0, 255);

        boolean middleFlag = true; // i know boolean markers are bad but whatever
        boolean topFlag = true;
        boolean bottomFlag = true;
        for(TreeInfo t : trees) {
            rc.setIndicatorDot(t.location, 255, 0, 0);
            if(middleFlag) {
                if(distance(t.location, thisLoc, middle) < t.radius) {
                    middleFlag = false;
                    rc.setIndicatorDot(t.location, 255, 255, 255);
                }
            }
            if(topFlag) {
                if(distance(t.location, thisLoc, top) < t.radius) {
                    topFlag = false;
                    rc.setIndicatorDot(t.location, 255, 255, 255);
                }
            }
            if(bottomFlag) {
                if(distance(t.location, thisLoc, bottom) < t.radius) {
                    bottomFlag = false;
                    rc.setIndicatorDot(t.location, 255, 255, 255);
                }
            }
        }
        for(RobotInfo r : team) {
            rc.setIndicatorDot(r.location, 255, 0, 0);
            if(middleFlag) {
                if(distance(r.location, thisLoc, middle) < r.getType().bodyRadius) {
                    middleFlag = false;
                    rc.setIndicatorDot(r.location, 255, 255, 255);
                }
            }
            if(topFlag) {
                if(distance(r.location, thisLoc, top) < r.getType().bodyRadius) {
                    topFlag = false;
                    rc.setIndicatorDot(r.location, 255, 255, 255);
                }
            }
            if(bottomFlag) {
                if(distance(r.location, thisLoc, bottom) < r.getType().bodyRadius) {
                    bottomFlag = false;
                    rc.setIndicatorDot(r.location, 255, 255, 255);
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

    public MapLocation hunting(RobotInfo[] robots, TreeInfo[] trees, MapLocation myLoc) throws GameActionException {
        // Figure out which robot to shoot at
    	huntDir = rc.getLocation().directionTo(enemyArchons[0]);
        RobotInfo[] friendly = rc.senseNearbyRobots(rc.getType().sensorRadius, ally);
        RobotType[] priority = {RobotType.SOLDIER, RobotType.TANK, RobotType.GARDENER, RobotType.LUMBERJACK, RobotType.SCOUT, RobotType.ARCHON}; //priority of shooting
        RobotInfo target = null;
        MapLocation shootMe = null; //
        MapLocation bestLocation = null;
        int z = 0;
        for(int j=0; j<3; j++) {
            // TODO: Make more efficient Mihir
            while (target == null && (z < priority.length && rc.getRoundNum() > 300 || z < priority.length - 1)) {
                for (int i = 0; i < robots.length; i++) {
                    if (robots[i].getType() == priority[z]) {
                        bestLocation = robots[i].location;
                        shootMe = isClear(robots[i], trees, friendly, myLoc);
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
                rc.setIndicatorLine(myLoc, shootMe, 0, 255, 0);
                Direction tDir = myLoc.directionTo(shootMe);
                huntDir = tDir;
                break;
            } else {
                if(bestLocation!=null) {
                    Direction dir = bestLocation.directionTo(myLoc);
                    if(j==0) {
                        dir = dir.rotateLeftDegrees(20f);
                        myLoc = bestLocation.add(dir, bestLocation.distanceTo(myLoc));
                        rc.setIndicatorLine(myLoc, bestLocation, 255, 255, 255);
                    }
                    if(j==1) {
                        dir = dir.rotateRightDegrees(40f);
                        myLoc = bestLocation.add(dir, bestLocation.distanceTo(myLoc));
                        rc.setIndicatorLine(myLoc, bestLocation, 255, 255, 255);
                    }
                    if(j==2) {
                        return null;
                    }
                }
            }
        }
        return myLoc;
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


    //=================END INTEGRATION 3============================================================
    //======================================================================================================
    //============================================================================================
}
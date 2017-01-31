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

    public Soldier2(RobotController rc, int id) throws GameActionException {
        super(rc, id);
    }

    public void run() throws GameActionException {
        int combatCounter = 0;
        MapLocation target = null;
        boolean altShot = false;
        try {
            while(true) {
                dailyTasks(); //checks VP win and shaking and if archon needs to be progressed
                BulletInfo[] nearbyBullets = getBullets();
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy);
                if(nearbyBullets.length>0 || combatCounter>0) { //COMBAT CASE. Combat counter maintains 5 turn fire
                    if(nearbyBullets.length>0) {//normal case
                        RobotInfo targetRobot = combatTarget(nearbyRobots); //find target if applicable
                        if(targetRobot!=null)
                            target = targetRobot.getLocation();

                        Direction front = getFront(nearbyBullets);
                        if(isSafe(rc.getLocation().add(front, rc.getType().strideRadius),nearbyBullets)==0) { //can I safely move backwards IF TARGET (length>0)
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

                        if(target!=null) {//shoot at target IF TARGET (port over nikhil's firing) TODO: Make sure u can hit it
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
                    else {//fire at enemy's last location case
                        combatCounter--;
                        if(rc.canFireTriadShot() && combatCounter%2==1 && target!=null) //odd counter fire at enemy
                            rc.fireSingleShot(rc.getLocation().directionTo(target));
                        else if(rc.canFireTriadShot() && target!=null) //offset shot on even counters
                            rc.fireTriadShot(rc.getLocation().directionTo(target).rotateRightDegrees(10.0f));
                    }
                }
                else if(nearbyRobots.length>0) { //TODO: account for sht theres a tank/soldier but it hasnt fired yet (Fire at soldier and it will fire back = good, for tank set target and combatcounter then back up)
                    //TODO: Hunting case
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

    public void dodge(Direction front, BulletInfo[] nearbyBullets) throws GameActionException {
        dodge(front, nearbyBullets,false);
    }

    //tries dodging to the side, failing that attempts to move backwards regardless of price
    public void dodge(Direction front, BulletInfo[] nearbyBullets, boolean debug) throws GameActionException {

        MapLocation[] moves = new MapLocation[8*1];
        int[] damage = new int[8*1];
        for(int i=0; i<1; i++) {
            for(int z=0; z<8; z++) {

                moves[i*8+z] = rc.getLocation().add( front.rotateRightDegrees(45.0f*z) ,(9 + i) / 9.0f * rc.getType().strideRadius );
                if(debug)
                    rc.setIndicatorLine(rc.getLocation(),moves[i*8+z],255,0,0);
                damage[i*8+z] = 100*isSafe(moves[i*8+z],nearbyBullets) + Math.abs((int)front.degreesBetween(front.rotateRightDegrees(45.0f*z)));
            }
//            moves[i * 4] = rc.getLocation().add(leftFar, (9 + i) / 9.0f * rc.getType().strideRadius);
//            moves[i * 4 + 1] = rc.getLocation().add(rightFar, (9 + i) / 9.0f * rc.getType().strideRadius);
//            moves[i * 4 + 2] = rc.getLocation().add(left, (9 + i) / 9.0f * rc.getType().strideRadius);
//            moves[i * 4 + 3] = rc.getLocation().add(right, (9 + i) / 9.0f * rc.getType().strideRadius);
//
//            damage[i*4] = isSafe(moves[i*4],nearbyBullets);
//            damage[i*4+1] = isSafe(moves[i*4+1],nearbyBullets);
//            damage[i*4+2] = isSafe(moves[i*4+2],nearbyBullets);
//            damage[i*4+3] = isSafe(moves[i*4+3],nearbyBullets);
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
    public void dailyTasks() throws  GameActionException {
        checkVPWin(); //check if can win game on VPs
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees(); //shake nearby bullet trees
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
}
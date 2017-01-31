package segfaultplayer;
import battlecode.common.*;

public strictfp class Soldier2 extends RobotBase
{
    public static final int SWARM_ROUND_NUM = 800;

    public float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
    public float curdirection = (float) Math.random() * 2 * (float) Math.PI;
    public int ctr = 0;

    public Soldier2(RobotController rc, int id) throws GameActionException {
        super(rc, id);
    }

    public void run() throws GameActionException {
        int combatCounter = 0;
        MapLocation target = null;
        try {
            while(true) {
                dailyTasks(); //checks VP win and shaking and if archon needs to be progressed
                BulletInfo[] nearbyBullets = getBullets();
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy); //TODO: Log coordinates into swarm
                if(nearbyBullets.length>0 || combatCounter>0) { //COMBAT CASE. Combat counter maintains 5 turn fire
                    if(nearbyBullets.length>0) {//normal case
                        combatCounter = 5;
                    }
                    else {//fire at enemy's last location case
                        combatCounter--;
                        if(rc.canFireTriadShot() && combatCounter%2==1) //odd counter fire at enemy
                            rc.fireSingleShot(rc.getLocation().directionTo(target));
                        else if(rc.canFireTriadShot()) //offset shot on even counters
                            rc.fireTriadShot(rc.getLocation().directionTo(target).rotateRightDegrees(10.0f));
                    }
                }
                else if(nearbyRobots.length>0) {
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

    //returns smallest distance between point and line segment  from l1 to l2
    private double willHitMe(MapLocation p, MapLocation l1, MapLocation l2)
    {
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
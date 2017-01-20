package segfaultplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
	
    
    
    public static void run(RobotController rc) throws GameActionException {
    	
        
        switch (rc.getType()) {
            case ARCHON:
                handleArchon(rc);
                break;
            case GARDENER:
                handleGardener(rc);
                break;
            case SOLDIER:
                handleSoldier(rc);
                break;
            case LUMBERJACK:
                handleLumberjack(rc);
                break;
            case SCOUT:
            	handleScout(rc);
            	break;
            default:
            	break;
        }
	}
    
    public static void handleArchon(RobotController rc) throws GameActionException {
    	Archon a = new Archon(rc);
    	a.run();
    }
    
    public static void handleGardener(RobotController rc) throws GameActionException {
    	Gardener g = new Gardener(rc);
    	g.run();
    }
    
    public static void handleSoldier(RobotController rc) throws GameActionException {
    	Soldier so = new Soldier(rc);
    	so.run();
    }
    
    public static void handleLumberjack(RobotController rc) throws GameActionException {
    	Lumberjack lj = new Lumberjack(rc);
    	lj.run();
    }
    
    public static void handleScout(RobotController rc) throws GameActionException {
    	Scout sc = new Scout(rc);
    	sc.run();
    }
    
}

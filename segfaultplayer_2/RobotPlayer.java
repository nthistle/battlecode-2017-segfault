package segfaultplayer_2;
import battlecode.common.*;

public strictfp class RobotPlayer {
    
    public static void run(RobotController rc) throws GameActionException {
    	
        int thisID = RobotBase.getAndAssignNextID(rc);

        switch (rc.getType()) {
            case ARCHON:
                handleArchon(rc, thisID);
                break;
            case GARDENER:
                handleGardener(rc, thisID);
                break;
            case SOLDIER:
                handleSoldier(rc, thisID);
                break;
            case LUMBERJACK:
                handleLumberjack(rc, thisID);
                break;
            case SCOUT:
                handleScout(rc, thisID);
                break;
            case TANK:
                handleTank(rc, thisID);
                break;
            default:
                break;
        }
    }

    public static void handleArchon(RobotController rc, int id) throws GameActionException {
        Archon a = new Archon(rc, id);
        a.run();
        //a.runAlternate(); //change to runAlternate() for testing
    }

    public static void handleGardener(RobotController rc, int id) throws GameActionException {
    	if(id == 0) {
    		FirstGardener fg = new FirstGardener(rc, id);
    		fg.run();
    	} else {
    		HexGardener hg = new HexGardener(rc, id);
    		hg.run();
    	}
        //Gardener g = new Gardener(rc, id);
        //g.run();
        //g.runAlternate(RobotType.SCOUT);
    }

    public static void handleSoldier(RobotController rc, int id) throws GameActionException {
        Soldier so = new Soldier(rc, id);
        so.run();
        //so.runAlt();
    }

    public static void handleLumberjack(RobotController rc, int id) throws GameActionException {
        Lumberjack lj = new Lumberjack(rc, id);
        lj.run();
    }

    public static void handleScout(RobotController rc, int id) throws GameActionException {
        Scout3 sc = new Scout3(rc, id);
        sc.run();
    }

    public static void handleTank(RobotController rc, int id) throws GameActionException {
        Tank tk = new Tank(rc, id);
        tk.run();
    }

}

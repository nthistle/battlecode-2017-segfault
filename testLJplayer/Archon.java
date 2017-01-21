package testLJplayer;
import battlecode.common.*;


public strictfp class Archon extends RobotBase
{
	private int rank;
	private boolean alpha;
	
	public Archon(RobotController rc, int id) throws GameActionException {
		super(rc, id);
		this.rank = CommunicationsHandler.assignAlphaArchonProtocol(this, true);
		this.alpha = this.rank == 0;
	}
	
	public void run() throws GameActionException {
		while(true) {
			
			
			//if(rc.getRoundNum() > 200)
			//	rc.resign(); // temporary for testing to prevent 3000 long games
			
			Direction dir = randomDirection();
			if(rc.canBuildRobot(RobotType.GARDENER,dir) && rc.getTeamBullets()>200)
				rc.buildRobot(RobotType.GARDENER,dir);


			Clock.yield();
		}
	}
}
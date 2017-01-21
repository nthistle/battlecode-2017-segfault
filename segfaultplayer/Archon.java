package segfaultplayer;
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
		int t = 40;
		while(true) {
			Direction dir = randomDirection();
			if(rc.canBuildRobot(RobotType.GARDENER,dir) && t > 30) {// && rc.getTeamBullets()>200)
				rc.buildRobot(RobotType.GARDENER,dir);
				t = 0;
			}
			Clock.yield();
			t ++;
		}
	}
}
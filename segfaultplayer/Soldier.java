package segfaultplayer;
import battlecode.common.*;

public strictfp class Soldier extends RobotBase
{
	public static final int SWARM_ROUND_NUM = 800;
	
	public float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
	public float curdirection = (float) Math.random() * 2 * (float) Math.PI;
	public int ctr = 0;
	public int pushWave = 0;

	public Soldier(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}

	public void run() throws GameActionException {
		try {
			while(true) {
				dailyTasks();
				decideMove();
				decideShoot();
				if(rc.getRoundNum() > SWARM_ROUND_NUM) {
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

	public void decideMove() throws GameActionException {
		decideMove(false);
	}

	//determines movement for the turn
	public void decideMove(boolean debug) throws GameActionException {
		int swarm = rc.readBroadcast(300);
		int swarmcoordinates = rc.readBroadcast(301);
		BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy);
		MapLocation goal;
		if(debug)
			System.out.println((robots.length>0)+" "+(swarm==1 && swarmcoordinates!=0)+" "+(ctr<enemyArchons.length));
		if(robots.length>0) { //if nearby units, move towards them
			int targetIndex = 0;
			for(int i=1; i<robots.length; i++) {
				if(scoreRobot(robots[targetIndex],targetIndex)<scoreRobot(robots[i],i))
					targetIndex = i;
			}
			goal = robots[targetIndex].getLocation();
			if(robots[0].getType()==RobotType.LUMBERJACK || rc.getLocation().distanceTo(robots[0].getLocation())<2.0)
				goal = rc.getLocation().subtract(rc.getLocation().directionTo(robots[0].getLocation()));
			MapLocation myLocation = robots[0].getLocation();
			rc.broadcast(301, CommunicationsHandler.pack(myLocation.x,myLocation.y));
		}
		else if(swarm==1 && swarmcoordinates!=0) {
			float[] swarmHere = CommunicationsHandler.unpack(swarmcoordinates);
			goal = new MapLocation(swarmHere[0],swarmHere[1]);
			rc.setIndicatorLine(rc.getLocation(), goal, 115, 202, 226);
		}
		else if(ctr<enemyArchons.length) //elif archons are alive, move towards them
			goal = enemyArchons[ctr];
		else { //move randomly
			if (Math.random() < 0.05)
				curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
			curdirection += curdiff + 2 * (float) Math.PI;
			while (curdirection > 2 * (float) Math.PI)
				curdirection -= 2 * (float) Math.PI;
			goal = rc.getLocation().add(new Direction(curdirection),rc.getType().strideRadius);
		}
		if(debug)
			System.out.println("Decided Move: "+Clock.getBytecodesLeft());
		if(nearbyBullets.length>0) //if there are bullets, dodge
			moveWithDodging(goal);
		else//  || pushWave<25 add swarm
			pathFind(goal);
		pushWave++;
	}

	public int scoreRobot(RobotInfo robot, int index) {
		int score = index;
		switch (rc.getType()) {
			case TANK:
				score+=120;
				break;
			case SOLDIER:
				score+=100;
				break;
			case GARDENER:
				score+=80;
				break;
			case LUMBERJACK:
				score+=60;
				break;
			case ARCHON:
				score+=40;
				break;
			case SCOUT:
				score+=20;
				break;
			default:
				break;
		}
		return score;
	}

	//determines shooting for the turn
	public void decideShoot() throws GameActionException {
		if(ctr>=enemyArchons.length)
			shoot(null);
		else
			shoot(rc.getLocation().directionTo(enemyArchons[ctr]));//,true);
	}

	public void shoot(Direction goal) throws GameActionException {
		shoot(goal, false);
	}

	//Does fire action
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
}
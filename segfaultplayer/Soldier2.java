package segfaultplayer;
import battlecode.common.*;

public strictfp class Soldier2 extends RobotBase
{

	public float curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
	public float curdirection = (float) Math.random() * 2 * (float) Math.PI;
	public int ctr = 0;
	
	public Soldier2(RobotController rc, int id) throws GameActionException {
		super(rc, id);
	}
	
	public void run() throws GameActionException {
		//TODO: Identify closest archon

		try {
			while(true) {
				pathFind(enemyArchons[0]);
				//dailyTasks();
				//decideMove();
				//decideShoot();
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

	//determines movement for the turn
//	public void decideMove() throws GameActionException {
//		BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
//		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius,enemy);
//		Direction goal;
//		if(robots.length>0) //if nearby units, move towards them
//			goal = rc.getLocation().directionTo(robots[0].getLocation());
//		else if(ctr<enemyArchons.length) //elif archons are alive, move towards them
//			goal = rc.getLocation().directionTo(enemyArchons[ctr]);
//		else { //move randomly
//			if (Math.random() < 0.05)
//				curdiff = (float) ((float) (Math.random() - 0.5) * 0.1 * (float) Math.PI);
//			curdirection += curdiff + 2 * (float) Math.PI;
//			while (curdirection > 2 * (float) Math.PI)
//				curdirection -= 2 * (float) Math.PI;
//			goal = new Direction(curdirection);
//		}
//		if(nearbyBullets.length>0) //if there are bullets, dodge
//			moveWithDodging(goal); //TODO: check to make sure not crowded in by trees
//		else //move normally
//			moveWithoutDodging(goal); //TODO: Replace with pathfinding / better movement
//	}

	//determines shooting for the turn
	public void decideShoot() throws GameActionException {
		if(ctr>=enemyArchons.length)
			shoot(null);
		else
			shoot(rc.getLocation().directionTo(enemyArchons[ctr]));
	}

	//Does fire action
	public void shoot(Direction goal) throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, enemy);
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius);
		if (robots.length > 0) { //there are nearby robots
			//TODO?: Make more efficient? Isn't an issue right now
			RobotType[] priority = {RobotType.SOLDIER, RobotType.TANK, RobotType.GARDENER, RobotType.LUMBERJACK, RobotType.SCOUT, RobotType.ARCHON}; //priority of shooting
			RobotInfo target = null;
			int z = 0;
			while (target == null && (z<priority.length && rc.getRoundNum()>300 || z<priority.length-1)) {
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
				if (rc.canFirePentadShot() && vPentad[1] > vPentad[0]) //does penta do more enemy dmg
					rc.firePentadShot(tDir);
				else if (rc.canFireTriadShot() && vTriad[0] == 0) //is triad safe
					rc.fireTriadShot(tDir);
				else if (rc.canFireSingleShot() && isSingleShotClear(tDir))
					rc.fireSingleShot(tDir);
			}
		}
		else if(rc.getRoundNum()>400 && rc.getTeamBullets()>200 && goal!=null && trees.length>0 && trees[0].getTeam()!=ally) { //are there nearby non-ally trees
			Direction tDir = rc.getLocation().directionTo(trees[0].getLocation());
			if( tDir.equals(goal,(float)(Math.PI/4.0)) && rc.getLocation().distanceTo(trees[0].getLocation())<3.0) { //are they in our way
				if (rc.canFireTriadShot()) //shoot em down
					rc.fireTriadShot(tDir);
				else if (rc.canFireSingleShot())
					rc.fireSingleShot(tDir);
			}
		}
	}
	
	
	//finds path
	
	public void pathFind(MapLocation endLoc) throws GameActionException {
		MapLocation marker = null;
		MapLocation marker2= null;
		float stride = rc.getType().strideRadius;
		while(rc.getLocation().distanceTo(endLoc)>5.0f) {
			MapLocation myLoc = rc.getLocation();
			Direction toEnd = myLoc.directionTo(endLoc);
			Direction[] myDirs = getDirections(toEnd, 30f);
			float[][] hyperMeme = new float[myDirs.length][2];
			for(int i=0; i<myDirs.length; i++) {
				if(rc.canMove(myDirs[i], stride)) {
					hyperMeme[i][0] = i;
					MapLocation newLoc = myLoc.add(myDirs[i], stride);
					hyperMeme[i][1] = newLoc.distanceTo(endLoc);
					System.out.print(hyperMeme[i][1] + " ");
					if(marker!=null) {
						hyperMeme[i][1] -= newLoc.distanceTo(marker)*1.6; //*2;
						rc.setIndicatorLine(myLoc, marker, 255, 0, 0);
						//System.out.print(newLoc.distanceTo(marker) + " ");
					}
					if(marker2!=null) {
						hyperMeme[i][1] -= newLoc.distanceTo(marker2)*.4; //*5;
						rc.setIndicatorLine(myLoc, marker, 255, 0, 0);
						System.out.println(newLoc.distanceTo(marker2) + " ");
					}
					rc.setIndicatorLine(myLoc,newLoc, 0, (int)hyperMeme[i][1]*5, 0);
					
				} else {
					hyperMeme[i][0] = i;
					hyperMeme[i][1] = Float.MAX_VALUE;
				}
			}
			
			java.util.Arrays.sort(hyperMeme, new java.util.Comparator<float[]>() {
				public int compare(float[] a, float[] b) {
					return Float.compare(a[1], b[1]); // sort by heuristic if damage is equal (lower = closer to goal)
				}
			});
			//for(int i=0; i<hyperMeme.length; i++) {
			//	for(int j=0; j<hyperMeme[0].length; j++) {
			//		System.out.print(hyperMeme[i][j] + " ");
			//	}
			//	System.out.println();
			//}
			marker2 = marker;
			marker = myLoc;
			for (int j=0; j<myDirs.length; j++) {
				if(rc.canMove(myDirs[(int)hyperMeme[j][0]])) {
					rc.move(myDirs[(int)hyperMeme[j][0]]);
					break;
				}
			}
			Clock.yield();
		}	
	}
	
	
	
	
	
	
	
}
package segfaultplayer;
import battlecode.common.*;


public enum SoldierStrategy {
	BLITZ(1), PATROL(2), SWARM(3);
	
	public final int val;
	
	SoldierStrategy(int val) {
		this.val = val;
	}
};
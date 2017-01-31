package segfaultplayer_2;
import battlecode.common.*;

enum OrderType { TREE, ROBOT};

public class Order {

	public final OrderType type;
	public final RobotType rt;
	
	// default no arguments is an order for a tree
	public Order() {
		this.type = OrderType.TREE;
		this.rt = null;
	}
	
	public Order(OrderType type) {
		this.type = type;
		this.rt = null;
	}
	
	public Order(OrderType type, RobotType rt) {
		this.type = type;
		this.rt = rt;
	}
}
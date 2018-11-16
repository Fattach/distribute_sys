package sys;

public class Cell {
	public String address;
	public int port;
	//0:join,1:run,2:leave,3:down
	public int stauts;
	boolean master;
	int weight;
	long timestamp;
	
	public Cell(String address, int port, int stauts,boolean master, int weight) {
		super();
		this.address = address;
		this.port = port;
		this.stauts = stauts;
		this.master = master;
		this.weight = weight;
	}

	@Override
	public String toString() {
		return "{\"address\":\"" + address + "\", \"port\":" + port + ", \"stauts\":"
				+ stauts + ", \"master\":" + master + ", \"weight\":" + weight
				+ ", \"timestamp\":" + timestamp + "}";
	}

}

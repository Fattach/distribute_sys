package sys;

public enum Type {
	JOIN("join"),HEARTBEAT("heartbeat"),LEAVE("leave"),VOTE("vote"),WELCOME("welcome"),ACK("ack");
	
	final String value;

	Type(String value) {
		this.value = value;
	}
}

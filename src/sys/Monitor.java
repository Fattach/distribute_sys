package sys;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Monitor implements Runnable{

	List<Cell> table;
	InetSocketAddress localhost;

	public Monitor(List<Cell> table, InetSocketAddress localhost) {
		this.table = table;
		this.localhost = localhost;
	}

	@Override
	public void run() {
		System.out.println("Current host:"+localhost.getAddress()+":"+localhost.getPort());
		try (ServerSocket server = new ServerSocket(localhost.getPort())){
			while(true) {
				Socket accept = server.accept();
				new Thread(new Connector(accept, table, localhost),"connector").start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

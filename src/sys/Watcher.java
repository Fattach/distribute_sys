package sys;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Watcher {
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		InetSocketAddress localhost = new InetSocketAddress("192.168.68.91", 10000);
		System.out.println(localhost.getHostName()+":"+localhost.getPort());
		Type type = Type.JOIN;
		List<Thread> threadList = Collections.synchronizedList(new ArrayList<>());
		threadList.add(Thread.currentThread());
		try (MulticastSocket multicastSocket = new MulticastSocket(8000)){
			InetAddress group = InetAddress.getByName("228.0.0.4");
			multicastSocket.joinGroup(group);
			new Thread(new Sender(multicastSocket, localhost, type ,group ,threadList)).start();
			Thread.currentThread().suspend();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

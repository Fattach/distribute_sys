package sys;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sender implements Runnable {

	private MulticastSocket sendSocket;
	private InetSocketAddress localhost;
	private Type type;
	private InetAddress group;
	private List<Thread> threadList;

	public Sender(MulticastSocket sendSocket, InetSocketAddress localhost,
			Type type, InetAddress group, List<Thread> threadList) {
		this.sendSocket = sendSocket;
		this.localhost = localhost;
		this.type = type;
		this.group = group;
		this.threadList = threadList;
	}

	@Override
	public void run() {
		try {
			boolean complieted = false;
			String msg = localhost.getHostName() + ":" + localhost.getPort()
					+ "\ntype:" + type.value;
			byte[] bytes = msg.getBytes();
			byte[] buf = new byte[512];
			List<Cell> table = Collections.synchronizedList(new ArrayList<Cell>());
			table.add(new Cell(localhost.getHostName(), localhost.getPort(),0,false,0));
			while (!complieted) {
				DatagramPacket sdp = new DatagramPacket(bytes, bytes.length,
						group, 8000);
				sendSocket.send(sdp);
				DatagramPacket rdp = new DatagramPacket(buf, buf.length);
				sendSocket.receive(rdp);
				String receiveMsg = new String(rdp.getData(), 0,
						rdp.getLength());
				String[] item = receiveMsg.split("\n");
				String[] i0 = item[0].split(":");
				String[] i1 = item[1].split(":");
				int sum = 0; 
				for (Cell cell : table) {
					if (!(cell.address.equals(i0[0])) || !(cell.port == Integer.parseInt(i0[1]))) {
						sum++;
					}
				}
				if((i1[1].equals(new String(Type.JOIN.value)) || i1[1].equals(new String(Type.VOTE.value))) 
						&& sum == table.size()) {
					table.add(new Cell(i0[0],Integer.parseInt(i0[1]), 0, false,0));
				}
				if (table.size()==2 || i1[1].equals(new String(Type.WELCOME.value))) {
					System.out.println("Begin to vote.....");
					new Thread(new BallotBox(sendSocket, localhost, Type.VOTE,
							group, table ,threadList)).start();
					complieted = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}

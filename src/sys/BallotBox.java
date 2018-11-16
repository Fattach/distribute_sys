package sys;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BallotBox implements Runnable{

	private MulticastSocket sendSocket;
	private InetSocketAddress localhost;
	private Type type;
	private InetAddress group;
	private List<Cell> table;
	private List<Thread> threadList;
	
	public BallotBox(MulticastSocket sendSocket, InetSocketAddress localhost, Type type, InetAddress group ,List<Cell> table, List<Thread> threadList) {
		this.sendSocket = sendSocket;
		this.localhost = localhost;
		this.type = type;
		this.group = group;
		this.table = table;
		this.threadList = threadList;
	}
	
	@Override
	public void run() {
		try {
			int rand = (int) (Math.random() * 1000)+1;
			boolean complieted=false;
			String msg = localhost.getHostName()+":"+localhost.getPort()+"\ntype:"+type.value+":"+rand;
			byte[] bytes = msg.getBytes();
			byte[] buf = new byte[512];
			int sum=0;
			ArrayList<String> foundNode = new ArrayList<String>();
			while (!complieted) {
				DatagramPacket sdp = new DatagramPacket(bytes, bytes.length, group,8000);
				sendSocket.send(sdp);
				DatagramPacket rdp = new DatagramPacket(buf,buf.length);
				sendSocket.receive(rdp);
				String receiveMsg = new String(rdp.getData(),0,rdp.getLength());
				String[] item = receiveMsg.split("\n");
				String[] i0 = item[0].split(":");
				String[] i1 = item[1].split(":");
				if(i1[1].equals(new String(Type.VOTE.value))) {
					for (Cell cell : table) {
						if (cell.address.equals(i0[0]) && cell.port == Integer.parseInt(i0[1]) && cell.weight <= 0) {
							if(Integer.parseInt(i1[2]) > 0) {
								cell.weight=Integer.parseInt(i1[2]);
								cell.stauts=1;
								sum++;
							}
						}
					}
					if(sum == table.size()) {
						int max = 0;
						Cell e = null;
						for (Cell cell : table) {
							if(cell.weight > max) {
								max = cell.weight;
								e = cell;
							}
						}
						e.master=true;
						System.out.println("Master:"+e.address+":"+e.port);
						complieted=true;
					}
				}else if(i1[1].equals(new String(Type.WELCOME.value))) {
					String  all = item[2].split(":")[1];
					String[] nodes = all.split(",");
					int existNode = nodes.length;
					if(foundNode.size()==0) {
						for (String node : nodes) {
							String[] attrs = node.split("\\+");
							String address=attrs[0];
							int port=Integer.parseInt(attrs[1]);
							int stauts=Integer.parseInt(attrs[2]);
							boolean master=Boolean.parseBoolean(attrs[3]);
							int weight=Integer.parseInt(attrs[4]);
							Cell cell = new Cell(address, port, stauts, master, weight);
							cell.timestamp=new Date().getTime();
							table.add(cell);
						}
						foundNode.add(item[0]);
						System.out.println(item[0]+" approved");
					}else {
						boolean exist = false;
						for (String host : foundNode) {
							if (host.equals(item[0])) {
								exist = true;
								break;
							}
						}
						if(!exist) {
							foundNode.add(item[0]);
							System.out.println(item[0]+" approved");
						}
					}
					if(foundNode.size()==existNode) {
						complieted=true;
					}
				}
			}
			new Thread(new Heart(sendSocket, localhost, Type.HEARTBEAT, group, table ,threadList),"beat").start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

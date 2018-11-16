package sys;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Heart implements Runnable{

	private MulticastSocket sendSocket;
	private InetSocketAddress localhost;
	private Type type;
	private InetAddress group;
	private List<Cell> table;
	private List<Thread> threadList;
	private Timer HBtimer=new Timer(true);
	private Timer monitorTimer=new Timer(true);

	public Heart(MulticastSocket sendSocket, InetSocketAddress localhost,
			Type type, InetAddress group, List<Cell> table, List<Thread> threadList) {
		this.sendSocket = sendSocket;
		this.localhost = localhost;
		this.type = type;
		this.group = group;
		this.table = table;
		this.threadList = threadList;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		try {
			boolean complieted=false;
			byte[] buf = new byte[512];
			for (Cell cell : table) {
				cell.timestamp=new Date().getTime();
			}
			
			Thread HBThread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					HBtimer.schedule(new TimerTask() {
						
						@Override
						public void run() {
							try {
								String msg = localhost.getHostName()+":"+localhost.getPort()+"\ntype:"+type.value+":"+new Date().getTime();
								byte[] bytes = msg.getBytes();
								DatagramPacket sdp = new DatagramPacket(bytes, bytes.length, group,8000);
								sendSocket.send(sdp);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}, 0, 10);
					
				}
			},"HBThread");
			HBThread.start();
			
			Thread monitorThread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					monitorTimer.schedule(new TimerTask() {
						
						@Override
						public void run() {
							for (Cell cell : table) {
								if(Math.abs(new Date().getTime()-cell.timestamp) > 6500 && cell.stauts!=3) {
									cell.stauts=3;
									System.out.println(cell.address+":"+cell.port+" > I'm down!");
								}
							}
						}
					}, 0, 3000);
					
				}
			},"monitorThread");
			monitorThread.start();
			
			Thread serverThread = new Thread(new Monitor(table,localhost),"serverThread");
			serverThread.setDaemon(true);
			serverThread.start();
			
			threadList.add(Thread.currentThread());
			ArrayList<String> foundNode = new ArrayList<String>();
			int stauts=1;
			while (!complieted) {
				DatagramPacket rdp = new DatagramPacket(buf,buf.length);
				sendSocket.receive(rdp);
				String receiveMsg = new String(rdp.getData(),0,rdp.getLength());
				String[] item = receiveMsg.split("\n");
				String[] i0 = item[0].split(":");
				String[] i1 = item[1].split(":");
				if(i1[1].equals(new String(Type.HEARTBEAT.value))) {
					boolean newCell = false;
					for (Cell cell : table) {
						if (cell.address.equals(localhost.getHostName()) && cell.port == localhost.getPort() && cell.stauts==2) {
							type=Type.LEAVE;
							stauts=2;
						}
						if (cell.address.equals(i0[0]) && cell.port == Integer.parseInt(i0[1])) {
								cell.timestamp = Long.parseLong(i1[2]);
								newCell = false;
								break;
						}else{
							newCell=true;
						}
					}
					if(newCell) {
						table.add(new Cell(i0[0], Integer.valueOf(i0[1]), 1, false, 1));
						System.out.println("A new node ,"+i0[0]+":"+i0[1]+", joined in!");
					}
				}else if(i1[1].equals(new String(Type.JOIN.value))) {
					int sum = 0;
					for (Cell cell : table) {
						if (!cell.address.equals(i0[0]) || !(cell.port == Integer.parseInt(i0[1]))) {
								sum++;
						}
					}
					if(sum==table.size()) {
						String msg = localhost.getHostName()+":"+localhost.getPort()+"\ntype:"+Type.WELCOME.value+"\ntable:";
						for (Cell cell : table) {
							msg+=cell.address+"+"+cell.port+"+"+cell.stauts+"+"+cell.master+"+"+cell.weight+",";
						}
						msg=msg.substring(0,msg.length()-1);
						byte[] bytes = msg.getBytes();
						DatagramPacket sdp = new DatagramPacket(bytes, bytes.length, group,8000);
						sendSocket.send(sdp);
					}
				}else if(i1[1].equals(new String(Type.LEAVE.value))) {
					try {
						String msg = localhost.getHostName()+":"+localhost.getPort()+"\ntype:"+Type.ACK.value;
						byte[] bytes = msg.getBytes();
						DatagramPacket sdp = new DatagramPacket(bytes, bytes.length, group,8000);
						sendSocket.send(sdp);
					} catch (IOException e) {
						e.printStackTrace();
					}
					Cell e = null;
					int max = 0;
					boolean newVote = false;
					Iterator<Cell> it = table.iterator();
					while(it.hasNext()) {
						Cell cell = it.next();
						if (cell.address.equals(i0[0]) && cell.port == Integer.parseInt(i0[1])) {
							it.remove();
							System.out.println(i0[0]+":"+i0[1]+":I'm tired and i want to resign!");
							if(cell.master) {
								newVote = true;
							}
						}else if(cell.weight > max ){
							max = cell.weight;
							e = cell;
						}
					}
					if(newVote && table.size()!=1) {
						e.master = true;
						System.out.println("New master:"+e.address+":"+e.port);
					}
				}else if (i1[1].equals(new String(Type.ACK.value)) && stauts==2) {
					boolean exist = false;
					for (String host : foundNode) {
						if (host.equals(item[0])) {
							exist = true;
							break;
						}
					}
					if(!exist) {
						foundNode.add(item[0]);
					}
					if(foundNode.size() == table.size() || table.size() == 0) {
						HBtimer.cancel();
						monitorTimer.cancel();
						for (Thread thread : threadList) {
							thread.stop();
						}
						complieted = true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

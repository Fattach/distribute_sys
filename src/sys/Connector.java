package sys;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Connector implements Runnable {
	
	Socket socket;
	List<Cell> table;
	InetSocketAddress localhost;
	
	public Connector(Socket socket, List<Cell> table, InetSocketAddress localhost) {
		this.socket = socket;
		this.table = table;
		this.localhost = localhost;
	}

	@SuppressWarnings({ "unused" })
	@Override
	public void run() {
		try {
			String name = "/";
			File file = null;
			String msg = "";
			InputStream in = socket.getInputStream();
			int len=-1;
			byte[] bytes = new byte[1024];
			if((len=in.read(bytes))!=-1) {
				msg = new String(bytes);
			}
			String rule = "^GET .* HTTP/1\\.1";
			Pattern pattern = Pattern.compile(rule);
			Matcher matcher = pattern.matcher(msg);
			if(matcher.find()) {
				String item = matcher.group(0);
				String[] words = item.split(" ");
				if(words[1].endsWith(".jpg")) {
					String url = URLDecoder.decode(this.getClass().getResource("/")+words[1].substring(1), "utf-8");
					file = new File(url.substring(6));
					if(file.exists()) {
						name = words[1];
					}
				}
				if(words[1].equals("/SD")) {
					for(Cell cell : table) {
						if (cell.address.equals(localhost.getHostName()) && cell.port == localhost.getPort()) {
							cell.stauts = 2;
						}
					}
				}
			}
			OutputStream out = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(out);
			if(name == "/") {
				writer.println("HTTP/1.1 200 OK");
				writer.println("Content-Type:text/html;charset=utf-8");
				writer.println();
				String json="{\"node\":[";
				for (Cell cell : table) {
					json+=cell.toString()+",";
				}
				json=json.substring(0,json.length()-1)+"]}";
				writer.println("<html><head><title>Monitor</title><style>div{width:100px;height:100px;margin:20px;background:#3c6;border-radius:50px;}"
						+ "button{width: 100px; height: 30px;margin: 20px;}</style></head><body><table id=\"table\"></table>"
						+ "<script src='https://code.jquery.com/jquery-3.3.1.min.js' integrity='sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=' crossorigin='anonymous'></script>"
						+ "<script>"
						+ "$(function() {\r\n" 
						+ "var json ="+json+";"
						+ "quickSort(json.node,0,json.node.length-1);"
						+ "    for (var i = 0; i < json.node.length; i++) {"+
						"$('#table').append('<tr id=\"node' + i + '\"><td><div id=\"nodeDiv'+i+'\"></div></td><td><img src=\"'+i+'.jpg\"/></td><td><p>' + json.node[i].address + ':' + json.node[i].port + '</p><p>Master:' + json.node[i].master + '</p><td></tr>');"+
						"$('#node' + i).append('<td><button class=\"sd\">Shutdown</button><td>');"+
						"if(json.node[i].master){"+
						"$('#nodeDiv'+i).css('background','#ff6');"+
						"}"+
						"if (json.node[i].stauts == 3) {"+
						"$('#nodeDiv' + i).css('background', '#c33');"+
						"}"+
						"}"+
						"$('.sd').click(function(){"+
						"var that = this;"+
						"$.ajax({"+
						"type:'GET',"+
						"url:\"http://\"+$(that).parent().parent().find('p:eq(0)').text()+\"/SD\","+
						"success:function(){"+
						"alert(\"You are home!\");"+
						"}"+
						"});"+
						"});"+
						"setTimeout(function() {location.href = '/'},2000);"+
						"function partition(a,left,right)\r\n" + 
						"	{\r\n" + 
						"		var i=left;\r\n" + 
						"		var j=right;\r\n" + 
						"		var temp=a[i];\r\n" + 
						"		while(i<j)\r\n" + 
						"		{\r\n" + 
						"			while(i<j && a[j].weight<=temp.weight){\r\n" + 
						"				j--;\r\n" + 
						"			}\r\n" + 
						"			if(i<j){\r\n" + 
						"				a[i]=a[j];\r\n" + 
						"			}\r\n" + 
						"			while(i<j && a[i].weight>=temp.weight){\r\n" + 
						"				i++;\r\n" + 
						"			}\r\n" + 
						"			if(i<j){\r\n" + 
						"				a[j]=a[i];\r\n" + 
						"			}\r\n" + 
						"		}\r\n" + 
						"		a[i]=temp;\r\n" + 
						"		return i;\r\n" + 
						"	}\r\n" + 
						"	function quickSort(a,left,right)\r\n" + 
						"	{\r\n" + 
						"	    var dp;\r\n" + 
						"	    if(left<right)\r\n" + 
						"	    {\r\n" + 
						"	        dp=partition(a,left,right);\r\n" + 
						"	        quickSort(a,left,dp-1);\r\n" + 
						"	        quickSort(a,dp+1,right);\r\n" + 
						"	    }\r\n" + 
						"	}"
						+ "})"
						+ "</script></body></html>");
			}else if(name.endsWith(".jpg")){
				out.write("HTTP/1.1 200 OK\r\n".getBytes());
				out.write("Content-Type:image/jpeg\r\n\r\n".getBytes());
				FileInputStream reader=new FileInputStream(file);
				len=-1;
				bytes= new byte[30720];
				while((len=reader.read(bytes))!=-1) {
					out.write(bytes);
				}
				reader.close();
			}
			writer.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

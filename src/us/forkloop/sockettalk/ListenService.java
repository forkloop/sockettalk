package us.forkloop.sockettalk;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ListenService extends IntentService {
	
	private ServerSocket inSocket;
	private Socket outSocket;
	private ServerSocketChannel channel;
	
	public ListenService() {
		super("ListenService");
	}
	
	@Override
	public void onDestroy() {
		try{
			inSocket.close();
			outSocket.close();
		} catch(IOException e) {
			Log.i("log", e.toString());
		}
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		
		try{
			SocketTalkActivity.selector = Selector.open();
			int port = intent.getIntExtra("myPort", 10000);
			channel = ServerSocketChannel.open();
			channel.configureBlocking(false);			
			inSocket = channel.socket();
			inSocket.bind(new InetSocketAddress("10.0.2.15", port));
			
			channel.register(SocketTalkActivity.selector, SelectionKey.OP_ACCEPT);
			//inSocket = new ServerSocket(port, 0, InetAddress.getByName("10.0.2.15"));
			//Log.i("log", "binding at "+ port);
			//Log.i("log", "binding address "+ inSocket.getInetAddress().toString());
			
			while (true) {
				SocketTalkActivity.selector.select();
				
				Iterator iter = SocketTalkActivity.selector.selectedKeys().iterator();
				
				while (iter.hasNext()) {
					SelectionKey key = (SelectionKey)iter.next();
					
					if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
						ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
						SocketChannel sc = ssc.accept();
						sc.configureBlocking(false);
						sc.register(SocketTalkActivity.selector, SelectionKey.OP_READ);
						SocketTalkActivity.out.add(new PrintWriter(sc.socket().getOutputStream(), true));
						
					}
					else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
						SocketChannel sc = (SocketChannel)key.channel();
					}

					iter.remove();
				}
			}
			/*
			outSocket = inSocket.accept();
		//	 http://thiranjith.wordpress.com/2010/11/03/
		//	 	sharing-information-objectsdata-between-activities-within-an-android-application/
		//	 
			SocketTalkActivity.out = new PrintWriter(outSocket.getOutputStream(), true);
			Log.i("log", "server output stream "+ SocketTalkActivity.out);
			BufferedReader in = new BufferedReader(new InputStreamReader(outSocket.getInputStream()));
			String inMsg;

			while((inMsg = in.readLine())!=null) {
				Log.i("log", "recv a msg "+ inMsg);
				if(inMsg.length()>0){
					Intent i = new Intent();
					i.putExtra("msg", inMsg);
					i.setAction("us.forkloop.sockettalk.RECV");
					Log.i("log", "send a broadcast msg");
					this.sendBroadcast(i);
				}
			}
			*/
		} catch(IOException e){
			Log.i("log", e.toString());
		}
		
	}
}

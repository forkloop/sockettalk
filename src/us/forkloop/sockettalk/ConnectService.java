package us.forkloop.sockettalk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ConnectService extends IntentService {

//	private Socket outSocket;
	
	public ConnectService() {
		super("ConnectService");
	}
	
	@Override
	public void onDestroy() {
//		try{
//			outSocket.close();
//		} catch(IOException e) {
//			Log.i("log", e.toString());
//		}
	}

	@Override
	protected void onHandleIntent (Intent intent) {

		try {
			int port = intent.getIntExtra("peerPort", 10000);
			String address = intent.getStringExtra("peerAddress");
			
			Log.i("log", "connecting to "+ address + ":" + port);
			
			SocketChannel sc = SocketChannel.open();
			sc.configureBlocking(false);
			sc.connect(new InetSocketAddress("10.0.2.2", port));
			while (!sc.finishConnect()) {
				Log.i("log", "NOT YET");
			}
			//sk.connect(new InetSocketAddress("10.0.2.2", port), 10000);
			Log.i("log", "Connecting successfully");

			SocketTalkActivity.selector = SocketTalkActivity.selector.wakeup();
			//Log.i("log", "WAKE UP");
			sc.register(SocketTalkActivity.selector, (SelectionKey.OP_READ | SelectionKey.OP_WRITE));
			Log.i("log", "Register OK " + SocketTalkActivity.selector.selectedKeys().size());
			//SocketTalkActivity.out.add(sk.getOutputStream());			
			Log.i("log", "Out connection " + SocketTalkActivity.out.size());
		/*
			outSocket = new Socket("10.0.2.2", 14214);
			//outSocket = new Socket(address, port);
			Log.i("log", outSocket.toString());
			SocketTalkActivity.out = new PrintWriter(outSocket.getOutputStream(), true);
			Log.i("log", "output stream "+ SocketTalkActivity.out);
			BufferedReader in = new BufferedReader(new InputStreamReader(outSocket.getInputStream()));
			String inMsg;
			while ((inMsg = in.readLine())!=null) {
				if(inMsg.length()>0){
					Intent i = new Intent();
					i.putExtra("msg", inMsg);
					i.setAction("us.forkloop.sockettalk.RECV");
					this.sendBroadcast(i);
				}
			}
		*/
		} catch (UnknownHostException e) {
			Log.i("log", e.toString());
			//Toast.makeText(getApplicationContext(), e.toString(), 2000).show();
		}
		catch (IOException e) {
			Log.i("log", e.toString());
		}
	}
}

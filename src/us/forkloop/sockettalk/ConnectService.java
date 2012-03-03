package us.forkloop.sockettalk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ConnectService extends IntentService {

	private Socket outSocket;
	
	public ConnectService() {
		super("ConnectService");
	}
	
	@Override
	public void onDestroy() {
		try{
			outSocket.close();
		} catch(IOException e) {
			Log.i("log", e.toString());
		}
	}

	@Override
	protected void onHandleIntent (Intent intent) {

		try {

			int port = intent.getIntExtra("peerPort", 10000);
			String address = intent.getStringExtra("peerAddress");
			Log.i("log", "connecting to "+ address + ":" + port);
			
			SocketChannel sc = SocketChannel.open();
			Socket sk = sc.socket();
			sk.connect(new InetSocketAddress("10.0.2.2", 10000));
			sc.register(SocketTalkActivity.selector, SelectionKey.OP_READ);
			
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

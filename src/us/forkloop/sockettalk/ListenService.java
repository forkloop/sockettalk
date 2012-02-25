package us.forkloop.sockettalk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class ListenService extends IntentService {
	
	private ServerSocket inSocket;
	private Socket outSocket;
	
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
			int port = intent.getIntExtra("myPort", 14214);
			inSocket = new ServerSocket(port, 0, InetAddress.getByName("10.0.2.15"));
			Log.i("log", "binding at "+ port);
			Log.i("log", "binding address "+ inSocket.getInetAddress().toString());
			Toast.makeText(this, "binding successful", 2000).show();
			outSocket = inSocket.accept();
			/* http://thiranjith.wordpress.com/2010/11/03/
				sharing-information-objectsdata-between-activities-within-an-android-application/
			*/
			SocketTalkActivity.out = new PrintWriter(outSocket.getOutputStream(), true);
			Log.i("log", "server output stream "+ SocketTalkActivity.out);
			BufferedReader in = new BufferedReader(new InputStreamReader(outSocket.getInputStream()));
			String inMsg;
			//Context context = 
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
		} catch(IOException e){
			Log.i("log", e.toString());
			Toast.makeText(getApplicationContext(), e.toString(), 2000).show();
		}
		
	}
}

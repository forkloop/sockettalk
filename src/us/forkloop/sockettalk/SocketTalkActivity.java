package us.forkloop.sockettalk;


import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SocketTalkActivity extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
    
	private static final String MSG_SENDER = "provider_key";
	private static final String MSG_CONTENT = "provider_value";
	private static final Uri TABLE_URI = Uri.parse("content://edu.buffalo.cse.cse486_586.provider");
	
	// used to break tie when two msgs have the same seq.#
	private int id;
	// total msg number sent on this device
	private static int msg_count = 0;
	private int my_port;
	private Button sendButton;
	private EditText sendMsg;
	private RelativeLayout display;
	private Receiver recvHandler;
	private Context activityContext;
	
	// global variables 
	// XXX replace with extends App
	static Selector selector;
	static ArrayList<OutputStream> out;
	//static ArrayList<PrintWriter> out;
	//static PrintWriter[] out;
	static LinkedList<Msg> hold_back;
	static ArrayList<Integer> stat;
	static ArrayList<Integer> seq;
	
	public Resources res;
	public Drawable shape;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	
        // keep the peers
        out = new ArrayList();
        activityContext = this;
        res = getResources();
        shape = res.getDrawable(R.drawable.msgbg);
        PreferenceManager.setDefaultValues(this, R.layout.preference, true);
        SharedPreferences sharePref = PreferenceManager.getDefaultSharedPreferences(this);
    	
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        id = Integer.parseInt(tel.getLine1Number().substring(tel.getLine1Number().length()-4));
        
        my_port = Integer.parseInt(sharePref.getString("portnum", "10000"));
        setContentView(R.layout.main);
        
        Intent intent = new Intent(this, ListenService.class);
        intent.putExtra("my_port", my_port);
        /* listen to port: my_port */
        startService(intent);
        
        sendButton = (Button) findViewById(R.id.send_button);
        sendButton.setOnClickListener(this);
        
        sendMsg = (EditText) findViewById(R.id.input_msg);
        display = (RelativeLayout) findViewById(R.id.msg_display);
    }
	
	@Override
	public void onResume() {
		super.onResume();
		if(recvHandler == null) recvHandler = new Receiver();
		IntentFilter intentFilter = new IntentFilter("us.forkloop.sockettalk.RECV");
		registerReceiver(recvHandler, intentFilter);
	}
    
	@Override
	public void onPause() {
		super.onPause();
		if(recvHandler != null) unregisterReceiver(recvHandler);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Intent intent;
		intent = new Intent(this, ListenService.class);
		stopService(intent);
		intent = new Intent(this, ConnectService.class);
		stopService(intent);
		unregisterReceiver(recvHandler);
	}
		
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.test:
    		Test();
    		return true;
    	case R.id.connect:
    		showDialog(0);
    		return true;
    	case R.id.setting:
    		Intent settingIntent = new Intent(this, SettingActivity.class);
    		startActivity(settingIntent);
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    		Dialog dialog;
    		switch(id) {
    		case 0:
    			Log.i("log", "connection setup...");
    			dialog = new Dialog(this);
    		    dialog.setContentView(R.layout.connect);
    		    dialog.setTitle("Settings");
    			Button cancelButton = (Button) dialog.findViewById(R.id.cancel_button);
    			Button okButton = (Button) dialog.findViewById(R.id.ok_button);
				final EditText portView = (EditText) dialog.findViewById(R.id.port);
				final EditText addressView = (EditText) dialog.findViewById(R.id.address);
    			cancelButton.setOnClickListener(new OnClickListener(){ 
    				public void onClick(View v) {
    					dismissDialog(0);
    				}
    			});
    			okButton.setOnClickListener(new OnClickListener(){
    				public void onClick(View v) {
    					int peerPort = Integer.parseInt(portView.getText().toString());
    					String peerAddress = addressView.getText().toString();
    				///////// replace with connect() method /////////
    				/*	Toast.makeText(getApplicationContext(), peerAddress+":"+peerPort, 1000).show();
    					Intent intent = new Intent(getApplicationContext(), ConnectService.class);
    		    		intent.putExtra("peerPort", peerPort);
    		    		intent.putExtra("peerAddress", peerAddress);
    		    		startService(intent);
    				*/
    					try {
							connect(peerAddress, peerPort);
						} catch (IOException e) {
							e.printStackTrace();
						}
    		    		dismissDialog(0);
    				}
    			});
    			break;
    		default:
    			dialog = null;
    		}
    		return dialog;
    }

    //
    // send out
    //
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.send_button:
			
			String text = sendMsg.getText().toString();
			/* send the messages if user DO enter something */
			if (text.length() > 0) {
				sendMsg.setText("");
				Msg msg = new Msg();
				msg.msg_content = text;
				msg.send_id = id;
				msg.msg_type = MsgType.MSG;
				msg.msg_id = msg_count;
				//XXX seq #
				
				for (OutputStream os : out) {
					try {
						ObjectOutputStream obj_out = new ObjectOutputStream(os);
						obj_out.writeObject(msg);
					} catch (IOException e) {
						Log.i("log", e.toString());
					}
					//pw.println(text);
				}
				/* now we should add it to the holdback queue */
				hold_back.add(msg);
				
//				TextView tv = new TextView(this);
//				tv.setText(text);
//				tv.setBackgroundDrawable(shape);
//				tv.setTextColor(Color.RED);				
//				tv.setId(++msg_count);
//				RelativeLayout.LayoutParams layRule = 
//						new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 
//								RelativeLayout.LayoutParams.WRAP_CONTENT);
//			    layRule.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//			    layRule.setMargins(2, 3, 2, 3);
//			    if (msg_count==0) {
//			    	layRule.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//			    }
//			    else {
//			    	layRule.addRule(RelativeLayout.BELOW, msg_count-1);
//			    }
//			    display.addView(tv, layRule);
//			    // store into db
//				ContentValues inserted = new ContentValues();
//				inserted.put(MSG_SENDER, "me");
//				inserted.put(MSG_CONTENT, text);
//				Uri uri = getApplicationContext().getContentResolver().insert(TABLE_URI, inserted);
			}
			break;
		}
		
	}
	
	//
	// For incoming messages, set color to blue, and align to right
	//
	private class Receiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("log", "recv a broadcast");
			if (intent.getAction().equals("us.forkloop.sockettalk.RECV")) {
				Log.i("log", "recv a broadcast msg");
				// each time a msg is displayed, it will received the agreed msg
				// first.
				
				TextView tv = new TextView(activityContext);
				String msg = intent.getStringExtra("msg");
				tv.setText(msg);
				tv.setBackgroundDrawable(shape);
				tv.setTextColor(Color.GREEN);
				tv.setId(++msg_count);
			    RelativeLayout.LayoutParams layRule = 
			    		new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 
			    				RelativeLayout.LayoutParams.WRAP_CONTENT);
			    layRule.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			    layRule.setMargins(2, 3, 2, 3);
			    if (msg_count == 0)
			    	layRule.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			    else
			    	layRule.addRule(RelativeLayout.BELOW, msg_count-1);
				display.addView(tv, layRule);
				// store into db
				ContentValues inserted = new ContentValues();
				inserted.put(MSG_SENDER, "peer");
				inserted.put(MSG_CONTENT, msg);
				Uri uri = getApplicationContext().getContentResolver().insert(TABLE_URI, inserted);				
			}
		}
	}
	
	//
	/////// connect to a remote host
	//
	public void connect(String address, int port) throws IOException {
		
		Log.i("log", "connecting to " + address + ":" + port);		
		SocketChannel sc = SocketChannel.open();
		Socket sk = sc.socket();
		sk.connect(new InetSocketAddress("10.0.2.2", 10000));
		sc.register(selector, SelectionKey.OP_READ);
		out.add(sk.getOutputStream());
		//out.add(new PrintWriter(sk.getOutputStream(), true));
	}
	
	public void Test() {
		
		String[] projection = {MSG_SENDER, MSG_CONTENT};
		
		Cursor c = getApplicationContext().getContentResolver().query(TABLE_URI, projection, null, null, null);
		
		int sender_index = c.getColumnIndexOrThrow(MSG_SENDER);
		int content_index = c.getColumnIndexOrThrow(MSG_CONTENT);
		
		if(c != null){
			int count = 0;
			while(c.moveToNext()) {
				TextView tv = new TextView(activityContext);
				tv.setText(c.getString(content_index));
				tv.setBackgroundDrawable(shape);
				tv.setId(++count);
				RelativeLayout.LayoutParams layRule = 
			    		new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 
			    				RelativeLayout.LayoutParams.WRAP_CONTENT);
				layRule.setMargins(2, 3, 2, 3);
				layRule.addRule(RelativeLayout.BELOW, count-1);
				display.addView(tv, layRule);
			}
		}
		else{
		Toast.makeText(getApplicationContext(), "no message", 1000).show();
		}
	}
}
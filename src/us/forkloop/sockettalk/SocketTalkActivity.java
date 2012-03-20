package us.forkloop.sockettalk;


import java.io.IOException;
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

public class SocketTalkActivity extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
    
	private static final String MSG_SENDER = "provider_key";
	private static final String MSG_CONTENT = "provider_value";
	private static final Uri TABLE_URI = Uri.parse("content://edu.buffalo.cse.cse486_586.provider/msgs");
	
	// Used to break tie when two msgs have the same seq.#
	static int id;
	
	// # of messages already deliveryed
	static int total_count = 0;
	// # of messages sent by me
	static int sent_count = 0;
	
	////////////////////
	static int Amax;
	static int Pmax;
	///////////////////
	private int my_port;
	private Button sendButton;
	private EditText sendMsg;
	private RelativeLayout display;
	private Receiver recvHandler;
	private Context activityContext;
	
	///////// Global variables /////////// 
	// XXX replace with extends App
	static Selector selector;
	static ArrayList<SocketChannel> out;
	static LinkedList<Msg> hold_back;
	static ArrayList<Integer> stat;
	static ArrayList<Integer> seq;
	
	public Resources res;
	public Drawable shape;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	
        out = new ArrayList<SocketChannel>();
        hold_back = new LinkedList<Msg>();
        stat = new ArrayList<Integer>(10);
        seq = new ArrayList<Integer>(10);
        
        // Fill in the damn first elements
        stat.add(0);
        seq.add(0);

        activityContext = this;
        // For the bubble
        res = getResources();
        shape = res.getDrawable(R.drawable.msgbg);
        
        PreferenceManager.setDefaultValues(this, R.layout.preference, true);
        SharedPreferences sharePref = PreferenceManager.getDefaultSharedPreferences(this);
    	
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        id = Integer.parseInt(tel.getLine1Number().substring(tel.getLine1Number().length()-4));
        Log.i("log", "Id " + id);
        
        my_port = Integer.parseInt(sharePref.getString("portnum", "10000"));
        Log.i("log", "Listening at " + my_port);
        
        setContentView(R.layout.main);
                
        try {
        	selector = Selector.open();
        	Log.i("log", "Selector " + selector.toString());
        } catch (IOException e) {
        	Log.i("log", e.toString());
        }

        /* listen to port: my_port */

        Intent intent = new Intent(this, ListenService.class);
        intent.putExtra("my_port", my_port);
        startService(intent);
        
        sendButton = (Button) findViewById(R.id.send_button);
        sendButton.setOnClickListener(this);
        
        sendMsg = (EditText) findViewById(R.id.input_msg);
        display = (RelativeLayout) findViewById(R.id.msg_display);
        
        Button testButton1 = (Button) findViewById(R.id.test1);
        testButton1.setOnClickListener(this);
        
        Button testButton2 = (Button) findViewById(R.id.test2);
        testButton2.setOnClickListener(this);
        
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
    		TestDB();
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
    				//	Toast.makeText(getApplicationContext(), peerAddress+":"+peerPort, 1000).show();
    					Intent intent = new Intent(getApplicationContext(), ConnectService.class);
    		    		intent.putExtra("peer_port", peerPort);
    		    		intent.putExtra("peer_address", peerAddress);
    		    		startService(intent);
    				
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
    // Sending out.........
    //
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.send_button:			
			String text = sendMsg.getText().toString();
			if (text.length() > 0) {
				
				sendMsg.setText("");

				stat.add(1);
				Log.i("log", "Stat[]: " + stat.toString());
				Pmax = Math.max(Amax, Pmax) + 1;
				seq.add(Pmax);
				Log.i("log", "Seq[]: " + seq.toString());
				
				Intent intent = new Intent(this, SendService.class);
				intent.putExtra("text", text);
				startService(intent);
			}
			break;
		case R.id.test1:
			Log.i("log", "Test One...");
			Test1();
			break;
		case R.id.test2:
			Log.i("log", "Test Two...");
			Test2();
			break;
		}	
	}
	
	//
	// For incoming messages, set color to blue, and align to right
	//
	private class Receiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			if ( intent.getAction().equals("us.forkloop.sockettalk.RECV") ) {
				Log.i("log", "Displaying a msg");
				
				TextView tv = new TextView(activityContext);
				String msg = intent.getStringExtra("msg");
				String peer = "" + intent.getIntExtra("id", 0);
				Log.i("log", "Displaying: " + msg);
				tv.setText(msg);
				tv.setBackgroundDrawable(shape);
				tv.setTextColor(Color.RED);
				tv.setId( ++total_count );
			    RelativeLayout.LayoutParams layRule = 
			    		new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 
			    				RelativeLayout.LayoutParams.WRAP_CONTENT);
			    layRule.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			    layRule.setMargins(2, 3, 2, 3);
			    if (total_count == 0)
			    	layRule.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			    else
			    	layRule.addRule(RelativeLayout.BELOW, total_count-1);
				display.addView(tv, layRule);
				
				//////////////////////////////////
				////// Save to database /////////
				////////////////////////////////
				ContentValues inserted = new ContentValues();
				inserted.put(MSG_SENDER, peer);
				inserted.put(MSG_CONTENT, msg);
				Log.i("log", "Inserted message: " + inserted.toString());
				Uri uri = getApplicationContext().getContentResolver().insert(TABLE_URI, inserted);
				Log.i("log", "Inserting a new message: " + uri.toString());
			}
		}
	}
	
	public void Test1() {
		
		Intent i = new Intent(this, TestOne.class);
		startService(i);
	}
	
	public void Test2() {
		
		Intent i = new Intent(this, TestTwo.class);
		startService(i);
	}
	
	public void TestDB() {
		
		String[] projection = {MSG_SENDER, MSG_CONTENT};
		
		Cursor c = getApplicationContext().getContentResolver().query(TABLE_URI, projection, null, null, null);
		Log.i("log", "# of saved messages is " + c.getCount());
		
//		int sender_index = c.getColumnIndexOrThrow(MSG_SENDER);
		int content_index = c.getColumnIndexOrThrow(MSG_CONTENT);
		
		if(c.getCount() > 0 ) {
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
		else {
			Log.i("log", "Ops, NO MESSAGES");
		}
	}
}
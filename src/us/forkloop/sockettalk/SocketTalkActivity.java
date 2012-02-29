package us.forkloop.sockettalk;


import java.io.PrintWriter;

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
//import java.net.ServerSocket;
//import java.net.Socket;

public class SocketTalkActivity extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
    
	private static final String MSG_SENDER = "provider_key";
	private static final String MSG_CONTENT = "provider_value";
	private static final Uri TABLE_URI = Uri.parse("content://edu.buffalo.cse.cse486_586.provider");
	
	private static int msgCount = 0;
	private int peerPort;
	private int myPort;
	private String peerAddress;
	private Button sendButton;
	private EditText sendMsg;
	private RelativeLayout display;
	//private Socket outSocket;
	//private ServerSocket inSocket;
	private Receiver recvHandler;
	private Context activityContext;
	public static PrintWriter out;
	
	public Resources res;
	public Drawable shape;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	
        activityContext = this;
        res = getResources();
        shape = res.getDrawable(R.drawable.msgbg);
        PreferenceManager.setDefaultValues(this, R.layout.preference, true);
        SharedPreferences sharePref = PreferenceManager.getDefaultSharedPreferences(this);
        Log.i("log", sharePref.getString("portnum", "14214"));
    	
        myPort = Integer.parseInt(sharePref.getString("portnum", "14214"));
    	//Log.i("log", "my listening port is "+ sharePref.getInt("portnum", 14214)); 	
        setContentView(R.layout.main);
        
        Intent intent = new Intent(this, ListenService.class);
        intent.putExtra("myPort", myPort);
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
	
	/* why is setter and getter instead of outSocket or this.outSocket
	 * 
	 */
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	// disable it after connecting
    	case R.id.test:
    		Test();
    		return true;
    	case R.id.connect:
    		showDialog(0);
    		return true;
//    	// or called disconnect ? and disable it before connect
//    	case R.id.signout:
//    		return true;
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
    			Log.i("log", "connecting setup");
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
    					peerPort = Integer.parseInt(portView.getText().toString());
    					peerAddress = addressView.getText().toString();
    					Toast.makeText(getApplicationContext(), peerAddress+":"+peerPort, 1000).show();
    					Log.i("log", "peer's address is "+ peerAddress);
    					Log.i("log", "peer's port is "+ peerPort);
    					Intent intent = new Intent(getApplicationContext(), ConnectService.class);
    		    		intent.putExtra("peerPort", peerPort);
    		    		intent.putExtra("peerAddress", peerAddress);
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

    // For incoming msg, set color to blue, and align to right
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.send_button:
			String msg = sendMsg.getText().toString();
			sendMsg.setText("");
			if(msg.length()>0) {
				Log.i("log", "activity output stream "+ out);
				out.println(msg);
				TextView tv = new TextView(this);
				tv.setText(msg);
				tv.setBackgroundDrawable(shape);
				tv.setTextColor(Color.RED);				
				tv.setId(++msgCount);
				//Log.i("log", ""+ msgCount +" msg");
				RelativeLayout.LayoutParams layRule = 
						new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 
								RelativeLayout.LayoutParams.WRAP_CONTENT);
			    layRule.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			    layRule.setMargins(2, 3, 2, 3);
			    if(msgCount==0){
			    	layRule.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			    }
			    else{
			    	layRule.addRule(RelativeLayout.BELOW, msgCount-1);
			    }
			    display.addView(tv, layRule);
			    // store into db
				ContentValues inserted = new ContentValues();
				inserted.put(MSG_SENDER, "me");
				inserted.put(MSG_CONTENT, msg);
				Uri uri = getApplicationContext().getContentResolver().insert(TABLE_URI, inserted);
			}
			break;
		}
		
	}
	
	private class Receiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("log", "recv a broadcast");
			if(intent.getAction().equals("us.forkloop.sockettalk.RECV")){
				Log.i("log", "recv a broadcast msg");
				TextView tv = new TextView(activityContext);
				String msg = intent.getStringExtra("msg");
				tv.setText(msg);
				tv.setBackgroundDrawable(shape);
				tv.setTextColor(Color.GREEN);
				tv.setId(++msgCount);
			    RelativeLayout.LayoutParams layRule = 
			    		new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 
			    				RelativeLayout.LayoutParams.WRAP_CONTENT);
			    layRule.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			    layRule.setMargins(2, 3, 2, 3);
			    if(msgCount==0)
			    	layRule.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			    else
			    	layRule.addRule(RelativeLayout.BELOW, msgCount-1);
				display.addView(tv, layRule);
				// store into db
				ContentValues inserted = new ContentValues();
				inserted.put(MSG_SENDER, "peer");
				inserted.put(MSG_CONTENT, msg);
				Uri uri = getApplicationContext().getContentResolver().insert(TABLE_URI, inserted);				
			}
		}
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
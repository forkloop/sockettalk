package us.forkloop.sockettalk;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class TestOne extends IntentService {

	public TestOne() {
		super("TestOne");
	}
	
	@Override
	public void onDestroy() {
		
	}
	
	@Override
	protected void onHandleIntent (Intent intent) {
		
		int id = SocketTalkActivity.id;
		
		for (int i = 1; i<=5; i++) {
			String m = "" + id + ":" + i;
			Log.i("log", m);
			SocketTalkActivity.stat.add(1);
			Log.i("log", "Stat[]: " + SocketTalkActivity.stat.toString());
			SocketTalkActivity.Pmax = Math.max(SocketTalkActivity.Amax, SocketTalkActivity.Pmax) + 1;
			SocketTalkActivity.seq.add(SocketTalkActivity.Pmax);
			Log.i("log", "Seq[]: " + SocketTalkActivity.seq.toString());
			
			Intent send_intent = new Intent(this, SendService.class);
			send_intent.putExtra("text", m);
			startService(send_intent);
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}

package us.forkloop.sockettalk;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class TestTwo extends IntentService {

	public TestTwo() {
		super("TestTwo");
	}
	
	@Override
	public void onDestroy() {
		
	}
	
	@Override
	protected void onHandleIntent (Intent intent) {

		SocketTalkActivity.TEST_TWO_FLAG = true;
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		int id = SocketTalkActivity.id;
		int i = 1;
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
	}
}

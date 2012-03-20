package us.forkloop.sockettalk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class SendService extends IntentService {

	public SendService() {
		super("SendService");
	}
	
	@Override
	public void onDestroy() {
		
	}
	
	@Override
	protected void onHandleIntent (Intent intent) {
		
		Msg msg = new Msg();
		msg.msg_content = intent.getStringExtra("text");
		//Log.i("log", intent.getStringExtra("text"));
		msg.msg_id = ++SocketTalkActivity.sent_count;
		Log.i("log", "# of msgs sent is " + SocketTalkActivity.sent_count);
//		SocketTalkActivity.Pmax = Math.max(SocketTalkActivity.Pmax, SocketTalkActivity.Amax) + 1;
		msg.msg_seq = SocketTalkActivity.Pmax;
		msg.send_id = SocketTalkActivity.id;
		msg.d_flag = false;
		SocketTalkActivity.hold_back.add(msg);
		
		byte[] bytes = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(msg);
			oos.flush();
			oos.close();
			bos.close();
			bytes = bos.toByteArray();
			
		} catch (IOException e) {
			Log.i("log", e.toString());
		}
		
		for (SocketChannel sc : SocketTalkActivity.out) {
			Log.i("log", "Send message....");
			try {
				sc.write(ByteBuffer.wrap(bytes));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}

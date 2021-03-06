package us.forkloop.sockettalk;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SocketTalkMsgOpenHelper extends SQLiteOpenHelper {

	private static final int DB_VERSION = 2;
	private static final String DB_NAME = "sockettalk.db";
	private static final String MSG_TABLE_NAME = "msg";
	private static final String MSG_SEND_NAME = "provider_key";
	private static final String MSG_CONTENT = "provider_value";
	private static final String MSG_TABLE_CREATE = 
			"CREATE TABLE " + MSG_TABLE_NAME + " (" +
					MSG_SEND_NAME + " TEXT, " +
					MSG_CONTENT + " TEXT);";
	
	public SocketTalkMsgOpenHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	public SocketTalkMsgOpenHelper(Context context, String dbname) {
		super(context, dbname, null, DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(MSG_TABLE_CREATE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i("log", "Upgrade database...");
		// XXX We are not here yet.
	}
}

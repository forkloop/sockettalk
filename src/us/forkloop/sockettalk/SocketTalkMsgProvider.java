package us.forkloop.sockettalk;

import java.util.HashMap;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class SocketTalkMsgProvider extends ContentProvider {

	private static final String DBNAME = "sockettalk";
	private static final String MSG_TABLE_NAME = "msg";
	private static final String AUTHORITY = "edu.buffalo.cse.cse486_586.provider";
	
	private SocketTalkMsgOpenHelper dbHelper;
	private static HashMap<String, String> msgProjectionMap;
	private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private SQLiteDatabase db;
	
	static {
		sUriMatcher.addURI(AUTHORITY, "", 1);	// all messages
		sUriMatcher.addURI(AUTHORITY, "*", 2);	// match message sender
		sUriMatcher.addURI(AUTHORITY, "#", 3);	// fetch specific message
		
		msgProjectionMap = new HashMap<String, String>();
		msgProjectionMap.put("_id", "_id");
		msgProjectionMap.put("provider_key", "provider_key");
		msgProjectionMap.put("provider_value", "provider_value");
	}
	
	@Override
	public String getType(Uri uri) {
		
		int match = sUriMatcher.match(uri);
		switch(match) {
		case 1:
			return "vnd.android.cursor.dir/vnd.us.forkloop.provider.msg";
		case 2:
			return "vnd.android.cursor.dir/vnd.us.forkloop.provider.msg";
		case 3:
			return "vnd.android.cursor.item/vnd.us.forkloop.provider.msg";
		default:
			return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		
		int match = sUriMatcher.match(uri);
		switch(match) {
		case 1:
			db = dbHelper.getWritableDatabase();
			long id = db.insert(MSG_TABLE_NAME, null, values);
			if(id > 0) {
				Uri inserted = ContentUris.withAppendedId(CONTENT_URI, id);
				// notify change
				return inserted;
			}
			throw new SQLException("Failed to insert " + uri);
		default:
			throw new IllegalArgumentException("Unknown URI" + uri);
		}
	}

	@Override
	public boolean onCreate() {
		
		dbHelper = new SocketTalkMsgOpenHelper(getContext(), DBNAME);
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(MSG_TABLE_NAME);
		
		switch (sUriMatcher.match(uri)) {
		case 1:
			// FIXME what's the point
			qb.setProjectionMap(msgProjectionMap);
			break;
		case 2:
			qb.setProjectionMap(msgProjectionMap);
			qb.appendWhere("provider_key=" + uri.getPathSegments().get(1));
			break;
		case 3:
			qb.setProjectionMap(msgProjectionMap);
			qb.appendWhere("_id=" + uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// FIXME
		return 0;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// FIXME
		return 0;
	}


	
}

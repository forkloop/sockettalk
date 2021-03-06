package us.forkloop.sockettalk;

import java.io.Serializable;

enum MsgType {
	MSG, PROP_SEQ, AGREE_SEQ;
}

/* class for messages */
public class Msg implements Serializable {

	int send_id;
	int msg_seq;
//	MsgType msg_type;
	String msg_content;
	int msg_id;
	boolean d_flag;		// Ready for delivery flag
	int[] recv;
	boolean test;
}

/* class for proposed sequence # */
class PropSeq implements Serializable {

	int msg_seq;
//	MsgType msg_type;
	int msg_id;
}

/* class for agreed sequence # */
class AgreeSeq implements Serializable {
	
	int msg_seq;
//	MsgType msg_type;
	int msg_id;
	int send_id;
}
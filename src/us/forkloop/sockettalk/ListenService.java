package us.forkloop.sockettalk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ListenService extends IntentService {
	
	private ServerSocket inSocket;
	private ServerSocketChannel channel;
	
	public ListenService() {
		super("ListenService");
	}
	
	@Override
	public void onDestroy() {
		try{
			inSocket.close();
		} catch(IOException e) {
			Log.i("log", e.toString());
		}
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		
		int id = SocketTalkActivity.id;
		Log.i("log", "Emulator: " + id);

		HashMap<Integer, Integer> delivery = new HashMap<Integer, Integer>();
		for(int j=5554; j<5564; j+=2)
			delivery.put(j, 0);
		
		// Connect to previous emulators first
		try {
			for (int i=5554; i<id; i+=2) {
				SocketChannel sc = SocketChannel.open(new InetSocketAddress("10.0.2.2", i*2));
				sc.configureBlocking(false);
				//WTF sc.connect(new InetSocketAddress("10.0.2.2", i*2));
				Socket sk = sc.socket();
				Log.i("log", "Really connect? " + sk.isConnected());
				sc.register(SocketTalkActivity.selector, (SelectionKey.OP_READ));
				SocketTalkActivity.out.add(sc);
				Log.i("log", "Connect to " + i);
			}
		} catch (IOException e) {
			Log.i("log", "Ops, Can't connect..." + e.toString());
		}
		
		// Start to listen
		try{
			int port = intent.getIntExtra("my_port", 10000);
			channel = ServerSocketChannel.open();
			channel.configureBlocking(false);			
			inSocket = channel.socket();
			inSocket.bind(new InetSocketAddress("10.0.2.15", port));
			Log.i("log", "Binding successfully at " + port);
			
			channel.register(SocketTalkActivity.selector, SelectionKey.OP_ACCEPT);
			
			while (true) {
				
				int num = SocketTalkActivity.selector.select();				
				
				if (num > 0) {
					Log.i("log", "...... " + num);
					Iterator<SelectionKey> iter = SocketTalkActivity.selector.selectedKeys().iterator();
					
					while (iter.hasNext()) {
						Log.i("log", "+++++++");
						SelectionKey key = iter.next();
						// New connection
						if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
							Log.i("log", "NEW CONNECTION");
							ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
							SocketChannel sc = ssc.accept();
							sc.configureBlocking(false);
							sc.register(SocketTalkActivity.selector, (SelectionKey.OP_READ));
							Log.i("log", "ACCEPT NEW CONNECTION " + sc.socket().isConnected());
							SocketTalkActivity.out.add(sc);
							Log.i("log", "# of connection: " + SocketTalkActivity.out.size());
						}
						// New message
						else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
							
							Log.i("log", "Receive a new message...");
							SocketChannel sc = (SocketChannel) key.channel();
							ByteBuffer bb = ByteBuffer.allocate(1000);
							sc.read(bb);
							byte[] bt = bb.array();							
							try {
								ByteArrayInputStream bis = new ByteArrayInputStream(bt);
								ObjectInputStream ois = new ObjectInputStream(bis);
								Object msg = ois.readObject();
								String msg_type = msg.getClass().getName();
								
								Log.i("log", "Receive a " + msg_type);
								
								if (msg_type.equals("us.forkloop.sockettalk.Msg")) {
									
									Msg mmsg = (Msg) msg;
									PropSeq pmsg = new PropSeq();
									// Determine proposed #
									SocketTalkActivity.Pmax = Math.max(SocketTalkActivity.Pmax, SocketTalkActivity.Amax) + 1;
									// Used to sort the messages
									mmsg.msg_seq = SocketTalkActivity.Pmax;
									// Add to hold back queue
									SocketTalkActivity.hold_back.add(mmsg);
									Log.i("log", "Proposed # is " + SocketTalkActivity.Pmax);
									pmsg.msg_id = mmsg.msg_id;
									pmsg.msg_seq = SocketTalkActivity.Pmax;
									
									byte[] bytes = null;
									ByteArrayOutputStream bos = new ByteArrayOutputStream();
									try {
										ObjectOutputStream oos = new ObjectOutputStream(bos);
										oos.writeObject(pmsg);
										oos.flush();
										oos.close();
										bos.close();
										bytes = bos.toByteArray();
									} catch (IOException e) {
										Log.i("log", e.toString());
									}
									sc.write(ByteBuffer.wrap(bytes));
								}

								else if (msg_type.equals("us.forkloop.sockettalk.AgreeSeq")) {
									
									AgreeSeq amsg = (AgreeSeq) msg;
									SocketTalkActivity.Amax = Math.max(SocketTalkActivity.Amax, amsg.msg_seq);
									
									int k = 0, pos = -1;
									int no = (Integer) delivery.get(amsg.send_id);
									
									int sm_seq, sm_ind = -1;
									sm_seq = amsg.msg_seq;
									boolean flag = true;
									
									for (Msg m : SocketTalkActivity.hold_back) {

//										if (m.msg_seq < sm_seq || (m.msg_seq == sm_seq && m.send_id < m.send_id)) {
//											flag = false;
//										}
										if (m.msg_id == amsg.msg_id && m.send_id == amsg.send_id ) {
											//&& (m.msg_id == no+1)) {
											//XXX Forget the FIFO first
											m.d_flag = true;
											m.msg_seq = amsg.msg_seq;
											pos = k;
											delivery.put(amsg.msg_id, no+1);
										}
										else {
											if (m.msg_seq < sm_seq || (m.msg_seq == sm_seq && m.send_id < m.send_id)) {
												flag = false;
											}
										}
										k++;
									}
									
									if (flag && pos >=0) {
										
										Msg m = SocketTalkActivity.hold_back.get(pos);
										SocketTalkActivity.hold_back.remove(pos);
										Intent i = new Intent();
										i.putExtra("id", m.send_id);
										i.putExtra("msg", m.msg_content);
										i.setAction("us.forkloop.sockettalk.RECV");
										this.sendBroadcast(i);
										Log.i("log", "Deliverying other message...");
										
										// Check if there are any blocked messages
										while (SocketTalkActivity.hold_back.size()>0 && flag) {
											flag = false;
											sm_seq = SocketTalkActivity.hold_back.getFirst().msg_seq;
											sm_ind = 0;
											int s_id = SocketTalkActivity.hold_back.getFirst().send_id;
											int j = 0;
											for (Msg mm: SocketTalkActivity.hold_back) {
												if (mm.msg_seq<sm_seq || (mm.msg_seq == sm_seq && mm.send_id < s_id)) {
													sm_ind = j;
													sm_seq = mm.msg_seq;
													s_id = mm.send_id;
												}
												j++;
											}
											
											Msg nm = SocketTalkActivity.hold_back.get(sm_ind);
											if (nm.d_flag) {
												SocketTalkActivity.hold_back.remove(sm_ind);
												Intent ii = new Intent();
												ii.putExtra("id", nm.send_id);
												i.putExtra("msg", nm.msg_content);
												ii.setAction("us.forkloop.sockettalk.RECV");
												this.sendBroadcast(ii);
												Log.i("log", "Deliverying blocked other message...");
												flag = true;
											}
										}
									}
								}
								
								else if (msg_type.equals("us.forkloop.sockettalk.PropSeq")) {

									PropSeq pmsg = (PropSeq) msg;
									int index = pmsg.msg_id;
									int value = SocketTalkActivity.seq.get(index);
									value = value > pmsg.msg_seq ? value : pmsg.msg_seq;
									SocketTalkActivity.seq.set(index, value);
									SocketTalkActivity.stat.set(index, SocketTalkActivity.stat.get(index)+1);
									
									// Received all proposed seq.
									if (SocketTalkActivity.stat.get(index) == SocketTalkActivity.out.size()+1) {
									
									//-------------------------------------------------------------	
									// Send agreed #
									//
										AgreeSeq amsg = new AgreeSeq();
										amsg.msg_id = pmsg.msg_id;
										amsg.send_id = SocketTalkActivity.id;
										amsg.msg_seq = value;
										// 
										byte[] bytes = null;
										ByteArrayOutputStream bos = new ByteArrayOutputStream();
										try {
											ObjectOutputStream oos = new ObjectOutputStream(bos);
											oos.writeObject(amsg);
											oos.flush();
											oos.close();
											bos.close();
											bytes = bos.toByteArray();
										} catch (IOException e) {
											Log.i("log", e.toString());
										}
										for (SocketChannel s : SocketTalkActivity.out) {
											Log.i("log", "Sending agreed message...");
											try {
												s.write(ByteBuffer.wrap(bytes));
											} catch (IOException e) {
												e.printStackTrace();
											}
										}
									//-------------------------------------------------------------
									// Display on my emulator
									// But we need to check wether it is the smallest one
										int k = 0, pos=-1;
										boolean flag = true;
										
										for (Msg m : SocketTalkActivity.hold_back) {
											
											if (m.send_id == id && m.msg_id == pmsg.msg_id) {
												m.d_flag = true;
												pos = k;
											}
											else {
												if (m.msg_seq<value || (m.msg_seq==value && m.send_id<id)) {
													flag = false;
												}
											}
											k++;
										}
										// Still need to delivery messages blocked by this message !!!
										if (flag && pos>=0) {
												Msg m = SocketTalkActivity.hold_back.remove(pos);

												Intent i = new Intent();
												i.putExtra("msg", m.msg_content);
												i.putExtra("id", id);
												i.setAction("us.forkloop.sockettalk.RECV");
												this.sendBroadcast(i);
												Log.i("log", "Deliverying my message...");
												
												while (SocketTalkActivity.hold_back.size()>0 && flag) {
													
													flag = false;
													int sm_seq = SocketTalkActivity.hold_back.getFirst().msg_seq;
													int sm_ind = 0;
													int s_id = SocketTalkActivity.hold_back.getFirst().send_id;
													int j = 0;
													for (Msg mm : SocketTalkActivity.hold_back) {
													
														if (mm.msg_seq < sm_seq || (mm.msg_seq == sm_seq && mm.send_id < s_id)) {
															sm_seq = mm.msg_seq;
															sm_ind = j;
															s_id = mm.send_id;
														}
														j++;
													}
													Msg nm = SocketTalkActivity.hold_back.get(sm_ind);
													if (nm.d_flag) {
														flag = true;
														SocketTalkActivity.hold_back.remove(sm_ind);
														Intent ii = new Intent();
														ii.putExtra("id", nm.send_id);
														i.putExtra("msg", nm.msg_content);
														ii.setAction("us.forkloop.sockettalk.RECV");
														this.sendBroadcast(ii);
														Log.i("log", "Deliverying my blocked message...");
													}
												}
										}
										
									}
								}
							} catch (ClassNotFoundException e) {
								Log.i("log", e.toString());
							}
						}
						iter.remove();
					}
				}
			}
		} catch(IOException e){
			Log.i("log", e.toString());
		}		
	}
}

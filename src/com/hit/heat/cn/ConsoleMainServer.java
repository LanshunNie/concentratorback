package com.hit.heat.cn;

import java.io.BufferedReader;
import java.io.File;

//import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
//import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import com.hit.heat.util.Frag_Recb;
import com.hit.heat.model.Energy;

import org.jfree.chart.axis.NumberTickUnit;
//import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.ls.LSException;

//import com.hit.heat.control.FTPMain;
import com.hit.heat.control.GlobalDefines;
import com.hit.heat.control.WriteFTPFile;
import com.hit.heat.data.SqlOperate;
import com.hit.heat.model.CurrentTime;
//import com.hit.heat.model.FloorInfor;
import com.hit.heat.model.Location;
import com.hit.heat.model.NetParameter;
import com.hit.heat.model.SynParameter;
import com.hit.heat.model.SystemParam;
import com.hit.heat.net.HeartOffLineHandler;
import com.hit.heat.net.HeartOnLineHandler;
import com.hit.heat.net.HeartProxy;
import com.hit.heat.net.NIOUDPServer;
import com.hit.heat.net.NIOUDPServerMsgHandler;
import com.hit.heat.net.NettyClient;
import com.hit.heat.net.NettyMsgHandler;
import com.hit.heat.net.NettyServer;
import com.hit.heat.net.ParamConfigProxy;
import com.hit.heat.net.ParamConfigResult;
import com.hit.heat.net.ProxyInvoke;
import com.hit.heat.net.UnicastProxy;
import com.hit.heat.util.BitMap;
//import com.hit.heat.util.ByteCrypt;
//import com.hit.heat.util.DESPlus;
import com.hit.heat.util.GConfig;
import com.hit.heat.util.GSynConfig;
import com.hit.heat.util.Util;
import com.hit.heat.util.WriteDataToFile;
import com.hit.heat.util.rdc_EF_Control;
//import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
//import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.sun.corba.se.impl.activation.CommandHandler;
import com.sun.org.apache.xalan.internal.xsltc.runtime.Parameter;
import com.sun.xml.internal.ws.resources.StreamingMessages;

import sun.awt.CharsetString;
import sun.tools.jar.resources.jar;

/* @lhy
 * @date 2016.4.4
 * @des
 */
public class ConsoleMainServer {

	private GConfig config;// The external file includes the IP address and port
	private GSynConfig synConfig;// External files include synchronous mess ages
	private SystemParam g_systemParam;// External files include parameter
										// configurations such as the number of
										// retransmissions

	private NetParameter parameter;// parameter
	private static SynParameter synParameter;
	// private WriteDataToFile mulicastDataFile;
	// private WriteDataToFile unicastDataFile;
	// private WriteDataToFile cmdFile;
	// private WriteDataToFile topoFile;
	private WriteDataToFile rdcControlFile;
	// private WriteDataToFile topogxnFile;
	// private WriteDataToFile unicastDataFile;
	// private WriteDataToFile offLineDataFile;
	private WriteDataToFile FragFile;

	private NettyClient nettyClient;// netty client
	private NettyClient remoteClient;
	private NettyClient configRemoteNettyClient;// config information
	private NettyClient netClient;
	private NettyServer nettyServer;// Netty NIo TCP server

	// private NettyServer webNettyServer;//

	private NIOUDPServer nioUdpServer;// nio UDP server
	private NIOUDPServer nioUpperServer;// nio upper server
	private NIOUDPServer nioNetDataServer;// nio net Data server??
	private NIOUDPServer nioSynConfigServer;// nio synchronize config server
	private NIOUDPServer nioCorrectTime;// nio correct time

	/***************************************************************************/
	private NIOUDPServer nioRdcControlServer;// rdc control server
	private int rdcControlPort = 3103;// rdc control port
	private int rdcPanPort = 3102;// rdc pan port??
	private rdc_EF_Control rdcControl;
	private int current_budget = 26000;// nA
	private int current_guard = 2000;// nA
	private boolean rdcControlInit = false;
	/***************************************************************************/

	private UnicastProxy unicastProxy;
	private HeartProxy heartProxy;// heart beat proxy
	private ParamConfigProxy paramConfigProxy;// parameter config proxy

	private BitMap bitMap;// bitmap queue to judge resend
	// private Timer timer;// global timer
	// private int cur_retransmition_count = 0;// current resend times

	final int NODE_UNICAST_PORT = 5656; // node receive unicast command

	private ByteBuffer contentByteBuffer;
	private List<String> ipList;// get ip list
	private Map<Integer, Location> locationsMap;//
	private List<Location> locationsList;//

	// private String webToken = null;
	// private String webDataToken = "12345678";
	// private String revcMsg = null;
	private boolean webKeyFlag;
	private int SynLocalPort = 6102;
	private int CorrectTimePort = 1026;// correct time port
	private int CorrectAckPort = 1028;// get ack from node port
	private int broadcastPort = 6104;// broadcast Port
	private boolean synStateFlag = false;
	private int seqCount;
	private int currect_rate = 1;// second
	// private List<FloorInfor> floorInfors;
	// private DESPlus webTokenDesPlus;
	// private DESPlus webDataDesPlus;
	// private String remoteDataToken=null;
	// private String webDataToken = null;
	// private String broadcastAddr="FF02::2";
	// private int schedule_Port = 1028;
	Timer CorrectTimer = new Timer();// timing
	Timer APPTimer = new Timer();// timing applicationdata report
	Timer CommandDownTimer = new Timer();// timing command down
	Timer HeartTimer = new Timer();
	Timer Appdrop = new Timer();
	Timer Netdrop = new Timer();
	Timer topoTimer = new Timer();
	Timer GPRSTimer = new Timer();

	private Map<String, String> IpidMap;
	private Map<Integer, String> topoMap = new HashMap<Integer, String>();
	// private Map<String, String> topoMap = new HashMap<String, String>();
	boolean flag = false;
	private int MaxCount = 10;
	private int Count = 0;
	private static int Net_Status_flag = 0;
	// private static Logger logger = Logger.getLogger(FTPMain.class);

	public ConsoleMainServer() {
		SqlOperate.connect("jdbc:sqlite:topo3.db");
		SqlOperate.close();
		// SqlOperate.CommandCache_get();
		try {
			/***********************************************************/
			int drop_length = 365;
			Appdrop.schedule(new TimerTask() {
				public void run() {
					try {
						SqlOperate.NetMonitor_drop();
						SqlOperate.ApplicationData_drop();

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, 0, 1000 * 3600 * 24 * drop_length);

			rdcControl = new rdc_EF_Control(current_budget, current_guard);// nA
			rdcControlFile = new WriteDataToFile("rdcControlFile.txt");
			/***********************************************************/
			unicastProxy = new UnicastProxy();
			// cmdFile = new WriteDataToFile("cmd.txt");
			// mulicastDataFile = new WriteDataToFile("mulicast.txt");
			// unicastDataFile = new WriteDataToFile("unicast.txt");
			// topoFile = new WriteDataToFile("topo.txt");
			// topogxnFile = new WriteDataToFile("topogxn.txt");//
			FragFile = new WriteDataToFile("fragFile.txt");

			config = new GConfig("config.json");// get parameter from
												// config.json
			parameter = config.getNetParameter();
			// webToken = "01234567";
			webKeyFlag = true;
			// remoteKeyFlag = false;
			seqCount = 0;
			synConfig = new GSynConfig("GSynConfig.json");
			synParameter = synConfig.getSynParameter();
			// synStateFlag is the syn file`s flag of synParameter
			synStateFlag = synParameter.isFlag();
			System.out.println(synStateFlag);
			IpidMap = new HashMap<String, String>();
			// desPlus = new DESPlus(token);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			parameter = new NetParameter("00000001", 40, 3, 30, "0.0.0.0", 12300, 12301, 12306, "aaaa::1", 8765,
					"aaaa:0:0:0:12:7400:1:13", 5678, "192.168.1.141", 12303, "192.168.1.141", 12304, 12307, 2, 3,
					"0.0.0.0", 12400, "xiaoming", "139.199.154.37", "xiaoming", 21);
			synParameter = new SynParameter(0, 0, 0, 0, 0, 0, "0".getBytes(), false, null);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// add the information of centor
		// String rootplace = "000000";
		// SqlOperate.NodePlace_a(parameter.getId(), "000", rootplace,"000");

		try {
			g_systemParam = Util.parseSystemParamFromFile("sysparam.json");
		} catch (Exception e1) {

			g_systemParam = new SystemParam();
		}

		try {
			locationsMap = Util.parseLocationsFromFile("location.txt");
		} catch (IOException e) {

			e.printStackTrace();
			locationsMap = new HashMap<Integer, Location>(64);
		}
		locationsList = new ArrayList<Location>();
		locationsList.addAll(locationsMap.values());
		ipList = new ArrayList<String>();
		for (Location l : locationsList) {
			ipList.add(l.getAddr());
		}
		/******************* TCP client inital *****************************/
		nettyClient = new NettyClient(parameter.getTcpWebServerAddr(), parameter.getTcpWebServerPort());

		remoteClient = new NettyClient(parameter.getRemoteAddr(), parameter.getRemotePort());

		configRemoteNettyClient = new NettyClient(parameter.getRemoteAddr(), parameter.getTcpRemoteConfigPort());
		System.out.println(parameter.getRemoteAddr());
		netClient = new NettyClient(parameter.getRemoteAddr(), parameter.getNetPort());// parameter.getRemotePort()
		System.out.println(parameter.getNetPort());
		try {
			nioSynConfigServer = new NIOUDPServer(parameter.getUdpAddr(), SynLocalPort);
			nioSynConfigServer.registerHandler(new NIOSynMessageHandler());

			nioCorrectTime = new NIOUDPServer(parameter.getUdpAddr(), CorrectAckPort);// correct
																						// time
			nioCorrectTime.registerHandler(new NIOCorrectTimeHandler());

			nioRdcControlServer = new NIOUDPServer(parameter.getUdpAddr(), rdcPanPort);
			nioRdcControlServer.registerHandler(new NIOrdcContronHandler());

			try {
				nioCorrectTime.start();
				nioRdcControlServer.start();
			} catch (IllegalStateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// correct time
			CorrectTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						CurrentTime currentTime = Util.getCurrentDateTime(Util.getCurrentDateTime());
						if (Count < MaxCount) {
							nioCorrectTime.sendto(
									Util.getCorrectTimeMessage2(0x13, currentTime.getHour(), currentTime.getMinute(),
											currentTime.getSecond()),
									getSocketAddressByName(parameter.getRootAddr(), CorrectTimePort));
							Count++;
							System.out
									.println(Util.getCurrentTime() + " send ROOT correct time " + currentTime.getHour()
											+ ":" + currentTime.getMinute() + ":" + currentTime.getSecond());// for

						} else {
							System.out.println(Util.getCurrentTime() + " send root restart command:" + Count);// for
							// log
							nioCorrectTime.sendto(
									Util.getCorrectTimeMessage2(0x14, currentTime.getHour(), currentTime.getMinute(),
											currentTime.getSecond()),
									getSocketAddressByName(parameter.getRootAddr(), CorrectTimePort));
							Count = 0;
						}
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, 0, 1000 * currect_rate * 60);

//			GPRSTimer.schedule(new TimerTask() {
//				public void run() {
//					Process process = null;
//					List<String> processList = new ArrayList<String>();
//					try {
//						process = Runtime.getRuntime().exec("ping -c 3 baidu.com");
//						BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
//						String line = "";
//						while ((line = input.readLine()) != null) {
//							processList.add(line);
//						}
//						input.close();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//					int t = 0;
//					for (String line : processList) {
//						System.out.println(line);
//						System.out.println(t);
//						t += 1;
//					}
//					if (t < 7) {
//						String[] params = {};
//						python("GPRSOnline.py", params);
//					}
//				}
//			}, 0, 1000 * 300);

			// timing report application data
			// APPTimer.schedule(new TimerTask() {
			// public void run() {
			// //System.out.println("APP down daylength:");// for log
			// int i = 0;
			// Util.getCurrentDateTime(Util.getCurrentDateTime());
			// int appsend_length = parameter.getappSendLength();
			// //System.out.println("APP send length:" + appsend_length);// for
			// // log
			// String gap = Integer.toHexString(appsend_length);
			// int dif = 6 - gap.length();
			// for (i = 0; i < dif; i++) {
			// gap = "0" + gap;
			// }
			// System.out.println("gap:"+gap);
			// byte[] command = Util.formatByteStrToByte(gap);
			// sendApplicationData(command);
			// //System.out.println(Util.getCurrentTime()+" upload Application
			// file");// for log
			// }
			// }, 0, 1000 * 47);
			// 0, 1000 * parameter.getdayLength() * 24 * 3600);

			try {
				nioSynConfigServer.start();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/*********** unicast proxy config ************************/
		unicastProxy.registerMethod(new UnicastMethodInvoke());
		unicastProxy.setDelay(g_systemParam.getSysNodeTransDelay() * 1000);
		unicastProxy.setAckWaitDelay(g_systemParam.getSysNodeWaitAckDelay() * 1000);
		unicastProxy.setReTransmitCount(g_systemParam.getSysUcastRetransTimes());
		/*************** heart Proxy config ******************/

		heartProxy = new HeartProxy(ipList, g_systemParam.getSysNodeHeartDelay() * 1000);
		heartProxy.setInverval(g_systemParam.getSysNodeHeartDelay() * 1000);
		// heartProxy.setInverval(g_systemParam.getSysNodeHeartDelay() * 1000);
		heartProxy.registerOffLineHandler(new HeartOffLineHandler() {

			@Override
			public void actionPerformed(String addr) {
				System.out.println(addr + "has dropped");
				// TODO Auto-generated catch block
				// generate information ,report to remote client and tools
				// nettyClient.asyncWriteAndFlush(formatUcastDataToJsonStr("Warning",addr,"warning"));//formatUcastDataToJsonStr(addr,
				// "warning")
				if (remoteClient.remoteHostIsOnline()) {
					try {
						remoteClient.asyncWriteAndFlush(formatDataToJsonStr("heart_warning", addr, "warning"));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		});
		heartProxy.registerOnLineHandler(new HeartOnLineHandler() {
			@Override
			public void actionPerformed(String addr) {
				System.out.println(addr + " online");
				if (remoteClient.remoteHostIsOnline()) {
					try {
						remoteClient.asyncWriteAndFlush(formatDataToJsonStr("heart_succeed", addr, "online"));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				// TODO Auto-generated mwthod
			}
		});
		// heartProxy.start();
		paramConfigProxy = new ParamConfigProxy(parameter.getRootAddr(), new ProxyInvoke() {
			@Override
			public void invoke(String addr, int port, byte[] message) {
				// TODO Auto-generated mwthod
				try {
					nioUdpServer.sendto(message, getSocketAddressByName(addr, port));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		// list of config fail nodes
		paramConfigProxy.registerConfigResultHandler(new ParamConfigResult() {

			@Override
			public void actionPerformed(List<String> failIpList) {
				// TODO Auto-generated method
				System.out.println("using！");
				JSONObject msgJson = null;
				if (failIpList == null) {
					System.out.println(" config succeed");
					msgJson = new JSONObject();
					try {
						msgJson.put("type", "config_succeed");
						msgJson.put("data", "config_succeed");
						// msgJson.put("", "");
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if (configRemoteNettyClient.remoteHostIsOnline()) {
						try {
							configRemoteNettyClient.asyncWriteAndFlush(msgJson.toString());
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					return;
				}
				StringBuilder sb = new StringBuilder();
				sb.append("有节点配置失败，配置失败节点ip为\r\n");
				for (String ip : failIpList) {
					sb.append("  " + ip + "\r\n");
					System.out.println("a node configed failure. The Node IP is:" + ip);
				}
				try {
					msgJson = new JSONObject();
					msgJson.put("type", "config_fail");
					msgJson.put("data", sb.toString());
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (configRemoteNettyClient.remoteHostIsOnline()) {
					try {
						// System.out.println(msgJson.toString());
						configRemoteNettyClient.asyncWriteAndFlush(msgJson.toString());
						// remoteClient.asyncWriteAndFlush(remoteDataDesPlus.encrypt(formatDataToJsonStr("config_fail",)));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println("sb.toString() " + sb.toString());
				sb = null;
				msgJson = null;
				return;
			}
		});

		nettyServer = new NettyServer(parameter.getTcpAddr(), parameter.getTcpPort(), new NettyMsgHandlerExecutor());
		contentByteBuffer = ByteBuffer.allocate(128);
		bitMap = new BitMap();
		startNIOTcpServer();// start NIO Tcp Server
		startNIOUdpServer();// start NIO Udp Server
		try {
			nioNetDataServer = new NIOUDPServer("0.0.0.0", 5688);
			nioNetDataServer.registerHandler(new NIOUdpNetDataHandler());
			nioNetDataServer.start();
			// nioSynConfigServer
		} catch (IOException | IllegalStateException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		startUpperUdpServer();
		// timing heart beat
		HeartTimer.schedule(new TimerTask() {
			public void run() {
				heartbeat();
			}
		}, 0, 1000 * parameter.getHeartIntSec());
		// }, 0, 1000 * parameter.getHeartIntSec() * 1);
		// // timing command down
		// CommandDownTimer.schedule(new TimerTask() {
		// public void run() {
		// try {
		// CommandDown();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// // }, 0, 1000 * 5);
		// }, 0, 1000 * 60 * 10);
		// nioNetDataServer.stop();
		// startUpperUdpServer();
		// System.out.println("!!!");
		// nioUpperServer.registerHandler(new UpperUdpMessageHandler());
	}

	// ***********************************************************************************method
	// realize
	// UDP server send control information to root byte

	public void TunSendToRootMessage(byte[] message) throws IOException {
		if (nioUdpServer == null) {
			throw new IOException();
		}
		System.out.println(Util.getCurrentTime() + " Send to ROOT command is:" + Util.formatByteToByteStr(message));// for
		nioUdpServer.sendto(message, getSocketAddressByName(parameter.getRootAddr(), parameter.getRootPort()));// for
																												// //
																												// log
		// System.out.println("Send To Root Message over");// for lag
	}

	// ×××××××
	// report heart beat
	public String python(String pythonPath, String[] params) {
		File file = new File(pythonPath);
		if (!file.exists()) {
			return "python脚本不存在！";
		}

		String[] command = Arrays.copyOf(new String[] { "python", pythonPath }, params.length + 2);
		System.arraycopy(params, 0, command, 2, params.length);

		List<String> res = new ArrayList<>();
		try {
			Process process = Runtime.getRuntime().exec(command, null, null);
			process.waitFor();

			Scanner scanner = new Scanner(process.getInputStream());
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				System.out.println(line);
				res.add(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return "success";
	}

	public void heartbeat() {
		// System.out.println("start consentrator heartbeat");// for log
		// System.out.println("start heart beat:" + parameter.getupperAddr() +
		// parameter.getupperPort());// for

		// byte[] currenttime = new byte[Util.getCurrentTime().length()];
		// byte[] currenttime = Util.getCurrentTime().getBytes();
		// System.out.println(currenttime);
		byte[] heartlength_b = new byte[1];
		heartlength_b[0] = 21;
		byte[] heart_flag = new byte[1];
		heart_flag[0] = 0;
		long epocurrent = System.currentTimeMillis() / 1000;
		byte[] currenttime = Util.longToBytes(epocurrent);
		byte[] currenttime_b = new byte[6];
		System.arraycopy(currenttime, currenttime.length - 6, currenttime_b, 0, 6);
		byte[] status_b = new byte[1];
		boolean flag_b = Util.Online_Judge(synParameter.getBitmap());
		boolean flag_c = true;
		try {
			flag_c = SqlOperate.CommandCache_empty();
		} catch (SQLException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		if (flag_b == false) {
			if (flag_c == true) {
				status_b[0] = 1;
			} else {
				status_b[0] = 0;
			}
		} else {
			if (flag_c == true) {
				status_b[0] = 3;
			} else {
				status_b[0] = 2;
			}
		}
		String status = String.valueOf(Util.StatusJuage(flag));
		String str_centor = parameter.getId();
		byte[] centor = Util.intToByteArray(Integer.parseInt(str_centor));
		// System.out.println(centor[0]+"a"+centor[1]+"b"+centor[2]+"c"+centor[3]);
		byte[] centor_b = new byte[3];
		System.arraycopy(centor, centor.length - 3, centor_b, 0, 3);
		// System.out.println(centor_b[0]+"a"+centor_b[1]+"b"+centor_b[2]);
		int Netcount = 0;
		int Appcount = 0;
		try {
			Netcount = SqlOperate.NetMonitor_count();
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		try {
			Appcount = SqlOperate.ApplicationData_count();
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		byte[] Net_count = Util.intToByteArray(Netcount);

		byte[] App_count = Util.intToByteArray(Appcount);
		// System.out.println(Net_count.length+"@@@@@"+App_count.length);
		byte[] checksum = new byte[1];
		checksum[0] = 0;
		// byte[] centor = parameter.getId().getBytes();

		// System.arraycopy(centor, 0,
		// Util.formatByteStrToByte(parameter.getId()), 0,
		// parameter.getId().length());
		try {
			int cache_number = (byte) SqlOperate.CommandCache_count();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println(centor);

		// System.out.println("current-l:"+currenttime.length + " centor-l:" +
		// centor.length);
		// System.out.println("mesge_l:"+mesge_length);
		byte[] mesge = new byte[21];
		// System.out.println(currenttime);
		System.arraycopy(heartlength_b, 0, mesge, 0, 1);

		// mesge[currenttime.length] = flag_b.getBytes();
		System.arraycopy(heart_flag, 0, mesge, 1, 1);
		System.arraycopy(currenttime_b, 0, mesge, 2, 6);
		// mesge[currenttime.length + 1 + centor.length] = status;
		System.arraycopy(status_b, 0, mesge, 8, 1);
		System.arraycopy(centor_b, 0, mesge, 9, 3);
		System.arraycopy(App_count, 0, mesge, 12, 4);
		System.arraycopy(Net_count, 0, mesge, 16, 4);

		System.arraycopy(checksum, 0, mesge, 20, 1);
		try {
			String str = new String(mesge);
			// System.out.println("str"+str);
			byte[] send_mes = str.getBytes();
			SendToupperMessage(mesge);
			System.out.println(Util.getCurrentTime() + " Heartbeat Nnm:" + Netcount + ",Anum:" + Appcount);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	// start Upper Udp Server
	public void startUpperUdpServer() {
		try {
			// System.out.println("start Upper Udp Server");// for log
			nioUpperServer = new NIOUDPServer(parameter.getUdpAddr(), parameter.getupperPort());
			// System.out.println(Util.getCurrentTime()+" start Udp Server:" +
			// parameter.getUdpAddr() + parameter.getupperPort());// for
			// log
			// byte[] mesge = { 1, 2, 4, 5 };
			// try {
			// SendToupperMessage(mesge);
			// } catch (IOException e1) {
			// // TODO Auto-generated catch block
			// e1.printStackTrace();
			// }
			nioUpperServer.registerHandler(new UpperUdpMessageHandler());
			nioUpperServer.start();
			System.out.println(Util.getCurrentTime() + " upper server start");// for
																				// log
																				// //115200
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Send Message To upper Message
	public void SendToupperMessage(byte[] message) throws IOException {
		if (nioUpperServer == null) {
			// System.out.println(nioUpperServer);
			throw new IOException();
		}
		// System.out.println("Send To Message:" + parameter.getftphost() +
		// ",port:" + "12400");// for
		// System.out.println(message+
		// "+getSocketAddressByName(parameter.getftphost(), 12400)"); // log
		nioUpperServer.sendto(message, getSocketAddressByName(parameter.getftphost(), 12400));
		// System.out.println("~~~~~~~~~~~~~~~~~~"+parameter.getupperAddr());
		// parameter.getRootPort()
	}

	// Send To Root Syn Message
	public void SendToRootSynMsg(byte[] message) throws IOException {
		System.out.println(message.length);
		if (nioSynConfigServer == null) {
			throw new IOException();
		}
		nioSynConfigServer.sendto(message, getSocketAddressByName(parameter.getRootAddr(), broadcastPort));
		// System.out.println("send to" + parameter.getRootAddr() +
		// broadcastPort);
	}

	// start Netty nio tcp server
	public void startNIOTcpServer() {
		try {
			nettyServer.start(parameter.getTcpAddr(), parameter.getTcpPort());// new
			// NettyMsgHandlerExecutor()

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	// start nio udp server
	public void startNIOUdpServer() {
		try {
			nioUdpServer = new NIOUDPServer(parameter.getUdpAddr(), parameter.getUdpPort());

			nioUdpServer.registerHandler(new NIOUdpMessageHandler());
			nioUdpServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	class NIOCorrectTimeHandler implements NIOUDPServerMsgHandler {

		@Override
		public byte[] messageHandler(String addr, byte[] message) {
			// TODO Auto-generated method stub
			byte[] ack = new byte[message.length - 3];
			System.arraycopy(message, 3, ack, 0, message.length - 3);
			if (ack[0] == 0x43) {
				System.out.println("get ACK " + Count);
				Count = 0;
			}
			return null;
		}
	}

	class NIOrdcContronHandler implements NIOUDPServerMsgHandler {

		@Override
		public byte[] messageHandler(String addr, byte[] message) {
			// TODO Auto-generated method stub
			byte[] rdcMessage = new byte[message.length - 3];
			System.arraycopy(message, 3, rdcMessage, 0, message.length - 3);
			System.out
					.println(Util.getCurrentTime() + " RDC Control Meaaage to Root：" + Util.formatBytesToStr(message));

			try {
				rdcControlFile.append(Util.getCurrentTime() + ":[" + addr + "]" + "RDC Control Meaaage : "
						+ Util.formatBytesToStr(message));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (rdcControlInit) {
				/*************************************************/
				try {
					byte currentFlag = rdcControl.getRdcStartFlag();
					rdcControl.rdcAnalysis(rdcMessage[0], rdcMessage[1]);
					if (currentFlag != rdcControl.getRdcStartFlag()) {
						nioRdcControlServer.sendto(
								Util.getRdcControlMessage(rdcControl.getRdcStartFlag(), rdcControl.getCurrent_budget(),
										rdcControl.getCurrent_guard()),
								getSocketAddressByName(parameter.getRootAddr(), rdcControlPort));
					}
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				/*************************************************/
			}

			if (!rdcControlInit) {
				rdcControlInit = true;
			}
			return null;
		}

	}

	/*
	 * @lhy Receiver: receive from the concentrator to the client's
	 * instructions, string JSON in the form of sending and receiving over,
	 * transform with an array of bytes to send a byte array format to the root
	 * node, the node with char shaped array receiving
	 *
	 */
	class NettyMsgHandlerExecutor implements NettyMsgHandler {

		// @SuppressWarnings({ "unchecked" })
		@Override
		public String messageHandler(String message) {
			System.out.println(Util.getCurrentTime() + " front command:" + message);

			Object retObject;
			try {
				// command = Command.parseCmdFromStream(message);
				retObject = new JSONObject(message);
				// System.out.println(retObject);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return null;
			}
			if (retObject instanceof JSONObject) {// Command to receive
													// configuration parameters
													// for concentrator front
				try {
					String type = ((JSONObject) retObject).get("type").toString();

					byte[] cmd = Util.formatByteStrToByte(((JSONObject) retObject).getString("pama_data"));
					byte[] buffer = null;
					StringBuilder sb = new StringBuilder(Util.getCurrentTime() + " send down command is  ");
					switch (type) {
					case "mcast":
						// System.out.println(222222);
						buffer = Util.packetMcastSend(cmd);
						if (buffer[2] == (byte) 0x00) {
							System.out.println(Util.getCurrentTime() + " command is sending back NetMonitor data");
							sb.append("上报能耗+拓扑");
						} else if (buffer[2] == (byte) 0x01) {
							System.out.println(Util.getCurrentTime() + " command is sending back parameter");
							sb.append("上报网络参数");
						} else if (buffer[2] == (byte) 0X80) {
							System.out.println(Util.getCurrentTime() + " command is multicast meter operate");
							sb.append("multicast读表command");
						} else if (buffer[2] == (byte) 0x82) {
							System.out.println(Util.getCurrentTime() + " command is command loading");
							sb.append("初始的multicast读表command");
						} else {
							System.out.println(Util.getCurrentTime() + " wrong~~");
						}
						// try {
						// cmdFile.append(sb.toString() + buffer);
						// } catch (IOException e2) {
						// // TODO Auto-generated catch block
						// e2.printStackTrace();
						// }
						try {
							TunSendToRootMessage(buffer);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case "unicast":
						bitMap.setPartReUploadList(parseAddrFromStr(((JSONObject) retObject).getString("addrList")));
						buffer = Util.packageUnicastSend(cmd, bitMap.getBitMap());
						if (buffer[3 + bitMap.getBitMap().length] == (byte) 0X80) {
							System.out.println(Util.getCurrentTime() + " command is local multicast read meter");
							// System.out.println("buffer:" +
							// Arrays.toString(buffer));
							// System.out.println(Arrays.toString(bitMap.getBitMap()));
							sb.append("局部multicast读表command");
						} else if (buffer[3 + bitMap.getBitMap().length] == (byte) 0x82) {
							System.out
									.println(Util.getCurrentTime() + "command is  initial local multicast read meter");
							System.out.println(Util.getCurrentTime() + " buffer:" + Arrays.toString(buffer));
							System.out
									.println(Util.getCurrentTime() + " bitmap:" + Arrays.toString(bitMap.getBitMap()));
							sb.append("初始的局部multicast 读表command");
						} else {
							System.out.println(Util.getCurrentTime() + " wrong~~~");
						}

						try {
							TunSendToRootMessage(buffer);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case "mcast_ack":
						buffer = Util.packetMcastSend(cmd);
						if (buffer[2] == (byte) 0x41) {
							System.out.println(Util.getCurrentTime() + " command is sending back NetMonitor data");
							sb.append("周期配置");
						} else if (buffer[2] == (byte) 0x40) {
							System.out.println(Util.getCurrentTime() + " command is sending back parameter");
							sb.append("配置网络参数");
						} else if (buffer[2] == (byte) 0xC1) {
							System.out.println(Util.getCurrentTime() + " command is multicast meter operate");
							sb.append("multicast节点初始化command");
						} else if (buffer[2] == (byte) 0xC0) {
							System.out.println(Util.getCurrentTime() + " command is command loading");
							sb.append("multicast节点重启command");
						} else if (buffer[2] == (byte) 0xC3) {
							System.out.println(Util.getCurrentTime() + " command is node wakeup ");
							sb.append("multicast节点唤醒command");
						}else if (buffer[2] == (byte) 0xC4) {
							System.out.println(Util.getCurrentTime() + " command is check node");
							sb.append("multicast节点查询command");
						}else {
							System.out.println(Util.getCurrentTime() + " wrong~~");
						}

						try {
							TunSendToRootMessage(buffer);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// paramConfigProxy.mcastConfig(buffer, ipList,
						// parameter.getRootPort(), NODE_UNICAST_PORT);
						break;
					case "unicast_ack":
						List<String> unicast_List = parseAddrFromStr(((JSONObject) retObject).getString("addrList"));
						bitMap.setPartReUploadList(parseAddrFromStr(((JSONObject) retObject).getString("addrList")));
						buffer = Util.packageUnicastSend(cmd, bitMap.getBitMap());
						if (buffer[3 + bitMap.getBitMap().length] == (byte) 0xC0) {
							System.out.println(Util.getCurrentTime() + " command is local multicast node initial");
							sb.append("局部multicast节点初始化");
						} else if (buffer[3 + bitMap.getBitMap().length] == (byte) 0xC1) {
							System.out.println(Util.getCurrentTime() + " command is  local multicast node restart");
							sb.append("局部multicast节点重启");
						} else {
							System.out.println(Util.getCurrentTime() + " wrong~~~");
						}

						try {
							TunSendToRootMessage(buffer);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

					case "schedule":// broadcast schedule
						try {
							System.out.println(Util.getCurrentTime() + " command is muticastconfig schedule"
									+ ((JSONObject) retObject).getString(("pama_data")));
							sb.append("下发调度" + ((JSONObject) retObject).getString(("pama_data")));
							JSONObject synJson = new JSONObject(((JSONObject) retObject).getString(("pama_data")));
							// System.out.println("=====111====+++++++++++++++++++++++++");
							// System.out.println(Util.getCurrentTime()+"ttttt"+(synJson.getString("bitmap")));
							// Util.formatByteStrBitmapToBytes(synJson.getString("bitmap"));

							// System.out.println("=========+++++++++++++++++++++++++");

							synParameter.setBitmap(Util.formatByteStrBitmapToBytes(synJson.getString("bitmap")));
							// System.out.println("1111111113");
							synParameter.setBit(synJson.getString("bitmap"));
							// System.out.println("1111111112");
							synParameter.setFlag(true);
							// System.out.println("1111111110");
							Util.writeSynConfigParamToFile(synParameter, "GSynConfig.json");
							TunSendToRootMessage(packScheduleConfigData(
									(Util.formatByteStrBitmapToBytes(synJson.getString("bitmap")))));

							synStateFlag = true;
							synJson = null;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					// for use
					case "pama_corr":
						try {

							currect_rate = Integer.valueOf(((JSONObject) retObject).getString(("pama_data")));
							System.out.println(
									Util.getCurrentTime() + "command is change correct time sequence pama_data:"
											+ ((JSONObject) retObject).getString(("pama_data")));

							CorrectTimer.cancel();
							CorrectTimer = new Timer();
							CorrectTimer.schedule(new TimerTask() {
								@Override
								public void run() {
									// TODO Auto-generated method stub
									try {

										CurrentTime currentTime = Util.getCurrentDateTime(Util.getCurrentDateTime());
										if (Count < MaxCount) {
											nioCorrectTime.sendto(
													Util.getCorrectTimeMessage2(0x13, currentTime.getHour(),
															currentTime.getMinute(), currentTime.getSecond()),
													getSocketAddressByName(parameter.getRootAddr(), CorrectTimePort));
											Count++;
											System.out.println("sendcorrect timecommand " + currentTime.getSecond());
										} else {
											System.out.println("send and node restart command" + Count);
											nioCorrectTime.sendto(
													Util.getCorrectTimeMessage2(0x14, currentTime.getHour(),
															currentTime.getMinute(), currentTime.getSecond()),
													getSocketAddressByName(parameter.getRootAddr(), CorrectTimePort));
											Count = 0;
										}
									} catch (UnknownHostException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}, 0, 1000 * currect_rate);
							// System.out.println("pama_corr2 " + currect_rate);

						} catch (Exception e) {
							// TODO: handle exception
						}
						break;
					// for use
					case "pama_syn":
						try {
							System.out.println(
									Util.getCurrentTime() + " command is change the change the schedule down period"
											+ ((JSONObject) retObject).getString(("pama_data")));

							JSONObject pama_synJson = new JSONObject(((JSONObject) retObject).getString(("pama_data")));

							synParameter.setSeqNum(pama_synJson.getInt("seqNum"));
							synParameter.setLevel(pama_synJson.getInt("level"));
							synParameter.setHour(pama_synJson.getInt("hour"));
							synParameter.setMinute(pama_synJson.getInt("minute"));
							synParameter.setSecond(pama_synJson.getInt("second"));
							synParameter.setPeriod(pama_synJson.getInt("period"));
							System.out.println(Util.getCurrentTime() + " period " + pama_synJson.getInt("period"));

							// System.out.println("syn come on!!!");
							synParameter.setBitmap(Util.formatByteStrBitmapToBytes(pama_synJson.getString("bitmap")));
							synParameter.setBit(pama_synJson.getString("bitmap"));
							synParameter.setFlag(true);
							Util.writeSynConfigParamToFile(synParameter, "GSynConfig.json");
							byte[] bit = Util.formatByteStrBitmapToBytes(pama_synJson.getString("bitmap"));
							CurrentTime currentTime = Util.getCurrentDateTime(Util.getCurrentDateTime());
							System.out.println("!!!!!!!!!!!!!!!!!!");
							SendToRootSynMsg(Util.getSynMessage(pama_synJson.getInt("seqNum"),
									pama_synJson.getInt("level"), currentTime.getHour(), currentTime.getMinute(),
									currentTime.getSecond(), pama_synJson.getInt("period"), bit));
							synStateFlag = true;
							pama_synJson = null;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

					default:
						break;
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return null;
		}
	}

	public String Upper_messageHandler(String message) {
		System.out.println(Util.getCurrentTime() + " front command:" + message);

		Object retObject;
		try {
			// command = Command.parseCmdFromStream(message);
			retObject = new JSONObject(message);
			// System.out.println(retObject);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		if (retObject instanceof JSONObject) {// Command to receive
												// configuration parameters for
												// concentrator front
			try {
				String type = ((JSONObject) retObject).get("type").toString();

				byte[] cmd = Util.formatByteStrToByte(((JSONObject) retObject).getString("pama_data"));
				byte[] buffer = null;
				StringBuilder sb = new StringBuilder(Util.getCurrentTime() + " send down command is  ");
				switch (type) {
				case "mcast":
					// System.out.println(222222);
					buffer = Util.packetMcastSend(cmd);
					if (buffer[2] == (byte) 0x00) {
						System.out.println(Util.getCurrentTime() + " command:sending back NetMonitor data");
						sb.append("上报能耗+拓扑");
					} else if (buffer[2] == (byte) 0x01) {
						System.out.println(Util.getCurrentTime() + " command:sending back parameter");
						sb.append("上报网络参数");
					} else if (buffer[2] == (byte) 0X80) {
						System.out.println(Util.getCurrentTime() + " command:multicast meter operate");
						sb.append("multicast读表command");
					} else if (buffer[2] == (byte) 0x82) {
						System.out.println(Util.getCurrentTime() + " command:loading");
						sb.append("初始的multicast读表command");
					} else {
						System.out.println(Util.getCurrentTime() + " wrong~~");
					}
					// try {
					// cmdFile.append(sb.toString() + buffer);
					// } catch (IOException e2) {
					// // TODO Auto-generated catch block
					// e2.printStackTrace();
					// }
					try {
						TunSendToRootMessage(buffer);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case "unicast":
					bitMap.setPartReUploadList(parseAddrFromStr(((JSONObject) retObject).getString("addrList")));
					buffer = Util.packageUnicastSend(cmd, bitMap.getBitMap());
					if (buffer[3 + bitMap.getBitMap().length] == (byte) 0X80) {
						System.out.println(Util.getCurrentTime() + " command:local multicast read meter");
						// System.out.println("buffer:" +
						// Arrays.toString(buffer));
						// System.out.println(Arrays.toString(bitMap.getBitMap()));
						sb.append("局部multicast读表command");
					} else if (buffer[3 + bitMap.getBitMap().length] == (byte) 0x82) {
						System.out.println(Util.getCurrentTime() + "command:initial local multicast read meter");
						System.out.println(Util.getCurrentTime() + " buffer:" + Arrays.toString(buffer));
						System.out.println(Util.getCurrentTime() + " bitmap:" + Arrays.toString(bitMap.getBitMap()));
						sb.append("初始的局部multicast 读表command");
					} else {
						System.out.println(Util.getCurrentTime() + " wrong~~~");
					}

					try {
						TunSendToRootMessage(buffer);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case "mcast_ack":
					buffer = Util.packetMcastSend(cmd);
					if (buffer[2] == (byte) 0x41) {
						System.out.println(Util.getCurrentTime() + " command:sending back NetMonitor data");
						sb.append("周期配置");
					} else if (buffer[2] == (byte) 0x40) {
						System.out.println(Util.getCurrentTime() + " command is sending back parameter");
						sb.append("配置网络参数");
					} else if (buffer[2] == (byte) 0xC1) {
						System.out.println(Util.getCurrentTime() + " command is multicast meter operate");
						sb.append("multicast节点初始化command");
					} else if (buffer[2] == (byte) 0xC0) {
						System.out.println(Util.getCurrentTime() + " command is command loading");
						sb.append("multicast节点重启command");
					}else if (buffer[2] == (byte) 0xC3) {
						System.out.println(Util.getCurrentTime() + " command is node wakeup ");
						sb.append("multicast节点唤醒command");
					}else if (buffer[2] == (byte) 0xC4) {
						System.out.println(Util.getCurrentTime() + " command is check node");
						sb.append("multicast节点查询command");
					} else {
						System.out.println(Util.getCurrentTime() + " wrong~~");
					}

					try {
						TunSendToRootMessage(buffer);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// paramConfigProxy.mcastConfig(buffer, ipList,
					// parameter.getRootPort(), NODE_UNICAST_PORT);
					break;
				case "unicast_ack":
					List<String> unicast_List = parseAddrFromStr(((JSONObject) retObject).getString("addrList"));
					bitMap.setPartReUploadList(parseAddrFromStr(((JSONObject) retObject).getString("addrList")));
					buffer = Util.packageUnicastSend(cmd, bitMap.getBitMap());
					if (buffer[3 + bitMap.getBitMap().length] == (byte) 0xC0) {
						System.out.println(Util.getCurrentTime() + " command is local multicast node initial");
						sb.append("局部multicast节点初始化");
					} else if (buffer[3 + bitMap.getBitMap().length] == (byte) 0xC1) {
						System.out.println(Util.getCurrentTime() + " command is  local multicast node restart");
						sb.append("局部multicast节点重启");
					} else {
						System.out.println(Util.getCurrentTime() + " wrong~~~");
					}

					try {
						TunSendToRootMessage(buffer);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;

				case "schedule":// broadcast schedule
					try {
						System.out.println(Util.getCurrentTime() + " command is muticastconfig schedule"
								+ ((JSONObject) retObject).getString(("pama_data")));
						sb.append("下发调度" + ((JSONObject) retObject).getString(("pama_data")));
						JSONObject synJson = new JSONObject(((JSONObject) retObject).getString(("pama_data")));
						// System.out.println("=====111====+++++++++++++++++++++++++");
						// System.out.println(Util.getCurrentTime()+"ttttt"+(synJson.getString("bitmap")));
						// Util.formatByteStrBitmapToBytes(synJson.getString("bitmap"));

						// System.out.println("=========+++++++++++++++++++++++++");

						synParameter.setBitmap(Util.formatByteStrBitmapToBytes(synJson.getString("bitmap")));
						// System.out.println("1111111113");
						synParameter.setBit(synJson.getString("bitmap"));
						// System.out.println("1111111112");
						synParameter.setFlag(true);
						// System.out.println("1111111110");
						Util.writeSynConfigParamToFile(synParameter, "GSynConfig.json");
						TunSendToRootMessage(
								packScheduleConfigData((Util.formatByteStrBitmapToBytes(synJson.getString("bitmap")))));

						synStateFlag = true;
						synJson = null;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case "debug":// broadcast schedule
					try {
						System.out.println(Util.getCurrentTime() + " command is debug schedule"
								+ ((JSONObject) retObject).getString(("pama_data")));
						sb.append("下发调度" + ((JSONObject) retObject).getString(("pama_data")));
						JSONObject synJson = new JSONObject(((JSONObject) retObject).getString(("pama_data")));
						// System.out.println("=====111====+++++++++++++++++++++++++");
						// System.out.println(Util.getCurrentTime()+"ttttt"+(synJson.getString("bitmap")));
						// Util.formatByteStrBitmapToBytes(synJson.getString("bitmap"));

						// System.out.println("=========+++++++++++++++++++++++++");

						synParameter.setBitmap(Util.formatByteStrBitmapToBytes(synJson.getString("bitmap")));
						// System.out.println("1111111113");
						synParameter.setBit(synJson.getString("bitmap"));
						// System.out.println("1111111112");
						synParameter.setFlag(true);
						// System.out.println("1111111110");
						Util.writeSynConfigParamToFile(synParameter, "GSynConfig.json");
						TunSendToRootMessage(
								packScheduleConfigData((Util.formatByteStrBitmapToBytes(synJson.getString("bitmap")))));

						synStateFlag = true;
						synJson = null;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case "end_debug":// broadcast schedule
					try {
						System.out.println(Util.getCurrentTime() + " command is end debug schedule"
								+ ((JSONObject) retObject).getString(("pama_data")));
						sb.append("下发调度" + ((JSONObject) retObject).getString(("pama_data")));
						JSONObject synJson = new JSONObject(((JSONObject) retObject).getString(("pama_data")));
						// System.out.println("=====111====+++++++++++++++++++++++++");
						// System.out.println(Util.getCurrentTime()+"ttttt"+(synJson.getString("bitmap")));
						// Util.formatByteStrBitmapToBytes(synJson.getString("bitmap"));

						// System.out.println("=========+++++++++++++++++++++++++");

						synParameter.setBitmap(Util.formatByteStrBitmapToBytes(synJson.getString("bitmap")));
						// System.out.println("1111111113");
						synParameter.setBit(synJson.getString("bitmap"));
						// System.out.println("1111111112");
						synParameter.setFlag(true);
						// System.out.println("1111111110");
						Util.writeSynConfigParamToFile(synParameter, "GSynConfig.json");
						TunSendToRootMessage(
								packScheduleConfigData((Util.formatByteStrBitmapToBytes(synJson.getString("bitmap")))));

						synStateFlag = true;
						synJson = null;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				// for use
				case "pama_corr":
					try {

						currect_rate = Integer.valueOf(((JSONObject) retObject).getString(("pama_data")));
						System.out.println(Util.getCurrentTime() + "command is change correct time sequence pama_data:"
								+ ((JSONObject) retObject).getString(("pama_data")));

						CorrectTimer.cancel();
						CorrectTimer = new Timer();
						CorrectTimer.schedule(new TimerTask() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								try {

									CurrentTime currentTime = Util.getCurrentDateTime(Util.getCurrentDateTime());
									if (Count < MaxCount) {
										nioCorrectTime.sendto(
												Util.getCorrectTimeMessage2(0x13, currentTime.getHour(),
														currentTime.getMinute(), currentTime.getSecond()),
												getSocketAddressByName(parameter.getRootAddr(), CorrectTimePort));
										Count++;
										System.out.println("sendcorrect timecommand " + currentTime.getSecond());
									} else {
										System.out.println("send and node restart command" + Count);
										nioCorrectTime.sendto(
												Util.getCorrectTimeMessage2(0x14, currentTime.getHour(),
														currentTime.getMinute(), currentTime.getSecond()),
												getSocketAddressByName(parameter.getRootAddr(), CorrectTimePort));
										Count = 0;
									}
								} catch (UnknownHostException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}, 0, 1000 * currect_rate);
						// System.out.println("pama_corr2 " + currect_rate);

					} catch (Exception e) {
						// TODO: handle exception
					}
					break;
				// for use
				case "pama_syn":
					try {
						System.out.println(
								Util.getCurrentTime() + " command is change the change the schedule down period"
										+ ((JSONObject) retObject).getString(("pama_data")));

						JSONObject pama_synJson = new JSONObject(((JSONObject) retObject).getString(("pama_data")));

						synParameter.setSeqNum(pama_synJson.getInt("seqNum"));
						synParameter.setLevel(pama_synJson.getInt("level"));
						synParameter.setHour(pama_synJson.getInt("hour"));
						synParameter.setMinute(pama_synJson.getInt("minute"));
						synParameter.setSecond(pama_synJson.getInt("second"));
						synParameter.setPeriod(pama_synJson.getInt("period"));
						System.out.println(Util.getCurrentTime() + " period " + pama_synJson.getInt("period"));

						// System.out.println("syn come on!!!");
						synParameter.setBitmap(Util.formatByteStrBitmapToBytes(pama_synJson.getString("bitmap")));
						synParameter.setBit(pama_synJson.getString("bitmap"));
						synParameter.setFlag(true);
						Util.writeSynConfigParamToFile(synParameter, "GSynConfig.json");
						byte[] bit = Util.formatByteStrBitmapToBytes(pama_synJson.getString("bitmap"));
						CurrentTime currentTime = Util.getCurrentDateTime(Util.getCurrentDateTime());
						System.out.println("!!!!!!!!!!!!!!!!!!");
						SendToRootSynMsg(Util.getSynMessage(pama_synJson.getInt("seqNum"), pama_synJson.getInt("level"),
								currentTime.getHour(), currentTime.getMinute(), currentTime.getSecond(),
								pama_synJson.getInt("period"), bit));
						synStateFlag = true;
						pama_synJson = null;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;

				default:
					break;
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
	}

	public List<String> parseAddrFromStr(String str) {
		List<String> list = new ArrayList<String>();
		// System.out.println(str.substring(1, str.length()-1));
		String[] addr = str.substring(1, str.length() - 1).split(",");
		for (int i = 0; i < addr.length; i++) {
			list.add(addr[i].substring(1, addr[i].length() - 1));
			// System.out.println(addr[i].substring(1, addr[i].length()-1));
		}
		// System.out.println(list.toString());
		return list;
	}

	private byte[] packScheduleConfigData(byte[] content) { // config
															// schedule，type7
		if (content == null || content.length == 0) {
			return null;
		}
		byte[] cmd = new byte[content.length + 3];
		cmd[0] = (byte) (content.length + 2);
		cmd[1] = (byte) (1); // 1 for multicast
		cmd[2] = GlobalDefines.GlobalCmd.G_SCHEDULE_CONFIG;
		System.arraycopy(content, 0, cmd, 3, content.length);
		return cmd;
	}

	// these two are used to comunicate with upper
	private byte[] packUnicastData(byte[] content) {// Unicast type 3
		if (content == null || content.length == 0) {
			return null;
		}
		byte[] cmd = new byte[content.length + 3];
		cmd[0] = (byte) (content.length + 2);
		cmd[1] = (byte) (1); // 1 for multicast
		cmd[2] = GlobalDefines.GlobalCmd.G_DEF_READ_DATA;
		System.arraycopy(content, 0, cmd, 3, content.length);
		return cmd;
	}

	private byte[] packageReadData(byte[] cmd) {// multicast only command type1
		if (cmd == null || cmd.length == 0) {
			return null;
		}
		int length = cmd.length;
		byte[] content = new byte[length + 3];
		content[0] = (byte) (length + 2);
		content[1] = (byte) (1); // 1 for multicast
		content[2] = GlobalDefines.GlobalCmd.G_DEF_READ_DATA;
		System.arraycopy(cmd, 0, content, 3, length);
		return content;
	}

	// for communicate with upper
	private byte[] packageReadDataAck(byte[] cmd, byte[] bitmap) {// multicast
																	// resend
																	// type 2
		// command+bitmap
		// type 2
		if (cmd == null || cmd.length == 0) {
			return null;
		}
		if (bitmap == null || bitmap.length == 0) {
			return null;
		}
		byte[] content = new byte[cmd.length + bitmap.length + 4];
		content[0] = (byte) (cmd.length + bitmap.length + 3);
		content[1] = (byte) (2); // 2 for local unicast
		System.arraycopy(bitmap, 0, content, 2, bitmap.length);
		content[2 + bitmap.length] = GlobalDefines.GlobalCmd.G_DEF_READ_DATA;
		content[3 + bitmap.length] = (byte) cmd.length;// command length
		System.arraycopy(cmd, 0, content, bitmap.length + 4, cmd.length);
		return content;
	}

	public String formatUcastDataToJsonStr(String addr, String content) {
		JSONObject object = new JSONObject();
		try {
			object.put("addr", addr);
			object.put("data", content);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "[" + object.toString() + "]";
	}

	/************* data generate ****************/
	public String formatUcastDataToJsonStr(String type, String addr, String content) {
		JSONObject object = new JSONObject();
		try {
			object.put("type", type);
			object.put("addr", addr);
			object.put("data", content);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "[" + object.toString() + "]";
	}

	public String formatDataToJsonStr(String type, String addr, String content) {
		JSONObject object = new JSONObject();
		try {
			object.put("type", type);
			object.put("addr", addr);
			object.put("data", content);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return object.toString();
	}

	class UnicastMethodInvoke implements ProxyInvoke {

		@Override
		public void invoke(String addr, int port, byte[] message) {

			// unicast
			try {
				// System.out.println(addr);
				UnicastSendMessage(addr, port, message);// ip address
				// poat，information
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// unicastsend
	public void UnicastSendMessage(String addr, int port, byte[] message) throws IOException {
		if (nioUdpServer == null) {
			throw new IOException();
		}
		nioUdpServer.sendto(message, getSocketAddressByName(addr, port));
	}

	// Dealing with network parameters and designing topological structure
	class NIOUdpNetDataHandler implements NIOUDPServerMsgHandler {

		@Override
		public byte[] messageHandler(String addr, byte[] message) {
			// oprl move 3 bit

			StringBuilder sb = new StringBuilder(addr);
			for (byte b : message) {
				sb.append(b);
			}
			int hash = sb.toString().hashCode();
			// topo generates hashcode
			// System.out.println(hash);
			if (!topoMap.containsKey(hash)) {
				if (!flag)
					flag = true;
				topoMap.put(hash, addr);
				System.out.print("-----------------------------");
				byte[] orpl = new byte[message.length - 3];
				System.arraycopy(message, 3, orpl, 0, message.length - 3);
				System.out.println(addr + "energy：" + Util.formatBytesToStr(message));

				Energy en = Util.Create_Energy(addr, orpl);
				SqlOperate.append(en);

				if (netClient.remoteHostIsOnline()) {
					try {
						// use jsonsend topo information
						netClient.asyncWriteAndFlush(formatDataToJsonStr("topo", addr, Util.formatByteToByteStr(orpl))); // json
																															// way
																															// to
																															// use
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			if (flag) {
				topoTimer.schedule(new TimerTask() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						flag = false;
						topoMap.clear();
					}

				}, 180 * 1000);
			}
			return null;
		}
	}

	// handle syn information udp server
	class NIOSynMessageHandler implements NIOUDPServerMsgHandler {
		@Override
		public byte[] messageHandler(String addr, byte[] message) {

			// System.out.println(String.valueOf(message));
			// oprl move 3 bit
			byte[] orpl = new byte[message.length - 3];
			System.arraycopy(message, 3, orpl, 0, message.length - 3);

			int recvLevel = Integer.parseInt(String.valueOf(String.format("%02X", orpl[1])), 16);// String.valueOf(String.format("%02X",
			int hour = Integer.parseInt(String.valueOf(String.format("%02X", orpl[2])), 16);
			int minute = Integer.parseInt(String.valueOf(String.format("%02X", orpl[3])), 16);
			int second = Integer.parseInt(String.valueOf(String.format("%02X", orpl[4])), 16);

			System.out.println(Util.getCurrentTime() + " syn status:" + synStateFlag + " recvLevel:" + recvLevel + " "
					+ hour + ":" + minute + ":" + second);
			// System.out.println();
			if (!synStateFlag) {

			} else {
				switch (recvLevel) {
				case GlobalDefines.GlobalSynLevelConfig.G_SYN_CONFIG_LEVEL:// request
					// send to root
					try {
						seqCount = 0;

						CurrentTime currentTime = Util.getCurrentDateTime(Util.getCurrentDateTime());
						SendToRootSynMsg(Util.getSynMessage(seqCount, 0, currentTime.getHour(), currentTime.getMinute(),
								currentTime.getSecond(), synParameter.getPeriod(), synParameter.getBitmap()));

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case GlobalDefines.GlobalSynLevelConfig.G_SYN_CONFIG_INIT_LEVEL:// request
					try {
						CurrentTime currentTime = Util.getCurrentDateTime(Util.getCurrentDateTime());
						seqCount = Integer.parseInt(String.valueOf(String.format("%02X", orpl[0])), 16) + 1;
						SendToRootSynMsg(Util.getSynMessage(seqCount, 0, currentTime.getHour(), currentTime.getMinute(),
								currentTime.getSecond(), synParameter.getPeriod(), synParameter.getBitmap()));
						// seqCount ++;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// }
					break;
				default:
					try {
						CurrentTime currentTime = Util.getCurrentDateTime(Util.getCurrentDateTime());
						seqCount = Integer.parseInt(String.valueOf(String.format("%02X", orpl[0])), 16) + 1;
						SendToRootSynMsg(Util.getSynMessage(seqCount, 0, currentTime.getHour(), currentTime.getMinute(),
								currentTime.getSecond(), synParameter.getPeriod(), synParameter.getBitmap()));
						// seqCount ++;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
			return null;
		}

	}

	public static void Frag_recbBegin(String addr, byte[] message) {
		if (Frag_Recb.fragHashMap.containsKey(addr)) {
			Frag_Recb.fragHashMap.get(addr).frag_phase(message);
		} else {
			Frag_Recb frag_Recb = new Frag_Recb(addr, message);
			frag_Recb.frag_phase(message);
			Frag_Recb.fragHashMap.put(addr, frag_Recb);
		}
	}

	// =====================================upper server msg handler
	// The data received by the UDP server is added to the database and the data
	// is derived from all nodes in the wireless network,
	// and the data is transmitted directly to the TCP control center
	class NIOUdpMessageHandler implements NIOUDPServerMsgHandler {

		@Override
		public byte[] messageHandler(String addr, byte[] message) {
			try {
				FragFile.append(Util.getCurrentTime() + ":[" + addr + "]" + "分片:" + Util.formatBytesToStr(message));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String rootAddr = parameter.getRootAddr();
			if (!addr.equals(rootAddr)) {
				Frag_recbBegin(addr, message);
			}
			return null;
		}

	}

	class UpperUdpMessageHandler implements NIOUDPServerMsgHandler {
		@Override
		public byte[] messageHandler(String addr, byte[] message) {
			// String s = new String(message);
			// System.out.println("Upper Udp Message Handler message:" + s);//
			// for
			// System.out.println(addr+" "+ " "+ message.length + " "
			// +message[0]);
			System.out.println("!!!!!!!!!!!!!!!!!!!");
			System.out.println("0:"+message[0]+" 1:"+message[1]+" 2:"+message[2]
					+" 3:"+message[3]+" 4:"+message[4] );
			byte[] command = new byte[message.length];
			System.arraycopy(message, 0, command, 0, message.length);
			int command_length = command[0];
			System.out.println("command_length = "+command_length);
			int command_flag = command[1];
			System.out.println("command-Flag = "+command_flag);
			int send_to_net = command[2] >> 7;
			System.out.println("send to net = "+send_to_net);
			int return_type = (command[2] >> 6) & 0x01;
			System.out.println("return_type = "+return_type);
			int has_return = ((command[2] >> 4) & 0x03) >> 1;
			System.out.println("has_return = "+has_return);
			int broadcast = ((command[2] >> 4) & 0x03) & 0x01;
			System.out.println("broadcast = "+broadcast);
			int unicast_number = command[2] & 0x0F;
			System.out.println("unicast = "+unicast_number);
			int com_length = command[3];
			System.out.println("com_length = "+com_length);
			
			System.out.println("comlength = "+command.length);
			//int comType = command[4];
			
			byte[] comTypeTemp = new byte[1];
			comTypeTemp[0]=command[4];
			String comType = Util.formatByteToByteStr(comTypeTemp);
			System.out.println("comType = "+Util.formatByteToByteStr(comTypeTemp));
			//int command_length = command[2];
			byte[] com = new byte[com_length];
			
			System.arraycopy(command, 5, com, 0, com_length);
			// System.out.println("com = "+com.toString());
			int check_sum = command[5 + com_length];
			for (int i = 0; i < command_length; i++) {
				// System.out.println("!!!" + com[i]);
			}
	// 下发指令合成
			String commands = "";
			String com_content = new String(com);
			System.out.println(com_content);
			if (send_to_net == 1 || send_to_net == -1) {
				commands = commandAssemble(broadcast, com_content, comType);
			}
			System.out.println("assemble:"+commands);
			getbit();
			// byte[] command = Util.formatByteStrToByte(s);
			// String c3 = "108ffffffff";
			// String c4 = "";
			// byte[] com3 = new byte[c3.length()];
			// for (int i = 0; i < 11; i++) {
			// com3[i] = (byte) Integer.parseInt(s.substring(i, i + 1), 16);
			// }
			// for (int i = 0; i < 11; i++) {
			// //System.out.println(" com:" + i + ":" + com3[i]);
			// }
			// command = com3;
			// System.out.println("command[0] = " + command[0]); // log
			// byte[] command = Util.formatByteStrToByte(s);
			// System.out.println("Start command handler");// for log
			CommandHandler(message);
			// CommandHandler(command);
			System.out.println(Util.getCurrentTime() + " command handle over");// for
																				// log
			// System.out.println(s);
			return null;
		}
	}

	// bitmap get bit
	public static byte[] getbit() {
		String currenttime = Util.getCurrentTime();
		String[] times = currenttime.split(":");
		// int hour = Integer.parseInt(times[0]);
		// int minute = Integer.parseInt(times[1]);
		byte[] bit = new byte[144];
		// byte[] bitmap = new byte[] { -1, -128, 35, 84, 72, -128, 61, -2, 16,
		// 2, 4, 68, 90, 48, 0, 0, 8, 10 };
		byte[] bitmap = synParameter.getBitmap();
		//byte[] bitmap = new byte[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

		int i, j, t = 0;
		byte bitmap_a = 0;
		// System.out.print(" ");
//		for (i = 0; i < 18; i++) {
//			//System.out.print(" " + i);
//			System.out.println(i + " " + bitmap[i]);
//			//t++;
//		}
		
		System.out.println("");
		for (i = 0; i < 18; i++) {
			//System.out.print(i);
			bitmap_a = bitmap[i];
			//System.out.print(bitmap_a+"&& ");
			for (j = 0; j < 8; j++) {
				bit[t] = (byte) (bitmap_a & 1);
				bitmap_a = (byte) (bitmap_a >> 1);
				//System.out.print(" " + bit[t]);
				//System.out.println(t + " " + bit[t]);
				t++;
			}
			//System.out.println();
		}
		System.out.print(" ");
		for (i = 0; i < 6; i++) {
			System.out.print(" " + i);
		}
		System.out.println(" ");
		for (i = 0; i < 24; i++) {
			System.out.print(i);
			for (j = 0; j < 6; j++) {
				System.out.print(" " + bit[6 * i + j]);
			}
			System.out.println("");
		}
//		while (bit[count + i] != 1) {
//			i += 1;
//		}
		return bit;
	}

	public static int time_diffence(int active, byte[] bit) {
		int difference = 0;
		String currenttime = Util.getCurrentTime();
		String[] times = currenttime.substring(11).split(":");
		// System.out.println(currenttime.substring(11));
		// System.out.println(times[0]+" "+times[1]+" "+times[2]+"end");
		int hour = Integer.parseInt(times[0]);
		int minute = Integer.parseInt(times[1]);
		int second = Integer.parseInt(times[2]);
		int minutes = minute % 10;
		int count = hour * 6 + minute / 10;
		int i = 1;
		// System.out.println("hour:" + hour + " minute:" + minute + " second:"
		// + second+" minutes:"+minutes);
		// System.out.print(" ");
		// for(i = 0;i<6;i++){
		// System.out.print(" "+i);
		// }
		// for(i = 0;i<24;i++){
		// System.out.print(i);
		// for (int j = 0;j<6;j++){
		// System.out.print(" "+bit[6*i+j]);
		// }
		// System.out.println("");
		// }
		// System.out.println("|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
		// while (bit[count + i] != 1) {
		// i += 1;
		// }
		if (active == 1) {
			i = 1;
			if (bit[count] == 1) {
				if ((minutes * 60 + second) < 330) {
					difference = 330 - (minutes * 60 + second);
				}
			} else {
				while (bit[count + i] != 1) {
					i += 1;
				}
				// System.out.println("active difference :" + difference + " i:"
				// + i);
				difference = (600 - (minutes * 60 + second)) + (i - 1) * 600 + 330;
			}
		} else {
			i = 1;
			while (bit[count + i] != 0) {
				i += 1;
			}
			difference = (600 - (minutes * 60 + second)) + (i - 1) * 600;
			// System.out.println("inactive difference:" + difference + " i:" +
			// i);
		}
		// System.out.println("count + i:" + count + i + " /6:" + (count + i) /
		// 6 + " %6:" + (count + i) % 6);

		return difference;
	}

	public void send_return(int has_return, String cacheCommand, byte[] com) throws IOException {
		int count = 0;
		int wait = 0;
		byte[] bit = new byte[144];
		String filename = null;
		String currenttime = Util.getCurrentTime();
		// String[] times = currenttime.split(":");
		// int hour = Integer.parseInt(times[0]);
		// int minute = Integer.parseInt(times[1]);
		// int second = Integer.parseInt(times[2]);
		// int minutes = minute % 10;
		// int minute_count = hour * 6 + minute / 10;
		if (has_return == 1) {
			try {
				count = SqlOperate.ApplicationData_count();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			filename = Util.getCurrentTime() + "-App-return";
			final String filename1 = filename;
			try {
				SqlOperate.NetMonitor_count_out(count, filename);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// SqlOperate.commandCache_a(cacheCommand);
			// System.out.println("TunSendToRootMessage(com);");
			System.out.println("upper command send to root");
			Upper_messageHandler(cacheCommand);
			// TunSendToRootMessage(com);
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				public void run() {
					System.out.println("upper wait for application data");
					WriteFTPFile write = new WriteFTPFile();
					write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(),
							parameter.getftpPort(), filename1);
				}
				// }, 5 * 1000);
			}, 30 * 1000);

		} else if (has_return == 2) {

			Upper_messageHandler(cacheCommand);
			try {
				count = SqlOperate.ApplicationData_count();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// SqlOperate.commandCache_a(cacheCommand);
			filename = Util.getCurrentTime() + "Net-return";
			final String filename1 = filename;
			SqlOperate.NetMonitor_count_out(count, filename);
			// System.out.println("TunSendToRootMessage(com);");
			System.out.println("upper command send to root");
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				public void run() {
					System.out.println("upper wait for Net Monitor data");

					WriteFTPFile write = new WriteFTPFile();
					write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(),
							parameter.getftpPort(), filename1);

				}
				// }, 5 * 1000);
			}, 30 * 1000);
			// wait 30s

		} else if (has_return == 3) {
			filename = "config.json";
			System.out.println("upper command send to root");
			// Upper_messageHandler(cacheCommand);
			// TunSendToRootMessage(com);
			System.out.println("upper wait for configration");
			WriteFTPFile write = new WriteFTPFile();
			write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(), parameter.getftpPort(),
					filename);
		} else {
			System.out.println("upper command send to root");
			Upper_messageHandler(cacheCommand);
		}
	}

	public void cache_wait(String comType, int cache, int has_return, String cacheCommand, byte[] com) throws IOException {
		// System.out.println("cache:" + cache + " has return:" + has_return + "
		// cacheCommand:" + cacheCommand);
		// System.out.println(" cacheCommand:" + cacheCommand);
		int count = 0;
		int wait = 0;
		byte[] bit = new byte[144];
		final int return1 = has_return;
		final String cache1 = cacheCommand;
		final byte[] com1 = com;
		// String filename = null;
		String currenttime = Util.getCurrentTime();
		String[] times = currenttime.substring(11).split(":");
		int hour = Integer.parseInt(times[0]);
		int minute = Integer.parseInt(times[1]);
		int second = Integer.parseInt(times[2]);
		int minutes = minute % 10;
		int minute_count = hour * 6 + minute / 10;
		if (has_return == 1) {
			try {
				count = SqlOperate.NetMonitor_count();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// SqlOperate.commandCache_a(cacheCommand);
			if (cache == 1) {
				Timer timer = new Timer();
				wait = time_diffence(1, getbit());
				System.out.println("wait for :" + wait + "s");
				timer.schedule(new TimerTask() {
					public void run() {
						try {
							send_return(return1, cache1, com1);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Net_Status_flag = 4;
						System.out.println("Net_Status_flag change:" + Net_Status_flag);
					}
					// }, 1 * 1000);
				}, wait * 1000);
				try {
					count = SqlOperate.ApplicationData_count();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		} else if (has_return == 2) {
			try {
				count = SqlOperate.ApplicationData_count();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// SqlOperate.commandCache_a(cacheCommand);
			// System.out.println("等待配置s");
			if (cache == 1) {
				wait = time_diffence(1, getbit());
				System.out.println(Util.getCurrentTime() + "wait for :" + wait + "s");
				Timer timer = new Timer();
				timer.schedule(new TimerTask() {
					public void run() {
						// System.out.println("等待配置2");
						try {
							send_return(return1, cache1, com1);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						Net_Status_flag = 4;
						System.out.println("Net_Status_flag change:" + Net_Status_flag);
					}
					// }, 2 * 1000);
				}, wait * 1000);
			}
		} else if (has_return == 3) {
			try {
				count = SqlOperate.ApplicationData_count();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// SqlOperate.commandCache_a(cacheCommand);
			if (cache == 1) {
				wait = time_diffence(1, getbit());
				System.out.println(Util.getCurrentTime() + " wait for :" + wait + "s");
				Timer timer = new Timer();
				timer.schedule(new TimerTask() {
					public void run() {
						// System.out.println("等待配置3");
						try {
							send_return(return1, cache1, com1);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Net_Status_flag = 4;
						System.out.println("Net_Status_flag change:" + Net_Status_flag);
					}
					// }, 3 * 1000);
				}, wait * 1000);
			}
		} else {
			if (cache == 1) {
				if (comType.equals("02")) {// debug command
					bit = getbit();
					if (getbit()[minute_count + 1] == 1) {
						wait = (600 - (minutes * 60 + second)) + 330;
					} else {
						wait = (600 - (minutes * 60 + second));
					}
					Timer timer = new Timer();
					System.out.println(Util.getCurrentTime() + " wait for :" + wait + "s");
					timer.schedule(new TimerTask() {
						public void run() {
							// System.out.println("等待配置4");
							try {
								send_return(return1, cache1, com1);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							Timer timer = new Timer();
							timer.schedule(new TimerTask() {
								public void run() {
									// System.out.println("wait for going into
									// debugging");
									String message = "The net start debugging";
									// System.out.println("SendToupperMessage(message.getBytes())");
									try {
										SendToupperMessage(message.getBytes());
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									Net_Status_flag = 6;
									System.out.println("Net_Status_flag change:" + Net_Status_flag);
								}
								// }, 3 * 1000);
							}, 30 * 1000);
						}
						// }, 4 * 1000);
					}, wait * 1000);
				} else {
					wait = time_diffence(1, getbit());
					System.out.println(Util.getCurrentTime() + " wait for :" + wait + "s");
					Timer timer = new Timer();
					timer.schedule(new TimerTask() {
						public void run() {
							// System.out.println("等待配置3");
							try {
								send_return(return1, cache1, com1);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							Net_Status_flag = 4;
							System.out.println("Net_Status_flag change:" + Net_Status_flag);
						}
						// }, 3 * 1000);
					}, wait * 1000);
				}
			}

		}
	}

	public static String commandAssemble(int broadcast, String com_content, String comType) {
		String commands = "";
		System.out.println(comType);
		if (broadcast == 1) {
			System.out.println(comType);
			//System.out.println(0x80);
			if (comType.equals("00") || comType.equals("01") || 
					comType.equals("80") || comType.equals("82")) {
				commands = "{\"type\": \"mcast\", \"pama_data\": \"" 
					+ comType + com_content + "\"}";
				// {"type": "mcast", "pama_data": "8005105BFE5916"}
				System.out.println(Util.getCurrentTime() + " " + commands);
			} else if (comType.equals("C0") || comType.equals("C1") ||
					comType.equals("C3") || comType.equals("C4")) {
				commands = "{\"addrList\": [], \"type\": \"mcast_ack\", \"pama_data\": \""
						+ comType + com_content + "\"}";
			} else if (comType.equals("40") || comType.equals("41")) {
				commands = "{\"type\": \"mcast_ack\", \"pama_data\": \"" + comType + com_content
						+ "\"}";
			} else if (comType.equals("C2")) {
				// String com_content = new String(com);
				String[] sourceStr = com_content.split(":");
				int addnum = sourceStr.length;
				commands = "{\"type\": \"pama_syn\", \"pama_data\": {\"hour\": \"" + sourceStr[0] + "\", \"level\": "
						+ sourceStr[1] + ", \"seqNum\": " + sourceStr[2] + ", \"period\": \"" + sourceStr[3]
						+ ", \"bitmap\": [" + sourceStr[4] + "], \"second\": \"" + sourceStr[5] + "\", \"state\": "
						+ sourceStr[6] + ", \"minute\": \"" + sourceStr[7] + "\"}}";
			} else if (comType.equals("02")) {
				String[] sourceStr = com_content.split(":");
				int addnum = sourceStr.length;
				String DebugBitmap = "-1, -1, -1, -1, -1," + " -1, -1, -1, -1, -1," + " -1, -1, -1, -1, -1, -1, -1, -1";
				commands = "{\"type\": \"debug\", \"pama_data\": {\"hour\": \"" + sourceStr[0] + "\", \"level\": "
						+ sourceStr[3] + ", \"seqNum\": " + sourceStr[4] + ", \"bitmap\": [" + DebugBitmap
						+ "], \"second\": \"" + sourceStr[2] + "\", \"minute\": \"" + sourceStr[1] + "\"}}";
			} else if (comType.equals("42")) {
				System.out.println(com_content);
				String[] sourceStr = com_content.split(":");
				int addnum = sourceStr.length;
				commands = "{\"type\": \"schedule\", \"pama_data\": {\"hour\": \"" + sourceStr[0] + "\", \"level\": "
						+ sourceStr[4] + ", \"seqNum\": " + sourceStr[5] + ", \"bitmap\": [" + sourceStr[3]
						+ "], \"second\": \"" + sourceStr[2] + "\", \"minute\": \"" + sourceStr[1] + "\"}}";
			} else if (comType.equals("81")) {
				String[] sourceStr = com_content.split(":");
				int addnum = sourceStr.length;
				commands = "{\"type\": \"end_debug\", \"pama_data\": {\"hour\": \"" + sourceStr[0] + "\", \"level\": "
						+ sourceStr[4] + ", \"seqNum\": " + sourceStr[5] + ", \"bitmap\": [" + sourceStr[3]
						+ "], \"second\": \"" + sourceStr[2] + "\", \"minute\": \"" + sourceStr[1] + "\"}}";
			} else {
				System.out.println("error " + com_content);
			}
		} else {

			String[] sourceStr = com_content.split(",");
			int addnum = sourceStr.length;
			// String adds = "\"addrList\": [";
			String adds = "[";
			for (int i = 0; i < addnum - 3; i++) {
				adds += "\"" + sourceStr[i] + "\", ";
			}
			adds = "[" + adds + "\"" + sourceStr[addnum - 2] + "\"]";
			if (comType.equals("00") || comType.equals("01") 
					||comType.equals("81") || comType.equals("82")) {

				commands = "{\"addrList\": " + adds + ", \"type\": \"mcast\", \"pama_data\": \""
						+ comType + sourceStr[addnum - 1] + "\"}";
				// {"type": "mcast", "pama_data": "8005105BFE5916"}
				System.out.println(Util.getCurrentTime() + " " + commands);
			} else if (comType.equals("C0") || comType.equals("C1")) {
				commands = "{\"addrList\": " + adds + ", \"type\": \"mcast_ack\", \"pama_data\": \""
						+ comType + sourceStr[addnum - 1] + "\"}";
				// commands = "{\"addrList\": [], \"type\": \"mcast_ack\",
				// \"pama_data\":
				// \""+Integer.toHexString(comType)+com_content+"\"}";
			} else if (comType.equals("40") || comType.equals("41")) {
				commands = "{\"addrList\": " + adds + ", \"type\": \"mcast_ack\", \"pama_data\": \""
						+ comType + sourceStr[addnum - 1] + "\"}";
				// commands = "{\"type\": \"mcast_ack\", \"pama_data\":
				// \""+Integer.toHexString(comType)+com_content+"\"}";
			} 
		}
		System.out.println(commands);
		return commands;
	}

	// upper command handler
	public byte[] CommandHandler(byte[] command) {

// 上位机指令解析
		// System.out.println(command[0]+" "+command[1]+" "+command[2]+"
		// "+command[3]+" "+command[4]+" end");
		int command_length = command[0];
		// System.out.println("command_length = "+command_length);
		int command_flag = command[1];
		// System.out.println("command-Flag = "+command_flag);
		int send_to_net = command[2] >> 7;
		// System.out.println("send to net = "+send_to_net);
		int return_type = (command[2] >> 6) & 0x01;
		// System.out.println("return_type = "+return_type);
		int has_return = ((command[2] >> 4) & 0x03) >> 1;
		// System.out.println("has_return = "+has_return);
		int broadcast = ((command[2] >> 4) & 0x03) & 0x01;
		// System.out.println("broadcast = "+broadcast);
		int unicast_number = command[2] & 0x0F;
		// System.out.println("unicast = "+unicast_number);
		int com_length = command[3];
		// System.out.println("com_length = "+com_length);

		// System.out.println("comlength = "+command.length);
		//int comType = command[4];
		byte[] comTypeTemp = new byte[1];
		comTypeTemp[0]=command[4];
		String comType = Util.formatByteToByteStr(comTypeTemp);
		System.out.println("comType = "+Util.formatByteToByteStr(comTypeTemp));
		// int command_length = command[2];
		byte[] com = new byte[com_length];
		
		System.arraycopy(command, 5, com, 0, com_length);
		// System.out.println("com = "+com.toString());
		int check_sum = command[5 + com_length];
		for (int i = 0; i < command_length; i++) {
			// System.out.println("!!!" + com[i]);
		}
// 下发指令合成
		String commands = "";
		String com_content = new String(com);
		if (send_to_net == 1 || send_to_net == -1) {
			commands = commandAssemble(broadcast, com_content, comType);
		}
		try {
			SqlOperate.commandCache_a(commands);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// String cacheCommand = Util.formatByteToByteStr(command);
		// System.out.println("Command Handler:" + commands);// for log
		// Net_Status_flag = 6;
		System.out.println("Net_Status_flag now:" + Net_Status_flag);
		// command = cacheCommand.;
		// System.out.println(command[0]+" "+command[1]+" "+command[2]);// for
		// 修改得到return_type的内容用于后面处理
		if (comType.equals("00")) {
			return_type = 2;

		} else if (comType.equals("01")) {
			has_return = 1;
		}
		if (send_to_net == 1 || send_to_net == -1) {
			if (Net_Status_flag != 6) {
				// byte[] bitmap = new
				// byte[]{-1,-128,35,84,72,-128,61,-2,16,2,4,68,90,48,0,0,8,10};
				//byte[] bitmap = new byte[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
				byte[] bitmap = synParameter.getBitmap();
				boolean flag = Util.Online_Judge(bitmap);
				// boolean flag = Util.Online_Judge(synParameter.getBitmap());
				Net_Status_flag = Util.StatusJuage(flag);
				System.out.println("Net_Status_flag change:" + Net_Status_flag);
				if (Net_Status_flag == 1) {
					if (comType.equals("02")) {
						try {
							send_return(has_return, commands, com);
							try {
								SqlOperate.CommandCache_get();
							} catch (SQLException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							Timer timer = new Timer();
							timer.schedule(new TimerTask() {
								public void run() {
									try {
										String message = "The net start debugging";
										SendToupperMessage(message.getBytes());
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								// }, 3 * 1000);
							}, 30 * 1000);

							Net_Status_flag = 6;
							// CommandHandler(command);
							// status change to debugging，
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						try {
							cache_wait(comType,1, has_return, commands, com);
							try {
								SqlOperate.CommandCache_get();
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							// CommandHandler(command);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else if (Net_Status_flag == 2) {
					try {
						cache_wait(comType,1, has_return, commands, com);
						try {
							SqlOperate.CommandCache_get();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// CommandHandler(command);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (Net_Status_flag == 3) {
					try {
						cache_wait(comType,1, has_return, commands, com);
						try {
							SqlOperate.CommandCache_get();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// Net_Status_flag = 4;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (Net_Status_flag == 4) {
					// cache_send(0, has_return, commands, com);
					try {
						send_return(has_return, commands, com);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (comType.equals("02")) {
						// equals
						final String commandss = commands;
						Timer timer = new Timer();
						timer.schedule(new TimerTask() {
							public void run() {
								String message = "The net start debugging";
								System.out.println("upper send command to net");
								// Upper_messageHandler(commandss);
								try {
									SqlOperate.CommandCache_get();
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							// }, 3 * 1000);
						}, 30 * 1000);

						// System.out.println("SendToupperMessage(message.getBytes());");
						// SendToupperMessage(message.getBytes());
						Net_Status_flag = 6;
						// CommandHandler(command);
					}
				} else if (Net_Status_flag == 5) {
					try {
						cache_wait(comType,1, has_return, commands, com);
						try {
							SqlOperate.CommandCache_get();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					System.out.println("Net_Status_flag is:" + Net_Status_flag + " error");
				}
			} else {
				if (comType.equals("81")) {
					// "xiafa diaodu";
					final String commandss = commands;
					final String message = "The net has been close";
					try {
						send_return(has_return, commands, com);
						Timer timer = new Timer();
						timer.schedule(new TimerTask() {
							public void run() {
								System.out.println("wait for ending debug");
								// System.out.println("SendToupperMessage(message.getBytes())");
								try {
									SendToupperMessage(message.getBytes());
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								try {
									SqlOperate.CommandCache_get();
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}, 30 * 1000);
						// }, 3 * 1000);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Net_Status_flag = 0;
				} else if (comType.equals("02")) {
					// "return information";
					String message = "The net has already in debugging status";
					// System.out.println("SendToupperMessage(message.getBytes())");
					try {
						SendToupperMessage(message.getBytes());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						SqlOperate.CommandCache_get();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						send_return(has_return, commands, com);
						try {
							SqlOperate.CommandCache_get();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} else {
			if (comType.equals("00")) {
				// 修改心跳间
				//String com_content = new String(com);
				int heartIntSec = Integer.valueOf(com_content);
				// String[] sourceStr = com_content.split(",");
				parameter.setHeartIntSec(heartIntSec);
			} else if (comType.equals("01")) {
				//String com_content = new String(com);
				int day_length = Integer.valueOf(com_content);
				sendApplicationData(day_length);
				// 获取最新上报的应用数据
			} else if (comType.equals("02")) {
				// 获取最新网络监测数据
				//String com_content = new String(com);
				int day_length = Integer.valueOf(com_content);
				sendApplicationData(day_length);
			} else if (comType.equals("03")) {
				// 获取集中器进程运行状态 supervisorctl status
				try {
					getSupervisorStatus();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (comType.equals("04")) {
				// 获取supervisor运行日志
				sendProcessLog("var/log/hit_log/supervisord.log");
			} else if (comType.equals("05")) {
				// 获取集中器后台进程运行日志
				sendProcessLog("var/log/hit_log/concentratorback.stderr.log");
				sendProcessLog("var/log/hit_log/concentratorback.stdout.log");
			} else if (comType.equals("06")) {
				// 获取集中器前台运行日志
				sendProcessLog("var/log/hit_log/gunicorn.stderr.log");
				sendProcessLog("var/log/hit_log/gunicorn.stdout.log");
			} else if (comType.equals("07")) {
				// 获取tunslip运行日志
				sendProcessLog("var/log/hit_log/tunslip6.stderr.log");
				sendProcessLog("var/log/hit_log/tunslip6.stdout.log");
			} else if (comType.equals("08")) {
				// 获取ppp运行日志
				sendProcessLog("var/log/hit_log/ppp-connect-errors");
			} else if (comType.equals("09")) {
				// 获取集中器指令下发记录
				sendCommandBefore();
			} else if (comType.equals("0A")) {
				// 重启集中器
				try {
					restartConcentrator();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (comType.equals("0B")) {
				//String com_content = new String(com);
				String[] sourceStr = com_content.split("|");
				parameter.setftpuser(sourceStr[0]);
				parameter.setftpPwd(sourceStr[1]);
				// 修改ftp配置项目 用户名密码
			} else if (comType.equals("0C")) {
				// 重启集中器后台进程
				try {
					concentratorBackRestart();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			else if (comType.equals("0D")) {
				// 重启tunslip
				try {
					tunslip6Restart();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (comType.equals("0E")) {
				// 重启边界路由器节点
			} else if (comType.equals("10")) {
				// 返回时间段的数据库
				try {
					sendDataBase_b(com_content);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				System.out.println("send to centor error");
			} 
		}
		return null;
	}
	public void sendDataBase_b(String begin) throws IOException {
		SqlOperate.dataBaseOut(begin);
		WriteFTPFile write = new WriteFTPFile();
		write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(), parameter.getftpPort(),
				"topo4.db");
		String cmd = "rm topo4.db";
		System.out.println(cmd);
		Process commandProcess = Runtime.getRuntime().exec(cmd);
		//final BufferedReader input = new BufferedReader(new InputStreamReader(commandProcess.getInputStream()));
		//final BufferedReader err = new BufferedReader(new InputStreamReader(commandProcess.getErrorStream()));
	}
	// get process information ,test over
	public byte[] getProcessState() throws IOException {
		String cmd = "supervisorctl status";
		System.out.println(cmd);
		Process commandProcess = Runtime.getRuntime().exec(cmd);
		final BufferedReader input = new BufferedReader(new InputStreamReader(commandProcess.getInputStream()));
		final BufferedReader err = new BufferedReader(new InputStreamReader(commandProcess.getErrorStream()));
		String line = "";
		String message = "";
		try {
			while ((line = input.readLine()) != null) {
				System.out.println(line);
				message = message + "\n" + line;
			}
			SendToupperMessage(message.getBytes());
			input.close();
		} catch (IOException e) {
			err.close();
		}

		return null;
	}

	// get concentrator ID ,test over

	public byte[] getConcentratorID() {
		String ConcentratorID = "1";
		ConcentratorID = parameter.getId();
		try {
			System.out.println("ConcentratorID:" + ConcentratorID);// for log
			SendToupperMessage(ConcentratorID.getBytes());
			System.out.println("ACK");// for log
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	// send history net monitor test over
	public byte[] sendtopoBefore(byte[] command) {
		// org.apache.log4j.BasicConfigurator.configure();
		int day_length = (command[1] << 8 | command[2]);
		System.out.println("send topo Before day_length " + day_length);// for
																		// log
		try {

			String TopouploadFile = new SimpleDateFormat("yyyy-MM-dd#HH:mm:ss").format(new Date()) + "-topo.txt";
			TopouploadFile = parameter.getId() + TopouploadFile;
			// System.out.println("parameter.getId()"+parameter.getId());
			SqlOperate.topo_out(day_length, TopouploadFile);
			WriteFTPFile write = new WriteFTPFile();
			write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(), parameter.getftpPort(),
					TopouploadFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	// send history application data
	public byte[] sendApplicationData(int day_length) {

		// int day_length = (command[1] << 8 | command[2]);
		System.out.println(Util.getCurrentTime() + " Send appdata to Remote server(" + day_length + "):");// for
																											// log
		try {
			String AppuploadFile = new SimpleDateFormat("yyyy-MM-dd#HH:mm:ss").format(new Date()) + "-App.txt";
			AppuploadFile = parameter.getId() + "-" + AppuploadFile;
			SqlOperate.ApplicationData_out(day_length, AppuploadFile);
			WriteFTPFile write = new WriteFTPFile();
			write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(), parameter.getftpPort(),
					AppuploadFile);
			System.out.println(
					Util.getCurrentTime() + " ftpuser:" + parameter.getftpuser() + ",ftpPwd:" + parameter.getftpPwd()
							+ ",ftphost:" + parameter.getftphost() + ",ftpPort:" + parameter.getftpPort());// for
																											// log
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public byte[] sendNetMonitorData(int day_length) {

		// int day_length = (command[1] << 8 | command[2]);
		System.out.println(Util.getCurrentTime() + " Send appdata to Remote server(" + day_length + "):");// for
																											// log
		try {
			String netUploadFile = new SimpleDateFormat("yyyy-MM-dd#HH:mm:ss").format(new Date()) + "-Net.txt";
			netUploadFile = parameter.getId() + "-" + netUploadFile;
			SqlOperate.topo_out(day_length, netUploadFile);
			WriteFTPFile write = new WriteFTPFile();
			write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(), parameter.getftpPort(),
					netUploadFile);
			System.out.println(
					Util.getCurrentTime() + " ftpuser:" + parameter.getftpuser() + ",ftpPwd:" + parameter.getftpPwd()
							+ ",ftphost:" + parameter.getftphost() + ",ftpPort:" + parameter.getftpPort());// for
																											// log
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	// send Process Log test over
	public byte[] sendProcessLog(String logName) {
		// org.apache.log4j.BasicConfigurator.configure();
		try {
			WriteFTPFile write = new WriteFTPFile();
			String UploadFile = logName;
			System.out.println("send Process Log filename:" + UploadFile);// for
																			// //
																			// log
			write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(), parameter.getftpPort(),
					UploadFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// sendcommand history
	public byte[] sendCommandBefore() {
		try {
			SqlOperate.commanddown_out();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		WriteFTPFile write = new WriteFTPFile();
		String UploadFile = "CommadDown.txt";
		System.out.println("send Command Before filename:" + UploadFile);// for
																			// log
		write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(), parameter.getftpPort(),
				UploadFile);
		return null;
	}

	// restart centor1
	public void restartConcentrator() throws IOException {
		String command1 = "./restart.sh";
		Process commandProcess = Runtime.getRuntime().exec(command1);
		final BufferedReader input = new BufferedReader(new InputStreamReader(commandProcess.getInputStream()));
		final BufferedReader err = new BufferedReader(new InputStreamReader(commandProcess.getErrorStream()));
		String line = "";
		try {
			while ((line = input.readLine()) != null) {
				System.out.println(line);
			}
			input.close();
		} catch (IOException e) {
			err.close();
		}
	}

	public void getSupervisorStatus() throws IOException {
		String command1 = "supervisorctl status";
		Process commandProcess = Runtime.getRuntime().exec(command1);
		final BufferedReader input = new BufferedReader(new InputStreamReader(commandProcess.getInputStream()));
		final BufferedReader err = new BufferedReader(new InputStreamReader(commandProcess.getErrorStream()));
		String line = "";
		try {
			while ((line = input.readLine()) != null) {
				System.out.println(line);
				SendToupperMessage(line.getBytes());
			}
			input.close();
		} catch (IOException e) {
			err.close();
		}
	}

	public void concentratorBackRestart() throws IOException {
		String command1 = "supervisorctl restart concentratorBack";
		Process commandProcess = Runtime.getRuntime().exec(command1);
		final BufferedReader input = new BufferedReader(new InputStreamReader(commandProcess.getInputStream()));
		final BufferedReader err = new BufferedReader(new InputStreamReader(commandProcess.getErrorStream()));
		String line = "";
		try {
			while ((line = input.readLine()) != null) {
				System.out.println(line);
				// SendToupperMessage(line.getBytes());
			}
			input.close();
		} catch (IOException e) {
			err.close();
		}
	}

	public void supervisorRestart() throws IOException {
		String command1 = "supervisorctl restart all";
		Process commandProcess = Runtime.getRuntime().exec(command1);
		final BufferedReader input = new BufferedReader(new InputStreamReader(commandProcess.getInputStream()));
		final BufferedReader err = new BufferedReader(new InputStreamReader(commandProcess.getErrorStream()));
		String line = "";
		try {
			while ((line = input.readLine()) != null) {
				System.out.println(line);
				// SendToupperMessage(line.getBytes());
			}
			input.close();
		} catch (IOException e) {
			err.close();
		}
	}

	public void tunslip6Restart() throws IOException {
		String command1 = "supervisorctl restart tunslip6";
		Process commandProcess = Runtime.getRuntime().exec(command1);
		final BufferedReader input = new BufferedReader(new InputStreamReader(commandProcess.getInputStream()));
		final BufferedReader err = new BufferedReader(new InputStreamReader(commandProcess.getErrorStream()));
		String line = "";
		try {
			while ((line = input.readLine()) != null) {
				System.out.println(line);
				// SendToupperMessage(line.getBytes());
			}
			input.close();
		} catch (IOException e) {
			err.close();
		}
	}

	public void sent_message(String addr, byte[] message) {
		// System.out.println("nodes final:");
		// String notes = Util.getCurrentTime() + ":[" + addr + "]" + "report
		// data:" +
		// Util.formatBytesToStr(message);
		// System.out.println("Udp server send message :" + notes);
		String rootAddr = parameter.getRootAddr();
		// System.out.println("G_DEF_READ_DATA :
		// "+GlobalDefines.GlobalCmd.G_DEF_READ_DATA);
		// System.out.println("G_DEF_CTL_ACK_READ_DATA :
		// "+GlobalDefines.GlobalCmd.G_DEF_CTL_ACK_READ_DATA);
		if (!addr.equals(rootAddr)) {
			// System.out.println("rootAddr:"+rootAddr);
			if (message.length == 1) {
				System.out.println("multicast config parameter " + addr);
				paramConfigProxy.setConfigAck(addr);
			} else {
				byte type = message[1];// 0 is globaltype 1 is type
				String[] nodesIP = addr.split(":");
				message = Arrays.copyOfRange(message, 2, message.length);
				switch (type) {
				// case GlobalDefines.GlobalCmd.G_DEF_READ_DATA:// multicast
				case GlobalDefines.GlobalCmd.G_DEF_READ_DATA:// multicast
					// add data to applicationdata table
					System.out.println(Util.getCurrentTime() + " Appdata:" + nodesIP[nodesIP.length - 1] + "|"
							+ Util.formatByteToByteStr(message));
					SqlOperate.ApplicationData_a(nodesIP[nodesIP.length - 1], Util.getCurrentTime(),
							//Util.formatBytesToStr(message));
					Util.formatByteToByteStr(message));
					break;
				case GlobalDefines.GlobalCmd.G_DEF_CTL_ACK_READ_DATA:// multicast
					// add data to applicationdata table
					SqlOperate.ApplicationData_a(nodesIP[nodesIP.length - 1], Util.getCurrentTime(),
							//Util.formatBytesToStr(message));
					Util.formatByteToByteStr(message));
					if (remoteClient.remoteHostIsOnline()) {
						try {
							remoteClient.asyncWriteAndFlush(formatDataToJsonStr("app", addr, "1"));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if (message.length >= (GlobalDefines.GlobalIpVsId.G_DEF_IdLocation
							+ GlobalDefines.GlobalIpVsId.G_DEF_IdLength)) {
						StringBuilder tempId = new StringBuilder();
						for (int i = GlobalDefines.GlobalIpVsId.G_DEF_IdLength - 1; i >= 0; i--) {
							String str = new String();
							if (message[GlobalDefines.GlobalIpVsId.G_DEF_IdLocation + i] < 0) {
								str = Long.toHexString(256 + message[GlobalDefines.GlobalIpVsId.G_DEF_IdLocation + i]);
							} else {
								str = Long.toHexString(message[GlobalDefines.GlobalIpVsId.G_DEF_IdLocation + i]);
							}
							tempId.append(str);
						}
						String Id = tempId.toString();
						String Ip = Util.getIpv6LastByte(addr);
						if (IpidMap.containsKey(Ip)) {
							// System.out.println("has stored the relationship
							// of IP and ID");
							if (IpidMap.get(Ip).equals(Id)) {
								// System.out.println("and ID did not change");
							} else {
								// System.out.println("ID changed,resend the
								// relationship");
								remoteClient.asyncWriteAndFlush(formatDataToJsonStr("ipidmatchup", addr, Id));
								IpidMap.put(Ip, Id);
							}
						} else {
							// System.out.println("did not stored the
							// relationship of IP and ID before，report the
							// relationship");
							remoteClient.asyncWriteAndFlush(formatDataToJsonStr("ipidmatchup", addr, Id));
							IpidMap.put(Ip, Id);
						}
					}
					byte[] buff = new byte[message.length - 1];
					System.arraycopy(message, 1, buff, 0, buff.length);
					if (nettyClient.remoteHostIsOnline() && webKeyFlag) {
						nettyClient.asyncWriteAndFlush(
								formatUcastDataToJsonStr("web_data", addr, Util.formatByteToByteStr(buff)));

					}
					// bitMap.setBit(addr, buff);
					buff = null;
					break;
				case GlobalDefines.GlobalCmd.G_DEF_REPORT_NET:
					System.out.println("nodes final&&&&&&&&&&&&&:" + nodesIP[nodesIP.length - 1]);
					System.arraycopy(message, 2, message, 0, message.length - 2);
					if (remoteClient.remoteHostIsOnline()) {
						try {
							remoteClient.asyncWriteAndFlush(
									formatDataToJsonStr("net", addr, Util.formatByteToByteStr(message)));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					break;
				default:
					System.out.println("dddddddddddddddddddddddddefault");
					break;
				}
			}

		}

	}

	public void putCommandToCache(byte[] content) {
		contentByteBuffer.clear();
		contentByteBuffer.put(content);
	}

	public byte[] getCommandFromCache() {
		int position = contentByteBuffer.position();
		byte[] content = new byte[position];
		contentByteBuffer.get(content);
		contentByteBuffer.position(position);
		System.arraycopy(contentByteBuffer.array(), 0, content, 0, position);
		return content;
	}

	public SocketAddress getSocketAddressByName(String host, int port) throws UnknownHostException {
		return new InetSocketAddress(InetAddress.getByName(host), port);
	}

	public static void main(String[] args) throws IOException {
		new ConsoleMainServer();
		// commandAssemble(1,"c0",0x80);
		// commandAssemble(1,"c0",0xc0);
		// commandAssemble(1,"c0",0x40);
		// commandAssemble(1,"c0",0xc1);
		
		String aa = commandAssemble(1,"15:0:17:-128, -127:40:57","42");
		//{"type": "pama_syn", "pama_data": {"hour": "21", "level": 0, "seqNum": 17, "period": "2", "bitmap": [-128, 0, 0, 0, 8, 0, 0, 0, 0, -128, 0, 0, 0, 8, 0, 0, 0, 0], "second": "59", "state": true, "minute": "10"}}

		//Upper_messageHandler(aa);
		// commandAssemble(1,"15:0:17:-128, -127:40:57",0x81);
		// commandAssemble(1,"15:0:17:40:57",0x02);
		// if (comType == 0x00 || comType == 0x01 || comType == 0x80 ||
		// comType == 0x82) {
		// commands = "{\"type\": \"mcast\", \"pama_data\": \"" +
		// Integer.toHexString(comType) + com_content
		// + "\"}";
		// // {"type": "mcast", "pama_data": "8005105BFE5916"}
		// System.out.println(Util.getCurrentTime() + " " + commands);
		// } else if (comType == 0xc0 || comType == 0xc1) {
		// commands = "{\"addrList\": [], \"type\": \"mcast_ack\",
		// \"pama_data\": \""
		// + Integer.toHexString(comType) + com_content + "\"}";
		// } else if (comType == 0x40 || comType == 0x41) {
		// commands = "{\"type\": \"mcast_ack\", \"pama_data\": \"" +
		// Integer.toHexString(comType)
		// + com_content + "\"}";
		// } else if (comType == 0xc1) {
		// commands = "{\"type\": \"mcast\", \"pama_data\": \"" + com_content +
		// "\"}";
		// } else if (comType == 0xc2) {
		// // String com_content = new String(com);
		// String[] sourceStr = com_content.split(",");
		// int addnum = sourceStr.length;
		// commands = "{\"type\": \"pama_syn\", \"pama_data\": {\"hour\": \"" +
		// sourceStr[0]
		// + "\", \"level\": " + sourceStr[1] + ", \"seqNum\": " + sourceStr[2]
		// + ", \"period\": \""
		// + sourceStr[3] + ", \"bitmap\": [" + sourceStr[4] + "], \"second\":
		// \"" + sourceStr[5]
		// + "\", \"state\": " + sourceStr[6] + ", \"minute\": \"" +
		// sourceStr[7] + "\"}}";
		// } else if (comType == 0x02) {
		// String[] sourceStr = com_content.split(",");
		// int addnum = sourceStr.length;
		// String DebugBitmap = "-1,-1,-1,-1,-1," + "-1,-1,-1,-1,-1," +
		// "-1,-1,-1,-1,-1,-1,-1,-1";
		// commands = "{\"type\": \"schedule\", \"pama_data\": {\"hour\": \"" +
		// sourceStr[0]
		// + "\", \"level\": " + sourceStr[1] + ", \"seqNum\": " + sourceStr[2]
		// + ", \"period\": \""
		// + sourceStr[3] + ", \"bitmap\": [" + DebugBitmap + "], \"second\":
		// \"" + sourceStr[4]
		// + "\", \"state\": " + sourceStr[5] + ", \"minute\": \"" +
		// sourceStr[6] + "\"}}";
		// } else if (comType == 0x81 || comType == 0x40) {
		// String[] sourceStr = com_content.split(",");
		// int addnum = sourceStr.length;
		// commands = "{\"type\": \"schedule\", \"pama_data\": {\"hour\": \"" +
		// sourceStr[0]
		// + "\", \"level\": " + sourceStr[1] + ", \"seqNum\": " + sourceStr[2]
		// + ", \"period\": \""
		// + sourceStr[3] + ", \"bitmap\": [" + sourceStr[4] + "], \"second\":
		// \"" + sourceStr[5]
		// + "\", \"state\": " + sourceStr[6] + ", \"minute\": \"" +
		// sourceStr[7] + "\"}}";
		// }else{
		// System.out.println("error "+com_content);
		// }
		// ConsoleMainServer main =
		// SqlOperate.connect("jdbc:sqlite:/root/build_jar/topo3.db");
		// SqlOperate.connect("jdbc:sqlite:topo3.db");
		// System.out.println("123");
		// sendProcessLog();
		// "2017-03-26 19:58:49"

		// sendProcessLog();
		// byte[] open = new byte[21];
		//
		// String c1 = "FFFFFFFe";
		// byte[] com1 = c1.getBytes();
		// String c2 = "ffffffff";
		// byte[] com2 = c2.getBytes();
		// getbit();
		// // byte[] com3 =
		// String c3 = "108ffffffff";
		// String c4 = "";
		// byte[] com3 = new byte[c3.length()];
		// for (int i = 0; i < 11; i++) {
		// com3[i] = (byte) Integer.parseInt(c3.substring(i, i + 1), 16);
		// }
		// for (int i = 0; i < 11; i++) {
		// System.out.println(" com:" + i + ":" + com3[i]);
		// }
		//
		// System.out.println("length c3:" + c3.length());
		// System.out.println("length com3:" + com3.length);
		// for (int i = 0; i < 11; i++) {
		// c4 += Integer.toHexString(com3[i]);
		// }
		// System.out.println(c4);
		// CommandHandler(com3);
		// time_diffence(0, getbit());
		// cache_wait(int cache, int has_return, String cacheCommand, byte[]
		// com)
		// cache_wait(1,1,c1,com1);
		// cache_wait(1,2,c1,com1);
		// cache_wait(1,3,c1,com1);
		// cache_wait(1,0,c1,com1);
		// send_return(1, c1, com1);
		// send_return(2, c1, com1);
		// send_return(3, c1, com1);
		// send_return(4, c1, com1);
		// byte[] bit = new byte[144];
		// byte[] bitmap = new
		// byte[]{-1,-128,35,84,72,-128,61,-2,16,2,4,68,90,48,0,0,8,10};;
		// boolean flag = Util.Online_Judge(bitmap);
		// //boolean flag = Util.Online_Judge(synParameter.getBitmap());
		// int Net_flag = Util.StatusJuage(flag);
		// System.out.println("Net_flag:"+Net_flag);

	}
}

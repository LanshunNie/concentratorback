package com.hit.heat.cn;

import java.io.BufferedReader;

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
import java.util.Timer;
import java.util.TimerTask;

import com.hit.heat.util.Frag_Recb;
import com.hit.heat.model.Energy;

//import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

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

import sun.awt.CharsetString;

/* @lhy
 * @date 2016年4月4日
 * @des
 */
public class ConsoleMainServer {

	private GConfig config;// 外部文件包括ip地址和端口
	private GSynConfig synConfig;// 外部文件包括同步消息
	private SystemParam g_systemParam;// 外部文件包括重传次数等参数配置
	private NetParameter parameter;// 对应config文件的对象类
	private SynParameter synParameter;
	// private WriteDataToFile mulicastDataFile;
	// private WriteDataToFile unicastDataFile;
	// private WriteDataToFile cmdFile;
	// private WriteDataToFile topoFile;
	private WriteDataToFile rdcControlFile;
	// private WriteDataToFile topogxnFile;
	// private WriteDataToFile unicastDataFile;
	// private WriteDataToFile offLineDataFile;
	private WriteDataToFile FragFile;

	private NettyClient nettyClient;// netty 客户端有关
	private NettyClient remoteClient;
	private NettyClient configRemoteNettyClient;// 配置信息
	private NettyClient netClient;
	private NettyServer nettyServer;// Netty NIo TCP 服务器 集中器前端

	// private NettyServer webNettyServer;// 集中器前台模拟web

	private NIOUDPServer nioUdpServer;// 应用数据nio UDP服务器时
	private NIOUDPServer nioUpperServer;// 应用数据nio UDP服务器时
	private NIOUDPServer nioNetDataServer;// 网络参数 只接收心跳数据
	private NIOUDPServer nioSynConfigServer;// 同步
	private NIOUDPServer nioCorrectTime;// 校正时间

	/***************************************************************************/
	private NIOUDPServer nioRdcControlServer;// rdc 控制server
	private int rdcControlPort = 3103;// rdc 控制端口
	private int rdcPanPort = 3102;// rdc 控制端口
	private rdc_EF_Control rdcControl;
	private int current_budget = 26000;// nA
	private int current_guard = 2000;// nA
	private boolean rdcControlInit = false;
	/***************************************************************************/

	private UnicastProxy unicastProxy;
	private HeartProxy heartProxy;// 心跳包代理
	private ParamConfigProxy paramConfigProxy;// 参数配置 多播代理和单播代理

	private BitMap bitMap;// 判断重传的bitMap队列
	// private Timer timer;// 全局计时器
	// private int cur_retransmition_count = 0;// 当前重传次数

	final int NODE_UNICAST_PORT = 5656; // // 节点接收单播指令

	private ByteBuffer contentByteBuffer;
	private List<String> ipList;// 获取ip列表
	private Map<Integer, Location> locationsMap;//
	private List<Location> locationsList;//

	// private String webToken = null;
	// private String webDataToken = "12345678";
	// private String revcMsg = null;
	private boolean webKeyFlag;
	private int SynLocalPort = 6102;
	private int CorrectTimePort = 1026;// 校正时间端口
	private int CorrectAckPort = 1028;// 接收跟节点回复的ACK
	private int broadcastPort = 6104;// 时间同步端口
	private boolean synStateFlag = false;
	private int seqCount;
	private int currect_rate = 1;// 单位秒
	// private List<FloorInfor> floorInfors;
	// private DESPlus webTokenDesPlus;
	// private DESPlus webDataDesPlus;
	// private String remoteDataToken=null;
	// private String webDataToken = null;
	// private String broadcastAddr="FF02::2";
	// private int schedule_Port = 1028;
	Timer CorrectTimer = new Timer();// 定时校时
	Timer APPTimer = new Timer();// 定时上报应用数据
	Timer CommandDownTimer = new Timer();// 定时下发指令
	Timer HeartTimer = new Timer();
	Timer Appdrop = new Timer();
	Timer Netdrop = new Timer();
	Timer topoTimer = new Timer();

	private Map<String, String> IpidMap;
	private Map<Integer, String> topoMap = new HashMap<Integer, String>();
	// private Map<String, String> topoMap = new HashMap<String, String>();
	boolean flag = false;
	private int MaxCount = 10;
	private int Count = 0;
	// private static Logger logger = Logger.getLogger(FTPMain.class);

	public ConsoleMainServer() {

		try {
			/***********************************************************/
			int drop_length = 1;
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
			// mulicastDataFile = new WriteDataToFile("mulicast.txt");// 存储多播消息
			// unicastDataFile = new WriteDataToFile("unicast.txt");// 存储单播消息
			// topoFile = new WriteDataToFile("topo.txt");
			// topogxnFile = new WriteDataToFile("topogxn.txt");//
			FragFile = new WriteDataToFile("fragFile.txt");

			config = new GConfig("config.json");// 读取外部文件的参数
			parameter = config.getNetParameter();
			// webToken = "01234567";
			webKeyFlag = true;
			// remoteKeyFlag = false;
			seqCount = 0;
			synConfig = new GSynConfig("GSynConfig.json");
			synParameter = synConfig.getSynParameter();
			// synStateFlag 为 同步文件synParameter中的标记位
			synStateFlag = synParameter.isFlag();
			System.out.println(synStateFlag);
			IpidMap = new HashMap<String, String>();
			// desPlus = new DESPlus(token);
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		} // 存储客户端下达的指令
		catch (JSONException e) {
			parameter = new NetParameter("00000001", 40, 3, 30, "0.0.0.0", 12300, 12301, 12306, "aaaa::1", 8765,
					"aaaa:0:0:0:12:7400:1:13", 5678, "192.168.1.141", 12303, "192.168.1.141", 12304, 12307, 2, 3,
					"0.0.0.0", 12400, "xiaoming", "139.199.154.37", "xiaoming", 21);
			synParameter = new SynParameter(0, 0, 0, 0, 0, 0, "0".getBytes(), false, null);
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		// 添加集中器的信息
		String rootplace = "000000";
		SqlOperate.NodePlace_a(parameter.getId(), "000", rootplace);

		try {
			g_systemParam = Util.parseSystemParamFromFile("sysparam.json");
		} catch (Exception e1) {
			// TODO 自动生成的 catch 块
			g_systemParam = new SystemParam();
		}

		try {
			locationsMap = Util.parseLocationsFromFile("location.txt");
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			locationsMap = new HashMap<Integer, Location>(64);
		}
		locationsList = new ArrayList<Location>();
		locationsList.addAll(locationsMap.values());
		ipList = new ArrayList<String>();
		for (Location l : locationsList) {
			ipList.add(l.getAddr());
		}
		/******************* TCP远程客户端初始化 *****************************/
		nettyClient = new NettyClient(parameter.getTcpWebServerAddr(), parameter.getTcpWebServerPort());

		remoteClient = new NettyClient(parameter.getRemoteAddr(), parameter.getRemotePort());

		configRemoteNettyClient = new NettyClient(parameter.getRemoteAddr(), parameter.getTcpRemoteConfigPort());
		System.out.println(parameter.getRemoteAddr());
		netClient = new NettyClient(parameter.getRemoteAddr(), parameter.getNetPort());// parameter.getRemotePort()
		System.out.println(parameter.getNetPort());
		try {
			nioSynConfigServer = new NIOUDPServer(parameter.getUdpAddr(), SynLocalPort);
			nioSynConfigServer.registerHandler(new NIOSynMessageHandler());

			nioCorrectTime = new NIOUDPServer(parameter.getUdpAddr(), CorrectAckPort);// 校正时间
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
			// 校时
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
							System.out.println(Util.getCurrentTime() + " 发送ROOT校时 " + currentTime.getHour() + ":"
									+ currentTime.getMinute() + ":" + currentTime.getSecond());// for

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

			// 定时上报数据
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
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e1) {
			// TODO 自动生成的 catch 块
			e1.printStackTrace();
		}

		/*********** 单播代理设置参数 ************************/
		unicastProxy.registerMethod(new UnicastMethodInvoke());
		unicastProxy.setDelay(g_systemParam.getSysNodeTransDelay() * 1000);
		unicastProxy.setAckWaitDelay(g_systemParam.getSysNodeWaitAckDelay() * 1000);
		unicastProxy.setReTransmitCount(g_systemParam.getSysUcastRetransTimes());
		/*************** 心跳代理 设置参数 ******************/

		heartProxy = new HeartProxy(ipList, g_systemParam.getSysNodeHeartDelay() * 1000);
		heartProxy.setInverval(g_systemParam.getSysNodeHeartDelay() * 1000);
		// heartProxy.setInverval(g_systemParam.getSysNodeHeartDelay() * 1000);
		heartProxy.registerOffLineHandler(new HeartOffLineHandler() {

			@Override
			public void actionPerformed(String addr) {
				System.out.println(addr + "掉线了");
				// TODO 自动生成的方法存根
				// 生成警告信息，并上报给远程客户端和工具
				// nettyClient.asyncWriteAndFlush(formatUcastDataToJsonStr("Warning",addr,"warning"));//formatUcastDataToJsonStr(addr,
				// "warning")
				if (remoteClient.remoteHostIsOnline()) {
					try {
						remoteClient.asyncWriteAndFlush(formatDataToJsonStr("heart_warning", addr, "warning"));
					} catch (Exception e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
				}

			}
		});
		heartProxy.registerOnLineHandler(new HeartOnLineHandler() {
			@Override
			public void actionPerformed(String addr) {
				System.out.println(addr + "在线");
				if (remoteClient.remoteHostIsOnline()) {
					try {
						remoteClient.asyncWriteAndFlush(formatDataToJsonStr("heart_succeed", addr, "online"));
					} catch (Exception e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
				}
				// TODO 自动生成的方法存根
			}
		});
		// heartProxy.start();
		paramConfigProxy = new ParamConfigProxy(parameter.getRootAddr(), new ProxyInvoke() {
			@Override
			public void invoke(String addr, int port, byte[] message) {
				// TODO 自动生成的方法存根
				try {
					nioUdpServer.sendto(message, getSocketAddressByName(addr, port));
				} catch (UnknownHostException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
				}
			}
		});
		// 配置失败的节点列表
		paramConfigProxy.registerConfigResultHandler(new ParamConfigResult() {

			@Override
			public void actionPerformed(List<String> failIpList) {
				// TODO 自动生成的方法存根
				System.out.println("调用！");
				JSONObject msgJson = null;
				if (failIpList == null) {
					System.out.println(" config succeed");
					msgJson = new JSONObject();
					try {
						msgJson.put("type", "config_succeed");
						msgJson.put("data", "config_succeed");
						// msgJson.put("", "");
					} catch (JSONException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}

					if (configRemoteNettyClient.remoteHostIsOnline()) {
						try {
							configRemoteNettyClient.asyncWriteAndFlush(msgJson.toString());
						} catch (Exception e) {
							// TODO 自动生成的 catch 块
							e.printStackTrace();
						}
					}
					return;
				}
				StringBuilder sb = new StringBuilder();
				sb.append("有节点配置失败，配置失败节点ip为\r\n");
				for (String ip : failIpList) {
					sb.append("  " + ip + "\r\n");
					System.out.println("有节点配置失败，配置失败节点ip为:  " + ip);
				}
				try {
					msgJson = new JSONObject();
					msgJson.put("type", "config_fail");
					msgJson.put("data", sb.toString());
				} catch (JSONException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
				}
				if (configRemoteNettyClient.remoteHostIsOnline()) {
					try {
						// System.out.println(msgJson.toString());
						configRemoteNettyClient.asyncWriteAndFlush(msgJson.toString());
						// remoteClient.asyncWriteAndFlush(remoteDataDesPlus.encrypt(formatDataToJsonStr("config_fail",)));
					} catch (Exception e) {
						// TODO 自动生成的 catch 块
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
		startNIOTcpServer();// 开启接收集中器前端远程客户端的指令的TCP server
		startNIOUdpServer();// 开启接收无线网络节点数据的UDP Server
		try {
			nioNetDataServer = new NIOUDPServer("0.0.0.0", 5688);
			nioNetDataServer.registerHandler(new NIOUdpNetDataHandler());
			nioNetDataServer.start();
			// nioSynConfigServer
		} catch (IOException | IllegalStateException | InterruptedException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		startUpperUdpServer();
		// 定时心跳
		HeartTimer.schedule(new TimerTask() {
			public void run() {
				heartbeat();
			}
			// }, 0, 1000 * 2);
		}, 0, 1000 * parameter.getHeartIntSec() * 1);
		// // 定时指令下发
		CommandDownTimer.schedule(new TimerTask() {
			public void run() {
				try {
					CommandDown();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// }, 0, 1000 * 5);
		}, 0, 1000 * 60 * 10);
		// nioNetDataServer.stop();
		// startUpperUdpServer();
		// System.out.println("!!!");
		// nioUpperServer.registerHandler(new UpperUdpMessageHandler());
	}

	// ***********************************************************************************方法实现
	// ***********************************************************************************方法实现
	// UDP 服务器 发送控制命令数据包给根节点 byte
	public void TunSendToRootMessage(byte[] message) throws IOException {
		if (nioUdpServer == null) {
			throw new IOException();
		}
		System.out.println(Util.getCurrentTime() + " Send to ROOT 命令为:" + Util.formatByteToByteStr(message));// for
		nioUdpServer.sendto(message, getSocketAddressByName(parameter.getRootAddr(), parameter.getRootPort()));// for
																												// //
																												// log
		System.out.println("Send To Root Message over");// for lag
	}

	// 指令下发给根节点
	public void CommandDown() throws IOException {
		// System.out.println("Command down start");// for log
		boolean flag = Util.Online_Judge(synParameter.getBitmap());
		String currenttime = Util.getCurrentDateTime();
		System.out.println("command down currentime:" + currenttime);// for log
		String[] times = currenttime.split(":");
		int minute = Integer.parseInt(times[1]);
		if (flag) {
			// if(true) {
			if (minute % 10 >= 6) {
				// System.out.println("minute:" + minute);// for log
				String com = SqlOperate.CommandCache_get();
				if (com == "500000")
					return;
				else {
					// System.out.println("command down " + com);// for log
					// byte[] command = Util.formatByteStrToByte(com);
					byte[] cmd = Util.formatByteStrToByte(com);
					byte[] buffer = null;
					buffer = Util.packetMcastSend(cmd);
					TunSendToRootMessage(buffer);

					SqlOperate.commanddown_a("1", "1", com);
					System.out.println(Util.getCurrentTime() + " 缓存指令［" + com + "]" + "下发成功!");// for
																								// lag
				}
			} else {

				// System.out.println(Util.getCurrentTime()+" 网络冲突,缓存指令下发失败
				// 等待下次下发!");// for log
				return;
			}
		}
	}

	// ×××××××
	// UDP 服务器 发送返回消息命令数据包给上位机
	// 上报心跳
	public void heartbeat() {
		// System.out.println("start consentrator heartbeat");// for log
		// System.out.println("start heart beat:" + parameter.getupperAddr() +
		// parameter.getupperPort());// for

		byte[] mesge = { 1, 2, 4, 5 };
		try {
			SendToupperMessage(mesge);
			System.out.println(Util.getCurrentTime() + " 发送心跳包以保持GPRS在线!");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	// 开启上位机UDP服务
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
			System.out.println("upper server start");// for log //115200
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 给上位机发送消息
	public void SendToupperMessage(byte[] message) throws IOException {
		if (nioUpperServer == null) {
			// System.out.println(nioUpperServer);
			throw new IOException();
		}
		// System.out.println("Send To Message:" + parameter.getftphost() +
		// ",port:" + "12400");// for
		// System.out.println(message+" @
		// "+getSocketAddressByName(parameter.getftphost(), 12400)); // log
		nioUpperServer.sendto(message, getSocketAddressByName(parameter.getftphost(), 12400));// parameter.getRootPort()
	}

	// 向跟接地那发送同步消息
	public void SendToRootSynMsg(byte[] message) throws IOException {
		if (nioSynConfigServer == null) {
			throw new IOException();
		}
		nioSynConfigServer.sendto(message, getSocketAddressByName(parameter.getRootAddr(), broadcastPort));
		// System.out.println("send to" + parameter.getRootAddr() +
		// broadcastPort);
	}

	// 开启 Netty 的nio tcp服务
	public void startNIOTcpServer() {
		try {
			nettyServer.start(parameter.getTcpAddr(), parameter.getTcpPort());// new
			// NettyMsgHandlerExecutor()

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	// 开启nio udp服务
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
				System.out.println("收到ACK " + Count);
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
	 * @lhy 接收端：接收来自集中器客户端发来的指令，接收发送过来的json形式的字符串，通过与字节数组之间进行变换，以字节数组的格式
	 * 发送给root节点,节点以char形数组接收
	 *
	 */
	class NettyMsgHandlerExecutor implements NettyMsgHandler {

		// @SuppressWarnings({ "unchecked" })
		@Override
		public String messageHandler(String message) {
			System.out.println(Util.getCurrentTime() + " 前台指令:" + message);

			Object retObject;
			try {
				// command = Command.parseCmdFromStream(message);
				retObject = new JSONObject(message);
				// System.out.println(retObject);
			} catch (JSONException e1) {
				// TODO 自动生成的 catch 块
				e1.printStackTrace();
				return null;
			}
			if (retObject instanceof JSONObject) {// 接收集中器前台的配置参数的命令
				try {
					String type = ((JSONObject) retObject).get("type").toString();

					byte[] cmd = Util.formatByteStrToByte(((JSONObject) retObject).getString("pama_data"));
					byte[] buffer = null;
					StringBuilder sb = new StringBuilder(Util.getCurrentTime() + " 下发指令为  ");
					switch (type) {
					case "mcast":
						// System.out.println(222222);
						buffer = Util.packetMcastSend(cmd);
						if (buffer[2] == (byte) 0x00) {
							System.out.println(Util.getCurrentTime() + " 收到的指令为 上报网络监测数据");
							sb.append("上报能耗+拓扑");
						} else if (buffer[2] == (byte) 0x01) {
							System.out.println(Util.getCurrentTime() + " 收到的指令为 上报网络参数");
							sb.append("上报网络参数");
						} else if (buffer[2] == (byte) 0X80) {
							System.out.println(Util.getCurrentTime() + " 收到的指令为 多播表操作指令");
							sb.append("多播读表指令");
						} else if (buffer[2] == (byte) 0x82) {
							System.out.println(Util.getCurrentTime() + " 收到的指令为 指令烧写");
							sb.append("初始的多播读表指令");
						} else {
							System.out.println(Util.getCurrentTime() + " 出错啦~~");
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
							// TODO 自动生成的 catch 块
							e.printStackTrace();
						}
						break;
					case "unicast":
						bitMap.setPartReUploadList(parseAddrFromStr(((JSONObject) retObject).getString("addrList")));
						buffer = Util.packageUnicastSend(cmd, bitMap.getBitMap());
						if (buffer[3 + bitMap.getBitMap().length] == (byte) 0X80) {
							System.out.println(Util.getCurrentTime() + " 收到的指令为  局部多播读表指令");
							// System.out.println("buffer:" +
							// Arrays.toString(buffer));
							// System.out.println(Arrays.toString(bitMap.getBitMap()));
							sb.append("局部多播读表指令");
						} else if (buffer[3 + bitMap.getBitMap().length] == (byte) 0x82) {
							System.out.println(Util.getCurrentTime() + " 收到的指令为 初始的局部多播 读表指令");
							System.out.println(Util.getCurrentTime() + " buffer:" + Arrays.toString(buffer));
							System.out
									.println(Util.getCurrentTime() + " bitmap:" + Arrays.toString(bitMap.getBitMap()));
							sb.append("初始的局部多播 读表指令");
						} else {
							System.out.println(Util.getCurrentTime() + " 出错啦~~~");
						}
						// 保存下发指令，存储在cmd.txt里
						// try {
						// cmdFile.append(sb.toString()+buffer.toString());
						// } catch (IOException e2) {
						// // TODO Auto-generated catch block
						// e2.printStackTrace();
						// }
						try {
							TunSendToRootMessage(buffer);
						} catch (IOException e) {
							// TODO 自动生成的 catch 块
							e.printStackTrace();
						}
						break;
					case "mcast_ack":
						buffer = Util.packetMcastSend(cmd);
						if (buffer[2] == (byte) 0x41) {
							System.out.println(Util.getCurrentTime() + " 收到的指令为 周期配置");
							sb.append("周期配置");
						} else if (buffer[2] == (byte) 0x40) {
							System.out.println(Util.getCurrentTime() + " 收到的指令为 配置网络参数");
							sb.append("配置网络参数");
						} else if (buffer[2] == (byte) 0xC1) {
							System.out.println(Util.getCurrentTime() + " 收到的指令为 多播节点初始化指令");
							sb.append("多播节点初始化指令");
						} else if (buffer[2] == (byte) 0xC0) {
							System.out.println(Util.getCurrentTime() + " 收到的指令为 多播节点重启指令");
							sb.append("多播节点重启指令");
						} else {
							System.out.println(Util.getCurrentTime() + " 出错啦~~");
						}
						// 保存下发指令，存储在cmd.txt里
						// try {
						// cmdFile.append(sb.toString()+buffer.toString());
						// } catch (IOException e2) {
						// // TODO Auto-generated catch block
						// e2.printStackTrace();
						// }
						try {
							TunSendToRootMessage(buffer);
						} catch (IOException e) {
							// TODO 自动生成的 catch 块
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
							System.out.println(Util.getCurrentTime() + " 收到的指令为 局部多播节点初始化");
							sb.append("局部多播节点初始化");
						} else if (buffer[3 + bitMap.getBitMap().length] == (byte) 0xC1) {
							System.out.println(Util.getCurrentTime() + " 收到的指令为 局部多播节点重启");
							sb.append("局部多播节点重启");
						} else {
							System.out.println(Util.getCurrentTime() + " 出错啦~~~");
						}

						try {
							TunSendToRootMessage(buffer);
						} catch (IOException e) {
							// TODO 自动生成的 catch 块
							e.printStackTrace();
						}
						break;

					case "schedule":// 广播配置调度
						try {
							System.out.println(Util.getCurrentTime() + " 收到的指令为广播配置调度"
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
							// TODO 自动生成的 catch 块
							e.printStackTrace();
						}
						break;
					// 有用
					case "pama_corr":
						try {

							currect_rate = Integer.valueOf(((JSONObject) retObject).getString(("pama_data")));
							System.out.println(Util.getCurrentTime() + "收到的指令为更改校时频率 pama_data:"
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
											System.out.println("发送correct time指令 " + currentTime.getSecond());
										} else {
											System.out.println("发送跟节点重启指令" + Count);
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
					// 有用
					case "pama_syn":
						try {
							System.out.println(Util.getCurrentTime() + " 收到的指令为修改网络调度下发周期"
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
							// TODO 自动生成的 catch 块
							e.printStackTrace();
						}
						break;

					default:
						break;
					}
				} catch (JSONException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
				}
			}

			return null;
		}
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

	private byte[] packScheduleConfigData(byte[] content) { // 配置调度，类型7
		if (content == null || content.length == 0) {
			return null;
		}
		byte[] cmd = new byte[content.length + 3];
		cmd[0] = (byte) (content.length + 2);
		cmd[1] = (byte) (1); // 1 表示 多播
		cmd[2] = GlobalDefines.GlobalCmd.G_SCHEDULE_CONFIG;
		System.arraycopy(content, 0, cmd, 3, content.length);
		return cmd;
	}

	// 下面两个与上位机通讯时使用
	private byte[] packUnicastData(byte[] content) {// 单播发指令,类型3
		if (content == null || content.length == 0) {
			return null;
		}
		byte[] cmd = new byte[content.length + 3];
		cmd[0] = (byte) (content.length + 2);
		cmd[1] = (byte) (1); // 1 表示 多播
		cmd[2] = GlobalDefines.GlobalCmd.G_DEF_READ_DATA;
		System.arraycopy(content, 0, cmd, 3, content.length);
		return cmd;
	}

	private byte[] packageReadData(byte[] cmd) {// 多播 纯指令类型1
		if (cmd == null || cmd.length == 0) {
			return null;
		}
		int length = cmd.length;
		byte[] content = new byte[length + 3];
		content[0] = (byte) (length + 2);
		content[1] = (byte) (1); // 1 表示 多播
		content[2] = GlobalDefines.GlobalCmd.G_DEF_READ_DATA;
		System.arraycopy(cmd, 0, content, 3, length);
		return content;
	}

	// 与上位机通讯时使用到
	private byte[] packageReadDataAck(byte[] cmd, byte[] bitmap) {// 多播重传类型2
		// 指令加bitmap
		// 类型2
		if (cmd == null || cmd.length == 0) {
			return null;
		}
		if (bitmap == null || bitmap.length == 0) {
			return null;
		}
		byte[] content = new byte[cmd.length + bitmap.length + 4];
		content[0] = (byte) (cmd.length + bitmap.length + 3);
		content[1] = (byte) (2); // 2 表示 局部单播
		System.arraycopy(bitmap, 0, content, 2, bitmap.length);
		content[2 + bitmap.length] = GlobalDefines.GlobalCmd.G_DEF_READ_DATA;
		content[3 + bitmap.length] = (byte) cmd.length;// 指令长度
		System.arraycopy(cmd, 0, content, bitmap.length + 4, cmd.length);
		return content;
	}

	public String formatUcastDataToJsonStr(String addr, String content) {
		JSONObject object = new JSONObject();
		try {
			object.put("addr", addr);
			object.put("data", content);
		} catch (JSONException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		return "[" + object.toString() + "]";
	}

	/************* 数据组装 ****************/
	public String formatUcastDataToJsonStr(String type, String addr, String content) {
		JSONObject object = new JSONObject();
		try {
			object.put("type", type);
			object.put("addr", addr);
			object.put("data", content);
		} catch (JSONException e) {
			// TODO 自动生成的 catch 块
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
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		return object.toString();
	}

	class UnicastMethodInvoke implements ProxyInvoke {

		@Override
		public void invoke(String addr, int port, byte[] message) {
			// TODO 自动生成的方法存根
			// 单播出去
			try {
				// System.out.println(addr);
				UnicastSendMessage(addr, port, message);// ip地址
				// 端口号，消息
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
		}
	}

	// 单播发送
	public void UnicastSendMessage(String addr, int port, byte[] message) throws IOException {
		if (nioUdpServer == null) {
			throw new IOException();
		}
		nioUdpServer.sendto(message, getSocketAddressByName(addr, port));
	}

	// 处理网络参数，设计拓扑结构
	// class NIOUdpNetDataHandler implements NIOUDPServerMsgHandler {
	//
	// @Override
	// public byte[] messageHandler(String addr, byte[] message) {
	// if (!flag)
	// flag = true;
	// topoMap.put(addr, addr);
	//
	// byte[] orpl = new byte[message.length - 3];
	// System.arraycopy(message, 3, orpl, 0, message.length - 3);
	// //System.out.println(Util.getCurrentTime()+" topo："+ addr);
	//
	// // topoFile.append(Util.getCurrentTime() + ":[" + addr + "]" + "能耗：" +
	// Util.formatBytesToStr(message));
	// // topogxnFile.append("" + Util.getUtcTime() + " " + addr + " " + " " +
	// Util.formatBytesToStrGxn(message));
	//
	// Energy en = Util.Create_Energy(addr, orpl);
	// SqlOperate.append(en);
	//
	// // SqlOperate.ApplicationData_a(NodeID, currenttime, data);
	// rdc_EF_Control.updateNetData(addr, en);
	//
	// if (netClient.remoteHostIsOnline()) {
	// try {
	// // 使用json发送 拓扑 信息
	// netClient.asyncWriteAndFlush(formatDataToJsonStr("topo", addr,
	// Util.formatByteToByteStr(orpl))); // json使用方式
	// } catch (Exception e) {
	// // TODO 自动生成的 catch 块
	// e.printStackTrace();
	// }
	// }
	// return null;
	// }
	// }
	class NIOUdpNetDataHandler implements NIOUDPServerMsgHandler {

		@Override
		public byte[] messageHandler(String addr, byte[] message) {
			// oprl 移3位

			StringBuilder sb = new StringBuilder(addr);
			for (byte b : message) {
				sb.append(b);
			}
			int hash = sb.toString().hashCode();
			// topo 生成的 hashcode
			// System.out.println(hash);
			if (!topoMap.containsKey(hash)) {
				if (!flag)
					flag = true;
				topoMap.put(hash, addr);
				System.out.print("-----------------------------");
				byte[] orpl = new byte[message.length - 3];
				System.arraycopy(message, 3, orpl, 0, message.length - 3);
				System.out.println(addr + "能耗：" + Util.formatBytesToStr(message));

				// topoFile.append(Util.getCurrentTime() + ":[" + addr + "]" +
				// "能耗：" + Util.formatBytesToStr(message));
				// topogxnFile.append("" + Util.getUtcTime() + " " + addr + " "
				// + " " + Util.formatBytesToStrGxn(message));
				Energy en = Util.Create_Energy(addr, orpl);
				SqlOperate.append(en);

				if (netClient.remoteHostIsOnline()) {
					try {
						// 使用json发送 拓扑 信息
						netClient.asyncWriteAndFlush(formatDataToJsonStr("topo", addr, Util.formatByteToByteStr(orpl))); // json使用方式
					} catch (Exception e) {
						// TODO 自动生成的 catch 块
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

	// 处理同步消息的udp 服务器
	class NIOSynMessageHandler implements NIOUDPServerMsgHandler {
		@Override
		public byte[] messageHandler(String addr, byte[] message) {

			// System.out.println(String.valueOf(message));
			// oprl 移3位
			byte[] orpl = new byte[message.length - 3];
			System.arraycopy(message, 3, orpl, 0, message.length - 3);
			// String noteStr = Util.getCurrentTime() + ":[" + addr + "]" +
			// "上报数据" + Util.formatBytesToStr(orpl);
			// System.out.println("hello bitmap" +
			// Util.formatBytesToStr(synParameter.getBitmap()));
			// System.out.println(noteStr);
			// char recvLevel =(char) message[2];
			int recvLevel = Integer.parseInt(String.valueOf(String.format("%02X", orpl[1])), 16);// String.valueOf(String.format("%02X",
			int hour = Integer.parseInt(String.valueOf(String.format("%02X", orpl[2])), 16);
			int minute = Integer.parseInt(String.valueOf(String.format("%02X", orpl[3])), 16);
			int second = Integer.parseInt(String.valueOf(String.format("%02X", orpl[4])), 16);

			System.out.println(Util.getCurrentTime() + " 同步状态:" + synStateFlag + " recvLevel:" + recvLevel + " " + hour
					+ ":" + minute + ":" + second);
			// System.out.println();
			if (!synStateFlag) {

			} else {
				switch (recvLevel) {
				case GlobalDefines.GlobalSynLevelConfig.G_SYN_CONFIG_LEVEL:// 请求
					// 发送给root
					try {
						seqCount = 0;

						CurrentTime currentTime = Util.getCurrentDateTime(Util.getCurrentDateTime());
						SendToRootSynMsg(Util.getSynMessage(seqCount, 0, currentTime.getHour(), currentTime.getMinute(),
								currentTime.getSecond(), synParameter.getPeriod(), synParameter.getBitmap()));

					} catch (IOException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
					break;
				case GlobalDefines.GlobalSynLevelConfig.G_SYN_CONFIG_INIT_LEVEL:// 请求
					try {
						CurrentTime currentTime = Util.getCurrentDateTime(Util.getCurrentDateTime());
						seqCount = Integer.parseInt(String.valueOf(String.format("%02X", orpl[0])), 16) + 1;
						SendToRootSynMsg(Util.getSynMessage(seqCount, 0, currentTime.getHour(), currentTime.getMinute(),
								currentTime.getSecond(), synParameter.getPeriod(), synParameter.getBitmap()));
						// seqCount ++;
					} catch (IOException e) {
						// TODO 自动生成的 catch 块
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
						// TODO 自动生成的 catch 块
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
	// 处理udp server收到的数据 ，添加到数据库，数据来源于无线网络内的所有节点,将数据直接传给TCP控制中枢
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

	// 处理udp server收到的数据 ，添加到数据库，数据来源于无线网络内的所有节点,将数据直接传给TCP控制中枢
	class UpperUdpMessageHandler implements NIOUDPServerMsgHandler {
		@Override
		public byte[] messageHandler(String addr, byte[] message) {
			String s = new String(message);
			// System.out.println("Upper Udp Message Handler message:" + s);//
			// for
			// System.out.println(addr+" "+ " "+ message.length + " "
			// +message[0]);
			// System.out.println(message[0]+""+""+message[1]+""+message[2]+""+message[3]+"");
			// System.out.println("s:"+s);
			byte[] command = Util.formatByteStrToByte(s);
			System.out.println("command[0] = " + command[0]); // log
			// byte[] command = Util.formatByteStrToByte(s);
			// System.out.println("Start command handler");// for log
			CommandHandler(command);
			System.out.println(Util.getCurrentTime() + " command handle over");// for
																				// log
			// System.out.println(s);
			System.out.println();
			return null;
		}
	}

	// 时间差计算
	public int time_diffence(int active) {
		int difference = 0;
		String currenttime = Util.getCurrentTime();
		String[] times = currenttime.split(":");
		int hour = Integer.parseInt(times[0]);
		int minute = Integer.parseInt(times[1]);
		int second = Integer.parseInt(times[2]);
		int minutes = minute % 10;
		int count = hour * 6 + minute / 10;
		int index = count / 8 - 1;
		int inbyte = count % 8;
		// System.out.println(hour+","+minute+","+count+","+index+","+inbyte);
		// System.out.println(bitmap[index]);
		// System.out.println(bitmap[index] >> inbyte);
		if ((bitmap[index] >> inbyte) % 2 == 1) {
			// System.out.println("true");
			return true;
		} else
			return false;
		return difference;
	}

	// 缓存发送
	public void cache_send(int cache, int has_return, String cacheCommand, byte[] com) throws IOException {
		int count = 0;
		String filename = null;
		if (has_return == 1) {
			try {
				count = SqlOperate.NetMonitor_count();
				SqlOperate.commandCache_a(cacheCommand);
				if (cache == 1) {
					// 状态时间差计算！！！！
				}
				TunSendToRootMessage(com);
				filename = Util.getCurrentTime() + "-App-return";
				SqlOperate.NetMonitor_count_out(count, filename);
				WriteFTPFile write = new WriteFTPFile();
				write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(),
						parameter.getftpPort(), filename);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 状态时间差计算！！！！
		} else if (has_return == 2) {
			try {
				count = SqlOperate.ApplicationData_count();
				SqlOperate.commandCache_a(cacheCommand);
				filename = Util.getCurrentTime() + "Net-return";
				if (cache == 1) {
					// 状态时间差计算！！！！
				}
				TunSendToRootMessage(com);
				SqlOperate.ApplicationData_count_out(count, filename);
				WriteFTPFile write = new WriteFTPFile();
				write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(),
						parameter.getftpPort(), filename);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (has_return == 3) {
			try {
				count = SqlOperate.ApplicationData_count();
				SqlOperate.commandCache_a(cacheCommand);
				if (cache == 1) {
					// 状态时间差计算！！！！
				}
				filename = "config.json";
				TunSendToRootMessage(com);
				WriteFTPFile write = new WriteFTPFile();
				write.upload(parameter.getftpuser(), parameter.getftpPwd(), parameter.getftphost(),
						parameter.getftpPort(), filename);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			if (cache == 1) {
				// 状态时间差计算！！！！
			}
			TunSendToRootMessage(com);
		}
	}

	// 上位机命令处理
	public byte[] CommandHandler(byte[] command) {
		// System.out.println(message);
		int send_to_net = command[0];
		int has_return = command[1];
		int command_length = command[2];
		byte[] com = new byte[command_length];
		System.arraycopy(command, 3, com, 0, command_length);
		String commands = Util.formatBytesToStr(com);
		String cacheCommand = Util.formatByteToByteStr(command);
		System.out.println("Command Handler:" + cacheCommand);// for log
		// command = cacheCommand.;

		// System.out.println(command[0]+" "+command[1]+" "+command[2]);// for
		// log
		if (send_to_net == 1) {
			boolean flag = Util.Online_Judge(synParameter.getBitmap());
			if (Util.StatusJuage(flag) == 1) {
				if (commands == "FFFFFFFF") {
					try {
						TunSendToRootMessage(com);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					// 状态时间差计算！！！！
					try {
						cache_send(1, has_return, cacheCommand, com);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else if (Util.StatusJuage(flag) == 2) {
				try {
					cache_send(1, has_return, cacheCommand, com);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (Util.StatusJuage(flag) == 3) {
				try {
					cache_send(1, has_return, cacheCommand, com);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (Util.StatusJuage(flag) == 4) {
				try {
					cache_send(0, has_return, cacheCommand, com);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (Util.StatusJuage(flag) == 5) {
				try {
					cache_send(1, has_return, cacheCommand, com);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} else {
			if (commands == "16") {// 测试通过
				getConcentratorID();
			} else if (commands == "17") {// 测试通过
				sendApplicationData(command);
			} else if (commands == "18") {// 测试通过
				sendProcessLog();
			} else if (commands == "19") {// 测试通过
				try {
					getProcessState();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (commands == "20") {
				sendtopoBefore(command);
			} else if (commands == "21") {
				sendCommandBefore();
			} else if (commands == "22") {
				try {
					restartConcentrator();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {// if***
				// System.out.println(cacheCommand);
			}
		}
		return null;
	}

	// 获取进程状态信息2 测试通过
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

	// 获取集中器ID3 测试通过
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

	// 发送历史网络检测 测试通过
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

	// 发送历史应用数据5 //测试通过
	public byte[] sendApplicationData(byte[] command) {

		int day_length = (command[1] << 8 | command[2]);
		System.out.println(Util.getCurrentTime() + " Send appdata to Remote server(" + day_length + "):"
				+ Util.formatByteToByteStr(command));// for log
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

	// 发送进程日志6 测试通过
	public byte[] sendProcessLog() {
		// org.apache.log4j.BasicConfigurator.configure();
		try {
			WriteFTPFile write = new WriteFTPFile();
			String UploadFile = "ssh.py";
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

	// 发送指令历史7
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

	// 重启集中器1
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

	// 处理udp server收到的数据 ，添加到数据库，数据来源于无线网络内的所有节点,将数据直接传给TCP控制中枢
	public void sent_message(String addr, byte[] message) {
		// System.out.println("nodes final:");
		// String notes = Util.getCurrentTime() + ":[" + addr + "]" + "上传数据:" +
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
				System.out.println("多播配置心跳参数" + addr);
				paramConfigProxy.setConfigAck(addr);
			} else {
				byte type = message[1];// 0 是 globaltype 1 才是 type
				String[] nodesIP = addr.split(":");
				message = Arrays.copyOfRange(message, 2, message.length);
				switch (type) {
				// case GlobalDefines.GlobalCmd.G_DEF_READ_DATA:// 多播
				case GlobalDefines.GlobalCmd.G_DEF_READ_DATA:// 多播
					// 添加数据到应用数据表
					System.out.println(Util.getCurrentTime() + " Appdata:" + nodesIP[nodesIP.length - 1]);
					SqlOperate.ApplicationData_a(nodesIP[nodesIP.length - 1], Util.getCurrentTime(),
							Util.formatByteToByteStr(message));
					break;
				case GlobalDefines.GlobalCmd.G_DEF_CTL_ACK_READ_DATA:// 多播
					// 添加数据到应用数据表
					SqlOperate.ApplicationData_a(nodesIP[nodesIP.length - 1], Util.getCurrentTime(),
							Util.formatByteToByteStr(message));
					if (remoteClient.remoteHostIsOnline()) {
						try {
							remoteClient.asyncWriteAndFlush(formatDataToJsonStr("app", addr, "1"));
						} catch (Exception e) {
							// TODO 自动生成的 catch 块
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
							// System.out.println("已经存储过IP和ID的对应关系");
							if (IpidMap.get(Ip).equals(Id)) {
								// System.out.println("而且对应的ID没有发生改变");
							} else {
								// System.out.println("对应的ID发生了改变，重新上报对应关系");
								remoteClient.asyncWriteAndFlush(formatDataToJsonStr("ipidmatchup", addr, Id));
								IpidMap.put(Ip, Id);
							}
						} else {
							// System.out.println("之前没有存储过IP和ID的对应关系，上报对应关系");
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
							// TODO 自动生成的 catch 块
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
		// ConsoleMainServer main =
		// SqlOperate.connect("jdbc:sqlite:/root/build_jar/topo3.db");
		SqlOperate.connect("jdbc:sqlite:topo3.db");
		System.out.println("123");
		// sendProcessLog();
		// "2017-03-26 19:58:49"
		// sendProcessLog();
		new ConsoleMainServer();
	}
}

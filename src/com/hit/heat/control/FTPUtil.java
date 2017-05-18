 
package com.hit.heat.control;
import java.io.IOException;
//import java.io.InputStream;
import java.net.SocketException;
//import java.util.Properties;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import com.hit.heat.util.Util;
//import org.apache.log4j.Logger;

public class FTPUtil {
//	private static Logger logger = Logger.getLogger(FTPUtil.class);

	
	
	/**
	 * 获取FTPClient对象
	 * @param ftpHost FTP主机服务器
	 * @param ftpPassword FTP 登录密码
	 * @param ftpUserName FTP登录用户名
	 * @param ftpPort FTP端口 默认为21
	 * @return
	 */
	public static FTPClient getFTPClient(String ftpHost, String ftpPassword,
			String ftpUserName, int ftpPort) {
		FTPClient ftpClient = null;
		try {
			ftpClient = new FTPClient();
			ftpClient.connect(ftpHost, ftpPort);// 连接FTP服务器
			ftpClient.login(ftpUserName, ftpPassword);// 登陆FTP服务器
			if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
				System.err.println(Util.getCurrentTime()+" 未连接到FTP，用户名或密码错误。");
				ftpClient.disconnect();
			}
		} catch (SocketException e) {
			e.printStackTrace();
			System.err.println(Util.getCurrentTime()+" FTP的IP地址可能错误，请正确配置。");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(Util.getCurrentTime()+" FTP的端口错误,请正确配置。");
		}
		return ftpClient;
	}
}

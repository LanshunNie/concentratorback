package com.hit.heat.util;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/* @lhy Lhy
 * @date 2015年8月18日
 * @des  
 */
public class WriteDataToFile {
	private BufferedWriter out;
	public WriteDataToFile(String filepath) throws IOException{
		File file = new File(filepath);
		out = new BufferedWriter(new FileWriter(file,true));
	}
	
	public void append(String message) throws IOException{
		out.write(message + "\r\n");//ubuntu下\n
		out.flush();
	}
	
	public void close() throws IOException{
		//out.flush();
		out.close();
	}
	
	public void clearAll(String path) throws IOException{
		BufferedWriter tWriter =  new BufferedWriter(new FileWriter(new File(path),false));
		tWriter.write("");
		tWriter.close();
	}
	
//	public static void main(String[] args) throws IOException {
//		WriteDataToFile file = new WriteDataToFile("data.txt");
//		for(int i= 0;i<100;i++){
//			file.append("hello world" + i);
//		}
//		//file.close();
//		file.clearAll("data.txt");
//		file.append("hello world");
//	}
}

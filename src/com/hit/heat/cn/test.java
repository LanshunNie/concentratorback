package com.hit.heat.cn;

import java.io.IOException;

import org.jfree.ui.LengthAdjustmentType;

import com.hit.heat.data.SqlOperate;
import com.hit.heat.model.Command;
import com.hit.heat.util.BitMap;
import com.hit.heat.util.Util;

public class test {
	public static boolean Online_Judge(byte[] bitmap) {
		String datetime = Util.getCurrentDateTime();
		String[] times = datetime.split(":");
		int hour = Integer.parseInt(times[0]);
		int minute = Integer.parseInt(times[1]);
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
	}

	public int StatusJuage(boolean online) {
		String datetime = Util.getCurrentDateTime();
		String[] times = datetime.split(":");
		// int hour = Integer.parseInt(times[0]);
		int minute = Integer.parseInt(times[1]);
		int second = 0;
		int time = second + minute * 60;
		int flag = 0;
		if (online) {
			if (time < 20)
				flag = 1;
			else if (time > 20 && time < 300)
				flag = 2;
		} else {
			if (time < 330)
				flag = 3;
			else if (time >= 330 && time < 480)
				flag = 4;
			else if (time >= 480 && time < 600)
				flag = 5;
		}
		return flag;
	}

	public int commandjiexi(boolean online) {
		int flag;
		byte[] command = new byte[10];
		boolean returntype = false;
		int send_to_net = command[0];
		int has_return = command[1];
		int command_length = command[2];
		byte[] com = new byte[command_length];
		System.arraycopy(command, 3, com, 0, command_length);
		String commands = Util.formatBytesToStr(com);
		return 0;
	}
	public static void change(){
		byte[] bitmap = new byte[3]; 
		byte[] bit = new byte[24];
		bitmap[0] = -1;
		bitmap[1] = 1;
		bitmap[2] = 5;
		int i ,j,t = 0;
		byte bitmap_a = 0;
		for (i = 0;i<3;i++){
			bitmap_a = bitmap[i];
			t = 0;
			for (j = 7;j>=0;j--){
				bit[t] = (byte) (bitmap_a & 1);
				bitmap_a = (byte) (bitmap_a >> 1);
				System.out.println(t +" "+bit[t]);
				t++;
			}
		}
	}

	public static void main(String[] args) throws IOException {
		change();
		//byte[] command = new byte[10];
//		command[0] = 0;
//		command[1] = 1;
//		System.out.println(command.length);
//		int len = command[1];
//		int second = 0;
//		int minute = 0;
//		int time = second + minute * 60;
//		int flag = 0;
//		if (command[0] == 0) {
//			if (time < 20)
//				flag = 1;
//			else if (time > 20 && time < 300)
//				flag = 2;
//		} else if (command[0] == 1) {
//			if (time < 330)
//				flag = 3;
//			else if (time >= 330 && time < 480)
//				flag = 4;
//			else if (time >= 480 && time < 600)
//				flag = 5;
//		}
	}
}

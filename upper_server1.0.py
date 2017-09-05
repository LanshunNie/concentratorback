import socket,traceback,time,struct
import sys
reload(sys)
sys.setdefaultencoding('utf8')
host=''
port=12400
s=socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
s.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
s.bind((host,port))
while 1: 
    try:
        message,address=s.recvfrom(8192)
        #arry = message.encode('utf-8')
        print(message)
        a,b,c,d,e,f,g,h,i,j,k = struct.unpack('3B1I2B1B1B2I1B',message)
        print(a,b,c,d,e,f,g,h,i,j,k)
        print(address)
        type = raw_input("please input the type:")
        print("the type is:",type)
        if type == "100":
            #detect report
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,-16,length,0,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "101":
            #net config report
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,-16,length,1,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "102":
            #debug
            times = time.strftime('%H:%M:%S',time.localtime(time.time()))
            print times
            secs = times+":0:17"
            length = len(secs)
            length_s = '%d'%length
            reply=struct.pack('5b'+length_s+'s1b',length+6,16,-96,length,2,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "140":
            #config the net
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,-96,length,64,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "141":
            #change report period
            period = raw_input("input the period:")
            if (int(period)<16):
                period_s = "0" + hex(int(period))[2:]
            else:
                period_s = hex(int(period))[2:]
            length = len(period_s)
            length_s = '%d'%length
            reply=struct.pack('5b'+length_s+'s1b',length+6,16,-96,length,65,period_s,0)
            print(period_s)
            s.sendto(reply,address)
        elif type == "142":
            #config schedule command
            times = time.strftime('%H:%M:%S',time.localtime(time.time()))
            print times
            schedule = raw_input("input the schedule:")
            secs = times+":"+schedule+":0:17"
            length = len(secs)
            length_s = '%d'%length
            reply=struct.pack('5b'+length_s+'s1b',length+6,16,-96,length,66,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "180":
            #meter command down
            secs='05105BFE5916'
            length = len(secs)
            reply=struct.pack('5b12s1b',length+6,16,-96,length,-128,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "181":
            #debug over
            times = time.strftime('%H:%M:%S',time.localtime(time.time()))
            print times
            schedule = raw_input("input the schedule:")
            secs = times+":"+schedule+":0:17"
            length = len(secs)
            length_s = '%d'%length
            reply=struct.pack('5b'+length_s+'s1b',length+6,16,-96,length,-127,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "182":
            #read meter command to flash
            secs='105BFE5916'
            length = len(secs)

            reply=struct.pack('5b10s1b',length+6,16,-96,length,-126,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "1c0":
            #nodes restart
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,-96,length,-64,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "1c1":
            #reset
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,-96,length,-63,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "1c2":
            #syn
            times = time.strftime('%H:%M:%S',time.localtime(time.time()))
            print times
            schedule = raw_input("input the schedule:")
            period = raw_input("input the period:")
            state = raw_input("input the state:")
            secs = times+":"+schedule+":"+period+":"+state+":0:17"
            length = len(secs)
            length_s = '%d'%length
            reply=struct.pack('5b'+length_s+'s1b',length+6,16,-96,length,-62,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "1c3":
            #wake
            secs = ""
            length = len(secs)

            reply=struct.pack('5b0s1b',length+6,16,-96,length,-61,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "1c4":
            #check wake
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,-96,length,-60,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "010":
            #data return
            begin_time = raw_input("input the begin time:")
            length = len(begin_time)
            reply=struct.pack('5b19s1b',length+6,16,112,length,16,begin_time,0)
            print(begin_time)
            s.sendto(reply,address)
        elif type == "000":
            #change heartbeat period
            period = raw_input("input the period:")
            length = len(period)
            length_s = '%d'%length
            reply=struct.pack('5b'+length_s+'s1b',length+6,16,48,length,0,period,0)
            s.sendto(reply,address)
        elif type == "003":
            #supervisor status
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,48,length,3,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "004":
            #get supervisor log
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,112,length,4,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "005":
            #get concentratorback log
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,112,length,5,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "007":
            #get tunslip log
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,112,length,7,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "00a":
            #restart Concentrator
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,48,length,10,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "00c":
            #restart Concentratorback
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,48,length,12,secs,0)
            print(secs)
            s.sendto(reply,address)
        elif type == "00d":
            #restart tunslip
            secs = ""
            length = len(secs)
            reply=struct.pack('5b0s1b',length+6,16,48,length,13,secs,0)
            print(secs)
            s.sendto(reply,address)
        else:
            #message,address=s.recvfrom(8192)
            #arry = message.encode('utf-8')
            print(message)
        time.sleep(15)
    except (KeyboardInterrupt,SystemExit):
        raise
    except:
        traceback.print_exc()




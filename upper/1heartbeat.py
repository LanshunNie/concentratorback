#coding=utf-8
import socket,traceback,time,struct
import sys
import os
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
        tem='%d' %address[1]
        print(address[0]+' '+tem)

        os.remove('address.txt')
        fp = open("address.txt",'w')
        fp.write(address[0]+' '+tem)
        fp.close()
        time.sleep(15)
    except (KeyboardInterrupt,SystemExit):
        raise
    except:
        traceback.print_exc()




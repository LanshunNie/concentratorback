#coding=utf-8
import socket,traceback,time,struct
import sys
reload(sys)
sys.setdefaultencoding('utf8')
host=''
port=12400
s=socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
s.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
s.bind((host,port))
ready = raw_input("read to go ?:")
if (ready == 'y'):
    f = open("address.txt")
    addString = f.read()
    add_port = addString.split()
    add = add_port[0];
    portb = int(add_port[1])
    address =(add,portb)
    f.close()

    lines = raw_input("input the log lines you want:")
    length = len(lines)
    length_s = '%d'%length
    reply=struct.pack('5b'+length_s+'s1b',length+6,16,112,length,7,lines,0)

    s.sendto(reply,address)
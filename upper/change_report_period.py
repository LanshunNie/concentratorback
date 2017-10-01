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
    
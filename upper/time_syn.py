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
    times = time.strftime('%H:%M:%S',time.localtime(time.time()))
    print times
    schedule_num = raw_input("input the schedule number(7 for other):")
    if schedule_num == "0":
        schedule =="-86,-86,-86,-86,-86,-86,-86,-86,-86,-86,-86,-86,-86,-86,-86,-86,-86,-86"
    elif schedule_num == "1"：
        schedule =="-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1"
    elif schedule_num == "2":
        schedule =="-128,0,0,0,8,0,0,0,-128,0,0,0,8,0,0,0"
    elif schedule_num == "7":
        schedule = raw_input("input the schedule :")
    period = raw_input("input the period:")
    state = raw_input("input the state:")
    secs = times+":"+schedule+":"+period+":"+state+":0:17"
    length = len(secs)
    length_s = '%d'%length
    reply=struct.pack('5b'+length_s+'s1b',length+6,16,-96,length,-62,secs,0)
    print(secs)
    s.sendto(reply,address)
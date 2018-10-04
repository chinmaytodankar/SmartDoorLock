import psycopg2
from psycopg2 import Error
from Adafruit_IO import MQTTClient as mqtt
import RPi.GPIO as GPIO
import time
import datetime
import socket

userName = "ChinmayTodankar"
aioKey = "70baad8ab6704f28980fa730936411aa"
subFeedName = "mca_logincheck"
pubFeedName = "mca_loginresponse"
doorFeed = "doorstat"
addUserFeed = "adduser"
viewLogFeed = "viewlog"
ip = [l for l in ([ip for ip in socket.gethostbyname_ex(socket.gethostname())[2] if not ip.startswith("127.")][:1], [[(s.connect(('8.8.8.8', 53)), s.getsockname()[0], s.close()) for s in [socket.socket(socket.AF_INET, socket.SOCK_DGRAM)]][0][1]]) if l][0][0]
client = mqtt(userName,aioKey)
client.connect()
servo = 22
GPIO.setmode(GPIO.BOARD)
GPIO.setup(servo,GPIO.OUT)
curDoorStat = False
prevDoorStat = False
fireSensor = 38
GPIO.setmode(GPIO.BOARD)
GPIO.setup(fireSensor,GPIO.IN)

floodSensor = 40
GPIO.setmode(GPIO.BOARD)
GPIO.setup(floodSensor,GPIO.IN)

p=GPIO.PWM(servo,50)# 50hz frequency
doorStat = False
p.start(2.5)# starting duty cycle ( it set the servo to 0 degree )
def connectDb():
    conn = psycopg2.connect(user="pi",
                                password="POPROTOTYPE98",
                                host=ip,
                                port="5432",
                                database = "MCA_PROJ")
    cur = conn.cursor()
    return conn,cur

def doorChange(n):
    global curDoorStat
    global prevDoorStat
    if prevDoorStat != curDoorStat:
        print("DoorChange",end="")
    if n:
        curDoorStat = True
        p.ChangeDutyCycle(6)
        
    elif not n:
        curDoorStat = False
        p.ChangeDutyCycle(11)
    prevDoorStat = curDoorStat
        
def connected(client):
    client.subscribe(subFeedName)
    client.subscribe(doorFeed)
    client.subscribe(addUserFeed)

def updateDatabase(usr,cur):
    ts = time.time()
    timeStamp = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
    cur.execute("""SELECT fname,lname FROM logincreds WHERE userId = '{}'""".format(usr))
    results = cur.fetchall()
    fname,lname = 0,0
    for result in results:
        fname,lname = result[0],result[1]
        print(fname,lname)
    cur.execute("""INSERT INTO log VALUES('{}',TIMESTAMP '{}','{}','{}');""".format(usr,timeStamp,fname,lname))
def checkUserPass(usr,pswd,matchCode):
    try:
        conn,cur = connectDb()
        cur.execute("""SELECT userId FROM logincreds""")
        results = cur.fetchall()
        flag = 0
        for result in results:
            if(usr == result[0]):
                flag = 1
        if(flag==0):
            print("Unkown User")
            client.publish(pubFeedName,"Unknown")
            return
        cur.execute("""SELECT pass FROM logincreds WHERE userid = '"""+usr+"""'""")
        results = cur.fetchall()
        if(pswd in results[:][0]):
            cur.execute("""SELECT adminrights FROM logincreds WHERE userid = '"""+usr+"""'""")
            results = cur.fetchall()
            print("Correct Password")
            global doorStat
            doorStatStr = ""
            if doorStat:
                doorStatStr = "ON"
            else:
                doorStatStr = "OFF"
            client.publish(pubFeedName,"Success,"+str(results[:][0][0])+","+matchCode+","+doorStatStr)
            updateDatabase(usr,cur)
            conn.commit()
        else:
            print("Incorrect Password")
            client.publish(pubFeedName,"NotSuccess")
        return
    except (Exception,psycopg2.DatabaseError) as error:
        GPIO.cleanup()
        if(conn):
            conn.rollback()
        print("Failed inserting record into mobile table {}".format(error))
    finally:
        if(conn):
            cur.close()
            conn.close()

def checkFireSensor():
    fireDetected = GPIO.input(fireSensor)
 #   print("Fire!!!!!!!!!")
    return not fireDetected

floodData = []
i = 0
def checkFloodSensor():
    floodDetected = GPIO.input(floodSensor)
    global floodData
    if(len(floodData)<10):
        floodData.append(floodDetected)
    else:
        global i
        floodData[i] = floodDetected
        i = (i+1)%10
        if(sum(floodData)==0):
          #  print("Flood!!!!!!!!!")
            return True
        else:
            return False
    return

def insertUser(usr,pswd,fname,lname,adminstat):
    try:
        conn,cur = connectDb()
        cur.execute("""InSERT INTO logincreds VALUES('{}','{}','{}','{}','{}')""".format(usr,fname,lname,pswd,adminstat))
        conn.commit()
        print("User Added")
        client.publish(pubFeedName,"UserCreated")
    except (Exception,psycopg2.DatabaseError) as error:
        GPIO.cleanup()
        if(conn):
            conn.rollback()
        print("Failed inserting record into mobile table {}".format(error))
    finally:
        if(conn):
            cur.close()
            conn.close()

def modifyVal(usr,col,val):
    try:
        conn,cur = connectDb()
        if(col == "delete"):
            cur.execute("""DELETE FROM logincreds WHERE userId = '{}' """.format(usr))
            print("user deleted")
        else:
            cur.execute("""UPDATE logincreds SET {} = '{}' WHERE userId = '{}'""".format(col,val,usr))
            print("value modified")
        conn.commit()
        client.publish(pubFeedName,"ChangesDone")
    except (Exception,psycopg2.DatabaseError) as error:
        GPIO.cleanup()
        if(conn):
            conn.rollback()
        print("Failed inserting record into mobile table {}".format(error))
    finally:
        if(conn):
            cur.close()
            conn.close()

def viewData():
    try:
        conn,cur = connectDb()
        cur.execute("""SELECT * FROM log ORDER BY accesstime DESC LIMIT 10""")
        results = cur.fetchall()
        payloadStr = []
        for result in results:
            payloadStr.append("{} {} accessed the system at {}".format(result[2],result[3],result[1]))
        for payload in payloadStr:
            client.publish(viewLogFeed,payload)
            time.sleep(0.03)
        print("Log Sent")
    except (Exception,psycopg2.DatabaseError) as error:
        GPIO.cleanup()
        if(conn):
            conn.rollback()
        print("Failed inserting record into mobile table {}".format(error))
    finally:
        if(conn):
            cur.close()
            conn.close()

def msgReceived(client,id1,msg,retain=True):
    if id1 == subFeedName :
        if(msg == "viewData"):
            viewData()
        else :
            username,password,matchCode = msg.split(",")
            print(username,password)
            checkUserPass(username,password,matchCode)
    elif id1 == doorFeed :
        a = False
        global doorStat
        if msg == "ON":
            a = True
            doorStat = True
        else:
            a = False
            doorStat = False
        doorChange(a)
        print(msg)
    elif id1 == addUserFeed:
        if(len(list(msg.split(","))) == 5):
            usr,pswd,fname,lname,adminstat = msg.split(",")
            print(usr,pswd,fname,lname,adminstat)
            insertUser(usr,pswd,fname,lname,adminstat)
        elif(len(list(msg.split(","))) == 3):
            usr,column,val = msg.split(",")
            print(usr,column,val)
            modifyVal(usr,column,val)
    

def disconnected(client):
    print("Disconnected.")
    client.connect()
    
client.on_connect = connected
client.on_message = msgReceived
client.on_disconnect = disconnected
client.loop_background()
doorChange(0)
client.publish(doorFeed,"OFF")
firstPub = True
while True:
    isFire = checkFireSensor()
    isFlood = checkFloodSensor()
    print(" isFire : ",isFire," isFlood : ",isFlood)
    if((isFire or isFlood) and firstPub):
        firstPub = False
        doorChange(1)
        client.publish(doorFeed,"ON")
    elif(not isFire and not isFlood):
        if not firstPub:
            firstPub = True
            if not curDoorStat:
                doorChange(0)
                client.publish(doorFeed,"OFF")

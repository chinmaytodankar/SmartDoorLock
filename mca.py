import psycopg2
from psycopg2 import Error
from Adafruit_IO import MQTTClient as mqtt
import time
import datetime

userName = "ChinmayTodankar"
aioKey = "70baad8ab6704f28980fa730936411aa"
subFeedName = "mca_logincheck"
pubFeedName = "mca_loginresponse"
doorFeed = "doorstat"
addUserFeed = "adduser"

client = mqtt(userName,aioKey)
client.connect()

def connected(client):
    print("Connected ")
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
def checkUserPass(usr,pswd):
    try:
        conn = psycopg2.connect(user="pi",
                                password="POPROTOTYPE98",
                                host="192.168.137.215",
                                port="5432",
                                database = "MCA_PROJ")
        cur = conn.cursor()
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
            client.publish(pubFeedName,"Success,"+str(results[:][0][0]))
            updateDatabase(usr,cur)
            conn.commit()
        else:
            print("Incorrect Password")
            client.publish(pubFeedName,"NotSuccess")
        return
    except (Exception,psycopg2.DatabaseError) as error:
        if(conn):
            conn.rollback()
        print("Failed inserting record into mobile table {}".format(error))
    finally:
        if(conn):
            cur.close()
            conn.close()
def insertUser(usr,pswd,fname,lname,adminstat):
    try:
        conn = psycopg2.connect(user="pi",
                                password="POPROTOTYPE98",
                                host="192.168.137.215",
                                port="5432",
                                database = "MCA_PROJ")
        cur = conn.cursor()
        cur.execute("""InSERT INTO logincreds VALUES('{}','{}','{}','{}','{}')""".format(usr,fname,lname,pswd,adminstat))
        conn.commit()
        print("User Added")
        client.publish(pubFeedName,"UserCreated")
    except (Exception,psycopg2.DatabaseError) as error:
        if(conn):
            conn.rollback()
        print("Failed inserting record into mobile table {}".format(error))
    finally:
        if(conn):
            cur.close()
            conn.close()

def msgReceived(client,id1,msg,retain=True):
    if id1 == subFeedName :
        username,password = msg.split(",")
        print(username,password)
        checkUserPass(username,password)
    elif id1 == doorFeed :
        
        print(msg)
    elif id1 == addUserFeed:
        usr,pswd,fname,lname,adminstat = msg.split(",")
        print(usr,pswd,fname,lname,adminstat)
        insertUser(usr,pswd,fname,lname,adminstat)
    

def disconnected(client):
    print("Disconnected.")
    sys.exit(1)
client.on_connect = connected
client.on_message = msgReceived
client.on_disconnect = disconnected
client.loop_background()

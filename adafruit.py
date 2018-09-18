from Adafruit_IO import MQTTClient as mqtt
import time

userName = "ChinmayTodankar"
aioKey = "70baad8ab6704f28980fa730936411aa"
feedName = "mcafeed"
feedKey = "mcafeed"

client = mqtt(userName,aioKey)
client.connect()

def connected(client):
    print("Connected ")
    client.subscribe(feedName)

def msgReceived(client,id1,msg,retain=True):
    data = msg
    print("Data : ",end="")
    print(data)

def disconnected(client):
    print("Disconnected.")
    sys.exit(1)
client.on_connect = connected
client.on_message = msgReceived
client.on_disconnect = disconnected
client.loop_background()

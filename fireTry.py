import RPi.GPIO as GPIO

fireSensor = 38
GPIO.setmode(GPIO.BOARD)
GPIO.setup(fireSensor,GPIO.IN)

floodSensor = 40
GPIO.setmode(GPIO.BOARD)
GPIO.setup(floodSensor,GPIO.IN)

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


while True:
    global floodData
    print("Flood ",checkFloodSensor()," ",floodData," Fire ",checkFireSensor())
    

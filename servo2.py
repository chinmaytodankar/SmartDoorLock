import RPi.GPIO as GPIO
import time

servo = 22
GPIO.setmode(GPIO.BOARD)
GPIO.setup(servo,GPIO.OUT)
p=GPIO.PWM(servo,50)# 50hz frequency

p.start(2.5)# starting duty cycle ( it set the servo to 0 degree )

def doorChange(n):
    if n:
        p.ChangeDutyCycle(6)
    elif not n:
        p.ChangeDutyCycle(11)
    
try:
    while True:
        a = bool(input())
        doorChange(a)
           
except KeyboardInterrupt:
    GPIO.cleanup()

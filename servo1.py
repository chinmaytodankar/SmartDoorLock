import RPi.GPIO as GPIO
import time

control = [5,5.5,6,6.5,7,7.5,8,8.5,9,9.5,10]

servo = 22
inPin = 8
GPIO.setmode(GPIO.BOARD)

GPIO.setup(servo,GPIO.OUT)
GPIO.setup(inPin,GPIO.IN)
# in servo motor,
# 1ms pulse for 0 degree (LEFT)
# 1.5ms pulse for 90 degree (MIDDLE)
# 2ms pulse for 180 degree (RIGHT)

# so for 50hz, one frequency is 20ms
# duty cycle for 0 degree = (1/20)*100 = 5%
# duty cycle for 90 degree = (1.5/20)*100 = 7.5%
# duty cycle for 180 degree = (2/20)*100 = 10%

p=GPIO.PWM(servo,50)# 50hz frequency

p.start(2.5)# starting duty cycle ( it set the servo to 0 degree )


try:
    while True:
        if(GPIO.input(inPin)):
            p.ChangeDutyCycle(5)
            print "1"
        else:
            p.ChangeDutyCycle(10)
            print "0"
           
except KeyboardInterrupt:
    GPIO.cleanup()

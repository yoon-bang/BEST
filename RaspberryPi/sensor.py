import datetime
import adafruit_dht as dht
from board import *
import time
from datetime import datetime
import requests

SENSOR_PIN = D4
POS = "TH0x"

url = "http://146.148.59.28:5000/detection"
headers = {'Content-Type' : 'application/json', 'charset' : 'utf-8'}

while True:
    try:
        #get temperature and humidity data from sensor, and send it to server via HTTP Request
        now = datetime.now()
        dht_device = dht.DHT11(SENSOR_PIN, use_pulseio = 0)
        t = dht_device.temperature
        h = dht_device.humidity
        #Which raspberry pie is sending this info? Which cell is affected by this?
        jsonData = {
            #'time' : str(now),
            'temperature' : str(t),
            'humidity' : str(h)
        }
        response = requests.post(url, json = jsonData, headers = headers)
        data = str(now)+ " Temperature: "+str(t)+", humidity: "+str(h)+"\n"
        print(data)
        #in every 60 sec
        time.sleep(60)
    except:
        #When sensor cannot send signal or cannot get data from sensor, retry.
        print("Error")
        

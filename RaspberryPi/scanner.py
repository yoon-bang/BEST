import ScanUtility
import bluetooth._bluetooth as bluez
import datetime
import time
import requests
from datetime import datetime

POS = "TH0x"
dev_id = 0 #Set bluetooth device. Default 0.
url = "http://146.148.59.28:5000/check_status"
headers = {'Content-Type' : 'application/json', 'charset' : 'utf-8'}

try:
    sock = bluez.hci_open_dev(dev_id)
    print("\n*** Looking for BLE Beacons ***\n")
    print("\n*** CTRL-C to Cancel ***\n")
except:
    print("Error accessing bluetooth")

ScanUtility.hci_enable_le_scan(sock)


#Actual program starts in here.
#1. When it dies sends notification
#2. When it revive sends notification
dic = {} #Key: Major(Always 0) + Minor(two digits number if it's one digit number put zero ahead)

def checkAlive():
    for i in range(1):
        for j in range(1, 23):
            str_key = str(i) + str(j).zfill(2)
            if str_key in dic and dic[str_key] > 0:
                #print(str_key + " is good")
                dic[str_key] = 0
            else:
                print(str_key, " died")
                jsonData = {
                    #'time' : str(datetime.now()),
                    'beacon' : str_key,
                    'availability' : 0
                }
                dic[str_key] = -1
                response = requests.post(url, json = jsonData, headers = headers)
                #FiX this point

try:
    start = time.time()
    while True:
        #Repeat it every 10 seconds. Collect the beacon signal for 10 seconds.
        if time.time() - start > 10:
            checkAlive()
            start = time.time()
        else:
            returnedList = ScanUtility.parse_events(sock, 10)
            for item in returnedList:
                #if uuid is what we want, save major and minor number, and send to server.
                if item['uuid'] == '020012ac-4202-649d-ec11-b6cbc8814ad7':
                    str_key = str(item['major']) + str(item['minor']).zfill(2)
                    if str_key in dic:
                        #if the beacon which was treated as dead, send signal, make it alive again.
                        if dic[str_key] == -1:
                            print(str_key, " revived")
                            jsonData = {
                                #'time' : str(datetime.now()),
                                'beacon' : str_key,
                                'availability' : 1
                            }
                            response = requests.post(url, json=jsonData, headers = headers)
                            dic[str_key] = 1
                        else:
                            dic[str_key] += 1
                    else:
                        print(str_key, "detected")
                        dic[str_key] = 1

except KeyboardInterrupt:
    pass
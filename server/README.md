# Server Development
## main.py
Initiate the program

## pie_flask.py

### 1. Receives datas from Raspberry Pi and ESP32. If temperature exceed threshold, server runs to the fire situation.

```
Usage: POST request to '/get_sensor_data'
Input: JSON
{
  'id': int(device ID)
  'temperature': int(temperature)
  'humidity': int(humidity)
}
Output: None (200)
```
### 2. Receive status of beacon from Raspberry Pi, and change the status of cells if needed
```
Usage: POST request to '/get_status_data'
Input: JSON
{
  'beacon': int(beaconID)
}
Output: None (200)
```

## Notification.py
### Sends Notification to mobile device via Firebase Cloud Message


7. main.py: initialize cells in q_learnig, run pie_flask and mobile_flask via process.
8. pie_flask.py: Receive datas from raspberry pie and ESP32, update cells in main.py. If fre occurs Send notification to mobile application.
9. q_learning.py: Module for Q learning. pie_flask.py calls update_disaster_area() and mobile_flask.py calls update_congested_area().
10. mobile_flask.py: Receive location from mobile application and send it back evacuation path.
11. user.py: Class definition for User in mobile_flask.py.
12. dummy_connection.py: SocketIO connection test.
13. notification.py: Sends Firebase Cloud Message to mobile application.

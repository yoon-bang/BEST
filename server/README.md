1. main.py: initialize cells in q_learnig, run pie_flask and mobile_flask via process.
2. pie_flask.py: Receive datas from raspberry pie and ESP32, update cells in main.py. If fre occurs Send notification to mobile application.
3. q_learning.py: Module for Q learning. pie_flask.py calls update_disaster_area() and mobile_flask.py calls update_congested_area().
4. mobile_flask.py: Receive location from mobile application and send it back evacuation path.
5. user.py: Class definition for User in mobile_flask.py.
6. dummy_connection.py: SocketIO connection test.
7. notification.py: Sends Firebase Cloud Message to mobile application.

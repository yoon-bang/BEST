import notification
from q_learning import *
from flask import Flask, request

# Due to the limit temperature that DHT11 capture, temperature_threshold is limited to 50.
# If using DHT22 or better sensor, this value may be changed.
app = Flask(__name__)
on_fire = False
temperature_threshold = 50
humidity_threshold = 20
cells_related_to_pie = [
	["A01", "A02"], #cells related to pie 0.
	["B01", "B02"]  #cells related to pie 1.
	#...
]
cells_related_to_beacon = {}
cells_related_to_beacon["001"] = ["A01", "A02"] #cells related to beacon 001
cells_related_to_beacon["002"] = ["B01", "B02"] #cells related to beacon 002
#...

def pie_flask_main():
	app.run(debug = True, host = "0.0.0.0", port = 5000)

def fire_detected():
	notification.send_notification()

def main():
	pie_flask_main()

# Receive temperature and humidity data from sensor.py and change the cells state if needed.
# Input data: JSON {Raspberry Pi ID, Temperature, Humidity}
# Returns nothing.
@app.route("/get_sensor_data", methods = ["POST"])
def post_sensor_data():
	data = request.get_json()
	pie_id = int(data["id"])
	temperature = int(data["temperature"])
	humidity = int(data["humidity"])
	print(pie_id, temperature, humidity)
	if temperature >= temperature_threshold or humidity <= humidity_threshold:
		#Update cells state
		for cell in cells_related_to_pie[pie_id]:
			cells[cell].update_fire_cell()
		#If this is the first occurence of fire, call fire_detected() and set server as on_fire mode
		if not on_fire: 
			on_fire = True
			fire_detected()

# Receive status of beacon from scanner.py and change the cells state if needed. 
# Input data = JSON {beacon ID number}
# Returns nothing
@app.route("/get_status_data", methods = ["POST"])
def post_status_data():
	data = request.get_json()
	beacon = data["beacon"]
	for cell in cells_related_to_beacon[beacon]:
		cells[cell].update_fire_cell()
	if not on_fire:
		on_fire = True
		fire_detected()

if __name__ == "__main__":
	main()

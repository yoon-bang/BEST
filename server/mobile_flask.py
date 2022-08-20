import random
from user import *
from q_learning import *
from threading import Thread
from flask import Flask, request
from flask_socketio import SocketIO

app = Flask(__name__)
socketio = SocketIO(app)
users = {} #sid to User

def path_finder(location):
	number_of_simulations = 250
	E01 = Agent(cells["E01"], number_of_simulations)
	E02 = Agent(cells["E02"], number_of_simulations)
	E03 = Agent(cells["E03"], number_of_simulations)
	S01 = Agent(cells["S01"], number_of_simulations)
	exits = [E01, E02, E03, S01]
	for exit in exits:
		exit.clear_table()
		exit.q_learn()
	starting_point = cells[location]
	E01_path = E01.get_evacuation_path(starting_point)
	E02_path = E02.get_evacuation_path(starting_point)
	E03_path = E03.get_evacuation_path(starting_point)
	S01_path = S01.get_evacuation_path(starting_point)
	E01_efficiency = efficiency_of_path(E01_path)
	E02_efficiency = efficiency_of_path(E02_path)
	E03_efficiency = efficiency_of_path(E03_path)
	S01_efficiency = efficiency_of_path(S01_path)
	dic = {}
	dic[E01_efficiency] = E01_path
	dic[E02_efficiency] = E02_path
	dic[E03_efficiency] = E03_path
	dic[S01_efficiency] = S01_path
	lst = [E01_efficiency, E02_efficiency, E03_efficiency, S01_efficiency]
	lst.sort()
	slow = lst[2]
	fast = lst[3]
	whole = fast + slow
	rand = random.randint(1, whole)
	key: int
	if rand <= slow:
		key = lst[2]
	else:
		key = lst[3]
	path = dic[key]
	return path

def mobile_flask_main():
	socketio.run(app, host = "0.0.0.0", port = 12000, debug = True)

def main():
	initialize_cells()
	mobile_flask_main()

@socketio.on("connect")
def connect(auth):
	user_id = request.sid
	location = auth["location"]
	new_user = User(user_id, location)
	users[user_id] = new_user
	path = path_finder(location)
	socketio.emit("path", path, room = user_id)
	print("Client {0} connected. Number of connected clients = {1}".format(user_id, len(users)))

@socketio.on("location")
def get_location(data):
	user_id = request.sid
	location = data
	previous_location = users[user_id].location
	cells[previous_location].number_of_people -= 1
	cells[previous_location].update_congested_area()
	cells[location].number_of_people += 1
	cells[location].update_congested_area()
	users[user_id].location = location

@socketio.on("disconnect")
def disconnect():
	user_id = request.sid
	del users[user_id]
	print("Client {0} disconnected. Number of connected clients = {1}".format(user_id, len(users)))

if __name__ == '__main__':
	main()

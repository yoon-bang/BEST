import random
import sys

cells = {}

class Cell():
	def __init__(self, id: str, cell_type: str):
		self.id = id
		self.cell_type = cell_type #E, R, C, S for Exit, Room, Cell, Stair
		self.adjacent_cells = []
		self.number_of_people = 0
		self.fire_cell = False
		self.fire_adjacent_cell = False
		self.congested_cell = False

	def update_congested_cell(self):
		if self.number_of_people >= 5:
			self.congested_cell = True
		else:
			self.congested_cell = False
	
	def update_fire_adjacent_cell(self):
		self.fire_adjacent_cell = True

	def update_fire_cell(self):
		self.fire_cell = True
		for cell in self.adjacent_cells:
			cell.update_fire_adjacent_cell()
	
	def undo_fire_cell(self):
		self.fire_cell = False
		for cell in self.adjacent_cells:
			cell.fire_adjacent_cell = False

	def get_reward(self, destination_cell):
			if self == destination_cell:
				return 1.0
			if self.fire_cell:
				return -0.8
			if self.fire_adjacent_cell or self.congested_cell:
				return -0.3
			else:
				return -0.01

def initialize_cells():
	#EXIT
	S01 = Cell("S01", "E"); cells["S01"] = S01
	E01 = Cell("E01", "E"); cells["E01"] = E01
	E02 = Cell("E02", "E"); cells["E02"] = E02
	E03 = Cell("E03", "E"); cells["E03"] = E03

	#ROOM
	R01 = Cell("R01", "R"); cells["R01"] = R01
	R02 = Cell("R02", "R"); cells["R02"] = R02
	R03 = Cell("R03", "R"); cells["R03"] = R03
	R04 = Cell("R04", "R"); cells["R04"] = R04
	R05 = Cell("R05", "R"); cells["R05"] = R05

	#STAIR
	S02 = Cell("S02", "S"); cells["S02"] = S02
	S04 = Cell("S04", "S"); cells["S04"] = S04
	S05 = Cell("S05", "S"); cells["S05"] = S05
	S06 = Cell("S06", "S"); cells["S06"] = S06
	S07 = Cell("S07", "S"); cells["S07"] = S07
	S08 = Cell("S08", "S"); cells["S08"] = S08
	S09 = Cell("S09", "S"); cells["S09"] = S09

	#CELL
	S03 = Cell("S03", "C"); cells["S03"] = S03
	H01 = Cell("H01", "C"); cells["H01"] = H01
	H02 = Cell("H02", "C"); cells["H02"] = H02
	U01 = Cell("U01", "C"); cells["U01"] = U01
	A01 = Cell("A01", "C"); cells["A01"] = A01
	A02 = Cell("A02", "C"); cells["A02"] = A02
	A03 = Cell("A03", "C"); cells["A03"] = A03
	A04 = Cell("A04", "C"); cells["A04"] = A04
	A05 = Cell("A05", "C"); cells["A05"] = A05
	A06 = Cell("A06", "C"); cells["A06"] = A06
	A07 = Cell("A07", "C"); cells["A07"] = A07
	A08 = Cell("A08", "C"); cells["A08"] = A08
	A09 = Cell("A09", "C"); cells["A09"] = A09
	A10 = Cell("A10", "C"); cells["A10"] = A10
	A11 = Cell("A11", "C"); cells["A11"] = A11

	#EXIT
	S01.adjacent_cells = [E01]
	E01.adjacent_cells = [R03, A01, S01, S02]
	E02.adjacent_cells = [S07, S08]
	E03.adjacent_cells = [A11]

	#ROOM
	R01.adjacent_cells = [A03]
	R02.adjacent_cells = [A01]
	R03.adjacent_cells = [E01]
	R04.adjacent_cells = [A01]
	R05.adjacent_cells = [A05]

	#STAIR
	S02.adjacent_cells = [E01, S03]
	S04.adjacent_cells = [H01, S03]
	S05.adjacent_cells = [S06, H01]
	S06.adjacent_cells = [S05, H02]
	S07.adjacent_cells = [E02, H02]
	S08.adjacent_cells = [E02, S09]
	S09.adjacent_cells = [S08, U01]

	#CELL
	S03.adjacent_cells = [S02, S04]
	H01.adjacent_cells = [S04, S05]
	H02.adjacent_cells = [A07, S07, S06]
	U01.adjacent_cells = [S09]
	A01.adjacent_cells = [E01, R02, R04, A02]
	A02.adjacent_cells = [A01, A03, A08]
	A03.adjacent_cells = [A04, A02, A08, R01]
	A04.adjacent_cells = [A05, A03, A08, A09]
	A05.adjacent_cells = [A09, A04, A06, R05]
	A06.adjacent_cells = [A05, A09, A07]
	A07.adjacent_cells = [H02, A10, A06, A09]
	A08.adjacent_cells = [A09, A04, A03, A02]
	A09.adjacent_cells = [A04, A05, A06, A07, A08, A10, A11]
	A10.adjacent_cells = [A11, A07, A09]
	A11.adjacent_cells = [E03, A10, A09]

class Agent():
	alpha = 0.1
	gamma = 0.8
	def __init__(self, destination_cell: Cell, number_of_simulations: int):
		self.reward_table = {}
		self.q_table = {}
		self.destination_cell = destination_cell
		self.number_of_simulations = number_of_simulations
		self.clear_table()
	
	def init_reward_table(self):
		for cell in cells.values():
			cell_to_value = {}
			for adj_cell in cell.adjacent_cells:
				cell_to_value[adj_cell] = adj_cell.get_reward(self.destination_cell)
			self.reward_table[cell] = cell_to_value

	def init_q_table(self):
		for cell in cells.values():
			cell_to_value = {}
			for adj_cell in cell.adjacent_cells:
				cell_to_value[adj_cell] = 0
			self.q_table[cell] = cell_to_value

	def clear_table(self):
		self.init_reward_table()
		self.init_q_table()

	def r(self, s: Cell, a: Cell):
		return self.reward_table[s][a]

	def q(self, s: Cell, a: Cell):
		next_state_max_q = max(self.q_table[a].values())
		self.q_table[s][a] = self.q_table[s][a] + self.alpha * (self.r(s, a) + self.gamma * next_state_max_q - self.q_table[s][a])
		return self.q_table[s][a]

	def get_max_cell(self, s: Cell):
		max = -sys.maxsize
		max_cell = None
		for cell in s.adjacent_cells:
			if self.q_table[s][cell] > max:
				max = self.q_table[s][cell]
				max_cell = cell
		return max_cell

	def q_learn(self):
		for _ in range(self.number_of_simulations):
			epsilon = 1 - _ / self.number_of_simulations
			curr_cell = random.choice(list(cells.values()))
			count = 0
			while curr_cell != self.destination_cell:
				if count > 10000:
					break
				count += 1
				rand_num = random.random()
				next_cell = None
				if rand_num < epsilon:
					next_cell = random.choice(curr_cell.adjacent_cells)
				else:
					next_cell = self.get_max_cell(curr_cell)
				self.q(curr_cell, next_cell)
				curr_cell = next_cell

	def get_evacuation_path(self, starting_cell: Cell):
		path = []
		curr_cell = starting_cell
		count = 0
		while curr_cell != self.destination_cell:
			if count > 500:
				return None
			count += 1
			path.append(curr_cell)
			curr_cell = self.get_max_cell(curr_cell)
		path.append(curr_cell)
		return path
	
def path_finder(path):
	if path == None:
		print("Path is not availiable")
		return
	for cell in path:
		print(cell.id, end = ' ')
	print("&", efficiency_of_path(path))

def efficiency_of_path(path):
	if path == None:
		return 0
	len_of_path = len(path)
	num_of_fire = 0
	num_of_adjacent = 0
	num_of_congested = 0	
	for cell in path:
		if cell.fire_cell:
			num_of_fire += 1
		elif cell.fire_adjacent_cell:
			num_of_adjacent += 1
		elif cell.congested_cell:
			num_of_congested += 1
	efficiency = 1 / (len_of_path + 15 * num_of_fire + 2 * num_of_adjacent + 2 * num_of_congested)
	return efficiency

def check_time(lst):
	count = 0
	while lst:
		all_done = True
		busy_door = False
		count += 1
		for i in range(len(lst)):
			if lst[i] == 0:
				continue
			elif lst[i] == 1:
				if not busy_door:
					busy_door = True
					lst[i] = 0
				else:
					all_done = False
			else:
				lst[i] -= 1
				all_done = False
		if all_done:
			break
	return count

def evacuation_simulator(multi_path, num_of_people):
	initialize_cells()
	number_of_simulations = 250
	cells["A01"].update_fire_cell()
	cells["A08"].update_congested_cell()
	cells["A09"].update_congested_cell()
	E01 = Agent(cells["E01"], number_of_simulations)
	E02 = Agent(cells["E02"], number_of_simulations)
	E03 = Agent(cells["E03"], number_of_simulations)
	S01 = Agent(cells["S01"], number_of_simulations)
	exits = [E01, E02, E03, S01]
	for exit in exits:
		exit.clear_table()
		exit.q_learn()

	sum = 0
	for i in range(20):
		E01_lst = []
		E02_lst = []
		E03_lst = []
		S01_lst = []
		for cell in cells.values():
			for i in range(num_of_people):
				E01_path = E01.get_evacuation_path(cell)
				E02_path = E02.get_evacuation_path(cell)
				E03_path = E03.get_evacuation_path(cell)
				S01_path = S01.get_evacuation_path(cell)
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
				key: int
				if multi_path:
					slow = lst[2]
					fast = lst[3]
					whole = fast + slow
					rand = random.randint(1, whole)
					if rand <= slow:
						key = lst[2]
					else:
						key = lst[3]
				else:
					key = lst[3]
				path = dic[key]
				goal = path[-1]
				if goal.id == "E01":
					E01_lst.append(len(path))
				elif goal.id == "E02":
					E02_lst.append(len(path))
				elif goal.id == "E03":
					E03_lst.append(len(path))
				else:
					S01_lst.append(len(path))
		e01_time = check_time(E01_lst)
		e02_time = check_time(E02_lst)
		e03_time = check_time(E03_lst)
		s01_time = check_time(S01_lst)
		lst = [e01_time, e02_time, e03_time, s01_time]
		lst.sort()
		sum += lst[3]
	result = sum / 20
	return result

def main():
	initialize_cells()
	cells["A01"].update_fire_cell()
	cells["A08"].update_congested_cell()
	number_of_simulations = 250
	E01 = Agent(cells["E01"], number_of_simulations)
	E02 = Agent(cells["E02"], number_of_simulations)
	E03 = Agent(cells["E03"], number_of_simulations)
	S01 = Agent(cells["S01"], number_of_simulations)
	exits = [E01, E02, E03, S01]
	for exit in exits:
		exit.clear_table()
		exit.q_learn()
	starting_point = cells["A02"]
	for exit in exits:
		path_finder(exit.get_evacuation_path(starting_point))

if __name__ == "__main__":
	main()
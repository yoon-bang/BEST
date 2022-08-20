import pie_flask
import mobile_flask
from q_learning import *
from threading import *

def main():
	try:
		initialize_cells()
		pie_process = Thread(target = pie_flask.pie_flask_main)
		mobile_process = Thread(target = mobile_flask.mobile_flask_main)
		pie_process.start()
		mobile_process.start()
		pie_process.join()
		mobile_process.join()

	except KeyboardInterrupt:
		pie_process.terminate()
		pie_process.join()
		mobile_process.terminate()
		mobile_process.join()

if __name__ == "__main__":
	main()
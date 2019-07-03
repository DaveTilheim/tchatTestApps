import socket as so
import threading as th
from time import sleep
from datetime import *
import os

interrupt = False
end_signal = '\\'
moment = datetime.now()
file_route = date.today().strftime("%b-%d-%Y")+"-"+str(moment.hour)+"-"+str(moment.minute)


class ClientThread(th.Thread):
	ID = 1
	def __init__(self, conn, adress, clients_list, conversation):
		th.Thread.__init__(self)
		self.conn = conn
		self.adress = adress
		self.conversation = conversation
		self.name = conn.recv(128).decode('utf8') + "#"+str(ClientThread.ID)
		
		with open("sessions/ips/ips"+file_route+".txt", "at") as f:
			f.write(self.name+": "+str(adress[0])+"\n")
			f.close()
		ClientThread.ID+=1
		self.connect = True
		self.clients_list = clients_list
		if len(self.name) == 0:
			self.name = 'client'
		print('new client: {}'.format(self.name))
		
		for conv in self.conversation:
			self.conn.sendall(conv.encode('utf8'))
			sleep(0.1)

		for c in self.clients_list:
			self.conn.sendall(('\a'+c.name).encode('utf8'))
			c.conn.sendall(('\a'+self.name).encode('utf8'))
			sleep(0.1)

	def run(self):
		while not interrupt:
			try:
				data = self.conn.recv(128).decode('utf8')
			except OSError:
				print('interrupt connection by server')
				return OSError
			if data == end_signal:
				break
			else:
				self.conversation.append(self.name+'\\'+data)
				for c in self.clients_list:
					c.conn.sendall((self.name+'\\'+data).encode('utf8'))
		for i in range(0, len(self.clients_list), 1):
			if self.clients_list[i] == self:
				del self.clients_list[i]
				break
		for c in self.clients_list:
			c.conn.sendall(('\v'+self.name).encode('utf8'))
		self.conn.sendall(end_signal.encode('utf8'))
		self.connect = False
		sleep(0.2)
		self.conn.close()
		print("self-down {}".format(self.name))

class Server:

	def __init__(self, port=5566, host='', typ=so.AF_INET, stream=so.SOCK_STREAM):
		self.socket = so.socket(typ, stream)
		self.socket.bind((host, port))
		self.port = port
		self.host = host
		self.typ = typ
		self.stream = stream
		self.client_threads = list()
		self.client_threads_static = list()
		self.conversation = list()

	def run_server(self):
		print("listening")
		while True:
			self.socket.listen(5)
			try:
				conn, adress = self.socket.accept()
			except KeyboardInterrupt:
				interrupt = True
				break
			client = ClientThread(conn, adress, self.client_threads, self.conversation)
			self.client_threads.append(client)
			self.client_threads_static.append(client)
			client.start()

	def down_server(self):
		for c in self.client_threads_static:
			if c.connect == True:
				print(c.name+" down")
				c.conn.sendall(end_signal.encode('utf8'))
				c.conn.close()
			c.join()
			print(c.name+" join")
		print("socket down")
		self.socket.close()
		with open("sessions/conv/conv"+file_route+".txt", "at") as f:
			for conv in self.conversation:
				f.write(conv)
			f.close()


server = Server(port=30000, host='10.222.2.2')
server.run_server()
server.down_server()



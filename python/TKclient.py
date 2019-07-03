import socket as so
import threading as th
import sys
from tkinter import *
from tkinter.messagebox import *
from tkinter.colorchooser import *
from time import sleep
from tkinter import filedialog

end_signal = '\\'
tk_close = False

class ClientInt(Tk):

	def __init__(self, host, socket=None):
		Tk.__init__(self)
		self.socket = socket
		self.id_frame = Frame(self)
		self.id_ip_label = Label(self.id_frame, text='server IP')
		self.id_ip_entry = Entry(self.id_frame, text='10.222.2.2', fg='green', justify=CENTER)
		self.id_ip_entry.insert(1, host)
		self.id_label = Label(self.id_frame, text="your username for this session", anchor=CENTER, fg='blue')
		self.id_entry = Entry(self.id_frame, width=15, justify=CENTER)
		self.id_button = Button(self.id_frame, text='send', command=None)
		self.pack_id_widgets()
		self.app_frame = Frame(self)
		self.app_conv_info_frame = Frame(self.app_frame)
		self.app_username_label = Label(self.app_conv_info_frame, text="client")
		self.app_conversation_frame = Frame(self.app_conv_info_frame)
		self.app_information_frame = Frame(self.app_conv_info_frame)
		self.app_information_label = Label(self.app_information_frame, text="Information", fg='orange')
		self.app_information_area = Listbox(self.app_information_frame)
		self.app_conversation_label = Label(self.app_conversation_frame, text="Tchat Box", fg='blue')
		self.app_textview_area = Listbox(self.app_conversation_frame, width=60)
		self.app_textarea_frame = Frame(self.app_conversation_frame)
		self.app_text_entry = Text(self.app_textarea_frame, height=4, width=60, bd=4, highlightbackground='grey', highlightcolor='cyan')
		self.app_send_text_button = Button(self.app_textarea_frame, text='send', height=4, command=self.send_data_to_conversation)
		self.app_color_frame = Frame(self.app_textarea_frame)
		self.text_color = Button(self.app_conversation_frame, text='text color', command=self.set_color_text_entry)
		self.conv_color = Button(self.app_conversation_frame, text='conv color', command=self.set_color_conv_entry)
	
	def set_color_text_entry(self):
		self.app_text_entry['fg'] = askcolor()[1]
	def set_color_conv_entry(self):
		self.app_textview_area['fg'] = askcolor()[1]


	def pack_app_widgets(self):
		self.app_frame.pack() 
		self.app_conv_info_frame.pack()
		self.app_username_label['text'] = self.socket.name
		self.app_username_label.pack()
		self.app_conversation_frame.pack(side=LEFT)
		self.app_information_frame.pack(side=RIGHT)
		self.app_information_label.pack()
		self.app_information_area.pack()
		self.app_conversation_label.pack()
		self.app_textview_area.pack()
		self.app_textarea_frame.pack()
		self.app_text_entry.pack(side=LEFT)
		self.app_send_text_button.pack(side=RIGHT)
		self.app_information_area.insert(1, "You: "+self.socket.name)
		self.app_color_frame.pack(side=BOTTOM)
		self.text_color.pack(side=LEFT)
		self.conv_color.pack(side=LEFT) 


	def pack_id_widgets(self):
		self.id_frame.pack()
		self.id_ip_label.pack()
		self.id_ip_entry.pack()
		self.id_label.pack()
		self.id_entry.pack()
		self.id_button.pack()

	def run(self):
		self.mainloop()

	def check_username(self):
		name = self.id_entry.get()
		if len(name) >= 25:
			name = name[0:25]
		if len(name) == 0:
			self.id_label['fg'] = 'red'
			self.id_label['text'] = 'NAME TOO SHORT'
			return False
		elif '#' in  name:
			self.id_label['fg'] = 'red'
			self.id_label['text'] = '\'#\' is a not allowed character'
		else:
			self.socket.name = name
			return True

	def unpack_id_widgets(self):
		self.id_button.pack_forget()
		self.id_entry.pack_forget()
		self.id_label.pack_forget()
		self.id_frame.pack_forget()

	def send_username(self):
		self.socket.socket.sendall(self.id_entry.get().encode('utf8'))

	def send_data_to_conversation(self):
		data = self.app_text_entry.get("1.0", END)
		if data == end_signal:
			self.app_text_entry['text'] = 'text unavailable'
		else:
			if len(data) >= 100:
				data = data[0:100]
			self.app_text_entry.delete('1.0', END)
			self.socket.write(data)


class ClientSocket:

	def __init__(self, interface, port=5566, host='localhost', typ=so.AF_INET, stream=so.SOCK_STREAM, name="client"):
		self.socket = None
		self.port = port
		self.host = host
		self.typ = typ
		self.stream = stream
		self.name = name
		self.interface = interface
		self.open = False
		self.read_thread = th.Thread(target=self.input_data_traitement)


	def connexion(self):
		try:
			so.gethostbyaddr(self.interface.id_ip_entry.get())
			self.host = self.interface.id_ip_entry.get()
		except:
			self.interface.id_ip_label['text'] = self.interface.id_ip_entry.get()+" not exists, try an other IP adress"
			self.interface.id_ip_label['fg'] = 'red'
			return None
		if self.interface.check_username() == True:
			self.socket = so.socket(self.typ, self.stream)
			try:
				self.socket.connect((self.host, self.port))
			except:
				self.interface.id_label['fg'] = 'red'
				self.interface.id_label['text'] = 'SERVER IS DOWN'
			else:
				print('connected')
				self.open = True
				self.interface.send_username()
				self.interface.unpack_id_widgets()
				self.interface.pack_app_widgets()
				self.read_thread.start()

	def send_end_signal(self):
		self.socket.sendall(end_signal.encode('utf8'))

	def deconnect(self):
		if self.open:
			self.send_end_signal()
			self.read_thread.join()
			self.socket.close()
			print("client deconnected")

	def send_msg(self, data):
		len_msg = data.find('\\')
		name = data[0:len_msg]
		if self.name == name[0:name.find('#')]:
			msg = "~~~~[ you ]~~~~\n"+data[len_msg+1:]
		else:
			msg = "~~~~[ "+name+" ]~~~~~\n"+data[len_msg+1:]
		msg = msg.split('\n')
		for m in msg:
			self.interface.app_textview_area.insert(END, m)
		self.interface.app_textview_area.see(END)


	def new_friend(self, friend_name):
		self.interface.app_information_area.insert(END, friend_name)
		self.interface.app_textview_area.insert(END, friend_name+"> has join the conversation")
		self.interface.app_textview_area.see(END)


	def del_friend(self, friend_name):
		for i in range(0, self.interface.app_information_area.size(), 1):
			if self.interface.app_information_area.get(i) == friend_name:
				self.interface.app_information_area.delete(i)
				self.interface.app_textview_area.insert(END, friend_name+"> has left the conversation\a")
				self.interface.app_textview_area.see(END)


	def input_data_traitement(self):
		data = str('data')
		global tk_close
		while data != end_signal:
			data = self.read()
			if data != end_signal and len(data):
				if data[0] == '\a':
					self.new_friend(data[1::])
				elif data[0] == '\v':
					self.del_friend(data[1::])
				else:
					self.send_msg(data)
					
		if not tk_close:
			self.lost_connection()

	def lost_connection(self):
		self.interface.app_username_label['text'] = 'CONNECTION TO SERVER LOST'
		self.interface.app_username_label['fg'] = 'red'
		self.interface.app_send_text_button['state'] = DISABLED
		self.interface.app_text_entry['state'] = DISABLED
		sleep(0.1)
		showinfo('user status', 'SERVER DISCONNECTED')

	def read(self):
		data = self.socket.recv(128).decode('utf8')
		return data

	def write(self, data='ACK'):
		self.socket.sendall(data.encode('utf8'))


class Client:

	def __init__(self, port=5566, host='localhost', typ=so.AF_INET, stream=so.SOCK_STREAM):
		self.interface = ClientInt(host)
		self.socket = ClientSocket(self.interface, port, host, typ, stream)
		self.socket_thread = None
		self.interface.socket = self.socket
		self.interface.id_button['command'] = self.socket.connexion

	def start(self):
		self.interface.run()
		global tk_close
		tk_close = True

	def down(self):
		self.socket.deconnect()



client = Client(port=30000, host='10.222.2.2')
client.start()
client.down()


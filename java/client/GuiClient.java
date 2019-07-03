import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.plaf.metal.MetalLookAndFeel;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;


public class GuiClient extends JFrame
{
	public Client client;
	public JPanel mainPanel;
	public MainToolBar mainToolBar;
	public FriendsStock mainFriendStock;
	public MainConvArea mainConvArea;
	public MainOptionConv mainOptionConv;

	public GuiClient()
	{
		super();
		try
		{
			UIManager.setLookAndFeel(new NimbusLookAndFeel());
		}
		catch(Exception e)
		{
			try
			{
				UIManager.setLookAndFeel(new MetalLookAndFeel());
			}
			catch(Exception ee)
			{
				System.exit(1);
			}
		}
		try
		{
			client = new Client("localhost", 1234, this);
			setSize(800,700);
			setWidgets();
			//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent e)
				{
					System.out.println("< disconnection >");
					client.disconnect();
					dispose();
					System.exit(0);
				}
			});
			setLocationRelativeTo(null);
			setResizable(true);
			new GuiConnexion();
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(null,"Server is down...","alert", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			System.exit(0);
		}
		if(client.connected == false) return;
		setPostWidgetsData();
		setVisible(true);
	}

	private void setWidgets()
	{
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainToolBar = new MainToolBar();
		mainConvArea = new MainConvArea();
		mainFriendStock = new FriendsStock();
		mainOptionConv = new MainOptionConv();
		mainPanel.add(mainToolBar, BorderLayout.NORTH);
		mainPanel.add(mainFriendStock, BorderLayout.WEST);
		mainPanel.add(mainOptionConv, BorderLayout.EAST);
		mainPanel.add(mainConvArea);
		add(mainPanel);
	}


	private void setPostWidgetsData()
	{
		mainFriendStock.setBorder(BorderFactory.createTitledBorder(client.data.getUsername()+"\'s friends"));
	}

	public void dialogServerDown()
	{
		JOptionPane.showMessageDialog(null,"Server is now down","external interruption", JOptionPane.ERROR_MESSAGE);
	}

	public void friendRequest()
	{
		System.out.println("----1----");
		System.out.println(this.mainToolBar);
		System.out.println(this.mainToolBar.friendRequestButton);
		System.out.println(this.client);
		this.mainToolBar.friendRequestButton.setText("friend requests("+Integer.toString(this.client.friendRequests.size())+")");
		System.out.println("----2----");
	}

	public void friendRequestFailed(String unknownFriend)
	{
		JOptionPane.showMessageDialog(null,unknownFriend+" not exists!","alert", JOptionPane.WARNING_MESSAGE);
	}

	public void friendRequestSuccess(String friend)
	{
		JOptionPane.showMessageDialog(null,"friend request has been sent to "+friend+" successfuly!", "success",JOptionPane.INFORMATION_MESSAGE);
	}
	//add friend to the side bar
	public void addFriendStock(String friend)
	{
		System.out.println(friend+ " added to the stock");
		mainFriendStock.addFriendButtonMethod(friend);
		revalidate();
	}
	//add a green dot
	public void friendStockConnect(String friend)
	{
		System.out.println(friend+ " online");
		mainFriendStock.setFriendStockState(friend, "online");
	}
	//add a red dot
	public void friendStockDisconnect(String friend)
	{
		System.out.println(friend+ " offline");
		mainFriendStock.setFriendStockState(friend, "offline");
	}

/*------------------------------*/
	public class MainOptionConv extends JPanel
	{
		public JRadioButton colorPickerButton;
		public JRadioButton fileSharingButton;
		public JRadioButton downloadFileSharingButton;
		public ButtonGroup group;
		public ArrayList<String> titleDownloadFileList = new ArrayList<>();
		public JRadioButton themeButton;

		public MainOptionConv()
		{
			super();
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBackground(ColorManager.bg);
			setBorder(BorderFactory.createTitledBorder("Options"));
			group = new ButtonGroup();
			colorPickerButton = new JRadioButton("color");
			fileSharingButton = new JRadioButton("share file");
			downloadFileSharingButton = new JRadioButton("download files");
			themeButton = new JRadioButton("theme");
			themeButton.addActionListener((e)->new ThemeChooseDialog());
			group.add(colorPickerButton);
			group.add(fileSharingButton);
			group.add(downloadFileSharingButton);
			group.add(themeButton);
			colorPickerButton.addActionListener(this::colorPickerDialogCallback);
			fileSharingButton.addActionListener(this::fileSharingCallback);
			downloadFileSharingButton.addActionListener((e)-> new DownloadFileDialog());
			add(colorPickerButton);
			add(fileSharingButton);
			add(downloadFileSharingButton);
			add(themeButton);
		}

		public void alertNewDownloadFile()
		{
			JOptionPane.showMessageDialog(null,"a new file has been updloaded","alert", JOptionPane.INFORMATION_MESSAGE);
		}

		public void alertDownloadedFile()
		{
			JOptionPane.showMessageDialog(null,"the file has been downloaded successfuly!","alert", JOptionPane.INFORMATION_MESSAGE);
		}

		public void fileSharingCallback(ActionEvent e)
		{
			final JFileChooser fc = new JFileChooser();
			int returnVal = fc.showOpenDialog(this);
			ArrayList<String> selectedFriends;
			if(returnVal == JFileChooser.APPROVE_OPTION)
			{
				File file = fc.getSelectedFile();
				FriendPickerDialog fpd = new FriendPickerDialog();
				selectedFriends = fpd.lastListSelectedFriends;
				if(selectedFriends.size() == 0)
				{
					JOptionPane.showMessageDialog(null,"the file will not be shared because you have not choose friend which can recieve the file","alert", JOptionPane.WARNING_MESSAGE);
				}
				else
				{
					client.uploadToServer(selectedFriends, file);
				}
			}
		}

		public void fileSharingDone()
		{
			JOptionPane.showMessageDialog(null,"file shared successfuly!","info", JOptionPane.INFORMATION_MESSAGE);
		}

		public void sharingFileFailedDialog(File f)
		{
			JOptionPane.showMessageDialog(null,f.getName()+" cannot be shared","alert", JOptionPane.ERROR_MESSAGE);
		}

		public void setBgColorOption(Color c)
		{
			setBackground(c);
		}

		public void colorPickerDialogCallback(ActionEvent e)
		{
			new ColorPickerChoiceDialog();
		}

		public class DownloadFileDialog extends JDialog
		{
			public JPanel panel;
			public  JScrollPane scrollpane;

			public DownloadFileDialog()
			{
				super((JFrame)null, "download files", true);
				setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
				scrollpane = new JScrollPane(panel);
				scrollpane.setBorder(BorderFactory.createTitledBorder("download files"));
				for(String s : titleDownloadFileList)
				{
					JRadioButton rb = new JRadioButton(s);
					rb.addActionListener(this::downloadFileFromServer);
					panel.add(rb);
				}
				add(scrollpane);
				setSize(350,150);
				setLocationRelativeTo(null);
				setResizable(false);
				setVisible(true);
			}

			public void downloadFileFromServer(ActionEvent e)
			{
				client.downloadFileFromServer(((JRadioButton)e.getSource()).getText());
			}
		}

		public class ThemeChooseDialog extends JDialog
		{
			public JPanel panel;
			public JButton nimbusButton;
			public JButton metalButton;
			public JButton classicButton;
			
			public ThemeChooseDialog()
			{
				super((JFrame)null, "choose theme", true);
				setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				panel = new JPanel();
				panel.setLayout(new FlowLayout());
				panel.setBorder(BorderFactory.createTitledBorder("choose theme"));
				nimbusButton = new JButton("nimbus");
				metalButton = new JButton("metal");
				classicButton = new JButton("classic");
				nimbusButton.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							try
							{
								UIManager.setLookAndFeel(new NimbusLookAndFeel());
								GuiClient.this.repaint();
								GuiClient.this.revalidate();
							}
							catch(Exception er)
							{
								JOptionPane.showMessageDialog(null,"nimbus cannot be applied","alert", JOptionPane.WARNING_MESSAGE);
							}
						}
					});
				metalButton.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							try
							{
								UIManager.setLookAndFeel(new MetalLookAndFeel());
								GuiClient.this.repaint();
								GuiClient.this.revalidate();
							}
							catch(Exception er)
							{
								JOptionPane.showMessageDialog(null,"metal cannot be applied","alert", JOptionPane.WARNING_MESSAGE);
							}
						}////UIManager.setLookAndFeel(new MetalLookAndFeel());
					});
				classicButton.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							try
							{
								UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
								GuiClient.this.repaint();
								GuiClient.this.revalidate();
							}
							catch(Exception er)
							{
								JOptionPane.showMessageDialog(null,"classic cannot be applied","alert", JOptionPane.WARNING_MESSAGE);
							}
						}
					});
				panel.add(nimbusButton);
				panel.add(metalButton);
				panel.add(classicButton);
				add(panel);
				setSize(350,150);
				setLocationRelativeTo(null);
				setResizable(false);
				setVisible(true);
			}
		}

		public class FriendPickerDialog extends JDialog
		{
			public JScrollPane scrollpane;
			public JPanel panel;
			public JButton okButton;
			public ArrayList<JRadioButton> listSelectedFriends = new ArrayList<>();
			public ArrayList<String> lastListSelectedFriends = new ArrayList<>();

			public FriendPickerDialog()
			{
				super((JFrame)null, "choose friends", true);
				setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				panel = new JPanel();
				panel.setLayout(new FlowLayout());
				scrollpane = new JScrollPane(panel);
				scrollpane.setBorder(BorderFactory.createTitledBorder("choose friends"));
				okButton = new JButton("upload");
				okButton.addActionListener(this::setSelectedFriendListCallback);
				for(FriendRequestData f : client.friends)
				{
					JRadioButton rb = new JRadioButton(f.username);
					listSelectedFriends.add(rb);
					panel.add(rb);
				}
				add(scrollpane);
				panel.add(okButton);
				setSize(350,150);
				setLocationRelativeTo(null);
				setResizable(false);
				setVisible(true);
			}

			public void setSelectedFriendListCallback(ActionEvent e)
			{
				lastListSelectedFriends = new ArrayList<>();
				for(JRadioButton rb : listSelectedFriends)
				{
					if(rb.isSelected())
						lastListSelectedFriends.add(rb.getText());
				}
				dispose();
			}
		}

		public class ColorPickerChoiceDialog extends JDialog
		{
			public JPanel panel;
			public JButton bgColorPickerButton;
			public JButton fgColorPickerButton;
			public JButton youBgColorPickerButton;
			public JButton youFgColorPickerButton;
			public JButton okButton;

			public ColorPickerChoiceDialog()
			{
				super((JFrame)null, "color editor", true);
				setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				panel = new JPanel();
				panel.setLayout(new FlowLayout());
				panel.setBorder(BorderFactory.createTitledBorder("color choice"));
				setBorder(BorderFactory.createLineBorder(Color.black, 2));
				bgColorPickerButton = new JButton("background color");
				fgColorPickerButton = new JButton("foreground color");
				youBgColorPickerButton = new JButton("you background color");
				youFgColorPickerButton = new JButton("you foreground color");
				bgColorPickerButton.addActionListener(this::bgColorPickerChoiceDialogCallback);
				fgColorPickerButton.addActionListener(this::fgColorPickerChoiceDialogCallback);
				youBgColorPickerButton.addActionListener(this::youBgColorPickerChoiceDialogCallback);
				youFgColorPickerButton.addActionListener(this::youFgColorPickerChoiceDialogCallback);
				okButton = new JButton("CLOSE");
				okButton.addActionListener((thisDialog) -> dispose());
				panel.add(bgColorPickerButton);
				panel.add(fgColorPickerButton);
				panel.add(youBgColorPickerButton);
				panel.add(youFgColorPickerButton);
				panel.add(okButton);
				add(panel);
				setSize(350,150);
				setLocationRelativeTo(null);
				setResizable(false);
				setVisible(true);
			}

			public void youBgColorPickerChoiceDialogCallback(ActionEvent e)
			{
				Color c = JColorChooser.showDialog(null, "Choose a color", Color.BLUE);
				ColorManager.youBg = c;
				mainConvArea.setYouConvElementBg(c);
				mainConvArea.setTextAreaColor();
			}

			public void youFgColorPickerChoiceDialogCallback(ActionEvent e)
			{
				Color c = JColorChooser.showDialog(null, "Choose a color", Color.BLUE);
				ColorManager.youFg = c;
				mainConvArea.setYouConvElementFg(c);
				mainConvArea.setTextAreaColor();
			}

			public void bgColorPickerChoiceDialogCallback(ActionEvent e)
			{
				Color c = JColorChooser.showDialog(null, "Choose a color", Color.BLUE);
				ColorManager.bg = c;
				mainConvArea.setConvElementBg(c);
				mainFriendStock.setBgColorFriendStock(c);
				MainOptionConv.this.setBgColorOption(c);
			}

			public void fgColorPickerChoiceDialogCallback(ActionEvent e)
			{
				Color c = JColorChooser.showDialog(null, "Choose a color", Color.BLUE);
				ColorManager.fg = c;
				mainConvArea.setConvElementFg(c);
			}
		}
	}
/*------------------------------*/

	public class MainConvArea extends JPanel
	{
		public JScrollPane scrollbar;
		public JScrollPane textScrollbar;
		public JPanel textAreaPanel;
		public JPanel fieldPanel;
		public JTextArea textField;
		public JButton senderTextButton;
		public JLabel dstLabel;
		public ArrayList<ConvElement> listConvElement = new ArrayList<>();

		public MainConvArea()
		{
			super();
			setLayout(new BorderLayout());
			textAreaPanel = new JPanel();
			textAreaPanel.setLayout(new BoxLayout(textAreaPanel, BoxLayout.Y_AXIS));
			dstLabel = new JLabel("friend: "+"unknown");
			dstLabel.setForeground(new Color(255,255,135));
			dstLabel.setFont(dstLabel.getFont().deriveFont(Font.BOLD));
			scrollbar = new JScrollPane(textAreaPanel);
			scrollbar.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			scrollbar.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			fieldPanel = new JPanel(new BorderLayout());
			textField = new JTextArea(3,10);
			textScrollbar = new JScrollPane(textField);
			senderTextButton = new JButton("send");
			senderTextButton.setEnabled(false);
			senderTextButton.addActionListener(this::sendTextToConvCallback);
			fieldPanel.add(textScrollbar);
			fieldPanel.add(senderTextButton, BorderLayout.EAST);
			fieldPanel.setBorder(BorderFactory.createTitledBorder("message area"));
			setBorder(BorderFactory.createLineBorder(Color.black, 2));
			add(dstLabel, BorderLayout.NORTH);
			add(scrollbar);
			add(fieldPanel, BorderLayout.SOUTH);
			setBackground(new Color(0,0,120));
			setTextAreaColor();
			//addConvElementToConv("Jean", "String text");
		}

		public void setTextAreaColor()
		{
			textField.setForeground(ColorManager.youFg);
			textField.setBackground(ColorManager.youBg);
			senderTextButton.setForeground(ColorManager.youFg);
			senderTextButton.setBackground(ColorManager.youBg);
		}

		public void dropConv()
		{
			textAreaPanel.removeAll();
			textAreaPanel.revalidate();
			textAreaPanel.repaint();
			scrollbar.revalidate();
			revalidate();
			repaint();
		}

		public void setConv(String friend)
		{
			client.getConvFromServer(friend);
		}

		private void sendTextToConvCallback(ActionEvent e)
		{
			if(textField.getText().indexOf('\\') != -1)
			{
				JOptionPane.showMessageDialog(null,"'\\' character is not allowed","alert", JOptionPane.WARNING_MESSAGE);
				return;
			}
			addConvElementToConv(client.data.getUsername(), textField.getText());
			//scrollbar.getVerticalScrollBar().setValue(scrollbar.getVerticalScrollBar().getMaximum());
			scrollbar.getViewport().setViewPosition(new Point(0,scrollbar.getVerticalScrollBar().getMaximum()));
			client.sendTextToServer(dstLabel.getText().split(" ")[1], textField.getText());
		}

		public void setConvText(String text, String friend)
		{
			if(!friend.equals(dstLabel.getText().split(" ")[1]) && !friend.equals(client.data.getUsername()))
			{
				JOptionPane.showMessageDialog(null,"you have a new message from "+friend,"alert", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			text = text.replace('\\', '\n');
			addConvElementToConv(friend, text);
			//scrollbar.getVerticalScrollBar().setValue(scrollbar.getVerticalScrollBar().getMaximum());
			scrollbar.getViewport().setViewPosition(new Point(0,scrollbar.getVerticalScrollBar().getMaximum()));
		}

		public void addConvElementToConv(String srcName, String text)
		{
			ConvElement ce = new ConvElement(srcName, text);
			listConvElement.add(ce);
			textAreaPanel.add(ce);
			textAreaPanel.revalidate();
			textAreaPanel.repaint();
			scrollbar.revalidate();
			revalidate();
			repaint();
		}

		public void setYouConvElementBg(Color c)
		{
			for(ConvElement ce : listConvElement)
			{
				if(ce.srcName.equals(client.data.getUsername()))
					ce.textPanel.setBackground(c);
			}
		}

		public void setYouConvElementFg(Color c)
		{
			for(ConvElement ce : listConvElement)
			{
				for(int i = 0; i < ce.textVec.length; i++)
				{
					if(ce.srcName.equals(client.data.getUsername()))
						ce.textVec[i].setForeground(c);
				}
			}
		}

		public void setConvElementBg(Color c)
		{
			for(ConvElement ce : listConvElement)
			{
				if(ce.srcName.equals(client.data.getUsername()))
					ce.textPanel.setBackground(ColorManager.youBg);
				else
					ce.textPanel.setBackground(c);
			}
		}

		public void setConvElementFg(Color c)
		{
			for(ConvElement ce : listConvElement)
			{
				for(int i = 0; i < ce.textVec.length; i++)
				{
					if(ce.srcName.equals(client.data.getUsername()))
						ce.textVec[i].setForeground(ColorManager.youFg);
					else
						ce.textVec[i].setForeground(c);
				}
			}
		}

		public class ConvElement extends JPanel
		{
			public JLabel [] textVec;
			public JPanel textPanel;
			public String srcName;

			public ConvElement(String src, String text)
			{
				super(new BorderLayout());
				srcName = src;
				setBackground(new Color(0,120,200));
				textPanel = new JPanel();
				textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
				String [] texts = text.split("\n");
				textVec = new JLabel[texts.length];
				for(int i = 0; i < texts.length; i++)
				{
					textVec[i] = new JLabel(texts[i]);
					if(src.equals(client.data.getUsername()))
						textVec[i].setForeground(ColorManager.youFg);
					else
						textVec[i].setForeground(ColorManager.fg);
					textPanel.add(textVec[i]);
				}
				if(src.equals(client.data.getUsername()))
					textPanel.setBackground(ColorManager.youBg);
				else
					textPanel.setBackground(ColorManager.bg);
				textPanel.setBorder(BorderFactory.createTitledBorder(src));
				setBorder(BorderFactory.createLineBorder(Color.black, 2));
				add(textPanel);
			}
		}
	}

/*------------------------------*/
	public class MainToolBar extends JToolBar
	{
		public JButton addFriendButton;
		public JButton friendRequestButton;

		public MainToolBar()
		{
			super();
			addFriendButton = new JButton("add friend");
			addFriendButton.addActionListener(this::addFriendCallBack);
			friendRequestButton = new JButton("friend request (0)");
			friendRequestButton.addActionListener(this::friendRequestTraitement);
			add(addFriendButton);
			add(friendRequestButton);
		}

		public void friendRequestTraitement(ActionEvent e)
		{
			new FriendRequestTraitementDialog();
		}

		public void addFriendCallBack(ActionEvent e)
		{
			boolean error = true;
			while(error)
			{
				String friendName = JOptionPane.showInputDialog("Your name's friend: ");
				if(friendName != null)
				{
					if(friendName.compareTo(client.data.getUsername()) == 0)
					{
						JOptionPane.showMessageDialog(null,"You cannot send a request to your own account","alert", JOptionPane.WARNING_MESSAGE);
					}
					else if(client.isAlreadyFriend(friendName))
					{
						JOptionPane.showMessageDialog(null,"You are already friend with "+friendName,"alert", JOptionPane.WARNING_MESSAGE);
					}
					else if(friendName.length() == 0)
					{
						JOptionPane.showMessageDialog(null,"The field is empty","alert", JOptionPane.WARNING_MESSAGE);
					}
					else if(friendName.indexOf(' ') != -1)
					{
						JOptionPane.showMessageDialog(null,"The field contains space","alert", JOptionPane.WARNING_MESSAGE);
					}
					else if(client.isWaitingRequest(friendName))
					{
						JOptionPane.showMessageDialog(null,friendName+" has already sent you a request", "alert", JOptionPane.WARNING_MESSAGE);
					}
					else
					{
						error = false;
						client.sendMessage(Message.FRIEND_REQUEST_SIG, Message.SERVER_ID, Message.intToSId(client.data.getId()), friendName);
					}
				}
				else
				{
					error = false;
				}
			}
		}
	}
/*------------------------------*/
	public class FriendsStock extends JScrollPane
	{
		public ArrayList<JRadioButton> friendButtons = new ArrayList<>();
		public JPanel panel = new JPanel();
		public ButtonGroup groupFriend = new ButtonGroup();

		public FriendsStock()
		{
			super();
			setViewportView(panel);
			panel.setBackground(ColorManager.bg);
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.setPreferredSize(new Dimension(130,0));
		}

		public void setBgColorFriendStock(Color c)
		{
			panel.setBackground(c);
		}

		public void addFriendButtonMethod(String friendName)
		{
			System.out.println(friendName+" as his own button!");
			JRadioButton button = new JRadioButton(friendName);
			button.addActionListener(this::friendSelectionCallback);
			button.setFont(button.getFont().deriveFont(Font.BOLD));
			groupFriend.add(button);
			friendButtons.add(button);
			panel.add(button);
		}

		private void friendSelectionCallback(ActionEvent e)
		{
			mainConvArea.senderTextButton.setEnabled(true);
			mainConvArea.dstLabel.setText("friend: "+((JRadioButton) e.getSource()).getText());
			mainConvArea.dropConv();
			mainConvArea.setConv(((JRadioButton) e.getSource()).getText());
		}

		public void setFriendStockState(String friendName, String state)
		{
			for(JRadioButton b : friendButtons)
			{
				if(b.getText().compareTo(friendName) == 0)
				{
					if(state.compareTo("online") == 0)
					{
						b.setForeground(Color.green);
					}
					else
					{
						b.setForeground(Color.red);
					}
				}
			}
		}

		public String getSelectedFriendStock()
		{
			for(JRadioButton b : friendButtons)
			{
				if(b.isSelected())
				{
					return b.getText();
				}
			}
			return "unknown";
		}
	}
/*------------------------------*/
	public class FriendRequestTraitementDialog extends JDialog
	{
		public ArrayList<WaitingRequestPanel> requests = new ArrayList<>();

		public FriendRequestTraitementDialog()
		{
			super((JFrame)null, "friend requests", true);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.setBorder(BorderFactory.createTitledBorder("requests"));
			JScrollPane scrollpane = new JScrollPane(panel);
			for(FriendRequestData frd : client.friendRequests)
			{
				WaitingRequestPanel tmp = new WaitingRequestPanel(frd.username);
				requests.add(tmp);
				panel.add(tmp);
			}
			add(scrollpane);
			setSize(300,175);
			setLocationRelativeTo(null);
			setResizable(false);
			setVisible(true);
		}

		private void delFriendRequest(String name)
		{
			int i = 0;
			for(WaitingRequestPanel wrp : requests)
			{
				if(wrp.friendLabel.getText().equals(name))
				{
					System.out.println("request removed");
					remove(wrp);
					requests.remove(wrp);
					wrp.okButton.setEnabled(false);
					wrp.friendLabel.setBackground(Color.green);
					revalidate();
					repaint();
					break;
				}
				i++;
			}
		}

		public class WaitingRequestPanel extends JPanel
		{
			public JLabel friendLabel;
			public JButton okButton;

			public WaitingRequestPanel(String friend)
			{
				super(new GridLayout(1,3,3,0));
				friendLabel = new JLabel(friend);
				okButton = new JButton("accept");
				okButton.addActionListener(this::acceptFriendRequest);
				add(friendLabel);
				add(okButton);
			}

			public void acceptFriendRequest(ActionEvent e)
			{
				mainToolBar.friendRequestButton.setText("friend requests("+Integer.toString(client.friendRequests.size()-1)+")");
				client.acceptFriendRequest(friendLabel.getText());
				addFriendStock(friendLabel.getText());
				delFriendRequest(friendLabel.getText());
			}
		}
	}

/*------------------------------*/
	public class GuiConnexion extends JDialog
	{
		public GuiConnexion()
		{
			super((JFrame)null, "connection", true);
			JPanel panel = new JPanel();
			panel.setBorder(BorderFactory.createTitledBorder("Complete form"));
			getContentPane().getInsets().set(10,10,10,10);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			panel.setLayout(new GridLayout(4,1,0,17));
			JLabel usernameLabel = new JLabel("      username: ");
			JLabel passwordLabel = new JLabel("      password: ");
			JTextField usernameField = new JTextField();
			JTextField passwordField = new JTextField();
			JRadioButton loginButton = new JRadioButton("log in");
			JRadioButton signupButton = new JRadioButton("sign up");
			JButton confirmButton = new JButton("confirm");
			JLabel msgLabel = new JLabel("");
			JPanel usernamePanel = new JPanel();
			JPanel passwordPanel = new JPanel();
			JPanel modePanel = new JPanel();
			JPanel buttonPanel = new JPanel();
			ButtonGroup group = new ButtonGroup();
			loginButton.setSelected(true);
			confirmButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					String mode;
					if(loginButton.isSelected())
					{
						mode = Message.LOGIN_SIG;
					}
					else
					{
						mode = Message.SIGNUP_SIG;
					}
					if(usernameField.getText().length() == 0 || usernameField.getText().length() == 0)
					{
						msgLabel.setText("<html><div style='color:red'>a field is empty</div></html>");
						return;
					}
					if(usernameField.getText().indexOf(' ') != -1 || passwordField.getText().indexOf(' ') != -1)
					{
						msgLabel.setText("<html><div style='color:red'>a field contains space</div></html>");
						return;
					}
					msgLabel.setText("<html><div style='color:green'>connection</div></html>");
					client.connProtocol(usernameField.getText(), passwordField.getText(), mode);
					if(client.connected == true)
					{
						System.out.println("run client");
						client.start();
						dispose();
					}
					else
					{
						if(mode.compareTo(Message.LOGIN_SIG) == 0)
						{
							msgLabel.setText("<html><div style='color:red'>connection with this profile failed</div></html>");
						}
						else
						{
							msgLabel.setText("<html><div style='color:red'>this username already exists</div></html>");
						}
					}
				}
			});
			group.add(loginButton);
			group.add(signupButton);
			usernamePanel.setLayout(new GridLayout(1,2));
			usernamePanel.add(usernameLabel);
			usernamePanel.add(usernameField);
			passwordPanel.setLayout(new GridLayout(1,2));
			passwordPanel.add(passwordLabel);
			passwordPanel.add(passwordField);
			modePanel.setLayout(new GridLayout(1,2));
			modePanel.add(loginButton);
			modePanel.add(signupButton);
			buttonPanel.setLayout(new FlowLayout());
			confirmButton.setPreferredSize(new Dimension(100,20));
			buttonPanel.add(confirmButton);
			buttonPanel.add(msgLabel);
			panel.add(usernamePanel);
			panel.add(passwordPanel);
			panel.add(modePanel);
			panel.add(buttonPanel);
			add(panel);
			setSize(400,220);
			setLocationRelativeTo(null);
			setResizable(false);
			setVisible(true);
		}
	}
}

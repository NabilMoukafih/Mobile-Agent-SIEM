package main;

import jade.core.Location;
import jade.gui.GuiEvent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableColumn;


/**
@author Nabil Moukafih, 13 January 2018.
@version $Date: 2018-01-09.
{@summary This class associates a graphical interface to the UserAgent. }
*/
public class UserAgentGui extends JFrame implements ActionListener{

	private static final long serialVersionUID = -8553806560432118690L;
	// Label and text of the source
	private JLabel jLabelSource=new JLabel("Location:");
	private JTextField jTextFieldSource=new JTextField(12);

	// Label and the text of the log file
	private JLabel jLabelLogType=new JLabel("Log type:");
	//private JTextField jTextFieldLogType=new JTextField(12);
	final String[] LogType = {"---", "Syslog", "Http", "Iptables", "Snort", "All"} ;

	private LocationTableModel availableSiteListModel;
	private JTable            availableSiteList;

	private static String SENDLABEL = "SEND";
	private static String REFRESHLABEL = "Refresh Locations";
	private static String EXITLABEL = "Exit";
	
	private ButtonGroup group;
	private SpinnerNumberModel model;
	private JSpinner spinner;

	final JComboBox<Object> liste;

	ListSelectionModel listSelectionModel;
	JTable table;


	//Button
	private JTextArea jTextAreaMess=new JTextArea();

	private UserAgent myAgent;

	public UserAgentGui(){
		///////////////////////////////
		// The first line of the GUI //
		///////////////////////////////
		JPanel FirstLine=new JPanel();
		FirstLine.setLayout(new BoxLayout(FirstLine,BoxLayout.X_AXIS));

		liste= new JComboBox<Object>(LogType);
		//Configure the menu log type

		liste.setMaximumRowCount (3) ; // at max 3 variables will be displayed
		liste.setSelectedIndex (0) ; // Force to select the first element by default

		FirstLine.add(jLabelSource);
		FirstLine.add(jTextFieldSource);
		FirstLine.add(jLabelLogType);
		FirstLine.add(liste);

		// Create the send button
		JButton jB=new JButton(SENDLABEL);
		jB.addActionListener(this);		
		FirstLine.add(jB);


		/////////////////////////////
		// The second line the GUI //
		/////////////////////////////
		JPanel SecondLine=new JPanel();
		SecondLine.setLayout(new BoxLayout(SecondLine,BoxLayout.X_AXIS));
		
		
		// text message
		JPanel messagesLine = new JPanel();
		messagesLine.setLayout(new BoxLayout(messagesLine,BoxLayout.Y_AXIS));
		
		jTextAreaMess.setEditable(false);
		messagesLine.add(new JScrollPane(jTextAreaMess),BorderLayout.CENTER);
		messagesLine.setBorder(BorderFactory.createTitledBorder("Received Messages"));


		// Add the list of available sites in new panel    
		JPanel locationsLine = new JPanel();
		locationsLine.setLayout(new BoxLayout(locationsLine,BoxLayout.Y_AXIS));
		
		availableSiteListModel = new LocationTableModel();
		availableSiteList = new JTable(availableSiteListModel);
		availableSiteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane avPane = new JScrollPane();
		avPane.getViewport().setView(availableSiteList);
		locationsLine.add(avPane, BorderLayout.EAST);
		locationsLine.setBorder(BorderFactory.createTitledBorder("Available Sources"));
		availableSiteList.setRowHeight(20);

		//SecondLine.add(availablePanel);
		TableColumn c;
		c = availableSiteList.getColumn(availableSiteList.getColumnName(0));
		c.setHeaderValue("ID");
		c = availableSiteList.getColumn(availableSiteList.getColumnName(1));
		c.setHeaderValue("Name");
		c = availableSiteList.getColumn(availableSiteList.getColumnName(2));
		c.setHeaderValue("Protocol");
		c = availableSiteList.getColumn(availableSiteList.getColumnName(3));
		c.setHeaderValue("Address");

		SecondLine.add(messagesLine);
		SecondLine.add(locationsLine, BorderLayout.EAST);
		
		///////////////////////////////
		// The third line of the GUI //
		///////////////////////////////

		JPanel ThirdLine = new JPanel();
		ThirdLine.setLayout(new BorderLayout());

		// Exit button
		jB=new JButton(EXITLABEL);
		jB.addActionListener(this);
		ThirdLine.add(jB, BorderLayout.WEST);
		// Refresh button
		jB=new JButton(REFRESHLABEL);
		jB.addActionListener(this);
		ThirdLine.add(jB, BorderLayout.EAST);
		
		// Information about the threads
		model = new SpinnerNumberModel(1, 1, 100, 1);
		spinner = new JSpinner(model);
		spinner.setMaximumSize(new Dimension(50, jLabelSource.getPreferredSize().height+6));


		/////////////////
		// Final phase //
		/////////////////

		this.setTitle("User Agent");
		this.setSize(1000,400);
		this.setVisible(true);

		JPanel allmain = new JPanel();
		allmain.setLayout(new BoxLayout(allmain,BoxLayout.Y_AXIS));

		allmain.add(FirstLine);
		allmain.add(SecondLine);
		allmain.add(ThirdLine);

		getContentPane().add(allmain, BorderLayout.CENTER);

	}


	public UserAgent getAgent() {
		return myAgent;
	}


	public void setAgent(UserAgent agent) {
		this.myAgent = agent;
	}


	public void showMessage(String msg, boolean append){
		if (append==true){
			jTextAreaMess.append(msg+"\n");
		}
		else {
			jTextAreaMess.setText(msg);
		}
	}

	public void updateLocations(Iterator<?> list) {
		availableSiteListModel.clear();
		for ( ; list.hasNext(); ) {
			Object obj = list.next();
			availableSiteListModel.add((Location) obj);
		}
		availableSiteListModel.fireTableDataChanged();
	}

	@SuppressWarnings("deprecation")
	void showCorrect()
	{
		///////////////////////////////////////////
		// Arrange and display GUI window correctly
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		show();
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		// Refresh button
		if (command.equalsIgnoreCase(REFRESHLABEL)) {
			GuiEvent guiEvent = new GuiEvent(null,UserAgent.REFRESH_EVENT);
			myAgent.postGuiEvent(guiEvent);
		}
		// Send button
		else if (command.equalsIgnoreCase(SENDLABEL)) {

			int viewRow = availableSiteList.getSelectedRow();
			if (viewRow < 0) {
				//Selection got filtered away.
				showMessage("Please select a source !",true);
			} 
			else {
				// the column that has the Container Name
				int viewCol = 1;
				String container=(String) availableSiteList.getValueAt(viewRow, viewCol); // the selected container

				String addresse = jTextFieldSource.getText(); //get the source text
				String logtype=(String) liste.getSelectedItem();
				//Get the parameters from the GUI
				Map <String, Object> params=new HashMap<>();
				params.put("Source", addresse);
				params.put("Type", logtype);
				params.put("Container", container);
				params.put("Norm_type", getSelectedButtonText(group));
				params.put("Thread_number", model.getNumber().toString());
				
				GuiEvent guiEvent = new GuiEvent(this,UserAgent.SEND);
				guiEvent.addParameter(params);

				myAgent.postGuiEvent(guiEvent);
			}			

		}
		// Exit button 
		else if (command.equalsIgnoreCase(EXITLABEL)) {
			GuiEvent guiEvent = new GuiEvent(null, UserAgent.EXIT);
			myAgent.postGuiEvent(guiEvent);

		}

	}
	
	// Get the of threading (Selected button)
	
	public String getSelectedButtonText(ButtonGroup buttonGroup) {
        for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();

            if (button.isSelected()) {
                return button.getText();
            }
        }

        return null;
    }


}



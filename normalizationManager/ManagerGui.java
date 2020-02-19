package normalizationManager;

import jade.gui.GuiEvent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
@author Nabil Moukafih, 13 January 2018.
@version $Date: 2018-01-09.

 * This classes associates a graphical interface to the Manager Agent.
 */
public class ManagerGui extends JFrame implements ActionListener {

	private static final long serialVersionUID = 3565289355421492970L;

	private Manager myAgent;

	private static String EXITLABEL = "EXIT";
	private static String DEPLOYLABEL = "DEPLOY";


	private JTextArea jTextAreaMess=new JTextArea();



	public ManagerGui(Manager a){
		super();
		myAgent = a;
		setTitle("GUI of "+a.getLocalName());
		this.setSize(500,400);

		JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main,BoxLayout.Y_AXIS));


		// Create the control buttons
		JPanel buttonpanel = new JPanel();

		JButton b=new JButton(DEPLOYLABEL);
		b.addActionListener(this);
		buttonpanel.add(b);
		// Exit button
		b = new JButton(EXITLABEL);
		b.addActionListener(this);
		buttonpanel.add(b);
		main.add(buttonpanel);

		// Text area with button

		JPanel textmessage = new JPanel();
		textmessage.setLayout(new FlowLayout());
		textmessage.setLayout(new BorderLayout());

		textmessage.add(new JScrollPane(jTextAreaMess),BorderLayout.CENTER);
		jTextAreaMess.setEditable(false);
		main.add(textmessage);



		JPanel allmain = new JPanel();
		allmain.setLayout(new BoxLayout(allmain,BoxLayout.X_AXIS));

		allmain.add(main);

		getContentPane().add(allmain, BorderLayout.CENTER);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		String command = e.getActionCommand();

		// Deploy button
		if (command.equalsIgnoreCase(DEPLOYLABEL)){
			GuiEvent gevent = new GuiEvent(this,Manager.DEPLOY);
			gevent.addParameter(true);
			myAgent.onGuiEvent(gevent);
		}
		// Exit button
		else if (command.equalsIgnoreCase(EXITLABEL)) {
			GuiEvent ev = new GuiEvent(null,Manager.EXIT);
			myAgent.postGuiEvent(ev);
		}
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

	public Manager getmyAgent() {
		return myAgent;
	}

	public void setmyAgent(Manager myAgent) {
		this.myAgent = myAgent;
	}

	public void showMessage(String msg, boolean append){
		if (append==true){
			jTextAreaMess.append(msg+"\n");
		}
		else {
			jTextAreaMess.setText(msg);
		}
	}




}

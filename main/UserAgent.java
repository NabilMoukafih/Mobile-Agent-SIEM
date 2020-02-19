package main;

import java.util.Map;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.FIPANames;
import jade.domain.mobility.MobilityOntology;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;

/**
@author Nabil Moukafih, 13 January 2018.
@version $Date: 2018-01-09.

{@summary This agent interacts with the users in order to retrieve the information
about the device to normalize (Log type, address , etc.). The agent sends
this information to the Manager agent.}
*/
public class UserAgent extends GuiAgent{

	private static final long serialVersionUID = -3616904988358179956L;

	transient protected UserAgentGui gui;

	public static final int EXIT = 1000;
	public static final int SEND = 1001;
	public static final int REFRESH_EVENT = 1002;


	@Override
	protected void setup() {

		// register the SL0 content language
		getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);
		// register the mobility ontology
		getContentManager().registerOntology(MobilityOntology.getInstance());

		gui=new UserAgentGui();
		gui.setAgent(this);
		System.out.println("Agent:"+getAID().getLocalName()+" is ready !");
		ParallelBehaviour parallelBehaviour=new ParallelBehaviour();
		addBehaviour(parallelBehaviour);

		// get the list of available locations and show it in the GUI
		parallelBehaviour.addSubBehaviour(new GetAvailableLocationsBehaviour(this));

		// A Cyclic method to receive messages
		parallelBehaviour.addSubBehaviour(new ServeIncomingMessagesBehaviour(this));


	}

	protected void takeDown() {
		// Dispose the GUI if it is there
		if (gui != null) {
			gui.dispose();
		}
		// Printout a dismissal message
		System.out.println("Collector-agent "+getAID().getName()+" terminated.");
	}

	@Override
	protected void onGuiEvent(GuiEvent ev) {
		switch (ev.getType()) {
		case SEND:
			@SuppressWarnings("unchecked") Map<String, Object> params=(Map<String, Object>) ev.getParameter(0);
			String location=(String) params.get("Source");
			String logtype=(String) params.get("Type");
			String container=(String) params.get("Container");
			String norm_type=(String) params.get("Norm_type");
			String threadNumber= (String) params.get("Thread_number");

			if (logtype == "---" || location.length() == 0) {gui.showMessage("Log location/type is not specified !", true );}
			else {
				
				ACLMessage aclMessage= new ACLMessage(ACLMessage.REQUEST);
				aclMessage.addReceiver(new AID("Normalisation Manager",AID.ISLOCALNAME));
				aclMessage.setOntology("parsing");
				aclMessage.setContent("deploy-me");
				aclMessage.addUserDefinedParameter("Source", location);
				aclMessage.addUserDefinedParameter("Type", logtype);
				aclMessage.addUserDefinedParameter("Container", container);
				aclMessage.addUserDefinedParameter("Norm_type", norm_type);
				aclMessage.addUserDefinedParameter("Thread_number", threadNumber);
				
				send(aclMessage);
			}
			break;

		case REFRESH_EVENT:
			addBehaviour(new GetAvailableLocationsBehaviour(this));
			break;
		case EXIT:
			gui.dispose();
			gui = null;
			doDelete();
			break;

		default:
			break;
		}

	} 

} // end class Collector

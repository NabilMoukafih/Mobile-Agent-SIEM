package normalizationManager;

import jade.core.behaviours.ParallelBehaviour;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/**
@author Nabil Moukafih, 13 January 2018.
@version $Date: 2018-01-09.

 * The Manager agent provide the following tasks:
 * 1. Receives messages from the collector agent about the log file to normalize (log-type, address)
 * 2. Deploy normalizer agents specifically for requested tasks: the normalized agent is deployed with
 * 	  the following parameters : The name of the source container, the location of the file, and the regex 
 * 	  will be used.
 * 3. Interacts with the database.
 */
public class Manager extends GuiAgent{

	private static final long serialVersionUID = -2714401899400195843L;

	transient protected ManagerGui gui;

	private static boolean IAmTheCreator = true;
	private int compteur=0, time=3000;
	String location, logtype, containerName, normalizationType, threadNumber;

	private AgentController t1 = null;
	private AgentContainer container;

	public static final int DEPLOY = 1000;
	public static final int EXIT = 1001;

	@Override
	protected void setup() {

		// creates and shows the GUI
		gui = new ManagerGui(this);
		gui.setVisible(true);

		System.out.println("Agent: "+getAID().getLocalName()+" is ready");
		ParallelBehaviour parallelBehaviour=new ParallelBehaviour();
		addBehaviour(parallelBehaviour);

		// A Cyclic method to receive messages
		parallelBehaviour.addSubBehaviour(new ServeIncomingMessagesBehaviour(this));

	}

	// Get the information about the source and log types, so the behavior can access them

	protected void takeDown() {
		// Dispose the GUI if it is there
		if (gui != null) {
			gui.dispose();
		}
		// Printout a dismissal message
		System.out.println("Collector-agent "+getAID().getName()+" terminated.");
	}



	//////////////////////////////////////////
	// GUI handling							//
	// Agent Operations following GUI event //
	//////////////////////////////////////////

	@Override
	protected void onGuiEvent(GuiEvent ev) {
		switch (ev.getType()) {
		case DEPLOY:			

			IAmTheCreator=(boolean) ev.getParameter(0);
			if (IAmTheCreator){
				IAmTheCreator=false;
				String AgentName="Norm"+compteur;
				++compteur;
				try {
					if (normalizationType.equals("Single Agent")) {
						// Create agent t1 on the same container of the creator agent
						// And forward the arguments to the created agent
						Object [] args = new Object[4];
						args[0]=containerName;
						args[1]=location;
						args[2]=time;
						args[3]=threadNumber;

						// get a container controller for creating new agents
						container = (AgentContainer)getContainerController();

						switch (logtype) {
						//create the agents with the variables

						case "Iptables":						
							t1 = container.createNewAgent(AgentName,IptablesNormalizer.class.getName(), args);
							t1.start();
							break;
						case "Http":
							t1 = container.createNewAgent(AgentName,HttpNormalizer.class.getName(), args);
							t1.start();
							break;
						case "Snort":
							t1 = container.createNewAgent(AgentName,SnortNormalizer.class.getName(), args);
							t1.start();
							break;
						case "Syslog":
							t1 = container.createNewAgent(AgentName,SyslogNormalizer.class.getName(), args);
							t1.start();
							break;
						case "All":
							t1 = container.createNewAgent(AgentName,AllNormalizer.class.getName(), args);
							t1.start();
							break;
						case "Scan31":
							t1 = container.createNewAgent(AgentName,Scan31Agent.class.getName(), args);
							t1.start();
							break;
						case "Sotm31-All":
							t1 = container.createNewAgent(AgentName,Somt31ALL.class.getName(), args);
							t1.start();
							break;
						default:
							break;
						}
					}
					else {
						// Create agent t1 on the same container of the creator agent
						// And forward the arguments to the created agent
						Object [] args = new Object[4];
						args[0]=containerName;
						args[1]=location;
						args[2]=time;
						// The thread number is the number of created agents
						args[3]=threadNumber;

						// get a container controller for creating new agents
						container = (AgentContainer)getContainerController();
						switch (logtype) {
						case "Scan31":

							t1 = container.createNewAgent("Producer",ProducerAgent.class.getName(), args);
							t1.start();
							int comp=0;
							for (int i =0; i< Integer.parseInt(threadNumber); i++) {
								AgentController t1 = null;
								String Agentname="Consumer"+comp;
								t1 = container.createNewAgent(Agentname,ConsumerAgent.class.getName(), args);
								t1.start();
								comp++;
							}

							AgentController t2 = null;
							t2 = container.createNewAgent("Writer",WriterAgent.class.getName(), args);
							t2.start();
							break;
						case "Scan31-C":
							// This will a create agents using a combined approach. Each Agent will have 3 threads. We will only change the consumer agent
							t1 = container.createNewAgent("Producer",ProducerAgentCombined.class.getName(), args);
							t1.start();
							int comp1=0;
							for (int i =0; i< Integer.parseInt(threadNumber); i++) {
								AgentController t1 = null;
								String Agentname="Consumer"+comp1;
								t1 = container.createNewAgent(Agentname,Scan31Consumer.class.getName(), args);
								t1.start();
								comp1++;
							}

							AgentController t3 = null;
							t3 = container.createNewAgent("Writer",WriterAgent.class.getName(), args);
							t3.start();
							
							break;

						default:
							break;
						}
					}

				} catch (Exception any) {
					any.printStackTrace();
				}
			}
			break;

		case EXIT:
			gui.dispose();
			gui = null;
			doDelete();
			break;

		default:
			break;
		}
	};
}

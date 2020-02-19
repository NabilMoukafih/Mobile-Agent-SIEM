package normalizationManager;

import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;

/**
@author Nabil Moukafih, 13 January 2018.
@version $Date: 2018-01-09.
{@summary This class creates a complete execution environment for agents in a normalization container}
*/
public class NormalizationContainer {

	public static void main(String[] args) {

		Runtime runtime=Runtime.instance();
		ProfileImpl profileImpl=new ProfileImpl(false);
		profileImpl.setParameter(ProfileImpl.MAIN_HOST, "localhost");
		AgentContainer agentContainer=runtime.createAgentContainer(profileImpl);
		if (agentContainer!=null){
			try {
				// To create Normalization Manager agent
				AgentController agentNMController=agentContainer.createNewAgent
						("Normalisation Manager", Manager.class.getName(), new Object []{});
				agentNMController.start();
				
				// To create Database agent
				AgentController agentDBController=agentContainer.createNewAgent
						("Database Agent", DatabaseAgent.class.getName(), new Object[] {});
				agentDBController.start();
			} 
			catch (ControllerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

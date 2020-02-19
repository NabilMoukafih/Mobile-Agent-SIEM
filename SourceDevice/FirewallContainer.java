package sources;

import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.ControllerException;

public class FirewallContainer {

	public static void main(String[] args) {
		
		Runtime runtime=Runtime.instance();
		ProfileImpl profileImpl=new ProfileImpl(false);
		profileImpl.setParameter(ProfileImpl.MAIN_HOST, "localhost");
		profileImpl.setParameter(ProfileImpl.CONTAINER_NAME, "Container Firewall");
		profileImpl.setParameter("jade_core_messaging_MessageManager_deliverytimethreshold", "6000");
		AgentContainer agentContainer=runtime.createAgentContainer(profileImpl);
		
		try {
			agentContainer.start();
		} catch (ControllerException e) {
			e.printStackTrace();
		}

	}

}

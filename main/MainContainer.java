package main;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;

/**
@author Nabil Moukafih, 13 January 2018.
@version $Date: 2018-01-09.
{@summary This class creates a complete execution environment for agents  }
*/
public class MainContainer {

	public static void main(String[] args) {
		try {
			Runtime runtime=Runtime.instance();
			Properties properties=new ExtendedProperties();
			properties.setProperty(Profile.GUI, "true");
			ProfileImpl profileImpl=new ProfileImpl(properties);
			AgentContainer agentContainer=runtime.createMainContainer(profileImpl);

			// To create an agent
			AgentController agentController=agentContainer.createNewAgent
					("User Agent", UserAgent.class.getName(), new Object []{});

			agentController.start();
		} catch (ControllerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} // end main

} // end class MainContainer

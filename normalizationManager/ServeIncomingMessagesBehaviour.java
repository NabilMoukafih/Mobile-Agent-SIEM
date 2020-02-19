/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
*****************************************************************/



package normalizationManager;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.StringTokenizer;

/**
This behaviour of the Agent serves all the received messages. In particular,
the following expressions are accepted as content of "request" messages:
- (move <destination>)  to move the Agent to another containerName. Example (move Front-End) or
(move (:location (:name containerName-1) (:transport-protocol JADE-IPMT) (:transport-address IOR:0000...) ))
- (exit) to request the agent to exit
- (stop) to stop the counter
- (continue) to continue counting
@author Giovanni Caire - CSELT S.p.A
@version $Date: 2008-10-09 14:04:02 +0200 (gio, 09 ott 2008) $ $Revision: 6051 $
*/
class ServeIncomingMessagesBehaviour extends SimpleBehaviour
{
	private static final long serialVersionUID = 5734195734750419115L;
	
	private String location, logtype, containerName, normalizationType, threadNumber;

	ServeIncomingMessagesBehaviour(Agent a)
	{
		super(a);
	}

	public boolean done()
	{
		return false;
	}

	public void action()
	{
		ACLMessage msg;
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

		// Get a message from the queue or wait for a new one if queue is empty
		msg = myAgent.receive(mt);
		if (msg == null) {
			block();
		 	return;
		}
		else {
			String replySentence = "";

			// Get action to perform
			//String s = msg.getContent().
			StringTokenizer st = new StringTokenizer(msg.getContent(), " ()\t\n\r\f");
			String action = (st.nextToken()).toLowerCase();
			// EXIT
			if      (action.equals("exit"))
			{
				System.out.println("They requested me to exit (Sob!)");
				// Set reply sentence
				replySentence = "\"OK exiting\"";
				myAgent.doDelete();
			}			
			else if (action.equals("deploy-me"))
			{
				// Get the variables
				location=msg.getUserDefinedParameter("Source");
				logtype=msg.getUserDefinedParameter("Type");
				containerName=msg.getUserDefinedParameter("Container");
				normalizationType=msg.getUserDefinedParameter("Norm_type");
				threadNumber=msg.getUserDefinedParameter("Thread_number");

				((Manager)myAgent).location=location;
				((Manager)myAgent).logtype=logtype;
				((Manager)myAgent).containerName=containerName;
				((Manager)myAgent).normalizationType=normalizationType;
				((Manager)myAgent).threadNumber=threadNumber;

				
				((Manager)myAgent).gui.showMessage(
						"Request to normalize :"+containerName+" Using: "+logtype+" with: "+normalizationType+" number "+threadNumber,true);
				((Manager)myAgent).gui.showMessage("Press to deploy ! ", true);


				ACLMessage aclMessage2=msg.createReply();
				aclMessage2.setPerformative(ACLMessage.INFORM);
				aclMessage2.setContent("Well received, please wait !");
				myAgent.send(aclMessage2);
			}

			// Reply
			ACLMessage replyMsg = msg.createReply();
			replyMsg.setPerformative(ACLMessage.INFORM);
			replyMsg.setContent(replySentence);
			myAgent.send(replyMsg);
			
		}

		return;
	}
}


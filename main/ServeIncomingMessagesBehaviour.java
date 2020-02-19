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



package main;

import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.StringTokenizer;

/**
This behaviour of the Agent serves all the received messages. In particular,
the following expressions are accepted as content of "request" messages:
- (move <destination>)  to move the Agent to another container. Example (move Front-End) or
(move (:location (:name Container-1) (:transport-protocol JADE-IPMT) (:transport-address IOR:0000...) ))
- (exit) to request the agent to exit
 */
class ServeIncomingMessagesBehaviour extends SimpleBehaviour
{

	private static final long serialVersionUID = -4699557668136160368L;

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
		//		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

		MessageTemplate mt = MessageTemplate.or
				(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.MatchPerformative(ACLMessage.INFORM));


		// Get a message from the queue or wait for a new one if queue is empty
		msg = myAgent.receive(mt);
		if (msg == null) {
			block();
			return;
		}
		else {

			switch (msg.getPerformative()) {
			case ACLMessage.REQUEST:

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
				// SAY THE CURRENT LOCATION 
				else if (action.equals("where-are-you"))
				{
					System.out.println();
					Location current = myAgent.here();
					System.out.println("Currently I am running on "+current.getName());
					// Set reply sentence
					replySentence = current.getName();
				}

				// Reply
				ACLMessage replyMsg = msg.createReply();
				replyMsg.setPerformative(ACLMessage.INFORM);
				replyMsg.setContent(replySentence);
				myAgent.send(replyMsg);
				break;

			case ACLMessage.INFORM:
				String answer = msg.getContent();
				if (!msg.getSender().equals(myAgent.getAMS())){
					((UserAgent)myAgent).gui.showMessage(answer, true);
				}
				break;

			default:
				break;
			}

		}

		return;
	}
}


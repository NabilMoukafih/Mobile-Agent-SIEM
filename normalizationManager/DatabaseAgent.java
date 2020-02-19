package normalizationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.introspection.AddedBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.misc.FileManagerClient;

public class DatabaseAgent extends Agent {

	private static final long serialVersionUID = 1462772236593279031L;
	private AID [] normalizers;

	/** 
	 * @author Nabil Moukafih, 13 January 2018.
	 * @version $Date: 2018-01-09.
	 * The Database Agent provides the following tasks:
	 *  - Insert data into the database, the data is sent via ACL messages
	 *  - The DatabaseAgent looks for data insertion requests/services using the DF
	 */

	@Override
	protected void setup() {
		System.out.println("The Database Agent is ready!");

		addBehaviour(new TickerBehaviour(this,3500) {

			@Override
			protected void onTick() {
				// Update the list of seller agents
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("data-storage");
				sd.setName("Normalized-data");
				template.addServices(sd);

				try {
					DFAgentDescription [] result = DFService.search(myAgent, template);
					normalizers = new AID [result.length];
					for (int i = 0 ; i < result.length ; ++i) {
						normalizers[i] = result[i].getName(); 
					}
					starTransfer();
				} catch (FIPAException e) {
					e.printStackTrace();
				}
			}
		});


		addBehaviour(new CyclicBehaviour() {
			private static final long serialVersionUID = 4804771124294848776L;

			@Override
			public void action() {
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
				ACLMessage msg= receive(mt);
				if (msg!=null) {
					System.out.println("Preparing to receive cluster: "+msg.getContent());
					FileManagerClient client = new FileManagerClient(normalizers[0],myAgent);

					File downloadFile = new File("/home/nabil/Desktop/Datasets/SotM34/MA-SIEM/" + msg.getContent());
					try {
						InputStream inputStream = client.download(msg.getContent());
						byte[] buffer = new byte[10000];

						FileOutputStream fileOut = new FileOutputStream(downloadFile);

						int count;
						while ((count = inputStream.read(buffer)) > 0)
						{
							fileOut.write(buffer, 0, count);
						}
						System.out.println("Transfer is done");
						fileOut.flush();
						fileOut.close();
						
						ACLMessage aclMessage2 =msg.createReply();
						aclMessage2.setContent("delete-cluster");
						aclMessage2.addUserDefinedParameter("cluster", msg.getContent());
						aclMessage2.setPerformative(ACLMessage.INFORM);
						send(aclMessage2);


						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
					block();
				}

			}
		});

	}

	private void starTransfer() {
		if (normalizers.length!=0) {
			System.out.println("Working with normalizer: "+normalizers[0]);

			ACLMessage aclMessage = new ACLMessage(ACLMessage.INFORM);
			aclMessage.addReceiver(normalizers[0]);
			aclMessage.setContent("what-cluster");
			send(aclMessage);
		}
	}

}

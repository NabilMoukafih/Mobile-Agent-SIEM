package normalizationManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ContainerID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.misc.FileManagerServer;
import jade.util.leap.List;
import jade.util.leap.Serializable;

/**
 * @author Nabil Moukafih, 13 January 2018.
 * @version $Date: 2018-01-09.
 * 
 *	Plugin-Agent for normalizing IPtables logs.
 *	The Normalizer agent migrates to the source device in order to normalize
 * 	the events. The agent is created with arguments (An array of Regex, 
 * 	the container name to migrate to, and the address which is the location
 * 	of the log file within the source device.
 */
public class IptablesNormalizer extends Agent{

	private static final long serialVersionUID = -3523639744548371318L;

	private String location, containerName;
	private ContainerID destination;
	private NormalizeLogBehaviour logBehaviour;
	private int period;
	private transient Path path;

	@Override
	protected void setup() {

		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(MobilityOntology.getInstance());
		System.out.println("Agent: "+getAID().getLocalName()+" is ready!");

		//Get the parameters of the created agent
		Object[] args = getArguments();
		if (args!=null){
			containerName=(String) args[0];
			location=(String) args[1];
			period=(int) args[2];
		}

		addBehaviour(new OneShotBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				// Create some variables and migrate
				destination = new ContainerID();
				destination.setName(containerName);
				myAgent.doMove(destination);
			}
		});

	}

	protected void beforeMove() 
	{
		System.out.println(getLocalName()+" is now moving elsewhere.");

	}

	@Override
	protected void afterMove() {
		// Verify the location at the source machine !
		path = Paths.get(location);
		if (Files.exists(path)){

			doClone(destination, "clone-"+getLocalName());

			logBehaviour = new NormalizeLogBehaviour(this,location,period);
			addBehaviour(logBehaviour);
		}
		else {
			System.out.println("File not found ! killing agent");
			takeDown();
			doDelete();
		}
	}

	@Override
	protected void afterClone() {
		removeBehaviour(logBehaviour);

		// Use the FileManagerServer API to transfer files
		path = Paths.get(location);
		String root = path.getParent().toString();
		FileManagerServer server = new FileManagerServer();
		server.init(this, root);
		// List of created clusters
		ArrayList<String> clusters = new ArrayList<String>();

		// System.out.println("Printing before looping "+clusters);
		// This behaviour updates the created lists every 15 seconds.
		addBehaviour(new TickerBehaviour(this,period+2000) {
			@Override
			protected void onTick() {
//				System.out.println("Going to update");
				updateClusterList(clusters);
//				System.out.println("Printing while looping "+clusters);
				check_Service(clusters);
			}
		});
		
		addBehaviour(new CyclicBehaviour(this) {
			
			@Override
			public void action() {
				ACLMessage msg;
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				// Get a message from the queue or wait for a new one if queue is empty
				msg = myAgent.receive(mt);
				if (msg == null) {
					block();
				 	return;
				}
				else {
					StringTokenizer st = new StringTokenizer(msg.getContent(), " ()\t\n\r\f");
					String action = (st.nextToken()).toLowerCase();
					if      (action.equals("what-cluster")) {
						
						System.out.println("Preparing to send cluster: "+clusters.get(0));
						ACLMessage message = msg.createReply();
						message.setPerformative(ACLMessage.CFP);
						message.setContent(clusters.get(0));
						send(message);
						
					}
					
					else if (action.equals("delete-cluster") ) {
						Path path2del=null;
						System.out.println("Deleting the cluster : "+msg.getUserDefinedParameter("cluster"));
						try {
							path2del = Paths.get(root+"/"+msg.getUserDefinedParameter("cluster"));
						    Files.delete(path2del);
						} catch (NoSuchFileException x) {
						    System.err.format("%s: no such" + " file or directory%n", path2del);
						} catch (DirectoryNotEmptyException x) {
						    System.err.format("%s not empty%n", path2del);
						} catch (IOException x) {
						    // File permission problems are caught here.
						    System.err.println(x);
						}
					}

				}
			}
		});


	}

	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Collector-agent "+getAID().getName()+" terminated.");
	}

	private ArrayList<String> updateClusterList (ArrayList<String> L) {
		// Update the list of created clusters
		String cvs = "^("+location+"-norm)"+"?\\d+"+"(.csv)$";
		Pattern cvspattern = Pattern.compile(cvs);
		File[] listOfFiles = path.getParent().toFile().listFiles();
		L.clear();
		for (File ele:listOfFiles) {
			Matcher cvsmatcher = cvspattern.matcher(ele.toString());
			if (ele.isFile() && cvsmatcher.find() && L.contains(ele.getName().toString())==false) {
				L.add(ele.getName().toString());
			} 
		}
		L.sort(String::compareToIgnoreCase);
		return L;
	}

	private void check_Service(ArrayList<String> clusters) {		
		// Register the data insertion service
		if (clusters.isEmpty()==false) {
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			ServiceDescription sd = new ServiceDescription();
			sd.setType("data-storage");
			sd.setName("Normalized-data");
			dfd.addServices(sd);
			if (verifyDF() == false )
			{
				try {
					System.out.println("------------ Publishing service ------------ ");
					DFService.register(this, dfd);
				} catch (FIPAException e) {
					e.printStackTrace();
				}
			}

			else {
				System.out.println("------------ Service is published ------------");
			}
		}
		else {
			// Deregister from the yellow pages
			try {
				if(verifyDF() == true)
				{
					System.out.println("------------ Deleting service ------------");
					DFService.deregister(this);
				}
				else {
					System.out.println("Service Deleted");
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
		}
	}

	private boolean verifyDF() {
		// Local template 
		DFAgentDescription ltemplate = new DFAgentDescription();
		ServiceDescription lsd = new ServiceDescription();
		lsd.setType("data-storage");
		lsd.setName("Normalized-data");
		ltemplate.addServices(lsd);
		DFAgentDescription[] result;
		try {
			result = DFService.search(this, ltemplate);
			for (int i = 0; i < result.length; ++i) {
				if (result[i].getName().equals(getAID()) ) {
					return true; }
			}
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		return false;
	}

	/*
	 * Inner class NormalizeLogBehaviour.
	 * This Behavior does the following task:
	 * Creates a default Cluster a construction to store the normalized logs
	 * Creates clusters every 5 seconds; as long as the log file has content. 
	 */

	private class NormalizeLogBehaviour extends OneShotBehaviour implements Serializable {

		private static final long serialVersionUID = 3667417990237644875L;

		private String location;
		private int CLUSTER_NUMBER=0;
		private float numline=0;
		private float numparsed=0;
		private long time;
		private long startTime, wakeupTime;

		// Regex  variables
		private String date, src_ip, dst_ip, protocol, src_port, dst_port;
		private String Server, SrcWpid, Sid, In, Out, Physout, Physin, Ttl, Len, Mac;

		// Prepare the .csv file to receive the normalized data
		String com=",";
		String arr[] = {"Date","Source IP", "Destination IP", "Source Port", "Destination Port", "Protocol"};
		String fileLine = ""+ arr[0] + com + arr[1] + com + arr[2] + com + arr[3] + com + arr[4] + com + arr[5] +"\t";

		// To read and write on the file
		protected transient BufferedWriter bufferedWriter;
		protected transient BufferedReader bufferedReader;

		public NormalizeLogBehaviour(Agent a, String str, long period) {
			location=str;
			if (period <= 0) {
				throw new IllegalArgumentException("Period must be greater than 0");
			}
			time=period;

			try {
				bufferedReader = new BufferedReader(new FileReader(location));
				// Create the first cluster
				createCluster();

			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}

		public void onStart() {
			startTime = System.currentTimeMillis();
			wakeupTime =  startTime + time;
		}

		@Override
		public void action() {
			//Normalize 
			String line=null;
			try {
				//measuring elapsed time using System.nanoTime
				long startTime = System.nanoTime();
				while ((line = bufferedReader.readLine()) != null) {

					// Count the read lines
					numline++;

					long blockTime = wakeupTime - System.currentTimeMillis();

					// Normalize
					String tmp=doNormalize(line);
					if (tmp!=null) {
						bufferedWriter.write(tmp+"\n");
						numparsed++;
					}


					// Create a cluster each x seconds
					if (blockTime<=0) {
						try {
							System.out.println("Total number of read lines for Cluster "+(CLUSTER_NUMBER-1)+" is: "+(numline));
							System.out.println("Total number of prased lines in Cluster "+(CLUSTER_NUMBER-1)+" is: "+numparsed);
							float moy=100*(numparsed/numline);
							System.out.format("Percentage of matched logs in Cluster "+(CLUSTER_NUMBER-1)+" is: %.2f %% \n",moy);
							bufferedWriter.flush();
							bufferedWriter.close();
							System.out.println("Creating cluster number "+CLUSTER_NUMBER);
							createCluster();
							// restore the values
							numline=0;
							numparsed=0;
						} catch (IOException e) {
							e.printStackTrace();
						}
						long currentTime = System.currentTimeMillis();
						wakeupTime = currentTime + time;
						blockTime = wakeupTime - currentTime;
					} 

				}
				if ((line = bufferedReader.readLine()) == null){
					long elapsedTime = System.nanoTime() - startTime;
					System.out.println("Total execution time to normalize in millis: "
							+ elapsedTime/1000000);
					System.out.println("Total number of read lines for last Cluster "+(numline));
					System.out.println("Total number of prased lines in last Cluster is: "+numparsed);
					float moy=100*(numparsed/numline);
					System.out.format("Percentage of matched logs in last Cluster is: %.2f %% \n",moy);

				}
			} catch (IOException e) {
				System.out.println("Problem reading the log file!");
				e.printStackTrace();
			}

		}

		@Override
		public int onEnd() {
			try {
				if (bufferedWriter != null) 
				{
					bufferedWriter.flush();
					bufferedWriter.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return super.onEnd();
		}

		private void createCluster(){
			// Prepare the cluster number i
			try {
				String clusterName=location+"-norm"+CLUSTER_NUMBER+".csv";
				System.out.println("Creating cluster: "+new File(clusterName).getName());
				bufferedWriter = new BufferedWriter(new FileWriter(clusterName, true));
				bufferedWriter.write(fileLine+ "\n");
				CLUSTER_NUMBER++;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//Pattern pattern, Matcher matcher
		private String doNormalize(String line){
			String rules [] = getRules();
			String tmp=null;

			for (String rule : rules) {
				Pattern pattern = Pattern.compile(rule);
				Matcher matcher = pattern.matcher(line);

				if (matcher.find()){
					date=matcher.group("SyslogDate");
					Server=matcher.group("Server");
					protocol=matcher.group("protocol");
					src_ip=matcher.group("SrcIp");
					dst_ip=matcher.group("DstIp");
					src_port=matcher.group("SrcPort");
					dst_port=matcher.group("DstPort");
					tmp = ""+ date + com + src_ip + com + dst_ip + com + src_port + com + dst_port +
							com + protocol+"\t";
					break;
				}
			}
			return tmp;

		}

		private String [] getRules() {

			final String IPregex = "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])";

			// accept, ACCEPT, REJECT, DROP, DENY, Inbound, Outbound, INBOUND CONN, OUTBOUND CONN
			final String regex0 = "(?<SyslogDate>\\S+\\s+\\d+\\s+\\d\\d:\\d\\d:\\d\\d)\\s+(?<Server>\\S+) (?<SrcWpid>\\S+)" +
					":.*?(?<Sid>[Aa][Cc][Cc][Ee][Pp][Tt]|[Rr][Ee][Jj][Ee][Cc][Tt]|[Dd][Rr][Oo][Pp]|[Dd][Ee][Nn][Yy]|[Ii][Nn][Bb][Oo][Uu][Nn][Dd]|[Ii][Nn][Bb][Oo][Uu][Nn][Dd] [Cc][Oo][Nn][Nn]|[Oo][Uu][Tt][Bb][Oo][Uu][Nn][Dd]|[Oo][Uu][Tt][Bb][Oo][Uu][Nn][Dd] [Cc][Oo][Nn][Nn])\\s+\\S*\\sIN=(?<in>\\S*)\\s+PHYSIN=(?<physin>\\S*)" +
					"\\s+OUT=(?<out>\\S*)\\s+PHYSOUT=(?<physout>\\S*)\\s+(?:MAC=(?<mac>[^\\s]*)\\s+)?SRC=(?<SrcIp>"+IPregex+")" +
					"\\s+DST=(?<DstIp>"+IPregex+") LEN=(?<len>\\d+) \\S+ \\S+ TTL=(?<ttl>\\d+) .*? PROTO=(?<protocol>\\S*)" +
					"(?: SPT=(?<SrcPort>\\d+) DPT=(?<DstPort>\\d+))?";

			String regex []= {regex0};	
			return regex;
		}
	} // End of Inner Class
} // End of Iptables Plugin Class


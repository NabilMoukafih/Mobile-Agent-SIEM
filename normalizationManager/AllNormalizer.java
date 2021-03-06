package normalizationManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jade.content.lang.sl.SLCodec;
import jade.core.Agent;
import jade.core.ContainerID;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.mobility.MobilityOntology;
import jade.util.leap.Serializable;

/**
 * @author Nabil Moukafih, 13 January 2018.
 * @version $Date: 2018-01-09.
 * 
 *	Plugin-Agent for normalizing all the SotM34 logs.
 *	The Normalizer agent migrates to the source device in order to normalize
 * 	the events. The agent is created with arguments (An array of Regex, 
 * 	the container name to migrate to, and the address which is the location
 * 	of the log file within the source device.
 */
public class AllNormalizer extends Agent{

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
			// doClone(destination, "clone-"+getLocalName());
			logBehaviour = new NormalizeLogBehaviour(this,location,period);
			addBehaviour(logBehaviour);
		}
		else {
			System.out.println("File not found ! killing agent");
			takeDown();
			doDelete();
		}
	}

	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Collector-agent "+getAID().getName()+" terminated.");
	}



	/*
	 * Inner class NormalizeLogBehaviour.
	 * This Behaviour does the following task:
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
		private String date, src_ipv4, src_ipv6, src_host, protocol, dst_port;
		private String ClientRequestLine, StatusCode, ObjectSize, Agent, serverity, data;

		// Prepare the .csv file to receive the normalized data
		String com=",";
		String arr0[] = {"Date","Source IP", "Destination Port", "Protocol" ,"Client Request Line", "Status Code", "Object Size", "Agent"};
//		String arr1[] = {"Date", "Source IP", "Serverity", "Data"};
		String fileLine = ""+ arr0[0] + com + arr0[1] + com + arr0[2] + com + arr0[3] + com + arr0[4] + com + arr0[5] + com + arr0[6] + com + arr0[7] + "\t";

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
			String tmp="";

			for (String rule : rules) {
				Pattern pattern = Pattern.compile(rule);
				Matcher matcher = pattern.matcher(line);

				if (matcher.find()){
					Set<String> namedGroups = getNamedGroupCandidates(rule);
					Iterator<String> itr = namedGroups.iterator();
					while(itr.hasNext()){
						String value=itr.next();
						tmp=tmp+", "+matcher.group(value);
					}
						tmp = tmp + "\t";
						break;
				}
			}
			return tmp;

		}

		private String [] getRules() {

			final String IPregex = "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
			//Http
			final String regex0 = "^(?:(?<NetworkSrcIpv4>(?:[0-9]{1,3}\\.){3}[0-9]{1,3})|(?<NetworkSrcIpv6>[:\\-0-9a-fA-F]+?)|(?<NetworkSrcHost>.+?)) - (?:-|(?<UserUsername>.+)) \\[(?<Time>.*)\\] \\\"(?<ApplicationCmd>(?<ApplicationHttpMethod>[A-Z]+)\\s(?:(?<ApplicationProto>.*?)://)?(?<NetworkFqdn>[^/]*?)(?:\\:(?<NetworkDstPort>\\d+))?(?<FilePath>/.*?)?(?:\\?(?<ApplicationHttpQueryString>.*?))?(?: HTTP/(?<ApplicationHttpVersion>[0-9\\.]+)?))\\\" (?<ApplicationHttpStatus>\\d+) (?<ApplicationLen>\\d+) (?:\"(?:-|(?<ApplicationHttpReferrer>.*))\")? (?:\"(?:-|(?<ApplicationHttpUserAgent>.*))\")$";
			final String regex1 = "^\\[(?<date>\\w{3}\\s+\\w{3}\\s+\\d{1,2} \\d\\d:\\d\\d:\\d\\d \\d{4})\\] \\[(?<type>(emerg|alert|crit|error|warn|notice|info|debug))\\] (\\[client (?<src>"+IPregex+")\\] )?(?<data>.*)";
			
			// Iptables
			// accept, ACCEPT, REJECT, DROP, DENY, Inbound, Outbound, INBOUND CONN, OUTBOUND CONN
			final String regex2 = "(?<SyslogDate>\\S+\\s+\\d+\\s+\\d\\d:\\d\\d:\\d\\d)\\s+(?<Server>\\S+) (?<SrcWpid>\\S+)" +
					":.*?(?<Sid>[Aa][Cc][Cc][Ee][Pp][Tt]|[Rr][Ee][Jj][Ee][Cc][Tt]|[Dd][Rr][Oo][Pp]|[Dd][Ee][Nn][Yy]|[Ii][Nn][Bb][Oo][Uu][Nn][Dd]|[Ii][Nn][Bb][Oo][Uu][Nn][Dd] [Cc][Oo][Nn][Nn]|[Oo][Uu][Tt][Bb][Oo][Uu][Nn][Dd]|[Oo][Uu][Tt][Bb][Oo][Uu][Nn][Dd] [Cc][Oo][Nn][Nn])\\s+\\S*\\sIN=(?<in>\\S*)\\s+PHYSIN=(?<physin>\\S*)" +
					"\\s+OUT=(?<out>\\S*)\\s+PHYSOUT=(?<physout>\\S*)\\s+(?:MAC=(?<mac>[^\\s]*)\\s+)?SRC=(?<SrcIp>"+IPregex+")" +
					"\\s+DST=(?<DstIp>"+IPregex+") LEN=(?<len>\\d+) \\S+ \\S+ TTL=(?<ttl>\\d+) .*? PROTO=(?<protocol>\\S*)" +
					"(?: SPT=(?<SrcPort>\\d+) DPT=(?<DstPort>\\d+))?";
			//Snort
			final String regex3 = "(?<date>\\w+\\s+\\d{1,2}\\s+\\d\\d:\\d\\d:\\d\\d)\\s+(?<device>[\\w\\-\\_]+|\\d+.\\d+.\\d+.\\d+)\\s+(?:suricata|snort)?.*?:?\\s*\\[(?<snortId>\\d+):\\s*(?<sid>\\d+):\\d+\\][^\\{]*\\{(?<protocol>\\w+)\\}\\s+(?<SrcIp>"+IPregex+"):?(?<SrcPort>\\d+)?\\s*.*?\\s*(?<DstIp>"+IPregex+"):?(?<DstPort>\\d+)?";
			//Syslog
			final String regex4 = "^(?<logline>(?<date>\\w{3}\\s+\\d{1,2}\\s\\d\\d:\\d\\d:\\d\\d)\\s+(?<sensor>\\S+)\\s+(?<source>\\S+)\\s+(?<loggedEvent>.*)$)$";

			String regex []= {regex0,regex1,regex2,regex3,regex4};	
			return regex;
		}
		
		private Set<String> getNamedGroupCandidates(String regex) {
			Set<String> namedGroups = new TreeSet<String>();

			Matcher m = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher(regex);

			while (m.find()) {
				namedGroups.add(m.group(1));
			}

			return namedGroups;
		}
	} // End of Inner Class
} // End of All Plugin Class


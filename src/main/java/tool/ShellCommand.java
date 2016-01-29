package tool;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import com.fasterxml.jackson.databind.JsonNode;

public class ShellCommand{

	private static Connection conn;
	private static SCPClient scp;
	private static String outStr;
	private static String outErr;
	private static String charset = Charset.defaultCharset().toString();
	private static String initialConfigFilePath = StringUtils.defaultString(System.getenv("SeleniumConfigFile"), "conf/sal/default.properties");
	private static String testRoot = GeneralMethods.getTestRoot();
	private static JsonNode initialConfig = GeneralMethods.getDataFromConfigFile(testRoot + initialConfigFilePath);
	private static String FILEDIR = testRoot + "data/sal/";
	private String IP = GeneralMethods.getConfigValue(initialConfig, "ip");
	private String DEVICEIP = GeneralMethods.getConfigValue(initialConfig, "deviceip");
	private String USER = GeneralMethods.getConfigValue(initialConfig, "user");
	private String PASSWORD = GeneralMethods.getConfigValue(initialConfig, "password");
	private String LOCALFILE = GeneralMethods.getConfigValue(initialConfig, "localFile");
	private String REMOTEDIR = GeneralMethods.getConfigValue(initialConfig, "remoteDir");

	/**
	 * Login the remote Linux PC/VM via SSH connection.
	 * 
	 * @return
	 * @throws IOException
	 */
	private boolean login() throws IOException{

		boolean flag = false;
		conn = new Connection(IP);
		conn.connect();
		flag = conn.authenticateWithPassword(USER, PASSWORD);
		return flag;

	}

	public void SendScriptFile(){

		SendScriptFile("");
	}

	public void SendScriptFile(String locaFile){

		SendScriptFile(locaFile, "");
	}

	/**
	 * Copy the local file into the remote directory via SCP protocol
	 * 
	 * @param localFile
	 *            : The file will be copied in local.
	 * @param remoteDir
	 *            : The remote directory that saved the copied file
	 */
	public void SendScriptFile(String localFile, String remoteDir){

		if(localFile.isEmpty())
			localFile = this.LOCALFILE;
		log("The test script file is " + localFile, 1);
		if(remoteDir.isEmpty())
			remoteDir = this.REMOTEDIR;
		log("Remote directory is " + remoteDir, 1);
		try{
			if(login())
				scp = new SCPClient(conn);
			scp.put(FILEDIR + localFile, 1024, remoteDir, "");
		} catch(IOException e){
			throw new RuntimeException("Upload file failed!");
		}
	}

	/**
	 * Login the remote PC/VM and execute shell command
	 * 
	 * @param cmds
	 * @return
	 * @throws Exception
	 */
	private int exec(String cmds) throws Exception{

		InputStream stdOut = null;
		InputStream stdErr = null;
		outStr = "";
		outErr = "";
		int ret = - 1;
		try{
			if(login()){
				// Open a new {@link Session} on this connection
				Session session = conn.openSession();
				// Execute a command on the remote machine.
				session.execCommand(cmds);

				stdOut = new StreamGobbler(session.getStdout());
				outStr = processStream(stdOut, charset);

				stdErr = new StreamGobbler(session.getStderr());
				outErr = processStream(stdErr, charset);

				session.waitForCondition(ChannelCondition.EXIT_STATUS, 1000 * 60 * 5);
				if( ! outStr.isEmpty())
					log(outStr, 1);
				if( ! outErr.isEmpty())
					ret = session.getExitStatus();
			}
		} finally{
			if(conn != null){
				conn.close();
			}
			IOUtils.closeQuietly(stdOut);
			IOUtils.closeQuietly(stdErr);
		}
		return ret;
	}

	private String processStream(InputStream in, String charset) throws Exception{

		byte[] buf = new byte[1024];
		StringBuilder sb = new StringBuilder();
		while(in.read(buf) != - 1){
			sb.append(new String(buf, charset));
		}
		return sb.toString();
	}

	/**
	 * Define the shell command which will be executed on the remote PC/VM
	 * 
	 * @param command
	 * @return
	 */
	public boolean execute(String command, String DEVICEIP){

		boolean flag = false;

		if(command.isEmpty()){
			if( ! DEVICEIP.isEmpty() || ! this.DEVICEIP.isEmpty()){
				if(DEVICEIP.isEmpty())
					DEVICEIP = this.DEVICEIP;
				command = "test_lib/bin/pyrun --device " + DEVICEIP + " " + REMOTEDIR + "/" + LOCALFILE;

			} else
				command = "test_lib/bin/pyrun " + REMOTEDIR + "/" + LOCALFILE;
		}
		try{
			log("<===============COMMENCING THE TEST================>", 1);
			log("Test==>" + LOCALFILE, 1);
			exec(command);

		} catch(Exception e){
			throw new RuntimeException("Execute shell command failed!");
		}

		if(outErr.isEmpty())
			flag = true;

		return flag;
	}

	public boolean execute(String DEVICEIP){

		return execute("", DEVICEIP);

	}

	public boolean execute(){

		return execute("", "");

	}

	private static void log(String content, Integer type){

		switch(type){
		case 1:{
			System.out.println(GeneralMethods.getCurrentTime() + " INFO - " + content);
			break;
		}
		case 2:{
			System.err.println(GeneralMethods.getCurrentTime() + " ERROR - " + content);
			break;
		}
		case 3:{
			System.out.println(GeneralMethods.getCurrentTime() + " WARNING - " + content);
			break;
		}
		}
	}
}

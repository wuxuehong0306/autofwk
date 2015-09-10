package core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mozilla.universalchardet.UniversalDetector;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class InitClass {

	static {
		disableLog4jLogs();
	}
	private static String initialConfigFilePath = "conf/main.properties";
	protected static String testRoot = getTestRoot();
	protected static String testConfigRoot;
	protected static String testDataRoot;
	private static String projectLevelConfConfigPath = "";
	protected static String projectLevelBrowserConfigPath = "";
	protected static String projectLevelUiConfigPath = "";

	protected String pageTimeout = "30000";
	protected String viewTimeout = "20000";
	protected String elementTimeout = "15000";

	protected String appUrl = "";
	protected String appLocale = "";

	protected static ObjectMapper jsonMapper = new ObjectMapper();
	protected static JsonNode initialConfig = jsonMapper.createObjectNode();
	protected JsonNode conf = jsonMapper.createObjectNode();

	protected final static String UIMAP_DELIM = ":";

	protected abstract String getAppType();

	protected InitClass() {

		log(">==================================================<");
		try {
			ITestResult it = Reporter.getCurrentTestResult();
			log("Now Starting: " + it.getTestClass().getName());
		} catch (Exception e) {
			log("None testNG executor detected, test may continue, but highly recommended to migrate your test to testNG.", 3);
		}
		frameworkInitiate();
		languageSupportInitiate();
		platformSupportInitiate("");
		applicationSupportInitiate();
	}

	private void frameworkInitiate() {

		testConfigRoot = testRoot.replace("target/test-classes/", "src/test/resources/conf/");
		testDataRoot = testRoot.replace("target/test-classes/", "src/test/resources/data/");
		initialConfig = getDataFromConfigFile(testRoot + initialConfigFilePath);

		projectLevelConfConfigPath = testConfigRoot + getAppType();
		conf = getDataFromConfigFile(projectLevelConfConfigPath + "/" + getDefaultConfFileName());

	}

	protected void languageSupportInitiate() {

	}

	protected void platformSupportInitiate(String profileName) {

	}

	protected void applicationSupportInitiate() {

	}

	protected JsonNode getConfProperty(String fileName) {

		JsonNode returnNode = jsonMapper.createObjectNode();
		File checkFile = new File(fileName);
		if (checkFile.exists() && checkFile.isFile())
			returnNode = getDataFromConfigFile(fileName);
		else {
			returnNode = null;
		}
		return returnNode;
	}

	protected String getInitialProperty(String propertyKey) {

		return getConfigValue(initialConfig, propertyKey);
	}

	private String getDefaultConfFileName() {

		String confFileKey = String.format("conf.%s.file", getAppType());

		String rtn = getInitialProperty(confFileKey);
		if (rtn == null || StringUtils.isEmpty(rtn)) {
			log("!Warning: can't find any key " + confFileKey + " in the initial config file. ", 3);
		}
		return rtn;
	}

	public String getProperty(String propertyKey) {

		return getConfigValue(conf, propertyKey);
	}

	protected void prepareTestEnvironment() {

	}

	protected String getAppUrl() {

		return appUrl;
	}

	protected JsonNode getJsonNodeMatching(JsonNode nodesToSearch, String fieldName, String regex) {

		for (JsonNode currentNode : nodesToSearch) {

			if (StringUtils.isNotEmpty(currentNode.path(fieldName).textValue()) && StringUtils.isNotEmpty(regex)) {

				if (StringUtils.isNotEmpty(currentNode.path(fieldName).textValue())
						&& currentNode.path(fieldName).textValue().matches(regex)) {

					return currentNode;
				}
			}
		}

		return jsonMapper.createObjectNode();
	}

	public void log(String content, Integer type) {

		switch (type) {
		case 1: {
			System.out.println(getCurrentTime() + " (" + Thread.currentThread().getId() + ") INFO - " + content);
			break;
		}
		case 2: {
			System.err.println(getCurrentTime() + " (" + Thread.currentThread().getId() + ") ERROR - " + content);
			break;
		}
		case 3: {
			System.out.println(getCurrentTime() + " (" + Thread.currentThread().getId() + ") WARNING - " + content);
			break;
		}
		case 4: {
			System.err.println(getCurrentTime() + " (" + Thread.currentThread().getId() + ") WARNING - " + content);
			break;
		}
		}

	}

	public void log(String content) {

		log(content, 1);
	}

	private static void disableLog4jLogs() {

		@SuppressWarnings("unchecked")
		List<Logger> loggers = Collections.<Logger> list(LogManager.getCurrentLoggers());
		loggers.add(LogManager.getRootLogger());
		for (Logger logger : loggers) {
			logger.setLevel(Level.OFF);
		}
	}

	public static String getTimeStamp() {

		String timeStamp = getDate().replace("-", "") + getCurrentTime().replace(":", "").replace(".", "");

		return timeStamp;
	}

	public static String getConfigValue(JsonNode configObject, String propertyName) {

		String returnValue = StringUtils.defaultString(System.getenv(propertyName));

		if (StringUtils.isBlank(returnValue)) {
			JsonNode targetNode = configObject.path(propertyName);
			returnValue = StringUtils.defaultString(targetNode.textValue());
		}

		while (returnValue.contains("{{") && returnValue.contains("}}")) {

			String embeddedPropertyName = StringUtils.substringBetween(returnValue, "{{", "}}");
			String embeddedPropertyValue = getConfigValue(configObject, embeddedPropertyName);

			returnValue = StringUtils.replace(returnValue, ("{{" + embeddedPropertyName + "}}"), embeddedPropertyValue);
		}

		return returnValue;
	}

	public static JsonNode getDataFromConfigFile(String configFileName, String testClass) {

		boolean fileFound = true;
		String fileName = configFileName;
		String testRoot = getTestRoot();

		JsonNode jsonNode = jsonMapper.createObjectNode();

		if (!(new File(fileName)).exists()) {

			fileFound = false;

			while (!(StringUtils.isBlank(testClass) || fileFound)) {

				testClass = StringUtils.substring(testClass, 0, StringUtils.lastIndexOf(testClass, "."));

				fileName = testRoot + StringUtils.replace(testClass, ".", "/") + "/data/" + configFileName;
				fileFound = (new File(fileName)).exists();
			}
		}

		try {

			try {

				jsonNode = jsonMapper.readValue(new File(fileName), JsonNode.class);
			}

			catch (Exception e) {

				Properties pro = new Properties();
				FileInputStream fis = new FileInputStream(fileName);
				InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
				BufferedReader brProp = new BufferedReader(isr);
				pro.load(brProp);
				brProp.close();
				ObjectMapper mapper = new ObjectMapper();
				ObjectNode node = mapper.createObjectNode();

				for (Entry<Object, Object> element : pro.entrySet()) {

					node.put(element.getKey().toString(),
							StringEscapeUtils.unescapeHtml(element.getValue().toString().replaceAll("\\<.*?>", "")));
				}

				jsonNode = (JsonNode) node;
			}
		} catch (IOException e) {
			throw new IllegalStateException("Can't locate config file " + fileName, e);
		}

		return jsonNode;
	}

	public static JsonNode getDataFromConfigFile(String configFileName) {

		return getDataFromConfigFile(configFileName, "");
	}

	public static String getCurrentTime() {

		Date today = new Date();
		SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss.SSS");
		String time = f.format(today);

		return time;
	}

	public static String getDate() {

		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.format(date);
	}

	public static String getTestRoot() {

		String testRoot = StringUtils.defaultString(Thread.currentThread().getContextClassLoader().getResource(".").getPath());

		if (StringUtils.contains(testRoot, ":/"))
			testRoot = StringUtils.substring(testRoot, 1);

		return testRoot;
	}

	public static JsonNode getInitialConfig() {

		String initialConfigFile = StringUtils.defaultString(System.getenv("SeleniumConfigFile"),
				"conf/AutotestConfig.properties");

		String configFile = getTestRoot().replace("target/test-classes/", "") + "src/test/resources/" + initialConfigFile;

		JsonNode initialConfig = getDataFromConfigFile(configFile);

		return initialConfig;
	}

	public static JsonNode getInitialConfig(String initialConfigFileName) {

		String configFile = getTestRoot().replace("target/test-classes/", "") + "src/test/resources/" + initialConfigFileName;

		JsonNode initialConfig = getDataFromConfigFile(configFile);

		return initialConfig;
	}

	public static String replaceIllegalFileName(String fileName, String newChar) {

		if (fileName != null) {

			fileName = fileName.replace("\\", newChar);
			fileName = fileName.replace("/", newChar);
			fileName = fileName.replace(":", newChar);
			fileName = fileName.replace("*", newChar);
			fileName = fileName.replace("?", newChar);
			fileName = fileName.replace("\"", newChar);
			fileName = fileName.replace("<", newChar);
			fileName = fileName.replace(">", newChar);
			fileName = fileName.replace("|", newChar);
		}

		return fileName;
	}

	@SuppressWarnings("deprecation")
	public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {

		Iterator<String> fieldNames = updateNode.fieldNames();
		while (fieldNames.hasNext()) {

			String fieldName = fieldNames.next();
			JsonNode jsonNode = mainNode.get(fieldName);
			if (jsonNode != null && jsonNode.isObject()) {
				merge(jsonNode, updateNode.get(fieldName));
			} else {
				if (mainNode instanceof ObjectNode) {
					JsonNode value = updateNode.get(fieldName);
					if (jsonNode != null) {
						if (jsonNode.isArray() && value.isArray()) {
							String temp1 = jsonNode.toString();
							String temp2 = value.toString();
							temp1 = temp1.substring(1, temp1.length() - 1);
							temp2 = temp2.substring(1, temp2.length() - 1);

							try {
								((ObjectNode) mainNode).put(fieldName,
										jsonMapper.readValue("[" + temp1 + "," + temp2 + "]", JsonNode.class));
							} catch (Exception e) {

							}

						} else {
							((ObjectNode) mainNode).put(fieldName, value);
						}
					} else
						((ObjectNode) mainNode).put(fieldName, value);

				}

			}

		}
		return mainNode;
	}

	public static JsonNode mergeProperties(JsonNode target, JsonNode... extraProperties) {

		for (JsonNode currentContent : extraProperties) {

			if (currentContent != null) {
				target = (ObjectNode) merge(target, currentContent);
			}
		}
		return target;
	}

	public static String detectCharset(byte[] buf) {

		UniversalDetector detector = new UniversalDetector(null);
		detector.handleData(buf, 0, buf.length);
		detector.dataEnd();
		String encoding = detector.getDetectedCharset();
		detector.reset();
		return encoding;
	}

	public static String detectCharset(InputStream in) {

		byte[] buf = new byte[4096];

		UniversalDetector detector = new UniversalDetector(null);

		int nread;
		try {
			while ((nread = in.read(buf)) > 0 && !detector.isDone()) {
				detector.handleData(buf, 0, nread);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		detector.dataEnd();
		String encoding = detector.getDetectedCharset();
		detector.reset();
		return encoding;
	}
}

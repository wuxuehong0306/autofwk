package fwk;

import io.appium.java_client.NetworkConnectionSetting;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidKeyCode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;

import tool.AndroidMonitor;
import core.UiClass;

public class AndroidFwk extends UiClass {

	public AndroidFwk() {

		super();
	}

	protected String getAppType() {

		return "AndroidApp";
	}

	public boolean openApp() {

		startAppiumDriver();
		boolean validate = uiMapUpdateView("");

		return validate;

	}

	@Override
	protected void platformSupportInitiate(String profileName) {

		prepareTestEnvironment();
	}

	public void getPageSource() {

		log(androidDriver.getPageSource());
	}

	@SuppressWarnings("rawtypes")
	protected void startAppiumDriver() {

		try {
			File classpathRoot = new File(System.getProperty("user.dir"));
			File app = new File(classpathRoot, getProperty("app.path"));
			log("Launch the application '" + app + "'.");

			DesiredCapabilities capabilities = new DesiredCapabilities();
			capabilities.setCapability(CapabilityType.BROWSER_NAME, getProperty("app.browser.Name"));
			capabilities.setCapability("platformVersion", getProperty("app.device.version"));
			capabilities.setCapability("platform", getProperty("app.os.platform"));
			capabilities.setCapability("deviceName", getProperty("app.device.name"));
			capabilities.setCapability("platformName", getProperty("app.device.platformName"));
			capabilities.setCapability("newCommandTimeout", getProperty("app.command.timeout"));
			capabilities.setCapability("app", app.getAbsolutePath());
			capabilities.setCapability("appPackage", getProperty("app.package"));
			capabilities.setCapability("appActivity", getProperty("app.activity"));
			capabilities.setCapability("appWaitActivity", getProperty("app.wait.activity"));

			androidDriver = new AndroidDriver(new URL("http://" + getProperty("app.appium.serverIP") + "/wd/hub"), capabilities);
			driver = androidDriver;
		} catch (Exception e) {
			log("Cannot launch application!", 2);
			throw new RuntimeException(e);
		}

		new WebDriverWait(androidDriver, 10);
	}

	public String getAndroidVersion() {

		try {
			Process process = Runtime.getRuntime().exec("adb shell getprop ro.build.version.release");
			InputStream is = process.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String version = br.readLine();
			log("device version " + version);
			int points = 0;
			for (int i = 0; i < version.length(); i++) {
				if (version.substring(i, i + 1).equals(".")) {
					points++;
					if (points == 2) {
						return version.substring(0, i);
					}
				}
			}
			return version;

		} catch (Exception e) {
			log("adb shell getprop ro.build.version.release", 2);
			throw new NullPointerException("get device version is null.");
		}
	}

	public void uninstallApp() {

		String apkPackage = getProperty("app_package");
		try {
			Runtime.getRuntime().exec("adb uninstall " + apkPackage);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void swipeToText(String text) {

		androidDriver.scrollTo(text);
		if (verifyBodyTextContainsExpectedText(text, true))
			log("'" + text + "' is found.");
		else {
			throw new RuntimeException("'" + text + "' is not found.");
		}
	}

	public void swipeUpToElement(String elementName) {

		int num = 0;
		while (!isElementShown(elementName)) {
			swipe("up");
			waitByTimeout(1000);
			num++;
			if (num > 10)
				break;
		}
		verifyIsShown(elementName);
	}

	private void swipeTo(int startX, int startY, int endX, int endY, int duration) {

		androidDriver.swipe(startX, startY, endX, endY, duration);
	}

	public void swipe(int startX, int startY, int endX, int endY) {

		log("Swipe from [" + startX + ":" + startY + "] to [" + endX + ":" + endY + "].");
		int windowlenX = androidDriver.manage().window().getSize().getWidth();
		int windowlenY = androidDriver.manage().window().getSize().getHeight();
		swipeTo((int) (windowlenX * startX / 10), (int) (windowlenY * startY / 10), (int) (windowlenX * endX / 10),
				(int) (windowlenY * endY / 10), 200);
	}

	public void swipe(double startX, double startY, double endX, double endY, int duration) {

		log("Swipe from [" + startX + ":" + startY + "] to [" + endX + ":" + endY + "].");
		int windowlenX = androidDriver.manage().window().getSize().getWidth();
		int windowlenY = androidDriver.manage().window().getSize().getHeight();
		swipeTo((int) (windowlenX * startX / 10), (int) (windowlenY * startY / 10), (int) (windowlenX * endX / 10),
				(int) (windowlenY * endY / 10), duration);
	}

	private void swipeOfType(String type) {

		log("Swiping " + type + ".");
		int windowlenX = androidDriver.manage().window().getSize().getWidth();
		int windowlenY = androidDriver.manage().window().getSize().getHeight();
		String swipeLeft = "left";
		String swipeRight = "right";
		String swipeUp = "up";
		String swipeDown = "down";
		if (type.equalsIgnoreCase(swipeLeft)) {
			swipeTo((int) (windowlenX * 0.9), (int) (windowlenY * 0.5), (int) (windowlenX * 0.2), (int) (windowlenY * 0.5), 500);
		}

		if (type.equalsIgnoreCase(swipeRight)) {
			swipeTo((int) (windowlenX * 0.2), (int) (windowlenY * 0.5), (int) (windowlenX * 0.9), (int) (windowlenY * 0.5), 500);
		}

		if (type.equalsIgnoreCase(swipeUp)) {
			swipeTo((int) (windowlenX * 0.2), (int) (windowlenY * 0.7), (int) (windowlenX * 0.2), (int) (windowlenY * 0.2), 500);
		}

		if (type.equalsIgnoreCase(swipeDown)) {
			swipeTo((int) (windowlenX * 0.2), (int) (windowlenY * 0.2), (int) (windowlenX * 0.2), (int) (windowlenY * 0.7), 500);
		}

	}

	private void tap(int fingers, String elementName, int duration) {

		tap(fingers, elementName, "", "", duration);

	}

	private void tap(int fingers, String itemList, Object itemMatching, int duration) {

		tap(fingers, itemList, itemMatching, "", duration);
	}

	private void tap(int fingers, String itemList, Object itemMatching, String elementName, int duration) {

		if (waitForElement(itemList, itemMatching, elementName)) {
			WebElement element = getElement(itemList, itemMatching, elementName);
			androidDriver.tap(fingers, element, duration);
			log("Tap on '" + (elementName.isEmpty() ? itemList : elementName) + "'.");
		} else {

			log("tap on element failed.");
			throw new RuntimeException("tapOn element failed.");

		}

	}

	public void doubleTap() {

		TouchAction action = new TouchAction(androidDriver);
		action.press(500, 500).release().waitAction(100).press(500, 500).release().perform();
		log("Double tap the screen.");
	}

	public void zoom(int x, int y) {

		androidDriver.zoom(x, y);
	}

	public void zoom(String elementName) {

		zoom(elementName, "", "");
	}

	public void zoom(String itemList, int itemMatching) {

		zoom(itemList, itemMatching, "");
	}

	private void zoom(String itemList, Object itemMatching, String elementName) {

		WebElement element = getElement(itemList, itemMatching, elementName);

		androidDriver.zoom(element);
	}

	public void pinch(String elementName) {

		pinch(elementName, "");

	}

	public void pinch(String itemList, String itemMatching) {

		pinch(itemList, itemMatching, "");

	}

	public void pinch(String itemList, int itemMatching) {

		pinch(itemList, itemMatching, "");

	}

	private void pinch(String itemList, Object itemMatching, String elementName) {

		WebElement element = getElement(itemList, itemMatching, elementName);

		androidDriver.pinch(element);

	}

	public void pinch(int x, int y) {

		androidDriver.pinch(x, y);
	}

	public void sendKey(int key) {

		androidDriver.sendKeyEvent(key);
	}

	public void closeKeyWord() {

		androidDriver.hideKeyboard();

	}

	public void back() {

		androidDriver.sendKeyEvent(AndroidKeyCode.BACK);

	}

	public void home() {

		androidDriver.sendKeyEvent(AndroidKeyCode.HOME);
	}

	public void rotate() {

		androidDriver.rotate(ScreenOrientation.LANDSCAPE);
	}

	public void setNetworkToAirplane() {

		androidDriver.setNetworkConnection(new NetworkConnectionSetting(true, false, false));

	}

	public void setNetworkToData() {

		androidDriver.setNetworkConnection(new NetworkConnectionSetting(false, false, true));

	}

	public void connectWifi() {

		log("连接到Wifi");
		androidDriver.setNetworkConnection(new NetworkConnectionSetting(false, true, true));

	}

	public void disconnectWifi() {

		log("断开Wifi");
		androidDriver.setNetworkConnection(new NetworkConnectionSetting(false, false, true));

	}

	private boolean tap(String elementName, int pressTime, String type) {

		return tap(elementName, "", "", pressTime, type);

	}

	private boolean tap(String itemList, Object itemMatching, int pressTime, String type) {

		return tap(itemList, itemMatching, "", pressTime, type);

	}

	public boolean tapOn(double x, double y) {

		waitByTimeout(2000);
		boolean returnValue = false;
		int windowlenX = androidDriver.manage().window().getSize().getWidth();
		int windowlenY = androidDriver.manage().window().getSize().getHeight();
		if (windowlenX > 0 && windowlenY > 0)
			returnValue = true;
		androidDriver.tap(1, windowlenX * (int) (x * 10) / 100, windowlenY * (int) (y * 10) / 100, 200);

		return returnValue;
	}

	private boolean tap(String itemList, Object itemMatching, String elementName, int pressTime, String type) {

		boolean returnValue = false;
		String locator = getElementLocator(itemList);
		String view = getTargetView(type, itemList, "");
		if (locator.contains("-")) {
			String xStr = locator.split("-")[0];
			String yStr = locator.split("-")[1];
			int x = Integer.valueOf(xStr);
			int y = Integer.valueOf(yStr);
			int windowlenX = androidDriver.manage().window().getSize().getWidth();
			int windowlenY = androidDriver.manage().window().getSize().getHeight();
			androidDriver.tap(1, windowlenX * x / 10, windowlenY * y / 10, pressTime);
		}
		boolean emptyItemMatching = true;
		if (itemMatching instanceof Integer) {
			if ((Integer) itemMatching > 0)
				emptyItemMatching = false;
		}
		if (itemMatching instanceof String) {
			if (!((String) itemMatching).isEmpty())
				emptyItemMatching = false;
		}
		if (emptyItemMatching) {
			waitForElement(itemList);
			tap(1, itemList, pressTime);
		} else {
			waitForElement(itemList, itemMatching, "");
			tap(1, itemList, itemMatching, pressTime);
		}
		returnValue = uiMapUpdated(view);
		if (!returnValue) {
			takeFullScreenShot(elementName.isEmpty() ? itemList : elementName);
			throw new RuntimeException("uiMap Update Error.");
		}

		return returnValue;

	}

	public boolean swipe(String direction) {

		return SwipeAndUpdateView(direction);
	}

	protected boolean SwipeAndUpdateView(String direction) {

		String swipeToView = getTargetView("gestures", "", direction);
		swipeOfType(direction);
		// if (!swipeToView.isEmpty())
		// log("swipe to view :" + swipeToView, 1);
		return uiMapUpdated(swipeToView);
	}

	public boolean tapOn(String elementName) {

		return tap(elementName, 500, "tap");
	}

	public boolean tapOn(String itemList, int itemMatching) {

		return tap(itemList, itemMatching, 500, "tap");
	}

	public boolean flickOn(String elementName) {

		return tap(elementName, "", "", 0, "tap");
	}

	public boolean flickOn(String itemList, int itemMatching) {

		return tap(itemList, itemMatching, "", 0, "tap");
	}

	public boolean pressOn(String elementName, int pressTime) {

		return tap(elementName, pressTime, "press");
	}

	public boolean pressOn(String itemList, int itemMatching, int pressTime) {

		return tap(itemList, itemMatching, pressTime, "press");
	}

	protected int getCoorinateX() {

		return androidDriver.manage().window().getSize().getWidth();

	}

	protected int getCoorinateY() {

		return androidDriver.manage().window().getSize().getHeight();

	}

	public void reset() {

		androidDriver.resetApp();
	}

	public void close() {

		androidDriver.closeApp();
	}

	public void removeApp(String packageName) {

		androidDriver.removeApp(packageName);
	}

	public void switchToActivity(String packageApp, String activityPage) {

		androidDriver.startActivity(packageApp, activityPage);
	}

	@Override
	protected void takeFullScreenShot(String failure) {

		String timeStamp = getDate().replace("-", "") + "_" + getCurrentTime().replace(":", "").replace(".", "");
		failure = timeStamp + "_" + replaceIllegalFileName(failure, "_");
		if (StringUtils.endsWith(failure, "_"))
			failure = timeStamp;

		String fileName = screenCapturePath + "/" + failure + ".png";

		getScreenShot(fileName);

	}

	@Override
	protected void getScreenShot(String fileName) {

		File screenshot = androidDriver.getScreenshotAs(OutputType.FILE);
		try {
			copyScreenShot(screenshot, new File(fileName));
		} catch (IOException e) {
			log("Exception happen when getting screen shot, detail is : [" + e.getMessage() + "]. "
					+ "The screen shot operatioin was ignored. ", 3);
		}

	}

	@SuppressWarnings("unchecked")
	private boolean getAndroidText(String className, String expectedText) {

		boolean validate = false;
		List<WebElement> listText = androidDriver.findElements(By.className(className));
		int textSize = listText.size();
		for (int i = 0; i < textSize; i++) {
			String bodyText = listText.get(i).getText();
			if (bodyText.contains(expectedText) || bodyText.matches(expectedText)) {
				validate = true;
				break;
			}
		}

		return validate;
	}

	@Override
	public String getValueOf(String elementName) {

		String text = "";
		waitForElement(elementName);
		WebElement element = getElement(elementName);
		text = element.getText();
		log("The content of '" + elementName + "' is '" + text + "'!");

		return text;
	}

	@Override
	public String getValueOf(String elementName, int itemMatching) {

		String text = "";
		waitForElement(elementName, itemMatching, "");
		WebElement element = getElement(elementName, itemMatching);
		text = element.getText();
		log("The content of '" + elementName + "' is '" + text + "'!");

		return text;
	}

	public boolean compareText(String elementName, String expectText) {

		boolean isSame = false;
		String text = "";
		waitForElement(elementName);
		WebElement element = getElement(elementName);
		text = element.getText().replace(" ", "");
		if (text.contains(expectText.replace(" ", "")))
			isSame = true;
		else {
			log("The text of element '" + elementName + "' is not " + expectText, 2);
			throw new RuntimeException();
		}

		return isSame;
	}

	@Override
	protected boolean verifyBodyTextContainsExpectedText(String expectedText, boolean isShown) {

		boolean returnValue = false;
		Long currentTimeMillis = System.currentTimeMillis();
		try {
			while ((System.currentTimeMillis() - currentTimeMillis) < Long.parseLong(elementTimeout)) {
				returnValue = getAndroidText("android.widget.TextView", expectedText);
				if (isShown == returnValue) {
					break;
				}
				waitByTimeout(500);
			}
		} catch (Exception e) {

		}
		return returnValue;
	}

	public double getFlow() {

		double flow = AndroidMonitor.Flow(getProperty("app.package"));

		return flow;
	}

	public double getCPU() {

		double CPU = AndroidMonitor.CPU(getProperty("app.package"));

		return CPU;
	}

	public double getMemory() {

		double Memory = AndroidMonitor.Memory(getProperty("app.package"));

		return Memory;
	}

	public void monkeyTest(int count) {

		AndroidMonitor am = new AndroidMonitor();
		String nStr = getProperty("test.monkey.times");
		if (nStr.isEmpty()) {
			am.Monkey(getProperty("app.package"), String.valueOf(count));
			log("随机事件次数：" + count);
		} else {
			int times = Integer.parseInt(nStr);
			am.Monkey(getProperty("app.package"), String.valueOf(times));
			log("随机事件次数：" + times);
		}
	}

}

package fwk;

import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.IOSElement;

import java.net.URL;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;

import core.UiClass;

public class IosFwk extends UiClass {

	public IosFwk() {

		super();
	}

	protected String getAppType() {

		return "IOS";
	}

	@Override
	protected void platformSupportInitiate(String profileName) {

		startAppiumDriver(profileName);
		prepareTestEnvironment();
	}

	public void close() {

		iosDriver.closeApp();
	}

	protected void startAppiumDriver(String app_apk) {

		try {

			DesiredCapabilities capabilities = new DesiredCapabilities();
			capabilities.setCapability(CapabilityType.BROWSER_NAME, getProperty("app.browser.Name"));
			capabilities.setCapability("platformVersion", getProperty("app.device.version"));
			capabilities.setCapability("platform", getProperty("app.os.platform"));
			capabilities.setCapability("udid", getProperty("app.device.udid"));
			capabilities.setCapability("deviceName", getProperty("app.device.name"));
			capabilities.setCapability("platformName", getProperty("app.device.platformName"));
			capabilities.setCapability("app", getProperty("app.name"));

			iosDriver = new IOSDriver<WebElement>(new URL("http://" + getProperty("app.appium.serverIP") + "/wd/hub"),
					capabilities);
			driver = iosDriver;
		} catch (Exception e) {
			log("Cannot launch application!", 2);
			throw new RuntimeException(e);
		}

		new WebDriverWait(iosDriver, 10);
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
			waitFor(1000);
			num++;
			if (num > 10)
				break;
		}
		isDisplay(elementName);
	}

	private void swipeTo(int startX, int startY, int endX, int endY, int duration) {

		iosDriver.swipe(startX, startY, endX, endY, duration);
	}

	public void swipe(int startX, int startY, int endX, int endY) {

		log("Swipe from [" + startX + ":" + startY + "] to [" + endX + ":" + endY + "].");
		int windowlenX = iosDriver.manage().window().getSize().getWidth();
		int windowlenY = iosDriver.manage().window().getSize().getHeight();
		swipeTo((int) (windowlenX * startX / 10), (int) (windowlenY * startY / 10), (int) (windowlenX * endX / 10),
				(int) (windowlenY * endY / 10), 200);
	}

	public void swipe(int startX, int startY, int endX, int endY, int duration) {

		log("Swipe from [" + startX + ":" + startY + "] to [" + endX + ":" + endY + "].");
		int windowlenX = iosDriver.manage().window().getSize().getWidth();
		int windowlenY = iosDriver.manage().window().getSize().getHeight();
		swipeTo((int) (windowlenX * startX / 10), (int) (windowlenY * startY / 10), (int) (windowlenX * endX / 10),
				(int) (windowlenY * endY / 10), duration);
	}

	private void swipeOfType(String type) {

		log("Swiping " + type + ".");
		int windowlenX = iosDriver.manage().window().getSize().getWidth();
		int windowlenY = iosDriver.manage().window().getSize().getHeight();
		String swipeLeft = "left";
		String swipeRight = "right";
		String swipeUp = "up";
		String swipeDown = "down";
		if (type.equalsIgnoreCase(swipeLeft)) {
			swipeTo((int) (windowlenX * 0.8), (int) (windowlenY * 0.5), (int) (windowlenX * 0.2), (int) (windowlenY * 0.5), 500);
		}

		if (type.equalsIgnoreCase(swipeRight)) {
			swipeTo((int) (windowlenX * 0.2), (int) (windowlenY * 0.5), (int) (windowlenX * 0.8), (int) (windowlenY * 0.5), 500);
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
			iosDriver.tap(fingers, element, duration);
			log("Tap on '" + (elementName.isEmpty() ? itemList : elementName) + "'.");
		} else {

			log("tap on element failed.");
			throw new RuntimeException("tapOn element failed.");

		}

	}

	public void doubleTap() {

		TouchAction action = new TouchAction(iosDriver);
		action.press(500, 500).release().waitAction(100).press(500, 500).release().perform();
		log("Double tap the screen.");
	}

	public void zoom(int x, int y) {

		iosDriver.zoom(x, y);
	}

	public void zoom(String elementName) {

		zoom(elementName, "", "");
	}

	public void zoom(String itemList, int itemMatching) {

		zoom(itemList, itemMatching, "");
	}

	private void zoom(String itemList, Object itemMatching, String elementName) {

		WebElement element = getElement(itemList, itemMatching, elementName);

		iosDriver.zoom(element);
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

		iosDriver.pinch(element);

	}

	public void pinch(int x, int y) {

		iosDriver.pinch(x, y);
	}

	public void closeKeyWord() {

		iosDriver.hideKeyboard();

	}

	public void rotate() {

		iosDriver.rotate(ScreenOrientation.LANDSCAPE);
	}

	private boolean tap(String elementName, int pressTime, String type) {

		return tap(elementName, "", "", pressTime, type);

	}

	private boolean tap(String itemList, Object itemMatching, int pressTime, String type) {

		return tap(itemList, itemMatching, "", pressTime, type);

	}

	public boolean tapOn(double x, double y) {

		boolean returnValue = false;
		int windowlenX = iosDriver.manage().window().getSize().getWidth();
		int windowlenY = iosDriver.manage().window().getSize().getHeight();
		if (windowlenX > 0 && windowlenY > 0)
			returnValue = true;
		iosDriver.tap(1, windowlenX * (int) (x * 10) / 100, windowlenY * (int) (y * 10) / 100, 200);

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
			int windowlenX = iosDriver.manage().window().getSize().getWidth();
			int windowlenY = iosDriver.manage().window().getSize().getHeight();
			iosDriver.tap(1, windowlenX * x / 10, windowlenY * y / 10, pressTime);
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
		returnValue = updateUiMap(view);
		if (!returnValue) {
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
		return updateUiMap(swipeToView);
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

	public void scrollTo(String text) {

		MobileElement element = (IOSElement) iosDriver.findElement(By.className("UIATableView"));
		((IOSElement) element).scrollTo(text);
	}

	@SuppressWarnings("unchecked")
	private boolean getIOSText(String className, String expectedText) {

		boolean validate = false;
		List<WebElement> listText = iosDriver.findElements(By.className(className));
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

	protected boolean verifyBodyTextContainsExpectedText(String expectedText, boolean isShown) {

		boolean returnValue = false;
		Long currentTimeMillis = System.currentTimeMillis();
		try {
			while ((System.currentTimeMillis() - currentTimeMillis) < Long.parseLong(elementTimeout)) {
				returnValue = getIOSText("UIATableView", expectedText);
				if (isShown == returnValue) {
					break;
				}
				wait(500);
			}
		} catch (Exception e) {

		}
		return returnValue;
	}

}

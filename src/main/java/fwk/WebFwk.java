package fwk;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebElement;
import org.testng.Assert;

import core.UiClass;

public class WebFwk extends UiClass {

	public WebFwk() {

		super();
	}

	protected String getAppType() {

		return "Web";
	}

	@Override
	protected void platformSupportInitiate(String profileName) {

		setProfilePath(profileName);

		startSeleniumServerAndBrowser();

		prepareTestEnvironment();
	}

	public boolean openApp() {

		try {

			boolean openApp = uiMapSetView("");

			driver.manage().window().maximize();
			return openApp;
		} catch (Exception e) {
		}
		return false;
	}

	public void openUrl(String url) {

		driver.get(url);
	}

	private void setProfilePath(String profileName) {

		browserName = getProperty("browser.name");
		browserProfilePath = getProfilePath(profileName);
	}

	private String getProfilePath(String profileName) {

		if (browserName.matches(".*firefox.*")) {

			return getBrowserProfileProperty("profile.firefox", profileName);

		} else if (browserName.matches(".*ie.*")) {
			return getBrowserProfileProperty("profile.iexplore");
		} else if (browserName.matches(".*chrome.*")) {
			return getBrowserProfileProperty("profile.chrome");
		}
		throw new RuntimeException("Can't find any profile property for browser [" + browserName + "] and profile ["
				+ profileName + "]. ");
	}

	protected void startSeleniumServerAndBrowser() {

		if (browserName.matches(".*firefox.*")) {
			log("Loading Firefox Profile and open Firefox...");
			File profileFile = new File(browserProfilePath);
			if (profileFile.exists()) {
				FirefoxProfile profile = new FirefoxProfile(new File(browserProfilePath));
				profile.setEnableNativeEvents(true);
				driver = new FirefoxDriver(profile);
			} else {
				driver = new FirefoxDriver();
			}
		}
		if (browserName.matches(".*chrome.*")) {
			System.setProperty("webdriver.chrome.driver", testDataRoot + "common\\browserProfiles\\drivers\\chromedriver.exe");
			String userProfile = browserProfilePath.replace("/", "\\");
			ChromeOptions options = new ChromeOptions();
			options.addArguments("user-data-dir=" + userProfile, "--disable-prerender-local-predictor", "--incognito",
					"--start-maximized");
			driver = new ChromeDriver(options);
		}

		main_window = driver.getWindowHandle();
		driver.manage().deleteAllCookies();

	}

	private String getBrowserProfileProperty(String profileType) {

		return getBrowserProfileProperty(profileType, "");
	}

	private String getBrowserProfileProperty(String profileType, String folderName) {

		String returnStr = "";

		if (folderName.isEmpty())
			folderName = getProperty("browser." + browserName + ".profile");
		returnStr = testDataRoot + getInitialProperty(profileType) + "/" + folderName;
		File profileFolder = new File(returnStr);

		if (!profileFolder.exists() || folderName.isEmpty()) {
			returnStr = returnStr.substring(0, returnStr.length() - folderName.length()) + "default";
			File defaultProfile = new File(returnStr);
			if (defaultProfile.exists()) {
				log("Browser Profile not found for " + appUrl + ", use default profile instead.", 3);
			} else {
				log("Browser Profile not found for " + appUrl + ", use local profile instead.", 3);
			}
		} else {
			log("Browser Profile used: " + returnStr);
		}

		return returnStr;
	}

	public String getTitle() {

		return driver.getTitle();
	}

	public void back() {

		log("Clicking 'Back' button");
		driver.navigate().back();
		waitByTimeout(Long.parseLong(pageTimeout));
		return;

	}

	public boolean forward() {

		driver.navigate().forward();
		return true;
	}

	public boolean refresh() {

		driver.navigate().refresh();
		driver.manage().timeouts().implicitlyWait(Integer.parseInt(pageTimeout), TimeUnit.SECONDS);
		return true;
	}

	public void clickElement(String jsScript) {

		JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
		String js = null;
		if (!jsScript.contains("=")) {
			throw new WebDriverException("The format of the navigate element is incorrect.");
		}
		if (jsScript.contains("id")) {

			jsScript = jsScript.split("=")[1];
			js = "document.getElementById('" + jsScript + "').click()";
			jsExecutor.executeScript(js);
		}

		else if (jsScript.contains("tagname")) {

			jsScript = jsScript.split("=")[1];
			js = "document.getElementsByTagName('" + jsScript + "').click()";
			jsExecutor.executeScript(js);
		} else {
			throw new WebDriverException("The parameter jsScript should only be 'id' or 'tagname'.");
		}
	}

	public void hover(String elementName) {

		waitForArea(getElementLocator(elementName), Long.parseLong(elementTimeout));
		RemoteWebElement element = (RemoteWebElement) getElement(elementName);
		log("Moving mouse to element: " + elementName + ".");

		Actions actions = new Actions(driver);
		actions.moveToElement(element).build().perform();
	}

	public void restartBrowser() {

		log("Restarting the browser session.");
		close();
		clearHistory();

		startSeleniumServerAndBrowser();
		log("Restarted.");
	}

	public void clearHistory() {

		uiMapViewList.clear();
		uiMapAreasAlreadyChecked.clear();
		uiMapViewIndex = -1;
	}

	public String getAttributeFromElementLocator(String elementName, String attribute) {

		WebElement element = getElement(elementName);
		String attributeValue = element.getAttribute(attribute);
		return attributeValue;
	}

	public void switchIfarme(String frameElement) {

		WebElement element = getElement(frameElement);
		driver.switchTo().frame(element);
		log("switch to ifarme");
	}

	public void clickElementIframe(String elementLocator) {

		boolean verify = driver.findElement(By.xpath(elementLocator)).isDisplayed();
		log("verify element if display=" + verify);
		if (verify) {
			driver.findElement(By.xpath(elementLocator)).click();
		}
	}

	public int getAllElements(String elementName) {

		waitForElement(elementName);

		return getElements(elementName).size();
	}

	public void scrollTo(String elementName) {

		waitForElement(elementName);
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView()", getElement(elementName));
	}

	public void scrollTo(int height) {

		String setscroll = "document.documentElement.scrollTop=" + height;
		((JavascriptExecutor) driver).executeScript(setscroll);
	}

	public void scrollTo(WebElement element) {

		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView()", element);
	}

	public void clickOnUseJS(String elementName) {

		WebElement element = getElement(elementName);
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
	}

	public void scrollToTop() {

		String Js = "var q=document.documentElement.scrollTop=0";
		JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
		jsExecutor.executeScript(Js);

	}

	public boolean closeSubTab(String subtabname) {

		boolean closeSubTab = false;

		if (switchWindow(subtabname)) {
			try {
				driver.close();
				closeSubTab = true;
			} catch (Exception e) {
				log("Failed to close window '" + subtabname + "'.", 3);
			}
		}
		returnToMain();
		return closeSubTab;
	}

	public String getCurrentUrl() {

		String actualPageUrl = "";
		Set<String> allWindowNames = driver.getWindowHandles();

		Set<String> currentWindowNamesList = new HashSet<String>();

		if (allWindowNames.size() > 1) {

			for (String window : allWindowNames) {

				if (!(window.equalsIgnoreCase("null") || window.equalsIgnoreCase("undefined") || window.isEmpty()))
					currentWindowNamesList.add(window);
			}

			for (String windowName : currentWindowNamesList) {

				boolean mainApplicationWindow = StringUtils.equalsIgnoreCase(windowName, main_window);

				if (!mainApplicationWindow) {

					driver.switchTo().window(windowName);
					waitForCondition("(selenium.browserbot.getCurrentWindow().document.readyState=='interactive') || "
							+ "(selenium.browserbot.getCurrentWindow().document.readyState=='complete');", pageTimeout);

					actualPageUrl = StringUtils.defaultString(driver.getCurrentUrl());

					driver.close();
					returnToMain();
				}
			}
		} else {
			actualPageUrl = StringUtils.defaultString(driver.getCurrentUrl());
		}

		return actualPageUrl;
	}

	@Override
	public boolean isTextShown(String elementNameOrMessage, boolean isShown) {

		boolean returnValue = false;
		boolean singleWindow = true;

		try {
			singleWindow = (driver.getWindowHandles().size() == 1);
		} catch (Exception e) {
		}

		if (singleWindow) {
			returnValue = verifyBodyTextContainsExpectedText(elementNameOrMessage, isShown);
		} else {
			String currentWindowName = driver.getWindowHandle();
			Set<String> allWindowNames = driver.getWindowHandles();
			Set<String> currentWindowNamesList = getCurrentValidWindowNamesListInAll(allWindowNames);

			for (String windowName : currentWindowNamesList) {
				driver.switchTo().window(windowName);
				waitForCondition("(selenium.browserbot.getCurrentWindow().document.readyState=='interactive') || "
						+ "(selenium.browserbot.getCurrentWindow().document.readyState=='complete');", pageTimeout);
				returnValue = verifyBodyTextContainsExpectedText(elementNameOrMessage, isShown);

				if (returnValue)
					break;
			}
			driver.switchTo().window(currentWindowName);
		}

		return returnValue;
	}

	@Override
	protected Set<String> getCurrentValidWindowNamesListInAll(Set<String> allWindowNames) {

		Set<String> currentWindowNamesList = new HashSet<String>();

		for (String window : allWindowNames) {

			if (!(window.equalsIgnoreCase("null") || window.equalsIgnoreCase("undefined") || window.isEmpty()))
				currentWindowNamesList.add(window);
		}
		return currentWindowNamesList;
	}

	@Override
	protected void waitForCondition(String js, String timeout) {

		((JavascriptExecutor) driver).executeScript("try {" + js + "} catch(err){false}", timeout);
	}

	public void deleteCookies() {

		driver.manage().deleteAllCookies();
	}

	public String getCookies() {

		String cookies = "";
		Set<Cookie> cookiesSet = driver.manage().getCookies();
		for (Cookie c : cookiesSet) {
			cookies += c.getName() + "=" + c.getValue() + ";";
		}
		return cookies;
	}

	public String getCookies(String cookieName) {

		String cookies = "";
		Set<Cookie> cookiesSet = driver.manage().getCookies();
		for (Cookie c : cookiesSet) {
			if (c.getName().equals(cookieName)) {
				cookies += c.getName() + "=" + c.getValue() + ";";
			}
		}
		return cookies;
	}

	public void close() {

		if (driver != null) {
			driver.quit();
		}
		log("<===================TEST TERMINATED================>");
	}

	public boolean switchWindow(String windowNames) {

		boolean flag = false;
		try {
			String currentHandle = driver.getWindowHandle();
			Set<String> handles = driver.getWindowHandles();

			for (String str : handles) {

				if (str.equals(currentHandle))
					continue;
				else {
					driver.switchTo().window(str);
					if (driver.getTitle().contains(windowNames)) {
						flag = true;
						log("Switch to window: '" + windowNames + "' successfully!");
						break;
					} else
						continue;
				}
			}
			Assert.assertTrue(flag, "Window: '" + windowNames + "' title is not found!");
		} catch (NoSuchWindowException e) {
			log("Window: '" + windowNames + "' cound not found!", 3);
			flag = false;
		}
		return flag;
	}

	public void returnToMain() {

		driver.switchTo().window(main_window);
	}

	public void closeCurrentWindow() {

		if (!driver.getWindowHandle().equals(main_window)) {
			driver.close();
			returnToMain();
		}
	}

	public void closeOtherWindows() {

		for (String windows : driver.getWindowHandles())
			if (!windows.equals(main_window)) {
				driver.switchTo().window(windows);
				driver.close();
			}
		returnToMain();
	}

	public boolean isAlertPresent() {

		try {
			driver.switchTo().alert();
			return true;
		} catch (NoAlertPresentException Ex) {
			return false;
		}
	}

	public String acceptAlert() {

		String alertText = "";
		try {
			Alert alert = driver.switchTo().alert();
			alertText = alert.getText();
			alert.accept();
		} catch (NoAlertPresentException e) {

		}
		if (alertText.isEmpty())
			log("There is no alert in current page");
		else
			log("The alert is '" + alertText + "'.");

		return alertText;

	}

	public String dismissAlert() {

		String alertText = "";
		try {
			Alert alert = driver.switchTo().alert();
			alertText = alert.getText();
			alert.dismiss();
		} catch (NoAlertPresentException e) {

		}
		if (alertText.isEmpty())
			log("There is no alert in current page");
		else
			log("The alert is '" + alertText + "'.");

		return alertText;
	}

	public void switchToNewWindow() {

		String currentHandle = driver.getWindowHandle();
		Set<String> handles = driver.getWindowHandles();
		Iterator<String> it = handles.iterator();
		while (it.hasNext()) {
			if (currentHandle == it.next())
				continue;
			driver.switchTo().window(it.next());
		}
	}

	public void switchToPromptWindow(Set<String> before, Set<String> after) {

		List<String> whs = new ArrayList<String>(after);
		whs.removeAll(before);
		whs.remove("");
		if (whs.size() > 0) {
			driver.switchTo().window(whs.get(0));
		} else {
			throw new WebDriverException("No new window prompted out.");
		}
	}

}

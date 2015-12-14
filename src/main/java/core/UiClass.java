package core;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import tool.ParseExcel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thoughtworks.selenium.HttpCommandProcessor;

@SuppressWarnings({ "deprecation", "rawtypes" })
public abstract class UiClass extends InitClass {

	private String iFrameName = "";
	private String iFrameAreaName = "";
	private boolean blankAreaLocator = false;

	protected static String browserHost = "";
	protected static String browserPort = "";
	protected static String browserName = "";
	protected static String browserSize = "";

	private static ObjectNode content = jsonMapper.createObjectNode();
	private JsonNode uiMapCurrentPage = jsonMapper.createObjectNode();
	private JsonNode uiMapCurrentView = jsonMapper.createObjectNode();
	private JsonNode uiMapCurrentArea = jsonMapper.createObjectNode();

	private JsonNode myCurrentPage = jsonMapper.createObjectNode();
	private JsonNode myCurrentView = jsonMapper.createObjectNode();
	private JsonNode myCurrentArea = jsonMapper.createObjectNode();

	private Integer maxPageTime = 20000;

	protected String browserProfilePath = "";

	private final static String PREVIOUS_VIEW = "previousView";
	protected ArrayList<String> uiMapViewList = new ArrayList<String>();
	protected Integer uiMapViewIndex = -1;

	protected ArrayList<String> uiMapAreasAlreadyChecked = new ArrayList<String>();
	private ArrayList<String> uiMapViewsAlreadyChecked = new ArrayList<String>();
	protected static String screenCapturePath = "";

	protected WebDriver driver;

	protected AndroidDriver androidDriver;
	protected IOSDriver iosDriver;
	protected HttpCommandProcessor proc;

	protected static String main_window;

	private String locator = "";

	protected JsonNode uiMap;

	protected UiClass() {

		super();
	}

	@Override
	protected void languageSupportInitiate() {

		projectLevelUiConfigPath = (testConfigRoot + getAppType());

		appLocale = getProperty("app.locale");
		String jsonFiles = getProperty("ui.json");
		if (jsonFiles.contains(",")) {
			String jsonFile[] = jsonFiles.split(",");
			uiMap = getDataFromConfigFile(getAppType() + jsonFile[0]);
			for (int i = 1; i < jsonFile.length; i++) {
				JsonNode uiMaps = getDataFromConfigFile(getAppType() + jsonFile[i].trim());
				uiMap = merge(uiMap, uiMaps);
			}
		} else {
			uiMap = getDataFromConfigFile(projectLevelUiConfigPath + "/" + getProperty("ui.json"));
		}

		content = (ObjectNode) getDataFromConfigFile(testConfigRoot + "/" + getContentFileFullName());

		if (!StringUtils.isEmpty(getInitialProperty("maxPageTime")))
			maxPageTime = Integer.parseInt(initialConfig.path("maxPageTime").textValue());

		appUrl = getProperty("app.url");

		pageTimeout = StringUtils.defaultIfEmpty(getProperty("test.timeout.page"), pageTimeout);
		viewTimeout = StringUtils.defaultIfEmpty(getProperty("test.timeout.view"), viewTimeout);
		elementTimeout = StringUtils.defaultIfEmpty(getProperty("test.timeout.element"), elementTimeout);

	}

	private String getContentFileFullName() {

		String contentFileFullName = getAppType() + "/" + getProperty("ui.content");
		if (appLocale.isEmpty())
			return contentFileFullName;
		else
			contentFileFullName += "-" + appLocale;
		return contentFileFullName;
	}

	@Override
	protected void prepareTestEnvironment() {

		super.prepareTestEnvironment();

		screenCapturePath = (testRoot.replace("test-classes/", "") + "screenCapture");

		File screenCaptureFolder = new File(screenCapturePath);

		if (!screenCaptureFolder.exists())
			screenCaptureFolder.mkdirs();

	}

	public boolean openApp() {

		return false;
	}

	public boolean click(String elementName) {

		return click(elementName, "", "", "");
	}

	public boolean click(String listName, int itemMatching) {

		return click(listName, itemMatching, "", "");
	}

	private boolean click(String listName, Object itemMatching, String elementName, String message) {

		boolean returnValue = false;

		String activator = getElementAtt("activator", listName);
		if (activator.isEmpty())
			activator = getElementAtt("activator", elementName);
		log("Clicking on '" + (elementName.isEmpty() ? listName : elementName) + "'.");

		String view = getTargetView("click", elementName.isEmpty() ? listName : elementName, "");
		returnValue = clickElement(listName, itemMatching, elementName, activator, message);

		if (returnValue) {
			returnValue = updateUiMap(view);
		} else {
			throw new RuntimeException("Element clicking Error.");
		}
		if (!returnValue) {
			throw new RuntimeException("uiMap Update Error.");
		}

		return returnValue;

	}

	private boolean clickElement(String listName, Object itemMatching, String elementName, String activator, String message) {

		boolean locatorIsVisible = false;

		if (!activator.isEmpty() && !isElementShown(elementName.isEmpty() ? listName : elementName)) {
			log("locator is invisible, clicking activator '" + activator + "'.");

			getActivatorisVisbleAndClick((elementName.isEmpty() ? listName : elementName), activator, locatorIsVisible);
		}

		return clickLocator(listName, itemMatching, elementName, message);
	}

	private boolean getActivatorisVisbleAndClick(String elementName, String activator, boolean locatorIsVisible) {

		if (!StringUtils.isEmpty(activator) && !locatorIsVisible) {

			if (waitForElement(activator)) {
				waitForCondition("selenium.browserbot.getUserWindow().$.active == 0;", pageTimeout);
				getElement(activator).click();

				if (!(locatorIsVisible = waitForElement(elementName))) {
					getElement(activator).click();
					locatorIsVisible = waitForElement(elementName);
				}

			}
		}
		return locatorIsVisible;
	}

	private boolean clickLocator(String listName, Object itemMatching, String elementName, String message) {

		boolean returnValue = false;
		if (waitForElement(listName, itemMatching, elementName)) {
			getElement(listName, itemMatching, elementName).click();
			returnValue = true;
			if (!StringUtils.isEmpty(message)) {

				returnValue = isDisplay(message);

				if (!returnValue) {
					returnValue = isDisplay(message);
				}
			}

			return returnValue;
		}

		Assert.fail("FAIL uiMapclickLocatorAndSetView() - Could not find '" + (elementName.isEmpty() ? listName : elementName)
				+ "'.");
		return returnValue;
	}

	protected boolean updateUiMap(String view) {

		boolean returnValue = false;
		if (StringUtils.isEmpty(view)) {
			returnValue = true;
		} else if (view.contains("[")) {
			String newView = view.substring(1, view.length() - 1);
			log("Checking views: '" + newView + "'.");
			String[] s = newView.split(",");
			for (int i = 0; i < s.length; i++) {
				String getView = s[i];
				log("Checking '" + getView + "'.");
				getView = getCurrentAreaisDisplayed(getView, 0);
				if (!getView.equals("")) {
					log("Setting view to '" + getView + "'.");
					returnValue = uiMapUpdateView(getView);
					break;
				}
			}
		} else {
			log("Setting view to '" + view + "'.");
			returnValue = uiMapUpdateView(view);

		}
		if (!returnValue) {
			takeFullScreenShot(view);
			throw new RuntimeException("uiMap Update Error.");
		}

		return returnValue;
	}

	public boolean viewTo(String requestedView) {

		boolean viewTo = uiMapSetView(requestedView);
		return viewTo;
	}

	public String getText(String elementName) {

		return getText(elementName, "", "", "");
	}

	public String getText(String listName, int itemMatching) {

		return getText(listName, itemMatching, "", "");
	}

	private String getText(String listName, Object itemMatching, String elementName, String attribute) {

		String actualValue = "";

		String elementLocator = "";
		if (waitForElement(listName, itemMatching, elementName)) {

			if (attribute == "") {
				String elementType = getElementType(listName, itemMatching, elementName);
				if (StringUtils.contains(elementType, "input")) {
					actualValue = StringUtils
							.defaultString(getElement(listName, itemMatching, elementName).getAttribute("value"));
				}

				if (StringUtils.contains(elementType, "text")) {
					actualValue = StringUtils.defaultString(getElement(listName, itemMatching, elementName).getText());
				}

				if (StringUtils.contains(elementType, "checkbox")) {

					String checkboxClass = getElement(listName, itemMatching, elementName).getAttribute("class");

					actualValue = StringUtils.contains(checkboxClass, "checked") ? "checked" : "unchecked";
				}

				if (StringUtils.contains(elementType, "select")) {

					WebElement element = getElement(listName, itemMatching, elementName);
					for (int i = 0; i < getElementsSize(elementName.isEmpty() ? listName : elementName); i++) {
						WebElement selectItem = element.findElements(By.tagName("option")).get(i);
						if (selectItem.isSelected())
							actualValue = StringUtils.defaultString(selectItem.getText());
					}

				}

				if (StringUtils.isBlank(actualValue)) {
					elementLocator = getElement(listName, itemMatching, elementName).getText();
					actualValue = StringUtils.defaultString(elementLocator);
				}
				if (!actualValue.isEmpty())
					log("get '" + (elementName.isEmpty() ? listName : elementName) + "' tagName is '" + actualValue + "'.");
				else
					log("getText() '" + (elementName.isEmpty() ? listName : elementName) + "' is Empty!");
			} else {

				actualValue = StringUtils.defaultString(getElement(listName, itemMatching, elementName).getAttribute(attribute));
				if (!actualValue.isEmpty())
					log("get '" + (elementName.isEmpty() ? listName : elementName) + "' tagName is '" + actualValue + "'.");
				else
					log("getText() '" + (elementName.isEmpty() ? listName : elementName) + "' is Empty!");
			}

			actualValue = actualValue.replaceAll("\n", "");

			return actualValue;
		}

		if (StringUtils.isEmpty(actualValue))
			log("getText() - Unable to locate element '" + (elementName.isEmpty() ? listName : elementName) + "' "
					+ elementLocator + "in current view " + uiMapCurrentView.path("viewName") + ".", 2);
		return actualValue;

	}

	private void clearAndSendkeys(String listName, Object itemMatching, String elementName, String value) {

		WebElement element = getElement(listName, itemMatching, elementName);
		element.clear();
		element.sendKeys(value);
	}

	private void click(String listName, Object itemMatching, String elementName) {

		waitForElementShown(listName, itemMatching, elementName);
		getElement(listName, itemMatching, elementName).click();
	}

	private String getElementType(String listName, Object itemMatching, String elementName) {

		WebElement element = getElement(listName, itemMatching, elementName);

		if (!element.isDisplayed()) {
			Assert.fail("Element is not found '" + elementName + "'.");
			throw new NullPointerException();
		}

		String elementType = element.getTagName();
		try {
			elementType += " " + element.getAttribute("type");
		} catch (Exception e) {
		}
		if (StringUtils.contains(elementType, "select")) {
			return elementType = "select";
		} else if (StringUtils.contains(elementType, "checkbox")) {
			return elementType = "checkbox";
		} else if (StringUtils.contains(elementType, "radio")) {
			return elementType = "radio";
		} else if (StringUtils.contains(elementType, "text")) {
			return elementType = "text";
		} else if (StringUtils.contains(elementType, "file")) {
			return elementType = "file";
		} else {
			// Default type, in case we can't tell.
			return elementType = "input";
		}
	}

	public String getAttribute(String listName, int itemMatching, String elementName, String attribute) {

		waitForElement(listName, itemMatching, elementName);
		String returnValue = getElement(listName, itemMatching, elementName).getAttribute(attribute);
		log("The attribute '" + attribute + "' of the element '" + (elementName.isEmpty() ? listName : elementName) + "' is '"
				+ (returnValue.isEmpty() ? "null" : returnValue) + "'.");
		return returnValue;
	}

	public String getAttribute(String listName, int itemMatching, String attribute) {

		return getAttribute(listName, itemMatching, "", attribute);
	}

	public String getAttribute(String elementName, String attribute) {

		return getAttribute(elementName, 0, "", attribute);
	}

	private boolean operateOnEveryElementType(String listName, Object itemMatching, String elementName, String elementType,
			String value) {

		boolean returnValue = false;
		if (StringUtils.equals(elementType, "input") || StringUtils.equals(elementType, "text")
				|| StringUtils.equals(elementType, "password") || StringUtils.equals(elementType, "email")) {

			value = StringUtils.defaultString(getLocalizedText(value), value);
			clearAndSendkeys(listName, itemMatching, elementName, value);
			returnValue = true;
		}

		if (StringUtils.equals(elementType, "checkbox")) {

			ArrayList<String> positiveValues = new ArrayList<String>();
			ArrayList<String> negativeValues = new ArrayList<String>();

			positiveValues.addAll(Arrays.asList("y", "yes", "true", "on", "checked"));
			negativeValues.addAll(Arrays.asList("n", "no", "false", "off", "unchecked"));

			if (positiveValues.contains(value.toLowerCase())) {
				returnValue = true;
				if (!StringUtils.containsIgnoreCase(
						StringUtils.defaultString(getElement(listName, itemMatching, elementName).getAttribute("class")),
						"checked"))
					click(listName, itemMatching, elementName);
			} else {
				if (negativeValues.contains(value.toLowerCase())) {
					returnValue = true;
					if (!StringUtils.containsIgnoreCase(
							StringUtils.defaultString(getElement(listName, itemMatching, elementName).getAttribute("class")),
							"checked"))
						click(listName, itemMatching, elementName);
				}
			}
		}

		if (StringUtils.equals(elementType, "select")) {

			value = StringUtils.defaultString(getLocalizedText(value), value);
			Select select = new Select(getElement(listName, itemMatching, elementName));
			select.selectByVisibleText(value);
			returnValue = true;
		}

		if (StringUtils.equals(elementType, "file")) {

			value = StringUtils.defaultString(getLocalizedText(value), value);
			WebElement element = getElement(listName, itemMatching, elementName);
			element.sendKeys(value);
			returnValue = true;
		}

		return returnValue;
	}

	private boolean sendKey(String listName, Object itemMatching, String elementName, String value) {

		boolean returnValue = false;
		if (waitForElement(listName, itemMatching, elementName)) {
			String elementType = getElementType(listName, itemMatching, elementName);
			returnValue = operateOnEveryElementType(listName, itemMatching, elementName, elementType, value);
		}

		if (!returnValue) {
			log("sendKey() - Could not set '" + (elementName.isEmpty() ? listName : elementName) + "' " + value
					+ "' in current view '" + uiMapCurrentView.path("viewName") + "'.", 2);
			throw new RuntimeException("sendKey() - Could not set '" + (elementName.isEmpty() ? listName : elementName) + "' '"
					+ locator + "' to '" + value + "' in current view " + uiMapCurrentView.path("viewName") + ".");
		} else
			log("Value '" + value + "' is set for '" + (elementName.isEmpty() ? listName : elementName) + "'.");
		return returnValue;
	}

	public boolean sendKey(String elementName, String value) {

		return sendKey(elementName, "", "", value);
	}

	public boolean sendKey(String listName, int itemMatching, String value) {

		return sendKey(listName, itemMatching, "", value);
	}

	private boolean isDisplay(String listName, Object itemMatching, String elementNameOrMessage, boolean isShown) {

		String errorMessage = "";
		String elementLocator = "";
		boolean returnValue = true;
		String expectedText = "";

		if (!elementNameOrMessage.isEmpty()) {
			log("Looking for '" + elementNameOrMessage + "'.");
			elementLocator = getElementLocator(elementNameOrMessage);
		} else {
			elementLocator = getElementLocator(listName);
			log("Looking for '" + listName + "'.");
		}

		if (!StringUtils.isEmpty(elementLocator)) {

			if (isShown) {
				returnValue = waitForElement(listName, itemMatching, elementNameOrMessage);
			} else {
				returnValue = !waitForElementNotShown(listName, itemMatching, elementNameOrMessage);
			}
			expectedText = elementLocator;

		}

		else {
			expectedText = getLocalizedText(elementNameOrMessage.isEmpty() ? listName : elementNameOrMessage);
			returnValue = isTextShown(expectedText, isShown);
		}

		elementNameOrMessage = elementNameOrMessage.isEmpty() ? listName : elementNameOrMessage;
		if (isShown) {

			if (elementNameOrMessage.isEmpty())
				errorMessage = "FAIL isDisplay() - Could not find '" + expectedText + "' ";
			else
				errorMessage = "FAIL isDisplay() - Could not find '" + elementNameOrMessage + "' ";
		} else {
			if (elementNameOrMessage.isEmpty())
				errorMessage = "FAIL isNotDisplay() - Found '" + expectedText + "' ";
			else
				errorMessage = "FAIL isNotDisplay() - Found '" + elementNameOrMessage + "' ";
			returnValue = !returnValue;
		}

		errorMessage = errorMessage + "in current window.";

		return verifyIsTrue(returnValue, errorMessage);
	}

	public boolean isNotDisplay(String elementName) {

		return isDisplay(elementName, "", "", false);
	}

	public boolean isNotDisplay(String listName, int itemMatching) {

		return isDisplay(listName, itemMatching, "", false);
	}

	public boolean isDisplay(String elementNameOrMessage) {

		return isDisplay(elementNameOrMessage, "", "", true);
	}

	public boolean isDisplay(String listName, int itemMatching) {

		return isDisplay(listName, itemMatching, "", true);
	}

	public boolean verifyURL(String expectedPageURL) {

		expectedPageURL = getLocalizedText(expectedPageURL);
		boolean verifyURL = false;
		String errorMessage = "";
		Set<String> allWindowNames = driver.getWindowHandles();

		Set<String> currentWindowNamesList = new HashSet<String>();

		if (allWindowNames.size() > 1) {
			currentWindowNamesList = getCurrentValidWindowNamesListInAll(allWindowNames);

			for (String windowName : currentWindowNamesList) {
				boolean mainApplicationWindow = StringUtils.equalsIgnoreCase(windowName, main_window);
				if (!mainApplicationWindow) {

					driver.switchTo().window(windowName);
					waitForCondition("(selenium.browserbot.getCurrentWindow().document.readyState=='interactive') || "
							+ "(selenium.browserbot.getCurrentWindow().document.readyState=='complete');", pageTimeout);
					String actualPageUrl = StringUtils.defaultString(driver.getCurrentUrl());

					if (StringUtils.equalsIgnoreCase(actualPageUrl, expectedPageURL) || actualPageUrl.matches(expectedPageURL))
						verifyURL = true;
					else
						errorMessage = "FAIL verifyURLIs() - expected URL='" + expectedPageURL + ", but actual Page='"
								+ actualPageUrl + ".";

					driver.close();
					driver.switchTo().window(main_window);
				}
			}
		} else {
			String actualPageUrl = StringUtils.defaultString(driver.getCurrentUrl());

			if (StringUtils.equalsIgnoreCase(actualPageUrl, expectedPageURL) || actualPageUrl.matches(expectedPageURL)) {
				verifyURL = true;
			} else

				errorMessage = "FAIL verifyURLIs() - expected URL='" + expectedPageURL + ", but actual Page='" + actualPageUrl
						+ ".";
		}

		if (!verifyURL)
			takeFullScreenShot("verifyURLFailed");

		return verifyIsTrue(verifyURL, errorMessage);
	}

	private String uiMapGetDefaults(String requestedView, boolean updateUiMap) {

		String[] viewArray = new String[4];

		Integer i = 0;
		for (String item : requestedView.split(UIMAP_DELIM, 4)) {
			viewArray[i++] = item;
		}

		viewArray = getUiMapWithCurrentView(viewArray);

		if (updateUiMap) {

			uiMapCurrentPage = (StringUtils.isEmpty(viewArray[0])) ? jsonMapper.createObjectNode() : myCurrentPage;
			uiMapCurrentView = (StringUtils.isEmpty(viewArray[1])) ? jsonMapper.createObjectNode() : myCurrentView;
			uiMapCurrentArea = (StringUtils.isEmpty(viewArray[2])) ? jsonMapper.createObjectNode() : myCurrentArea;
		}

		return String.format("%1$s:%2$s:%3$s:%4$s", (Object[]) viewArray);
	}

	private String[] getUiMapWithCurrentView(String[] viewArray) {

		if (StringUtils.isEmpty(viewArray[0])) {

			viewArray[0] = StringUtils
					.defaultString(uiMap.path("application").path("properties").path("defaultPage").textValue());
		}

		if (!StringUtils.isEmpty(viewArray[0])) {

			for (JsonNode currentPage : uiMap.path("application").path("pages")) {

				if (StringUtils.equalsIgnoreCase(currentPage.path("properties").path("pageName").textValue(), viewArray[0])) {

					myCurrentPage = currentPage;
					break;
				}
			}

			if (StringUtils.isEmpty(viewArray[1])) {

				viewArray[1] = StringUtils.defaultString(myCurrentPage.path("properties").path("defaultView").textValue());
			}

			if (!StringUtils.isEmpty(viewArray[1])) {

				myCurrentView = getJsonNodeMatching(myCurrentPage.path("views"), "viewName", viewArray[1]);

				if (StringUtils.isEmpty(viewArray[2])) {

					viewArray[2] = StringUtils.defaultString(myCurrentView.path("defaultArea").textValue());
				}

				if (!StringUtils.isEmpty(viewArray[2])) {

					myCurrentArea = getJsonNodeMatching(myCurrentPage.path("areas"), "areaName", viewArray[2]);

				}
			}
		}
		return viewArray;
	}

	private ArrayList<String> uiMapGetPathToRequestedView(String virtualCurrentView, String requestedView,
			ArrayList<String> pathSoFar, int viewIndex) {

		if (!(uiMapViewsAlreadyChecked.contains(StringUtils.defaultString(virtualCurrentView))))
			uiMapViewsAlreadyChecked.add(StringUtils.defaultString(virtualCurrentView));

		ArrayList<JsonNode> areasToCheck = new ArrayList<JsonNode>();
		ArrayList<String[]> otherViewsToCheck = new ArrayList<String[]>();

		JsonNode myCurrentPage = jsonMapper.createObjectNode();
		JsonNode myCurrentView = jsonMapper.createObjectNode();

		String[] viewArray = new String[2];

		int virtualViewIndex = viewIndex;

		boolean foundMatchingElement = false;
		boolean walkNoFurther = false;

		ArrayList<String> pathToRequestedView = new ArrayList<String>();
		pathToRequestedView.addAll(pathSoFar);

		int i = 0;
		for (String item : virtualCurrentView.split(UIMAP_DELIM, 2)) {
			viewArray[i++] = StringUtils.defaultString(item);
		}

		for (JsonNode currentPage : uiMap.path("application").path("pages")) {

			if (StringUtils.equalsIgnoreCase(currentPage.path("properties").path("pageName").textValue(), viewArray[0])) {

				myCurrentPage = currentPage;
				break;
			}
		}

		myCurrentView = getJsonNodeMatching(myCurrentPage.path("views"), "viewName", viewArray[1]);
		if (myCurrentView.has("gestures")) {
			String directions[] = myCurrentView.path("gestures").toString().replace("[", "").replace("]", "").replace("{", "")
					.replace("}", "").replace("\"", "").split(",");
			String dir;
			String targetView;
			for (String direction : directions) {
				dir = direction.split(":")[0].trim();
				targetView = direction.substring(dir.length() + 1).trim();
				if (StringUtils.equalsIgnoreCase(targetView, requestedView)) {
					pathToRequestedView.add("gestures:" + dir);
					foundMatchingElement = true;
					break;
				} else {
					if (!(uiMapViewsAlreadyChecked.contains(StringUtils.defaultString(targetView)))) {
						String[] directionAndView = { "gestures:" + dir, targetView };
						otherViewsToCheck.add(directionAndView);
					}
				}
			}
		}
		if (!foundMatchingElement) {
			if (!(uiMapAreasAlreadyChecked.contains(StringUtils.defaultString(myCurrentView.path("defaultArea").textValue()))))
				areasToCheck.add(getJsonNodeMatching(myCurrentPage.path("areas"), "areaName", myCurrentView.path("defaultArea")
						.textValue()));

			for (JsonNode currentArea : myCurrentView.path("activeAreas")) {

				JsonNode currentAreaNode = getJsonNodeMatching(myCurrentPage.path("areas"), "areaName",
						StringUtils.defaultString(currentArea.textValue()));

				if (!uiMapAreasAlreadyChecked.contains(StringUtils.defaultString(currentArea.textValue()))
						&& !areasToCheck.contains(currentAreaNode))
					areasToCheck.add(currentAreaNode);
			}

			for (JsonNode currentArea : areasToCheck) {

				String areaName = StringUtils.defaultString(currentArea.path("areaName").textValue());

				for (JsonNode currentElement : currentArea.path("elements")) {

					String elementName = StringUtils.defaultString(currentElement.path("elementName").textValue());

					String elementView = StringUtils.defaultIfEmpty(currentElement.path("view").textValue(),
							StringUtils.defaultString(currentElement.path("view").toString()));

					String dontNavigate = StringUtils.defaultString(currentElement.path("dontNavigate").textValue());

					if (!StringUtils.isBlank(areaName))
						elementName = (areaName + UIMAP_DELIM + elementName);

					if (StringUtils.contains(elementView, "\",\"")) {

						String[] elementViewArray = StringUtils.split(elementView, "[\",]");

						for (String currentView : elementViewArray) {

							if (StringUtils.equalsIgnoreCase(currentView, requestedView)) {
								elementView = currentView;
							}
						}
					}

					if (!elementView.isEmpty()) {

						walkNoFurther = false;

						if (StringUtils.equalsIgnoreCase(elementView, requestedView)) {

							pathToRequestedView.add(elementName);
							foundMatchingElement = true;
							break;
						} else {

							walkNoFurther = (pathSoFar.contains(elementName) || StringUtils.equalsIgnoreCase(elementView,
									virtualCurrentView));

							if (!walkNoFurther) {

								if (StringUtils.equalsIgnoreCase(elementView, PREVIOUS_VIEW)) {

									if (!StringUtils.equalsIgnoreCase(dontNavigate, "true")
											&& ((pathToRequestedView.size() == 0) || (virtualViewIndex < uiMapViewIndex))
											&& (virtualViewIndex > 0)) {
										elementView = uiMapViewList.get(--virtualViewIndex);
									} else {
										walkNoFurther = true;

									}
								}

								if (!walkNoFurther) {

									String[] elementNameAndView = { elementName, elementView };
									otherViewsToCheck.add(elementNameAndView);
								}
							}
						}
					}
				}

				if (foundMatchingElement)
					break;

				uiMapAreasAlreadyChecked.add(currentArea.path("areaName").textValue());
			}
		}

		if (!(foundMatchingElement || otherViewsToCheck.isEmpty())) {

			ArrayList<String> possiblePathToRequestedView = new ArrayList<String>();
			ArrayList<ArrayList<String>> successfulPaths = new ArrayList<ArrayList<String>>();

			for (String[] elementNameAndView : otherViewsToCheck) {

				ArrayList<String> pathToTry = new ArrayList<String>();

				String elementName = elementNameAndView[0];
				String elementView = elementNameAndView[1];

				pathToTry.addAll(pathSoFar);
				pathToTry.add(elementName);

				possiblePathToRequestedView = uiMapGetPathToRequestedView(elementView, requestedView, pathToTry, virtualViewIndex);
				foundMatchingElement = (!possiblePathToRequestedView.equals(pathSoFar));

				if (foundMatchingElement)
					successfulPaths.add(possiblePathToRequestedView);
			}

			if (!successfulPaths.isEmpty()) {

				int indexOfShortestPath = -1;

				for (int index = 0; (index < successfulPaths.size()); index++) {

					if ((indexOfShortestPath == -1)
							|| (successfulPaths.get(index).size() < successfulPaths.get(indexOfShortestPath).size()))
						indexOfShortestPath = index;
				}

				pathToRequestedView = successfulPaths.get(indexOfShortestPath);
				foundMatchingElement = true;
				walkNoFurther = false;
			}

		}

		if (walkNoFurther || !foundMatchingElement) {

			if (!pathToRequestedView.isEmpty())
				pathToRequestedView.remove(pathToRequestedView.size() - 1);
		}

		return pathToRequestedView;
	}

	private ArrayList<String> splitrequestedViewToCorrectFormat(boolean loadInitialPage, String requestedView) {

		ArrayList<String> viewArray = new ArrayList<String>();
		String requestedViewWithDefaults = uiMapGetDefaults(requestedView, loadInitialPage);

		for (String item : requestedViewWithDefaults.split(UIMAP_DELIM, 4)) {
			viewArray.add(item);
		}

		return viewArray;
	}

	private boolean getToRequestedViewWay(boolean loadInitialPage, String requestedView) {

		boolean returnValue = false;
		ArrayList<String> pathToRequestedView = new ArrayList<String>();
		String currentView = String.format("%1$s:%2$s", uiMapCurrentPage.path("properties").path("pageName").textValue(),
				uiMapCurrentView.path("viewName").textValue());

		if (!loadInitialPage)
			pathToRequestedView = uiMapGetPathToRequestedView(currentView, requestedView, pathToRequestedView, uiMapViewIndex);

		if (!pathToRequestedView.isEmpty()) {

			log("Using viewTo() to navigate to '" + requestedView + "'.");

			uiMapWalkToRequestedView(pathToRequestedView);
			returnValue = true;
		}
		return returnValue;
	}

	private void waitForRequestViewToLoad(JsonNode currentPage) {

		if (getAppType().equals("WebApp")) {
			String uimapPath = currentPage.path("properties").path("path").textValue();
			if (uimapPath.contains("://")) {
				driver.get(uimapPath);
			} else {
				String appPath[] = uimapPath.split("/", 2);
				String actualPath = "";
				String domain = getAppUrl();
				if (appPath.length > 1) {
					actualPath = appPath[1];
					if (appPath[0].contains("<") && appPath[0].contains(">"))
						domain = getProperty("application." + appPath[0].replace("<", "").replace(">", ""));

				} else {
					if (uimapPath.contains("<") && uimapPath.contains(">"))
						domain = getProperty("application." + uimapPath.replace("<", "").replace(">", ""));
					if (domain.isEmpty())
						domain = getAppUrl();
				}
				driver.get(domain + actualPath);
			}

		}
	}

	private void waitForArea() {

		String locator = StringUtils.defaultString(uiMapCurrentArea.path("locator").textValue());

		if (!(locator.isEmpty() || waitForArea(locator, Long.parseLong(pageTimeout) / 1000))) {

			String message = "Area Load TimeOut:" + uiMapCurrentPage.path("pageName").textValue() + "_"
					+ uiMapCurrentView.path("viewName").textValue() + "_" + uiMapCurrentArea.path("areaName").textValue();

			takeFullScreenShot(message);
			Assert.assertTrue(false, message);
		}
	}

	protected boolean waitForArea(String areaLocator, Long timeout) {

		if (timeout == 0)
			timeout = Long.parseLong(viewTimeout) / 1000;
		final String locator = areaLocator;
		if (locator.isEmpty()) {
			if (!blankAreaLocator) {
				log("AreaLocator is empty, skip area checking.", 3);
				blankAreaLocator = true;
			}
			return true;
		}

		WebDriverWait driverWait = (WebDriverWait) new WebDriverWait(driver, timeout, 500).ignoring(
				StaleElementReferenceException.class).withMessage(
				"waitForAreaToLoad() timed out after " + timeout + " milliseconds.");
		try {

			driverWait.until(new ExpectedCondition<Boolean>() {

				public Boolean apply(WebDriver driver) {

					return getElements("", locator, "", "").size() > 0;
				}

			});

		} catch (TimeoutException e) {
			log("Area is not found!", 3);
			return false;
		}
		return true;
	}

	private boolean loadNewPage(boolean loadNewPage, ArrayList<String> viewArray, boolean loadInitialPage) {

		boolean returnValue = false;
		if (loadNewPage) {

			for (JsonNode currentPage : uiMap.path("application").path("pages")) {

				if (StringUtils.equalsIgnoreCase(currentPage.path("properties").path("pageName").textValue(), viewArray.get(0))) {

					waitForRequestViewToLoad(currentPage);

					returnValue = uiMapUpdateView(String.format("%1$s:%2$s", viewArray.get(0), viewArray.get(1)), true);
					waitForArea();

					if (loadInitialPage)
						uiMapUpdateView(String.format("%1$s:%2$s", viewArray.get(0), viewArray.get(1)), true);

					break;
				}
			}
		}

		return returnValue;
	}

	protected boolean uiMapSetView(String requestedView) {

		ArrayList<String> viewArray = new ArrayList<String>();
		boolean loadNewPage = true;
		boolean loadInitialPage = (uiMapViewIndex < 0);
		boolean returnValue = false;

		String currentPageName = uiMapCurrentPage.path("properties").path("pageName").textValue();

		if (!StringUtils.contains(requestedView, UIMAP_DELIM) && !StringUtils.isBlank(currentPageName))
			requestedView = (currentPageName + UIMAP_DELIM + requestedView);

		viewArray = splitrequestedViewToCorrectFormat(loadInitialPage, requestedView);

		if (!loadInitialPage
				&& StringUtils.equalsIgnoreCase(uiMapCurrentPage.path("properties").path("pageName").textValue(),
						viewArray.get(0))) {

			if (StringUtils.equalsIgnoreCase(uiMapCurrentView.path("viewName").textValue(), viewArray.get(1)))
				return true;

			loadNewPage = false;
		}

		uiMapAreasAlreadyChecked.clear();
		uiMapViewsAlreadyChecked.clear();

		returnValue = getToRequestedViewWay(loadInitialPage, requestedView);

		if ((!returnValue) && loadNewPage)

			returnValue = loadNewPage(loadNewPage, viewArray, loadInitialPage);

		waitForArea();
		String defaultView = uiMapViewList.get(uiMapViewIndex);

		if (returnValue)
			log("Successfully set view to '" + StringUtils.defaultIfBlank(requestedView, defaultView) + "'.");

		return returnValue;
	}

	public boolean uiMapUpdateView(String view) {

		return uiMapUpdateView(view, true);
	}

	private boolean uiMapUpdateView(String view, boolean addNewView) {

		boolean returnValue = true;
		boolean updateUiMap = true;

		if (StringUtils.containsIgnoreCase(view, PREVIOUS_VIEW)) {

			if (uiMapViewIndex > 0) {

				uiMapViewIndex--;
				view = uiMapViewList.get(uiMapViewIndex);
			} else
				updateUiMap = false;
		} else {

			if (StringUtils.contains(view, "\",\"")) {

				view = getCurrentAreaisDisplayed(view, 0);

			}

			if (addNewView && ((uiMapViewIndex < 0) || !StringUtils.equalsIgnoreCase(uiMapViewList.get(uiMapViewIndex), view))) {

				while (uiMapViewList.size() > (uiMapViewIndex + 1)) {
					uiMapViewList.remove(uiMapViewList.size() - 1);
				}

				uiMapViewList.add(view);
				uiMapViewIndex++;
			}
		}

		if (updateUiMap) {

			uiMapGetDefaults(view, true);

		}

		return returnValue;
	}

	private String getCurrentAreaisDisplayed(String view, long timeOut) {

		String[] viewArray = StringUtils.split(view, "[\",]");
		String areaLocator = "";
		view = "";

		for (String thisPageAndView : viewArray) {

			String thisPage = StringUtils.split(thisPageAndView, ":")[0];
			String thisView = StringUtils.split(thisPageAndView, ":")[1];

			for (JsonNode currentPage : uiMap.path("application").path("pages")) {

				if (StringUtils.equalsIgnoreCase(currentPage.path("properties").path("pageName").textValue(), thisPage)) {

					for (JsonNode currentView : currentPage.path("views")) {
						String actualView = StringUtils.defaultString(currentView.path("viewName").textValue());
						if (StringUtils.equalsIgnoreCase(actualView, thisView)) {
							String defaultArea = StringUtils.defaultString(currentView.path("defaultArea").textValue());
							for (JsonNode currentArea : currentPage.path("areas")) {
								if (StringUtils.equals(currentArea.path("areaName").textValue(), defaultArea)) {
									uiMapCurrentArea = currentArea;
									areaLocator = StringUtils.defaultString(currentArea.path("locator").textValue());

									JsonNode currentElements = currentArea.path("elements");
									String defaultAreaElementLocator = currentElements.path(0).path("locator").textValue();
									String defaultElement = currentElements.path(0).path("elementName").textValue();
									if (areaLocator.isEmpty()) {
										uiMapCurrentView = currentView;
										if (waitForElement(defaultElement)) {
											view = thisPageAndView;
											break;
										}
									} else {
										if (waitForArea(areaLocator, timeOut)) {
											view = thisPageAndView;

											if (waitForElementReadyWithElementLocator(areaLocator, defaultAreaElementLocator, "",
													"", timeOut)) {
												break;
											} else {
												log("The Element is not found: '" + defaultAreaElementLocator + "'.", 3);
												return "";
											}
										} else {
											log("The Area is not found : '" + areaLocator + "'.", 3);
											return "";
										}
									}
								}
							}

							if (!StringUtils.isEmpty(view))

								break;
						}
					}

					if (!StringUtils.isEmpty(view))
						break;
				}
			}

			if (!StringUtils.isEmpty(view))
				break;
		}
		return view;
	}

	public void switchToIFrame() {

		switchToIFrame("");
	}

	public void switchToIFrame(String areaName) {

		JsonNode myNode;
		if (areaName.isEmpty())
			myNode = uiMapCurrentArea;
		else {
			myNode = getJsonNodeMatching(uiMapCurrentPage.path("areas"), "areaName", areaName);
		}

		String currentIFrame = "";
		if (myNode.has("iFrame")) {
			currentIFrame = myNode.path("iFrame").textValue();
			if (!iFrameName.isEmpty()) {
				if (currentIFrame.equalsIgnoreCase(iFrameName) && iFrameAreaName.equalsIgnoreCase(areaName)) {
					return;
				}
				driver.switchTo().defaultContent();
			}
			if (currentIFrame.matches("<\\S*>=.*") && currentIFrame.startsWith("<"))
				driver.switchTo().frame(driver.findElement(getByObject(currentIFrame)));
			else
				driver.switchTo().frame(currentIFrame);
			iFrameName = currentIFrame;
			iFrameAreaName = areaName;
		} else {
			if (!iFrameName.isEmpty()) {
				driver.switchTo().defaultContent();
				iFrameName = "";
			}
		}

	}

	private void uiMapWalkToRequestedView(ArrayList<String> pathToRequestedView) {

		for (String clickMe : pathToRequestedView) {

			long startTime = System.currentTimeMillis();

			if (StringUtils.contains(clickMe, UIMAP_DELIM) && !StringUtils.contains(clickMe, "gestures")) {
				clickMe = clickMe.split(UIMAP_DELIM)[1];
				if (!click(clickMe))
					break;
			} else {
				clickMe = clickMe.split(UIMAP_DELIM)[1];
				if (StringUtils.contains(clickMe, "home") || StringUtils.contains(clickMe, "back")) {
					if (!click(clickMe))
						break;
				} else {
					if (!swipe(clickMe))
						break;
				}
			}

			float elapsedTime = (System.currentTimeMillis() - startTime) / 1000f;

			String requestedView = getElementAtt("view", clickMe);

			String viewType = (uiMapCurrentView.path("activeAreas").size() == 1) ? "dialog" : "view";
			System.out.printf(getCurrentTime() + " INFO - <UI_RESPONSE_TIME> - Opened " + viewType + " '" + requestedView
					+ "' in %.3f seconds.\n", elapsedTime);

			Assert.assertTrue(elapsedTime < maxPageTime);
		}
	}

	protected Set<String> getCurrentValidWindowNamesListInAll(Set<String> allWindowNames) {

		return null;
	}

	protected boolean verifyBodyTextContainsExpectedText(String expectedText, boolean isShown) {

		boolean returnValue = false;
		String bodyText = "";
		Long currentTimeMillis = System.currentTimeMillis();
		while ((System.currentTimeMillis() - currentTimeMillis) < Long.parseLong(elementTimeout)) {
			bodyText = driver.findElement(By.tagName("body")).getText();
			returnValue = bodyText.contains(expectedText) || bodyText.matches(expectedText);
			if (isShown == returnValue) {
				break;
			}
			waitByTimeout(500);
		}

		return returnValue;
	}

	public boolean isElementShown(String elementName) {

		boolean returnValue = false;

		try {
			returnValue = getElement(elementName).isDisplayed();
		} catch (Exception e) {
		}
		return returnValue;
	}

	protected boolean isTextShown(String elementNameOrMessage, boolean isShown) {

		return false;
	}

	private boolean verifyIsTrue(boolean expression, String errorMessage) {

		return verifyIsTrue(expression, errorMessage, true);
	}

	private boolean verifyIsTrue(boolean expression, String errorMessage, boolean failOnError) {

		if (!expression)
			if (failOnError) {
				String shotName = errorMessage.split("'", 3)[1];
				takeFullScreenShot(shotName);
				throw new RuntimeException(errorMessage);

			} else
				log(errorMessage, 2);
		return expression;
	}

	protected void waitForCondition(String js, String timeout) {

	}

	public void waitByTimeout(long waitFor) {

		try {
			Thread.sleep(waitFor);
		} catch (Exception e) {
		}
	}

	public void pressKey(Keys key) {

		Actions action = new Actions(driver);
		action.sendKeys(key).perform();
	}

	protected void takeFullScreenShot(String failure) {

		String timeStamp = getDate().replace("-", "") + "_" + getCurrentTime().replace(":", "").replace(".", "");
		failure = timeStamp + "_" + replaceIllegalFileName(failure, "_");
		String fileName = screenCapturePath + "/" + failure + ".png";

		getScreenShot(fileName);
	}

	private File getScreenShotFile() {

		File screenshot = null;
		if (!(driver instanceof TakesScreenshot)) {
			WebDriver augmentDriver = new Augmenter().augment(driver);
			screenshot = ((TakesScreenshot) augmentDriver).getScreenshotAs(OutputType.FILE);
		} else
			screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		return screenshot;
	}

	protected void getScreenShot(String fileName) {

		File screenshot = getScreenShotFile();
		try {
			copyScreenShot(screenshot, new File(fileName));
		} catch (IOException e) {
			log("Exception happen when getting screen shot, detail is : '" + e.getMessage() + "'. "
					+ "The screen shot operatioin was ignored. ", 3);
		}
	}

	protected static void copyScreenShot(File screenShotFile, File outputFile) throws IOException {

		FileInputStream imgIs = new FileInputStream(screenShotFile);
		FileOutputStream imageOs = new FileOutputStream(outputFile);
		FileChannel imgCin = imgIs.getChannel();
		FileChannel imgCout = imageOs.getChannel();
		imgCin.transferTo(0, imgCin.size(), imgCout);
		imgCin.close();
		imgCout.close();
		imgIs.close();
		imageOs.close();
	}

	protected WebElement getElement(String itemName, int itemMatching) {

		return getElement(itemName, itemMatching, "");

	}

	protected WebElement getElement(String elementName) {

		return getElement(elementName, "", "");
	}

	protected WebElement getElement(String itemName, Object itemMatching, String elementName) {

		return getElement(itemName, itemMatching, elementName, 0);
	}

	private WebElement getElement(String itemName, Object itemMatching, String elementName, int timeout) {

		String areaLocator = getAreaLocator(elementName.isEmpty() ? itemName : elementName);
		String elementLocator = getElementLocator(elementName);
		String listLocator = getElementLocator(itemName);
		WebElement returnValue = null;
		String errorMsg = "";

		List<WebElement> elements;
		boolean emptyItemMatching = true;
		if (itemMatching instanceof Integer)
			if ((Integer) itemMatching > 0)
				emptyItemMatching = false;
		if (itemMatching instanceof String)
			if (!((String) itemMatching).isEmpty())
				emptyItemMatching = false;
		if (emptyItemMatching) {
			if (listLocator.isEmpty()) {
				elements = getElements("", areaLocator, "", "");
				if (elements.size() > 0)
					returnValue = elements.get(0);
				else
					errorMsg = itemName;
			} else {
				elements = getElements(areaLocator, listLocator, "", "");
				if (elements.size() > 0)
					returnValue = elements.get(0);
				else
					errorMsg = itemName;

			}

		} else {
			if (elementLocator.isEmpty()) {
				List<WebElement> listElements = waitForElementList(areaLocator, listLocator, timeout);

				int matchingIndex = getMatchingIndex(listElements, itemMatching);
				if (matchingIndex >= 0)
					returnValue = listElements.get(matchingIndex);

			} else {
				elements = getElements(areaLocator, listLocator, itemMatching, elementLocator, timeout);
				if (elements.size() > 0)
					returnValue = elements.get(0);
				else
					errorMsg = elementName + " in " + itemName;
			}
		}

		if (returnValue == null) {
			throw new IndexOutOfBoundsException("Element '" + errorMsg + "' Not Found!");
		} else
			return returnValue;
	}

	private List<WebElement> getElements(String areaLocator, String itemLocator, Object itemMatching, String elementLocator) {

		return getElements(areaLocator, itemLocator, itemMatching, elementLocator, 0);
	}

	private List<WebElement> getElements(String areaLocator, String itemLocator, Object itemMatching, String elementLocator,
			int timeout) {

		if (areaLocator.isEmpty() && itemLocator.isEmpty() && elementLocator.isEmpty()) {
			throw new NoSuchElementException("Element Locator Error!");
		}
		String arealocatorType = getLocatorType(areaLocator);
		String arealocatorStr = getLocatorStr(areaLocator);

		String locator = "";
		WebElement parentElement = null;
		boolean emptyItemMatching = true;
		if (itemMatching instanceof Integer)
			if ((Integer) itemMatching > 0)
				emptyItemMatching = false;
		if (itemMatching instanceof String)
			if (!((String) itemMatching).isEmpty())
				emptyItemMatching = false;
		if (emptyItemMatching) {
			if (!arealocatorStr.isEmpty())
				if (waitForArea(areaLocator, (long) timeout)) {
					parentElement = driver.findElement(getByObjectByType(arealocatorStr, arealocatorType));
				} else
					throw new NoSuchElementException("Area Not Found!");
			locator = itemLocator;
		} else {
			if (elementLocator.isEmpty())
				throw new NoSuchElementException("Please use getElement(item,itemMatching) instead!");
			if (elementLocator.contains("<xpath>")) {
				log("It appears you are using <xpath> as element locator to find an element in a list, for current Webdriver version, it may not find what you exactly want. Please change the locator for elementLocator '"
						+ elementLocator + "'. Test may continue, but may result in a failure.", 3);
			}
			List<WebElement> listElements = waitForElementList(areaLocator, itemLocator, timeout);

			int index = getMatchingIndex(listElements, itemMatching);

			if (index >= 0)
				parentElement = listElements.get(index);
			locator = elementLocator;
		}

		String elementLocatorType = getLocatorType(locator);
		String elementLocatorStr = getLocatorStr(locator);

		if (parentElement == null)
			return driver.findElements(getByObjectByType(elementLocatorStr, elementLocatorType));
		else {
			return parentElement.findElements(getByObjectByType(elementLocatorStr, elementLocatorType));
		}
	}

	private String getLocatorType(String locator) {

		if (locator.isEmpty())
			return "";

		if (locator.matches("<\\S*>=.*") && locator.startsWith("<"))
			return locator.split("=")[0];

		return "css";
	}

	private String getLocatorStr(String locator) {

		if (locator.isEmpty())
			return "";

		String locatorType = getLocatorType(locator);

		if (locator.startsWith(locatorType))
			return locator.substring(locatorType.length() + 1);
		else
			return locator;
	}

	private List<WebElement> waitForElementList(final String areaLocator, final String listLocator, final int timeInSec) {

		WebDriverWait driverWait = (WebDriverWait) new WebDriverWait(driver, timeInSec, 500).ignoring(
				StaleElementReferenceException.class).withMessage("Refersh the element failure to time out " + timeInSec);
		try {

			driverWait.until(new ExpectedCondition<Boolean>() {

				public Boolean apply(WebDriver driver) {

					return (getElements(areaLocator, listLocator, "", "").size() > 0);
				}
			});
		} catch (TimeoutException e) {
			takeFullScreenShot(listLocator);
			log("Element List is not loaded in " + timeInSec + " seconds.");
		}
		return getElements(areaLocator, listLocator, "", "");

	}

	private int getMatchingIndex(List<WebElement> listElements, Object itemMatching) {

		int index = -1;

		if (itemMatching instanceof Integer)
			index = (Integer) itemMatching - 1;
		if (itemMatching instanceof String) {
			String matchingString = (String) itemMatching;
			if (StringUtils.isNumeric(matchingString)) {
				index = Integer.valueOf(matchingString) - 1;
			} else {
				itemMatching = getLocalizedText(matchingString);
				String getAttText = "";

				for (int i = 0; i < listElements.size(); i++) {
					getAttText = listElements.get(i).getText();
					if (StringUtils.containsIgnoreCase(getAttText, matchingString)) {
						index = i;
						break;
					}
				}
			}
		}

		return index;
	}

	protected List<WebElement> getElements(String elementName) {

		String areaLocator = getAreaLocator(elementName);

		String elementLocator = getElementLocator(elementName);
		return getElements(areaLocator, elementLocator, "", "");

	}

	private By getByObject(String elementLocator) {

		if (!elementLocator.contains("="))
			throw new RuntimeException("Incorrect elementlocator Format. type=locator expected.");
		String locatorType = elementLocator.split("=")[0];
		String locatorStr = elementLocator.substring(locatorType.length() + 1);
		String subLocatorType = getSubSelectorType(locatorStr);
		String subLocatorStr = null;
		By returnObject;
		if (subLocatorType != null) {
			subLocatorStr = locatorStr.split(subLocatorType + "=")[1];
			locatorStr = locatorStr.split(subLocatorType + "=")[0];
			returnObject = getByObjectByType(subLocatorStr, subLocatorType);
		} else
			returnObject = getByObjectByType(locatorStr, locatorType);

		return returnObject;
	}

	private By getByObjectByType(String locatorStr, String locatorType) {

		if (locatorType.equals("<id>")) {
			return By.id(locatorStr);
		}

		if (locatorType.equals("<class>")) {
			return By.className(locatorStr);
		}

		if (locatorType.equals("<name>")) {
			return By.name(locatorStr);
		}

		if (locatorType.equals("<xpath>")) {
			return By.xpath(locatorStr);
		}

		if (locatorType.equals("<tagName>")) {
			return By.tagName(locatorStr);
		}

		if (locatorType.equals("<linkText>")) {
			return By.linkText(locatorStr);
		}

		if (locatorType.equals("<partialLinkText>")) {
			return By.partialLinkText(locatorStr);
		}

		return By.cssSelector(locatorStr.replace("css=", "").replace("<css>=", ""));
	}

	private String getSubSelectorType(String locatorStr) {

		String returnValue = null;

		if (locatorStr.contains("<id>=")) {
			returnValue = "<id>";
		}

		if (locatorStr.contains("<class>=")) {
			returnValue = "<class>";
		}

		if (locatorStr.contains("<name>=")) {
			returnValue = "<name>";
		}

		if (locatorStr.contains("<xpath>=")) {
			returnValue = "<xpath>";
		}

		if (locatorStr.contains("<tagName>=")) {
			returnValue = "<tagName>";
		}

		if (locatorStr.contains("<linkText>=")) {
			returnValue = "<linkText>";
		}

		if (locatorStr.contains("<partialLinkText>=")) {
			returnValue = "<partialLinkText>";
		}

		if (locatorStr.contains("<css>=")) {
			returnValue = "<css>";
		}

		return returnValue;
	}

	public String getElementLocator(String elementName) {

		return getElementAtt("locator", elementName);
	}

	private String getConnectValueAndReplace(String elementLocator) {

		while (elementLocator.contains("{{") && elementLocator.contains("}}")) {

			String embeddedPropertyName = StringUtils.substringBetween(elementLocator, "{{", "}}");
			String connectText = getLocalizedText(embeddedPropertyName);

			if (connectText.isEmpty() && connectText.equals("")) {
				log("Cannot find string for '" + embeddedPropertyName + "'.");
				throw new NullPointerException("The '" + embeddedPropertyName + "' is error, '" + connectText + "' is not null.");
			} else
				log("Replaced '" + embeddedPropertyName + "' with '" + connectText + "'.");
			elementLocator = StringUtils.replace(elementLocator, ("{{" + embeddedPropertyName + "}}"), connectText);
		}
		return elementLocator;
	}

	private boolean waitForElementNotShown(final String listName, final Object itemMatching, final String elementName,
			Long timeInSec) {

		long timeOut = timeInSec;
		if (timeInSec == 0) {
			timeOut = Long.parseLong(elementTimeout) / 1000;
		}
		try {
			WebDriverWait driverWait = (WebDriverWait) new WebDriverWait(driver, timeOut, 500).ignoring(
					StaleElementReferenceException.class).withMessage("Refersh the element failure to time out " + timeOut);
			return driverWait.until(new ExpectedCondition<Boolean>() {

				public Boolean apply(WebDriver driver) {

					try {
						if (getElement(listName, itemMatching, elementName, 1).isDisplayed()) {
							return false;
						}
					} catch (IndexOutOfBoundsException e) {
					} catch (NoSuchElementException e) {
					}

					log("Element '" + (elementName.isEmpty() ? listName : elementName) + "' is not present on this page.");
					return true;
				}
			});
		} catch (Exception e) {
			log("Element '" + (elementName.isEmpty() ? listName : elementName) + "' still present in " + timeOut + " secs.");
			takeFullScreenShot(elementName.isEmpty() ? listName : elementName);
			return false;
		}

	}

	private boolean waitForElementNotShown(String listName, Object itemMatching, String elementName) {

		return waitForElementNotShown(listName, itemMatching, elementName, (long) 0);
	}

	private boolean waitForElementReadyWithElementLocator(final String areaLocator, final String listLocator,
			final Object itemMatching, final String elementLocator, Long timeInSec) {

		long timeOut = timeInSec;
		try {

			WebDriverWait driverWait = (WebDriverWait) new WebDriverWait(driver, timeOut).ignoring(
					StaleElementReferenceException.class).withMessage("Refersh the element failure to time out " + timeOut);

			return driverWait.until(new ExpectedCondition<Boolean>() {

				public Boolean apply(WebDriver driver) {

					List<WebElement> elements = getElements(areaLocator, listLocator, itemMatching, elementLocator);
					return elements.size() > 0 && elements.get(0).isDisplayed();
				}
			});
		} catch (Exception e) {
			log("The Element is not found <areaLocator> = '" + areaLocator + "': <elementLocator> = '" + elementLocator + "' in "
					+ timeOut + " seconds.", 3);

			takeFullScreenShot("ChangeViewFailed");
			return false;
		}

	}

	private boolean waitForElement(final String listName, final Object itemMatching, final String elementName, Long timeInSec) {

		long timeOut = timeInSec;
		try {

			WebDriverWait driverWait = (WebDriverWait) new WebDriverWait(driver, timeOut).ignoring(
					StaleElementReferenceException.class).withMessage("Refresh the element failure to time out " + timeOut);
			return driverWait.until(new ExpectedCondition<Boolean>() {

				public Boolean apply(WebDriver driver) {

					boolean returnValue = false;
					try {

						returnValue = getElement(listName, itemMatching, elementName).isDisplayed();
					} catch (IndexOutOfBoundsException e) {

					}

					return returnValue;
				}
			});
		} catch (TimeoutException e) {
			log("The Element is not found elementName '" + (elementName.isEmpty() ? listName : elementName) + "' in " + timeOut
					+ " seconds.", 3);
			takeFullScreenShot(elementName.isEmpty() ? listName : elementName);
			return false;
		}
	}

	protected boolean waitForElement(String listName, Object itemMatching, String elementName) {

		return waitForElement(listName, itemMatching, elementName, Long.parseLong(elementTimeout) / 1000);

	}

	private boolean waitForElementShown(String listName, Object itemMatching, String elementName) {

		return waitForElement(listName, itemMatching, elementName);
	}

	protected boolean waitForElement(String elementName) {

		return waitForElement(elementName, "", "");
	}

	private String getLocalizedText(String textString) {

		if (StringUtils.isBlank(textString) || StringUtils.isNumeric(textString))
			return textString;
		String returnValue = getConfigValue(content, textString).trim();

		if (StringUtils.isBlank(returnValue)) {

			returnValue = textString;
		}

		returnValue = StringUtils.replace(returnValue, "%s", ".*?");
		return returnValue;
	}

	public String getElementText(String elementName) {

		waitForElement(elementName);
		String elementText = getElement(elementName).getText();
		log("The text of the element '" + elementName + "' is '" + elementText + "'.");
		return elementText;

	}

	protected String getElementAllText(String elementName) {

		String allText = "";
		List<WebElement> allElement = getElements(elementName);
		for (WebElement element : allElement) {
			allText += element.getText();
		}
		return allText;
	}

	public int getElementsSize(String elementName) {

		List<WebElement> elements = getElements(elementName);
		return elements.size();
	}

	protected String getTargetView(String type, String elementName, String direction) {

		String targetView = "";
		if (!StringUtils.isEmpty(type)) {
			if (type.equalsIgnoreCase("click")) {
				return getElementAtt("view", elementName);
			}
			if (type.equalsIgnoreCase("gestures")) {
				if (!StringUtils.isEmpty(direction)) {
					try {
						JsonNode Currentview = uiMapCurrentView.path(type);
						targetView = Currentview.path(direction).textValue();

						if (targetView.contains(":")) {
							return targetView;
						} else {
							throw new NumberFormatException("The format is error");
						}
					} catch (Exception e) {
					}

				} else {
					throw new NullPointerException("The " + type + " is null.");
				}
			}

			if (type.equalsIgnoreCase("tap")) {
				return getElementAtt("view", elementName);
			}

			if (type.equalsIgnoreCase("press")) {
				return getElementAtt("pressView", elementName);
			}
		} else {
			throw new AssertionFailedError("The type is null");
		}
		return "";

	}

	public boolean isNotEnabled(String listName, int itemMatching) {

		return !isEnabled(listName, itemMatching);
	}

	public boolean isNotEnabled(String elementName) {

		return !isEnabled(elementName);
	}

	private void waitForElementEnabled(String listName, Object itemMatching, String elementName) {

		boolean returnValue = false;
		for (int i = 0; i < Long.parseLong(elementTimeout) / 1000; i++) {
			returnValue = isEnabled(listName, itemMatching, elementName);
			waitByTimeout(1000);
			if (returnValue)
				break;
		}
		if (!returnValue) {
			throw new RuntimeException("Wait for <" + (elementName.isEmpty() ? listName : elementName) + "> enable timeout "
					+ Long.parseLong(elementTimeout) / 1000);
		}

	}

	public void waitForElementEnabled(String listName, int itemMatching) {

		waitForElementEnabled(listName, itemMatching, "");
	}

	public void waitForElementEnabled(String elementName) {

		waitForElementEnabled(elementName, "", "");
	}

	private boolean isEnabled(String listName, Object itemMatching, String elementName) {

		return getElement(listName, itemMatching, elementName).isEnabled();
	}

	public boolean isEnabled(String listName, int itemMatching) {

		return getElement(listName, itemMatching, "").isEnabled();
	}

	public boolean isEnabled(String elementName) {

		return getElement(elementName, "", "").isEnabled();
	}

	private String getAreaLocator(String elementName) {

		return getElementAtt("areaLocator", elementName);

	}

	private String getElementAtt(String attributeName, String elementName) {

		String elementAttributeValue = "";
		String areaLocator = "";
		String areaName = "";

		ArrayList<JsonNode> areas = new ArrayList<JsonNode>();

		if (uiMapCurrentArea.has("areaName"))
			areas.add(uiMapCurrentArea);

		String activeAreas = uiMapCurrentView.path("activeAreas").toString();

		activeAreas = StringUtils.removeStart(activeAreas, "[\"");
		activeAreas = StringUtils.removeEnd(activeAreas, "\"]");

		String[] activeAreasArray = activeAreas.split("\\W,\\W");

		for (String currentArea : activeAreasArray) {

			if (!StringUtils.isEmpty(currentArea)) {

				for (JsonNode item : uiMapCurrentPage.path("areas")) {

					String itemArea = item.path("areaName").textValue();

					if (StringUtils.equals(itemArea, currentArea) && !areas.contains(item))
						areas.add(item);
				}
			}
		}
		ArrayList<JsonNode> elementMatches = new ArrayList<JsonNode>();
		JsonNode matchedElement = jsonMapper.createObjectNode();
		;
		for (JsonNode currentArea : areas) {

			for (JsonNode currentElement : currentArea.path("elements")) {

				if (StringUtils.equalsIgnoreCase(currentElement.path("elementName").textValue(), elementName)) {
					matchedElement = currentElement.deepCopy();
					((ObjectNode) matchedElement).put("areaLocator",
							StringUtils.defaultString(currentArea.path("locator").textValue()));
					((ObjectNode) matchedElement).put("areaName",
							StringUtils.defaultString(currentArea.path("areaName").textValue()));
					elementMatches.add(matchedElement);
				}
			}
		}
		for (JsonNode currentElement : elementMatches) {

			elementAttributeValue = "";

			elementAttributeValue = currentElement.path(attributeName).toString().replace("\"", "");
			areaLocator = StringUtils.defaultString(currentElement.path("areaLocator").textValue());
			areaName = StringUtils.defaultString(currentElement.path("areaName").textValue());

			switchToIFrame(areaName);

			if (elementMatches.size() > 1) {
				if (waitForElementReadyWithElementLocator(areaLocator, "", "",
						StringUtils.defaultString(currentElement.path("locator").textValue()), (long) 3)) {
					break;
				}
			}
		}

		if (attributeName.equalsIgnoreCase("areaLocator")) {
			return getConnectValueAndReplace(areaLocator);
		}
		if (attributeName.equalsIgnoreCase("areaName")) {
			return getConnectValueAndReplace(areaName);
		} else
			return getConnectValueAndReplace(elementAttributeValue);

	}

	private boolean swipe(String direction) {

		return false;
	}

	public void runTest(String sheetName) {

		runTest("", sheetName);
	}

	public void runTest(String file, String sheetName) {

		ParseExcel pe = new ParseExcel();
		String excelFile = getProperty("app.case.file");
		file = excelFile.isEmpty() ? file : excelFile;
		String action = "";
		String element = "";
		String value = "";

		ArrayList<String> steps = pe.getSteps(excelFile, sheetName);

		for (String step : steps) {

			if (!step.contains(":"))
				action = step;
			else {
				action = step.split(":")[0];
				String elementstr = step.split(":", 2)[1];

				if (elementstr.contains(":")) {
					element = elementstr.split(":")[0];
					value = elementstr.split(":")[1];
				} else
					element = elementstr;
			}

			if (action.equals("isDisplay"))
				if (value.isEmpty())
					isDisplay(element);
				else
					isDisplay(element, Integer.parseInt(value));
			if (action.equals("sendKey"))
				sendKey(element, value);
			if (action.equals("click"))
				if (value.isEmpty())
					click(element);
				else
					click(element, Integer.parseInt(value));

			value = "";

		}
	}
}

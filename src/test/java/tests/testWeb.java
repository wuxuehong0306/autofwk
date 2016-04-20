package tests;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import fwk.WebFwk;

public class testWeb {

	private WebFwk web;

	@BeforeMethod
	public void setUp() {

		web = new WebFwk();
		web.openApp();
	}

	@AfterMethod
	public void tesrDown() {

		web.close();
	}

	@Test
	public void test010RunTest() {

		web.openApp();

		web.isDisplay("logo");
		web.isDisplay("输入内容");
		web.sendKey("输入内容", "test");
		web.click("百度一下");
//		web.waitFor(2000);
		web.isDisplay("搜索结果");

	}

	@Test
	public void test020getStepFromExcel() {

		web.runTest("Homepage");

	}
}

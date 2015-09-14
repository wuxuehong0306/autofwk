package tests;

import org.testng.annotations.Test;

import fwk.WebFwk;

public class testWeb {

	private WebFwk web;

	@Test
	public void test020getStepFromExcel() {

		web = new WebFwk();
		web.openApp();

		web.runTest("Homepage");

		web.close();
	}
}

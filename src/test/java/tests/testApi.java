package tests;

import org.testng.annotations.Test;

import fwk.ApiFwk;

public class testApi {

	private ApiFwk api;

	@Test
	public void test020getStepFromExcel() {

		api = new ApiFwk("get");

		api.runTest();
		api.getResponseBody();

	}
}

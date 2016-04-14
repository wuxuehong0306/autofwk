package tests;

import org.testng.annotations.Test;

import fwk.ApiFwk;

public class testApi {

	private ApiFwk api;


	@Test
	public void testBaidu() {

		api = new ApiFwk("get");
		System.out.println(api.runTest());
	}
}

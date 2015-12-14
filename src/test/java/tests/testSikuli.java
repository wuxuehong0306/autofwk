package tests;

import org.testng.annotations.Test;

import fwk.SikuliFwk;

public class testSikuli {

	@Test
	public void test01() {

		SikuliFwk st = new SikuliFwk();
		st.doubleClick("main");
		st.click("login");

		st.wait("我的自选");
		st.click("我的自选");

		st.click("close");

		st.click("logout");
	}
}

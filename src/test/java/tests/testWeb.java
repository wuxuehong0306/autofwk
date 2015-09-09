package tests;

import org.testng.annotations.Test;

import fwk.WebFwk;

public class testWeb {

	private WebFwk web;

	@Test
	public void test010baidu() {

		web = new WebFwk();
		web.openApp();

		web.verifyIsShown("logo");
		web.verifyIsShown("输入内容");
		web.setValueTo("输入内容", "test");
		web.clickOn("百度一下");

		web.verifyIsShown("搜索结果");
		web.log(web.getValueOf("搜索结果"));

		web.close();
	}
}

package tests;

import org.testng.annotations.Test;

import fwk.WebFwk;

public class testWeb {

	private WebFwk web;

	@Test
	public void test010baidu() {

		web = new WebFwk();
		web.openApp();

		web.isDisplay("logo");
		web.isDisplay("输入内容");
		web.sendKey("输入内容", "test");
		web.click("百度一下");

		web.isDisplay("搜索结果");
		web.log(web.getText("搜索结果"));

		web.close();
	}
}

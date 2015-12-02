package tests;

import org.testng.annotations.Test;

import fwk.ApiFwk;

public class testApi {

	/**
	 * 1.股票锦囊每日领券
	 * 2.股票锦囊-分享
	 * 3.每天实时解盘中领取
	 * 4.实时解盘-分享
	 * 5.优品有礼界面-分享
	 * 6.竞猜大盘涨跌-分享
	 */
	private ApiFwk api;
	private String user = "zxc9988888";

	@Test
	public void test010get() {

		// for (int use = 0; use <= 9; use++) {
		for (int i = 1; i <= 6; i++) {
			api = new ApiFwk("get");
			api.path("/action/getTicket");
			api.param("userName", user);
			api.param("actionId", Integer.toString(i));
			api.param("deviceType", "IOS");
			api.param("deviceId", "355674588393238");
			if (i == 1)
				api.param("source", "股票锦囊每日领券");
			if (i == 2)
				api.param("source", "股票锦囊-分享");
			if (i == 3)
				api.param("source", "每天实时解盘中领取");
			if (i == 4)
				api.param("source", "实时解盘-分享");
			if (i == 5)
				api.param("source", "优品有礼界面-分享");
			if (i == 6)
				api.param("source", "竞猜大盘涨跌-分享");
			api.runTest();
			api.getResponseBody();
			api.waitByTimeout(1000);
		}
		// }
	}

	@Test
	public void testBaidu() {

		api = new ApiFwk("get");
		System.out.println(api.runTest());
	}
}

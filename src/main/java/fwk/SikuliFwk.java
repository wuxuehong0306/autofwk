package fwk;

import org.sikuli.script.Screen;

import tool.getScreenShot;

public class SikuliFwk {

	private Screen s = new Screen();
	private getScreenShot gss = new getScreenShot();
	private String root;
	private String path;

	public SikuliFwk() {

		root = Thread.currentThread().getContextClassLoader().getResource(".").getPath();
		path = root.replace("target/test-classes/", "src/test/resources/data/imgs/");
	}

	private String fullPath(String imgName) {

		return path + imgName + ".png";
	}

	public void click(String imgName) {

		try {
			System.out.println("Click on 【" + imgName + "】");
			wait(imgName);
			s.click(fullPath(imgName));
		} catch (Exception e) {
			throw new RuntimeException("Cannot find the img file: " + imgName);
		}
	}

	public void doubleClick(String imgName) {

		try {
			System.out.println("Double click on 【" + imgName + "】");
			wait(imgName);
			s.doubleClick(fullPath(imgName));
		} catch (Exception e) {
			throw new RuntimeException("Cannot find the img file: " + imgName);
		}
	}

	public void rightClick(String imgName) {

		try {
			System.out.println("Right click on 【" + imgName + "】");
			wait(imgName);
			s.rightClick(fullPath(imgName));
		} catch (Exception e) {
			throw new RuntimeException("Cannot find the img file: " + imgName);
		}
	}

	public void hover(String imgName) {

		try {
			System.out.println("Hover on 【" + imgName + "】");
			wait(imgName);
			s.hover(fullPath(imgName));
		} catch (Exception e) {

			throw new RuntimeException("Cannot find the img file: " + imgName);
		}
	}

	public void type(String text) {

		System.out.println("Type text: 【" + text + "】");
		s.type(text);
	}

	public void wait(String imgName) {

		try {
			s.wait(fullPath(imgName));
		} catch (Exception e) {
			gss.takeShot(imgName);
			throw new RuntimeException("Cannot find the img file: " + imgName);
		}
	}

	public void find(String imgName) {

		try {
			s.find(fullPath(imgName));
		} catch (Exception e) {
			throw new RuntimeException("Cannot find the img file: " + imgName);
		}
	}

	public void findText(String imgName) {

		try {
			s.findText(fullPath(imgName));
		} catch (Exception e) {
			throw new RuntimeException("Cannot find the img file: " + imgName);
		}
	}

}

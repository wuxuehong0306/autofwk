package tool;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;

public class getScreenShot {

	static int serialNum = 0;
	Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

	private String getPath(String fileName) {

		Date d = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String date = dateFormat.format(d);

		Date today = new Date();
		SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss.SSS");
		String time = f.format(today);

		String timeStamp = date.replace("-", "") + "_" + time.replace(":", "").replace(".", "");
		fileName = timeStamp + "_" + fileName;

		String testRoot = StringUtils.defaultString(Thread.currentThread().getContextClassLoader().getResource(".").getPath())
				.replace("test-classes/", "");

		String screenCapturePath = (testRoot.replace("test-classes/", "") + "screenCapture");

		File screenCaptureFolder = new File(screenCapturePath);

		if (!screenCaptureFolder.exists())
			screenCaptureFolder.mkdirs();

		fileName = screenCapturePath + "/" + fileName + ".png";

		return fileName;

	}

	public void takeShot(String fileName) {

		try {
			BufferedImage screenshot = (new Robot()).createScreenCapture(new Rectangle(0, 0, (int) d.getWidth(), (int) d
					.getHeight()));
			serialNum++;
			String name = getPath(fileName);
			File f = new File(name);
			ImageIO.write(screenshot, "png", f);
		} catch (Exception ex) {
		}
	}

}

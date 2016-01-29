package tool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jxl.Workbook;
import jxl.common.Logger;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class CreateExcel {

	private String[] title;
	private String sheetName;

	private static WritableWorkbook book = null;
	private static WritableSheet sheet = null;

	private static Logger logger = Logger.getLogger(CreateExcel.class);
	private String testRoot = GeneralMethods.getTestRoot().replace("target/test-classes/", "src/test/resources/data/")
			+ "Performance/";
	private File file;

	/*
	 * titleName[0]表示Excel文件的名字
	 * titleName后面的值是Excel第一行要填入的值
	 */
	public CreateExcel(String... titleName) {

		sheetName = GeneralMethods.getDate();
		file = new File(testRoot + titleName[0]);
		List<String> temp = new ArrayList<String>();

		for (String s : titleName) {
			temp.add(s);
		}

		temp.remove(0);

		titleName = (String[]) temp.toArray(new String[temp.size()]);

		create(titleName);
	}

	private void create(String[] titleNames) {

		title = titleNames;

		try {
			Workbook wb = Workbook.getWorkbook(file);
			book = Workbook.createWorkbook(file, wb);
			sheet = addTitle();

		} catch (Exception e) {
			logger.debug("CreateWorkbook failed !");
			e.printStackTrace();
		}
	}

	public void addDataToExcel(String testname, double... data) {

		try {
			Workbook wb = Workbook.getWorkbook(file);

			book = Workbook.createWorkbook(file, wb);
			sheet = book.getSheet(sheetName);

			int length = sheet.getRows();

			Label name = new Label(0, length, testname);
			sheet.addCell(name);
			for (int i = 1; i < data.length + 1; i++) {
				Label label = new Label(i, length, Double.toString(data[i - 1]));
				sheet.addCell(label);
			}
			book.write();
			book.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private WritableSheet addTitle() {

		String nameStrs = "";
		String[] sheetNames = book.getSheetNames();
		for (String sheetName : sheetNames) {
			nameStrs += sheetName;
		}
		try {
			if (nameStrs.contains(sheetName))
				sheet = book.getSheet(sheetName);
			else {
				sheet = book.createSheet(sheetName, 0);
				sheet.getSettings().setDefaultColumnWidth(25);

				for (int i = 0; i < title.length; i++) {
					sheet.addCell(new Label(i, 0, title[i]));
				}

			}
			book.write();
			book.close();
		} catch (Exception e) {
		}
		return sheet;
	}

}

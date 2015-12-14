package tool;

import java.io.File;
import java.util.ArrayList;

import jxl.Sheet;
import jxl.Workbook;

public class ParseExcel {

	private Workbook book;
	private String ExcelPath;

	private String excelPath(String ExcelPath) {

		try {
			book = Workbook.getWorkbook(new File(ExcelPath));
			this.ExcelPath = ExcelPath;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this.ExcelPath;
	}

	private Sheet createSteps(String sheetName) {

		Sheet sheet = book.getSheet(sheetName);

		String action = sheet.getCell(0, 0).getContents();
		String element = sheet.getCell(1, 0).getContents();
		String value = sheet.getCell(2, 0).getContents();
		if (action.isEmpty() || element.isEmpty() || value.isEmpty())
			throw new RuntimeException("The Excel format is incorrect.");

		return sheet;

	}

	public ArrayList<String> getSteps(String excelFile, String sheetName) {

		excelPath(excelFile);
		ArrayList<String> steps = new ArrayList<String>();
		Sheet sheet = createSteps(sheetName);
		int rows = sheet.getRows();
		for (int row = 1; row < rows; row++) {
			String action = sheet.getCell(0, row).getContents();
			String element = sheet.getCell(1, row).getContents();
			String value = sheet.getCell(2, row).getContents();
			if (value.isEmpty())
				steps.add(action + ":" + element);
			else
				steps.add(action + ":" + element + ":" + value);
		}

		return steps;
	}

}

package tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

import core.InitClass;

public class AndroidMonitor extends InitClass {

	private static String PID(String PackageName) {

		Process proc = null;
		String str3 = null;
		try {
			Runtime runtime = Runtime.getRuntime();
			proc = runtime.exec("adb2 shell ps |grep  " + PackageName);

			if (proc.waitFor() != 0) {
				System.err.println("exit value = " + proc.exitValue());
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			StringBuffer stringBuffer = new StringBuffer();
			String line = null;
			while ((line = in.readLine()) != null) {
				stringBuffer.append(line + " ");

			}
			String str1 = stringBuffer.toString();
			String str2 = str1.substring(str1.indexOf(" " + PackageName) - 46, str1.indexOf(" " + PackageName));
			String PID = str2.substring(0, 7);
			PID = PID.trim();

			str3 = PID;
		} catch (Exception e) {
			System.err.println(e);
		} finally {
			try {
				proc.destroy();
			} catch (Exception e2) {
			}
		}

		return str3;
	}

	public static double Flow(String PackageName) {

		double flow = 0;
		try {

			String Pid = PID(PackageName);

			Runtime runtime = Runtime.getRuntime();
			/* Process proc2 = runtime.exec(""); */
			Process proc = runtime.exec("adb2 shell cat /proc/" + Pid + "/net/dev");
			try {
				if (proc.waitFor() != 0) {
					System.err.println("exit value = " + proc.exitValue());
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				StringBuffer stringBuffer = new StringBuffer();
				String line = null;
				while ((line = in.readLine()) != null) {
					stringBuffer.append(line + " ");

				}
				String str1 = stringBuffer.toString();
				String str2 = str1.substring(str1.indexOf("wlan0:"), str1.indexOf("wlan0:") + 90);
				String str4 = str2.substring(7, 16);
				str4 = str4.trim();
				String str6 = str2.substring(67, 75);
				str6 = str6.trim();
				int b = Integer.parseInt(str4);
				int a = Integer.parseInt(str6);

				double sendFlow = a / 1024;
				double revFlow = b / 1024;
				flow = sendFlow + revFlow;

			} catch (InterruptedException e) {
				System.err.println(e);
			} finally {
				try {
					proc.destroy();
				} catch (Exception e2) {
				}
			}
		} catch (Exception StringIndexOutOfBoundsException) {
			return -0.1;

		}

		return flow;
	}

	public static double CPU(String PackageName) {

		double Cpu = 0;
		try {

			Runtime runtime = Runtime.getRuntime();
			Process proc = runtime.exec("adb2 shell top -n 1| grep " + PackageName);
			try {
				if (proc.waitFor() != 0) {
					System.err.println("exit value = " + proc.exitValue());
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				StringBuffer stringBuffer = new StringBuffer();
				String line = null;
				while ((line = in.readLine()) != null) {
					stringBuffer.append(line + " ");

				}
				String str1 = stringBuffer.toString();
				String str3 = str1.substring(str1.indexOf(PackageName) - 43, str1.indexOf(PackageName));
				String cpu = str3.substring(0, 4);
				cpu = cpu.trim();
				Cpu = Double.parseDouble(cpu);

			} catch (InterruptedException e) {
				System.err.println(e);
			} finally {
				try {
					proc.destroy();
				} catch (Exception e2) {
				}
			}
		} catch (Exception StringIndexOutOfBoundsException) {

			System.out.print("请检查设备是否连接");

			return -0.1;
		}

		return Cpu;

	}

	public static double Memory(String PackageName) {

		double Heap = 0;

		try {
			Runtime runtime = Runtime.getRuntime();
			Process proc = runtime.exec("adb2 shell dumpsys meminfo " + PackageName);
			try {
				if (proc.waitFor() != 0) {
					System.err.println("exit value = " + proc.exitValue());
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				StringBuffer stringBuffer = new StringBuffer();
				String line = null;
				while ((line = in.readLine()) != null) {
					stringBuffer.append(line + " ");

				}

				String str1 = stringBuffer.toString();
				String str2 = str1.substring(str1.indexOf("Objects") - 60, str1.indexOf("Objects"));
				String str3 = str2.substring(0, 10);
				str3 = str3.trim();
				Heap = Double.parseDouble(str3) / 1024;
				DecimalFormat df = new DecimalFormat("#.000");
				String memory = df.format(Heap);
				Heap = Double.parseDouble(memory);

			} catch (InterruptedException e) {
				System.err.println(e);
			} finally {
				try {
					proc.destroy();
				} catch (Exception e2) {
				}
			}
		}

		catch (Exception StringIndexOutOfBoundsException) {
			System.out.print("请检查设备是否连接");

		}
		return Heap;
	}

	public void Monkey(String PackageName, String count) {

		Monkey("", PackageName, "", count, "C:\\monkey\\" + getTimeStamp() + ".txt");

	}

	private void Monkey(String DevicesName, String PackageName, String sleepTime, String count, String path) {

		Runtime runtime1 = Runtime.getRuntime();
		String cmd = "adb " + DevicesName + " shell monkey -p " + PackageName + " --throttle 150 " + count + " > " + path;
		try {
			String[] args = new String[] { "cmd", "/c", cmd };

			Process pro1 = runtime1.exec(args);
			if (pro1.waitFor() != 0) {
				System.err.println("exit value:" + pro1.exitValue());
				throw new RuntimeException("shell monkey failed!");

			}
			Thread.sleep(5000);

		} catch (Exception e) {
			throw new RuntimeException("error Message:" + e.getMessage());
		}
		String log = checkLog(path);
		if (log.contains("crash") || log.contains("NullPointerException"))
			throw new RuntimeException(log);

	}

	@SuppressWarnings("resource")
	private String checkLog(String path) {

		String log = "";
		try {
			File file = new File(path);
			FileInputStream fis = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);
			StringBuffer sb = new StringBuffer();
			String str = "";
			while ((str = br.readLine()) != null) {
				if (str.contains("at android."))
					log(str, 2);
				sb.append(str);

			}
			sb.deleteCharAt(sb.length() - 1);
			log = sb.toString().split("System appears to have ")[1];
		} catch (Exception e) {

		}
		return log;
	}

	@Override
	protected String getAppType() {

		// TODO Auto-generated method stub
		return null;
	}
}

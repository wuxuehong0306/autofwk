package tool;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class WindowsInfoUtil {

	private Sigar sigar;
	double cpuUsed;
	double memUsed;

	public WindowsInfoUtil() {

		System.setProperty("java.library.path", "C:\\");
		sigar = new Sigar();

	}

	public double getMem() {

		try {
			Mem mem = sigar.getMem();
			long memUsed = mem.getUsed() / 1024L / 1024;
			this.memUsed = (double) memUsed;
			// System.out.println("内存占用: " + this.memUsed + "M");

		} catch (SigarException e) {
		}
		return memUsed;
	}

	public double getCpu() {

		try {

			CpuPerc perc = sigar.getCpuPerc();
			double cpuUsed = perc.getCombined();
			this.cpuUsed = Double.parseDouble(String.format("%.2f", cpuUsed * 100));
			// System.out.println("CPU占用: " + this.cpuUsed + "%");
		} catch (SigarException e) {
		}
		return cpuUsed;
	}

	public void compareMem(double startMem, double endMem) {

		boolean validate = (endMem - startMem) < 100;
		if (!validate)
			throw new RuntimeException("内存溢出5%");
	}

	public void compareCPU(double startCpu, double endCpu) {

		boolean validate = (endCpu - startCpu) < 5;
		if (!validate)
			throw new RuntimeException("CPU占用提高5%");
	}
}
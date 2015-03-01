package edu.isi.wubble.util;

public class WubbleMath {

	public static double entropy(double[] list) {
		double total = 0;
		for (double p : list) {
			total += (p * (Math.log(p) / Math.log(2)));
		}
		return -total;
	}
}
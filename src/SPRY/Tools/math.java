package SPRY.Tools;

public class math {
	public static double commonDivisor(double n, double m) {
		while (n % m != 0) {
			double temp = n % m;
			n = m;
			m = temp;
		}
		return m;
	}

	public static double commonMultiple(double n, double m) {
		return n * m / commonDivisor(n, m);
	}
	
	public static double commonMultiple(double[] a) {
		double value = a[0];
		for (int i = 1; i < a.length; i++) {
			value = commonMultiple(value, a[i]);
		}
		return value;
	}
}

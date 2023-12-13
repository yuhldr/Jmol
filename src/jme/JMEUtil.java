package jme;

import java.awt.FontMetrics;
import java.lang.reflect.Array;
import java.util.Calendar;
import java.util.StringTokenizer;

import javajs.J2SIgnoreImport;

@J2SIgnoreImport({Calendar.class})
public abstract class JMEUtil {

	public static final int ALIGN_LEFT = 0;
	public static final int ALIGN_CENTER = 1;
	public static final int ALIGN_RIGHT = 2;

	public static int[] growArray(int[] array, int newSize) {
		int newArray[] = createArray(newSize);
		System.arraycopy(array, 0, newArray, 0, array.length);

		return newArray;
	}

	/* shallow copy of the array */
	public static int[] copyArray(int[] array) {
		int copy[] = new int[array.length];
		System.arraycopy(array, 0, copy, 0, array.length);

		return copy;

	}

	public static <T> T[] growArray(T[] array, int newSize) {
		assert (newSize >= array.length);
		// the new array is made of null
		// T newArray[] = (T[])new Object[newSize]; //Cannot create a generic array of T
		// . Java is a crappy programming language
		T[] newArray = copyOf(array, newSize);
		// System.arraycopy(array,0,newArray,0,array.length);

		return newArray;
	}
// Cloning

	/**
	 * Copies the specified array, truncating or padding with nulls (if necessary)
	 * so the copy has the specified length. For all indices that are valid in both
	 * the original array and the copy, the two arrays will contain identical
	 * values. For any indices that are valid in the copy but not the original, the
	 * copy will contain <tt>null</tt>. Such indices will exist if and only if the
	 * specified length is greater than that of the original array. The resulting
	 * array is of exactly the same class as the original array.
	 *
	 * @param           <T> the class of the objects in the array
	 * @param original  the array to be copied
	 * @param newLength the length of the copy to be returned
	 * @return a copy of the original array, truncated or padded with nulls to
	 *         obtain the specified length
	 * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	 * @throws NullPointerException       if <tt>original</tt> is null
	 * @since 1.6
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] copyOf(T[] original, int newLength) {
		return (T[]) copyOf(original, newLength, original.getClass());
	}

	/**
	 * Copies the specified array, truncating or padding with nulls (if necessary)
	 * so the copy has the specified length. For all indices that are valid in both
	 * the original array and the copy, the two arrays will contain identical
	 * values. For any indices that are valid in the copy but not the original, the
	 * copy will contain <tt>null</tt>. Such indices will exist if and only if the
	 * specified length is greater than that of the original array. The resulting
	 * array is of the class <tt>newType</tt>.
	 *
	 * @param           <U> the class of the objects in the original array
	 * @param           <T> the class of the objects in the returned array
	 * @param original  the array to be copied
	 * @param newLength the length of the copy to be returned
	 * @param newType   the class of the copy to be returned
	 * @return a copy of the original array, truncated or padded with nulls to
	 *         obtain the specified length
	 * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	 * @throws NullPointerException       if <tt>original</tt> is null
	 * @throws ArrayStoreException        if an element copied from
	 *                                    <tt>original</tt> is not of a runtime type
	 *                                    that can be stored in an array of class
	 *                                    <tt>newType</tt>
	 * @since 1.6
	 */
	public static <T, U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
		@SuppressWarnings("unchecked")
		T[] copy = ((Object) newType == (Object) Object[].class) ? (T[]) new Object[newLength]
				: (T[]) Array.newInstance(newType.getComponentType(), newLength);
		System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
		return copy;
	}

	public static String[] growArray(String[] array, int newSize) {
		String newArray[] = createSArray(newSize);
		System.arraycopy(array, 0, newArray, 0, array.length);

		return newArray;
	}

	public static double[] growArray(double[] array, int newSize) {
		double newArray[] = createDArray(newSize);
		System.arraycopy(array, 0, newArray, 0, array.length);

		return newArray;
	}

	public static int[][] growArray(int[][] array, int newSize) {
		int secondarySize = array[0].length;
		int newArray[][] = createArray(newSize, secondarySize); // new int[newSize][secondarySize];
		System.arraycopy(array, 0, newArray, 0, array.length);

		return newArray;
	}

	public static boolean equals(int[] a1, int[] a2) {
		if (a1.length == a2.length) {
			for (int i = 0; i < a1.length; i++) {
				if (a1[i] != a2[i]) {
					return false;
				}
			}
			return true;
		}

		return false;
	}

	public static int[] intersection(int[] array1, int[] array2) {
		int common[] = new int[0];
		for (int v1 : array1) {
			if (contains(array2, v1)) {
				common = growArray(common, common.length + 1);
				common[common.length - 1] = v1;
			}
		}

		return common;

	}

	public static boolean contains(int[] array, int v) {
		for (int each : array) {
			if (each == v) {
				return true;
			}
		}

		return false;
	}

	public static <T> void swap(T[] array, int i, int j) {
		T temp = array[j];
		array[j] = array[i];
		array[i] = temp;

	}

	/* shallow copy of the array */
	public static int[] copyArray(int[] array, int n) {
		int copy[] = new int[array.length];
		System.arraycopy(array, 0, copy, 0, n);

		return copy;

	}

	/* shallow copy of the array */
	public static String[] copyArray(String[] array) {
		String copy[] = new String[array.length];
		System.arraycopy(array, 0, copy, 0, array.length);

		return copy;

	}

	/* shallow copy of the array */
	public static double[] copyArray(double[] array) {
		double copy[] = new double[array.length];
		System.arraycopy(array, 0, copy, 0, array.length);

		return copy;

	}

	public static int[] createArray(int size) {
		return new int[size];
	}

	public static String[] createSArray(int size) {
		return new String[size];
	}

	public static double[] createDArray(int size) {
		return new double[size];
	}

	public static long[] createLArray(int size) {
		return new long[size];
	}

	public static boolean[] createBArray(int size) {
		return new boolean[size];
	}

	public static int[][] createArray(int size1, int size2) {
		return new int[size1][size2];
	}

	/**
	 * Check if the applet is showing in highDPI or not. In a web browser, this can
	 * change with the zoom factor, thus this function should be called before each
	 * drawing
	 * 
	 * @return
	 */

	public static boolean isHighDPI() {
		return false;
	}

	/**
	 * Do nothing , support for JSME code splitting.
	 */
	public static class GWT {

		public static boolean isScript() {
			// TODO Auto-generated method stub
			return false;
		}

		public static void log(String string) {
			// TODO Auto-generated method stub

		}

	}

	/**
	 * Used by JSME for code splitting
	 * 
	 * 
	 * 
	 */
	/**
	 * A callback meant to be used by
	 */
	public interface RunAsyncCallback {
		/**
		 * Called when, for some reason, the necessary code cannot be loaded. For
		 * example, the web browser might no longer have network access.
		 */
		public void onFailure(Throwable reason);

		/**
		 * Called once the necessary code for it has been loaded.
		 */
		public void onSuccess();
	}

	public static abstract class JSME_RunAsyncCallback implements RunAsyncCallback {

		@Override
		public void onFailure(Throwable reason) {
			// Window.alert("Loading JS code failed");

		}
	}

	public interface RunWhenDataReadyCallback {

		/**
		 * Called when, for some reason, the necessary code cannot be loaded. For
		 * example, the web browser might no longer have network access.
		 */
		void onFailure(Throwable reason);

		/**
		 * Called once the necessary code for it has been loaded.
		 */
		void onSuccess(String data);

		void onWarning(String message);

	}

	public static void runAsync(RunAsyncCallback runAsyncCallback) {
	
		runAsyncCallback.onSuccess();
	
	}

	// ----------------------------------------------------------------------------
	public static long[] generatePrimes(int n) {
		/*
	Prime Number Generator
	code by Mark Chamness (modified slightly by Peter Ertl)
	This subroutine calculates first n prime numbers (starting with 2)
	It stores the first 100 primes it generates. Then it evaluates the rest
	based on those up to prime[100] squared
		 */
		int npn;
		long[] pn = createLArray(n+2);
		int[] prime = createArray(100);
		int test=5, index=0;
		int num=0;
		boolean check=true;
		prime[0]=3;
		pn[1] = 2; pn[2] = 3; npn=2;
		if (n<3) return pn; // very rear case
		while(test<(prime[num]*prime[num])) {
			index=0; check=true;
			while(check==true && index<=num && test>=(prime[index]*prime[index])) {
				if(test%prime[index] == 0)  check=false;
				else index++;
			}
			if(check==true) {
				pn[++npn] = test;
				if (npn >= n) return pn;
				if(num<(prime.length-1)) {
					num++;
					prime[num]=test;
				}
			}
			test+=2;
		}
		System.err.println("ERROR - Prime Number generator failed !");
		return pn;
	}

  public static String nextData(StringTokenizer st, String separator) {
    // dost tricky, musi uvazit aj bez \n aj sa \n |\n za sebou ...
    // musi osetrit aj lines with zero length (2 x po sebe \n alebo |)
    while (st.hasMoreTokens()) {
      String s = st.nextToken();
      if (s.equals(separator))
        return " ";
      if (!st.nextToken().equals(separator)) { // ukoncujuci separator
        System.err.println("mol file line separator problem!");
      }
      // musi vyhodit z konca pripadne | (napr v appletviewer)
      while (true) {
        char c = s.charAt(s.length() - 1);
        if (c == '|' || c == '\n' || c == '\r') {
          s = s.substring(0, s.length() - 1);
          if (s.length() == 0)
            return " "; // v textboox \r\n ??
        } else {
          break;
        }
      }
      return s;
    }
    return null;
  }

  // ----------------------------------------------------------------------------
  public static String findLineSeparator(String molFile) {
    //if (c=='\t' || c=='\n' || c=='\r' || c=='\f' || c=='|') 
    //  molFile = " " + molFile;
    //StringTokenizer st = new StringTokenizer(molFile,"\t\n\r\f|",true);
    // osetrene aj separator 2x tesne za sebou  
    StringTokenizer st = new StringTokenizer(molFile, "\n", true);
    if (st.countTokens() > 4)
      return "\n";

    st = new StringTokenizer(molFile, "|", true);
    if (st.countTokens() > 4)
      return "|";
    System.err.println("Cannot process mol file, use | as line separator !");
    return null;
  }

	public static double squareEuclideanDist(double x1, double y1, double x2, double y2) {
		double dx = x2-x1;
		double dy = y2-y1;
	
		return dx*dx+dy*dy; //equal to dot product
	}

	public static double dotProduct(double x1, double y1, double x2, double y2) {
		return x1*x2 + y1*y2;
	}

	/**
	 * Compute the height of a triangle knowing the length of each side.
	 * Use Heron's formula.
	 * @param a
	 * @param b -base of the triangle
	 * @param c
	 * @return height
	 */
	public static double triangleHeight(double a, double b, double c) {
		double s = (a+b+c)/2; //half the perimeter of the triangle
		double area = Math.sqrt( s * (s-a) * (s-b) * (s-c));
		double h = 0;
	
		if(b != 0) {
			h = area / b * 2;
		}
	
		return h;
	
	
	}

	// ----------------------------------------------------------------------------
	public static int compareAngles(double sina, double cosa, double sinb, double cosb) {
		// returns 1 if a < b (clockwise) -1 a > b, 0 ak a = b
		int qa = 0, qb = 0; // kvadrant
		if (sina >= 0. && cosa >= 0.)
			qa = 1;
		else if (sina >= 0. && cosa < 0.)
			qa = 2;
		else if (sina < 0. && cosa < 0.)
			qa = 3;
		else if (sina < 0. && cosa >= 0.)
			qa = 4;
		if (sinb >= 0. && cosb >= 0.)
			qb = 1;
		else if (sinb >= 0. && cosb < 0.)
			qb = 2;
		else if (sinb < 0. && cosb < 0.)
			qb = 3;
		else if (sinb < 0. && cosb >= 0.)
			qb = 4;
		if (qa < qb)
			return 1;
		else if (qa > qb)
			return -1;
		// su v rovnakom kvadrante
		switch (qa) {
		case 1:
		case 4:
			return (sina < sinb ? 1 : -1);
		case 2:
		case 3:
			return (sina > sinb ? 1 : -1);
		}
		System.err.println("stereowarning #31");
		return 0;
	}

	// ----------------------------------------------------------------------------
	public static void stereoTransformation(int t[], int ref[]) {
		// System.out.println(t[0]+" "+t[1]+" "+t[2]+" "+t[3]+" --- ");
		int d = 0;
		if (ref[0] == t[1]) // 0,1 2,3
		{
			d = t[0];
			t[0] = t[1];
			t[1] = d;
			d = t[2];
			t[2] = t[3];
			t[3] = d;
		} else if (ref[0] == t[2]) // 0,2 1,3
		{
			d = t[2];
			t[2] = t[0];
			t[0] = d;
			d = t[1];
			t[1] = t[3];
			t[3] = d;
		} else if (ref[0] == t[3]) // 0,3 1,2
		{
			d = t[3];
			t[3] = t[0];
			t[0] = d;
			d = t[1];
			t[1] = t[2];
			t[2] = d;
		}
	
		if (ref[1] == t[2]) // 1,2 2,3
		{
			d = t[1];
			t[1] = t[2];
			t[2] = d;
			d = t[2];
			t[2] = t[3];
			t[3] = d;
		} else if (ref[1] == t[3]) // 1,3 2,3
		{
			d = t[1];
			t[1] = t[3];
			t[3] = d;
			d = t[2];
			t[2] = t[3];
			t[3] = d;
		}
	}

	// ----------------------------------------------------------------------------
	/**
	 * Return the JME atoming number associated to the given symbol
	 * 
	 * @param s
	 * @return zLabel index for this symbol
	 */
	public static int checkAtomicSymbol(String s) {
	
		// BB simplification
		for (int an = 1; an < JME.zlabel.length; an++) {
			if (s.equals(JME.zlabel[an]))
				return an;
		}
		// there is a problem for R groups beyond R9: it will be interpreted as AN_X
		// see also protected int mapActionToAtomNumber(int action, int notFound) {
		return JME.AN_X;
	}
	// ----------------------------------------------------------------------------

	// ----------------------------------------------------------------------------
	/**
	 * See CTFile -- this line is NOT optional. It is critical in showing whether we
	 * have a 2D or 3D MOL file.
	 * 
	 * @param version
	 * @return SDF header line 2 with no \n
	 */
	public static String getSDFDateLine(String version) {
		String mol = (version + "         ").substring(0, 10);
		int cMM, cDD, cYYYY, cHH, cmm;
		/**
		 * for convenience only, no need to invoke Calendar for this simple task.
		 * 
		 * @j2sNative
		 * 
		 * 			var c = new Date(); cMM = c.getMonth(); cDD = c.getDate(); cYYYY =
		 *            c.getFullYear(); cHH = c.getHours(); cmm = c.getMinutes();
		 */
		{
			Calendar c = Calendar.getInstance();
			cMM = c.get(Calendar.MONTH);
			cDD = c.get(Calendar.DAY_OF_MONTH);
			cYYYY = c.get(Calendar.YEAR);
			cHH = c.get(Calendar.HOUR_OF_DAY);
			cmm = c.get(Calendar.MINUTE);
		}
		mol += rightJustify("00", "" + (1 + cMM));
		mol += rightJustify("00", "" + cDD);
		mol += ("" + cYYYY).substring(2, 4);
		mol += rightJustify("00", "" + cHH);
		mol += rightJustify("00", "" + cmm);
		mol += "2D 1   1.00000     0.00000     0";
		// This line has the format:
		// IIPPPPPPPPMMDDYYHHmmddSSssssssssssEEEEEEEEEEEERRRRRR
		// A2<--A8--><---A10-->A2I2<--F10.5-><---F12.5--><-I6->
		return mol;
	}

	// ----------------------------------------------------------------------------
	/**
	 * right-justify using spaces
	 * 
	 * @param number with no more than len digits
	 * @param len max 8
	 * @return right-justified number or ?
	 */
	public static String iformat(int number, int len) {
		return rightJustify("        ".substring(0, len), "" + number);
	}

	public static String rightJustify(String s1, String s2) {
		int n = s1.length() - s2.length();
		return (n == 0 ? s2 : n > 0 ? s1.substring(0, n) + s2 : s1.substring(0, s1.length() - 1) + "?");
	}
	
	/**
	 * Truncate to dec digits after the decimal place and left-pad to length len.
	 * 
	 * @param number
	 * @param len guaranteed length of string to return
	 * @param dec the number of decimal places or 0 for integer rounding down
	 * @return the formatted number or right-justified "?" 
	 */
	public static String fformat(double number, int len, int dec) {
		// este pridat zmensovanie dec, ked dlzka nestaci
		if (dec == 0)
			return iformat((int) number, len);
		if (Math.abs(number) < 0.0009)
			number = 0.; // 2012 fix 1.0E-4
		double m = Math.pow(10, dec);
		number = (int) Math.round(number * m) / m;
		String s = new Double(number).toString(); // this sometimes return 1.0E-4
		int dotpos = s.indexOf('.');
		if (dotpos < 0) {
			s += ".";
			dotpos = s.indexOf('.');
		}
		int slen = s.length();
		for (int i = 1; i <= dec - slen + dotpos + 1; i++)
			s += "0";
		return (len == 0 ? s : rightJustify("        ".substring(0, len), s));
	}

	/**
	 * Provide the ideal height of a string consisting of usual upper case
	 * characters. Purpose: centering of String in the center of a box. Does not
	 * work for $ , y ; and others
	 * 
	 * @param fm 
	 * @return 
	 */
	public static double stringHeight(FontMetrics fm) {
//	  return fm.getHeight();
		return fm.getAscent() - fm.getDescent();
	}

//	static {
//		for (int i = -20; i < 20; i++) {
//			System.out.println(i + " >" + fformat(i/100.,5,2) + "<>" + iformat(i, 2) + "<");
//		}
//		System.out.println("???");
//	}

}

package libsvm;

/**
 * Kernel evaluation
 */
abstract class QMatrix {
	/**
	 * Returns one column from the Q Matrix
	 */
	abstract float[] get_Q(int column, int len);
	abstract double[] get_QD();
	abstract void swap_index(int i, int j);
}

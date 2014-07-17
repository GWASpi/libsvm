package libsvm;

public class svm_model implements java.io.Serializable
{
	/** parameter */
	public svm_parameter param;
	/** number of classes, 2 in regression/One-Class-SVM */
	public int nr_class;
	/** The total number of support vectors */
	public int l;
	/** The support vectors (SV[l]) */
	public svm_node[][] SV;
	/**
	 * The coefficients for support vectors in decision functions.
	 * These are commonly referred to as 'alpha's.
	 * (<code>sv_coef[k-1][l]</code>)
	 */
	public double[][] sv_coef;
	/**
	 * The indices for support vectors.
	 * These are indices of all the SV vectors in the original data-set.
	 * (<code>sv_indices[k-1][l]</code>)
	 */
	public int[][] sv_indices;
	/** The constants in decision functions (<code>rho[k*(k-1)/2]</code>) */
	public double[] rho;
	/** pairwise probability information, first parts */
	public double[] probA;
	/** pairwise probability information, second parts */
	public double[] probB;
	public int[] sv_indices;       // sv_indices[0,...,nSV-1] are values in [1,...,num_traning_data] to indicate SVs in the training set

	// for classification only

	/** label of each class (<code>label[k]</code>) */
	public int[] label;
	/**
	 * The number of support vectors for each class (<code>nSV[k]</code>).
	 * <code>nSV[0] + nSV[1] + ... + nSV[k-1] = l</code>
	 */
	public int[] nSV;
}

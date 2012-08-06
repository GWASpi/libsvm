package libsvm;

public class svm_parameter implements Cloneable,java.io.Serializable
{
	// svm_type
	public static final int C_SVC = 0;
	public static final int NU_SVC = 1;
	public static final int ONE_CLASS = 2;
	public static final int EPSILON_SVR = 3;
	public static final int NU_SVR = 4;

	// kernel_type
	public static final int LINEAR = 0;
	public static final int POLY = 1;
	public static final int RBF = 2;
	public static final int SIGMOID = 3;
	public static final int PRECOMPUTED = 4;

	public int svm_type;
	public int kernel_type;
	/** for poly */
	public int degree;
	/** for poly/RBF/sigmoid */
	public double gamma;
	/** for poly/sigmoid */
	public double coef0;

	// these are for training only
	/** The cache size in MB */
	public double cache_size;
	/** stopping criteria */ // TODO improve this comment
	public double eps;
	/** for C_SVC, EPSILON_SVR and NU_SVR */
	public double C;
	/** for C_SVC */
	public int nr_weight;
	/** for C_SVC */
	public int[] weight_label;
	/** for C_SVC */
	public double[] weight;
	/** for NU_SVC, ONE_CLASS, and NU_SVR */
	public double nu;
	/** for EPSILON_SVR */
	public double p;
	/** use the shrinking heuristics */
	public int shrinking;
	/** do probability estimates */
	public int probability;

	@Override
	public Object clone()
	{
		try
		{
			return super.clone();
		} catch (CloneNotSupportedException e)
		{
			return null;
		}
	}
}

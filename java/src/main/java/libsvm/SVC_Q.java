package libsvm;

/**
 * Q matrices for various formulations
 */
class SVC_Q extends Kernel
{
	private final byte[] y;
	private final Cache cache;
	private final double[] QD;

	SVC_Q(svm_problem prob, svm_parameter param, byte[] y_)
	{
		super(prob.l, prob.x, param);
		y = y_.clone();
		cache = new Cache(prob.l,(long)(param.cache_size*(1<<20)));
		QD = new double[prob.l];
		for(int i=0;i<prob.l;i++)
			QD[i] = kernel_function(i,i);
	}

	@Override
	float[] get_Q(int i, int len)
	{
		float[][] data = new float[1][];
		int start, j;
		if((start = cache.get_data(i,data,len)) < len)
		{
			for(j=start;j<len;j++)
				data[0][j] = (float)(y[i]*y[j]*kernel_function(i,j));
		}
		return data[0];
	}

	@Override
	double[] get_QD()
	{
		return QD;
	}

	@Override
	void swap_index(int i, int j)
	{
		cache.swap_index(i,j);
		super.swap_index(i,j);
		{ // swap(byte, y[i], y[j]);
			byte tmp = y[i];
			y[i] = y[j];
			y[j] = tmp;
		}
		{ // swap(double, QD[i], QD[j]);
			double tmp = QD[i];
			QD[i] = QD[j];
			QD[j] = tmp;
		}
	}
}

package libsvm;

abstract class Kernel extends QMatrix
{
	private svm_node[][] x;
	/**
	 * The sample serial numbers.
	 * <code>x[i][0] with i=0...(len-1)</code>.
	 * This is only used in case of a precomputed kernel.
	 * This is merely a cache, which brings us a huge speedup,
	 * because we spare ourselves the repeated float2int conversion,
	 * and we enhance array access locality.
	 */
	private int[] sampleSerialNumbers;
	private final double[] x_square;

	// svm_parameter
	private final int kernel_type;
	private final int degree;
	private final double gamma;
	private final double coef0;

	@Override
	abstract float[] get_Q(int column, int len);
	@Override
	abstract double[] get_QD();

	@Override
	void swap_index(int i, int j)
	{
		{ // swap(svm_node[], x[i], x[j]);
			svm_node[] tmp = x[i];
			x[i] = x[j];
			x[j] = tmp;
			if (kernel_type == svm_parameter.PRECOMPUTED) {
				// also swap the cached index
				final int tmpIndex = sampleSerialNumbers[i];
				sampleSerialNumbers[i] = sampleSerialNumbers[j];
				sampleSerialNumbers[j] = tmpIndex;
			}
		}
		if(x_square != null)
		{ // swap(double, x_square[i], x_square[j]);
			double tmp = x_square[i];
			x_square[i] = x_square[j];
			x_square[j] = tmp;
		}
	}

	double kernel_function(int i, int j)
	{
		switch(kernel_type)
		{
			case svm_parameter.LINEAR:
				return dot(x[i],x[j]);
			case svm_parameter.POLY:
				return Math.pow(gamma*dot(x[i],x[j])+coef0,degree);
			case svm_parameter.RBF:
				return Math.exp(-gamma*(x_square[i]+x_square[j]-2*dot(x[i],x[j])));
			case svm_parameter.SIGMOID:
				return Math.tanh(gamma*dot(x[i],x[j])+coef0);
			case svm_parameter.PRECOMPUTED:
				return x[i][sampleSerialNumbers[j]].value;
			default:
				return 0; // java
		}
	}

	/**
	 * Prepares to calculate the l*l kernel matrix
	 */
	Kernel(int l, svm_node[][] x_, svm_parameter param)
	{
		this.kernel_type = param.kernel_type;
		this.degree = param.degree;
		this.gamma = param.gamma;
		this.coef0 = param.coef0;

		x = x_.clone();

		// extract the sample serial numbers from x
		if(kernel_type == svm_parameter.PRECOMPUTED)
		{
			sampleSerialNumbers = new int[x.length];
			for (int i = 0; i < x.length; i++) {
				sampleSerialNumbers[i] = (int) x[i][0].value;
			}
		}

		if(kernel_type == svm_parameter.RBF)
		{
			x_square = new double[l];
			for(int i=0;i<l;i++)
				x_square[i] = dot(x[i],x[i]);
		}
		else x_square = null;
	}

	static double dot(svm_node[] x, svm_node[] y)
	{
		double sum = 0;
		int xlen = x.length;
		int ylen = y.length;
		int i = 0;
		int j = 0;
		while(i < xlen && j < ylen)
		{
			if(x[i].index == y[j].index)
				sum += x[i++].value * y[j++].value;
			else
			{
				if(x[i].index > y[j].index)
					++j;
				else
					++i;
			}
		}
		return sum;
	}

	/**
	 * For doing single kernel evaluation
	 */
	static double k_function(svm_node[] x, svm_node[] y,
					svm_parameter param)
	{
		switch(param.kernel_type)
		{
			case svm_parameter.LINEAR:
				return dot(x,y);
			case svm_parameter.POLY:
				return Math.pow(param.gamma*dot(x,y)+param.coef0,param.degree);
			case svm_parameter.RBF:
			{
				double sum = 0;
				int xlen = x.length;
				int ylen = y.length;
				int i = 0;
				int j = 0;
				while(i < xlen && j < ylen)
				{
					if(x[i].index == y[j].index)
					{
						double d = x[i++].value - y[j++].value;
						sum += d*d;
					}
					else if(x[i].index > y[j].index)
					{
						sum += y[j].value * y[j].value;
						++j;
					}
					else
					{
						sum += x[i].value * x[i].value;
						++i;
					}
				}

				while(i < xlen)
				{
					sum += x[i].value * x[i].value;
					++i;
				}

				while(j < ylen)
				{
					sum += y[j].value * y[j].value;
					++j;
				}

				return Math.exp(-param.gamma*sum);
			}
			case svm_parameter.SIGMOID:
				return Math.tanh(param.gamma*dot(x,y)+param.coef0);
			case svm_parameter.PRECOMPUTED:
				return	x[(int)(y[0].value)].value;
			default:
				return 0;	// java
		}
	}
}

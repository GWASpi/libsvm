package libsvm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Q matrices for various formulations
 */
class SVC_Q extends Kernel
{
	private final byte[] y;
	private final Cache cache;
	private final double[] QD;

	private class QDCalculator implements Runnable
	{
		private final int i;

		QDCalculator(int i)
		{
			this.i = i;
		}

		@Override
		public void run() {
			QD[i] = kernel_function(i,i);
		}
	}

	SVC_Q(svm_problem prob, svm_parameter param, byte[] y_)
	{
		super(prob.l, prob.x, param);


		{
			File BASE_DIR = new File(System.getProperty("user.home"), "Projects/GWASpi/var/data/marius/example/extra");
			String encoderString = "ANY";
			File generatedLibSvmKernelFile = new File(BASE_DIR, "intermediate_" + encoderString + "_after.txt");
			System.err.println("\nXXX libSVM XXX ### writing generated libSVM PRECOMPUTED kernel file to " + generatedLibSvmKernelFile + " ...");
			try {
				OutputStreamWriter kernOut = new FileWriter(generatedLibSvmKernelFile);
				for (int si = 0; si < prob.x.length; si++) {
					kernOut.write(String.valueOf(prob.y[si]));
					for (int mi = 0; mi < prob.x[si].length; mi++) {
						kernOut.write(' ');
						kernOut.write(String.valueOf(prob.x[si][mi].index));
						kernOut.write(':');
						kernOut.write(String.valueOf(prob.x[si][mi].value));
					}
					kernOut.write('\n');
				}
				kernOut.close();
			} catch (FileNotFoundException ex) {
				throw new RuntimeException(ex);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			System.err.println("XXX libSVM XXX ### done writing kernel file.");
		}


		ExecutorService pool = Executors.newFixedThreadPool(8);
		y = y_.clone();
		cache = new Cache(prob.l,(long)(param.cache_size*(1<<20)));
		QD = new double[prob.l];
		for(int i=0;i<prob.l;i++)
			pool.execute(new QDCalculator(i));
		pool.shutdown();
		try {
			pool.awaitTermination(990, TimeUnit.DAYS);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			System.exit(66);
		}
	}

	private class DataCalculator implements Runnable
	{
		private float[][] data;
		private final int i;
		private final int j;

		DataCalculator(float[][] data, int i, int j)
		{
			this.data = data;
			this.i = i;
			this.j = j;
		}

		@Override
		public void run() {
			data[0][j] = (float)(y[i]*y[j]*kernel_function(i,j));
		}
	}

	@Override
	float[] get_Q(int i, int len)
	{
		float[][] data = new float[1][];
		int start, j;
		ExecutorService pool = Executors.newFixedThreadPool(8);
		if((start = cache.get_data(i,data,len)) < len)
		{
			for(j=start;j<len;j++)
				pool.execute(new DataCalculator(data, i, j));
		}
		pool.shutdown();
		try {
			pool.awaitTermination(99, TimeUnit.DAYS);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			System.exit(66);
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

class svm_train
{
	private static final Logger LOG = Logger.getLogger(svm_train.class.getName());

	/** set by parse_command_line */
	private svm_parameter param;
	/** set by read_problem */
	private svm_problem prob;
	private svm_model model;
	/** set by parse_command_line */
	private String input_file_name;
	/** set by parse_command_line */
	private String model_file_name;
	private String error_msg;
	private int cross_validation;
	private int nr_fold;

	private static void logHelp()
	{
		LOG.info("Usage: svm_train [options] training_set_file [model_file]");
		LOG.info("");
		LOG.info("Trains a model on training data, which may be used with svm-predict.");
		LOG.info("");
		LOG.info("Options:");
		LOG.info("-s svm_type : set type of SVM (default 0)");
		LOG.info("	0 -- C-SVC       (multi-class classification)");
		LOG.info("	1 -- nu-SVC      (multi-class classification)");
		LOG.info("	2 -- one-class SVM");
		LOG.info("	3 -- epsilon-SVR (regression)");
		LOG.info("	4 -- nu-SVR      (regression");
		LOG.info("-t kernel_type : set type of kernel function (default 2)");
		LOG.info("	0 -- linear: u'*v");
		LOG.info("	1 -- polynomial: (gamma*u'*v + coef0)^degree");
		LOG.info("	2 -- radial basis function: exp(-gamma*|u-v|^2)");
		LOG.info("	3 -- sigmoid: tanh(gamma*u'*v + coef0)");
		LOG.info("	4 -- precomputed kernel (kernel values in training_set_file)");
		LOG.info("-d degree : set degree in kernel function (default 3)");
		LOG.info("-g gamma : set gamma in kernel function (default 1/num_features)");
		LOG.info("-r coef0 : set coef0 in kernel function (default 0)");
		LOG.info("-c cost : set the parameter C of C-SVC, epsilon-SVR, and nu-SVR (default 1)");
		LOG.info("-n nu : set the parameter nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)");
		LOG.info("-p epsilon : set the epsilon in loss function of epsilon-SVR (default 0.1)");
		LOG.info("-m cachesize : set cache memory size in MB (default 100)");
		LOG.info("-e epsilon : set tolerance of termination criterion (default 0.001)");
		LOG.info("-h shrinking : whether to use the shrinking heuristics, 0 or 1 (default 1)");
		LOG.info("-b probability_estimates : whether to train a SVC or SVR model for probability estimates, 0 or 1 (default 0)");
		LOG.info("-wi weight : set the parameter C of class i to weight*C, for C-SVC (default 1)");
		LOG.info("-v n : n-fold cross validation mode");
		LOG.info("-q : quiet mode (no outputs)");
		LOG.info("--help : display this help and exit");
		LOG.info("--version : output version information and exit");
	}

	private void do_cross_validation()
	{
		int i;
		int total_correct = 0;
		double total_error = 0;
		double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
		double[] target = new double[prob.l];

		svm.svm_cross_validation(prob,param,nr_fold,target);
		if(param.svm_type == svm_parameter.EPSILON_SVR ||
		   param.svm_type == svm_parameter.NU_SVR)
		{
			for(i=0;i<prob.l;i++)
			{
				double y = prob.y[i];
				double v = target[i];
				total_error += (v-y)*(v-y);
				sumv += v;
				sumy += y;
				sumvv += v*v;
				sumyy += y*y;
				sumvy += v*y;
			}
			LOG.log(Level.INFO, "Cross Validation Mean squared error = {0}", total_error/prob.l);
			LOG.log(Level.INFO, "Cross Validation Squared correlation coefficient = {0}",
					((prob.l*sumvy-sumv*sumy)*(prob.l*sumvy-sumv*sumy))
					/ ((prob.l*sumvv-sumv*sumv)*(prob.l*sumyy-sumy*sumy)));
		}
		else
		{
			for(i=0;i<prob.l;i++)
				if(target[i] == prob.y[i])
					++total_correct;
			LOG.log(Level.INFO, "Cross Validation Accuracy = {0}%", 100.0*total_correct/prob.l);
		}
	}

	public void run(String argv[]) throws IOException
	{
		long begin = System.currentTimeMillis();
		parse_command_line(argv);
		long end = System.currentTimeMillis();
		LOG.log(Level.INFO, "part: parse command line: {0}ms", (end - begin));

		begin = System.currentTimeMillis();
		read_problem();
		error_msg = svm.svm_check_parameter(prob,param);
		end = System.currentTimeMillis();
		LOG.log(Level.INFO, "part: read: {0}ms", (end - begin));
//System.exit(77);

		if(error_msg != null)
		{
			LOG.severe(error_msg);
			System.exit(1);
		}

		if(cross_validation != 0)
		{
			do_cross_validation();
		}
		else
		{
//			for (int i = 0; i < 5; i++) {
				begin = System.currentTimeMillis();
				model = svm.svm_train(prob,param);
				end = System.currentTimeMillis();
				LOG.log(Level.INFO, "part: train: {0}ms", (end - begin));
//			}

			begin = System.currentTimeMillis();
			svm.svm_save_model(model_file_name,model);
			end = System.currentTimeMillis();
			LOG.log(Level.INFO, "part: write model: {0}ms", (end - begin));
		}
	}

	private static class BasicFormatter extends Formatter {

		private final SimpleFormatter messageFormatter = new SimpleFormatter();
		private final String newLine = System.getProperty("line.separator", "\n");

		@Override
		public String format(LogRecord record) {

			String messageStr = messageFormatter.formatMessage(record) + newLine;

			if (record.getLevel().intValue() > Level.INFO.intValue())
			{
				messageStr = record.getLevel().getName() + " " + messageStr;
			}

			if (record.getThrown() != null)
			{
				StringWriter exStr = new StringWriter();
				record.getThrown().printStackTrace(new PrintWriter(exStr));
				messageStr = messageStr + exStr.toString();
			}

			return messageStr;
		}
	}

	public static void setupLogging()
	{
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();
		ConsoleHandler consoleHandler = new ConsoleHandler();
		Formatter basicFormatter = new BasicFormatter();
		consoleHandler.setFormatter(basicFormatter);
		Logger.getLogger("").addHandler(consoleHandler);
	}

	public static void main(String argv[]) throws IOException
	{
		setupLogging();

		svm_train t = new svm_train();

		String[] myArgs = argv;

//		String workDir = "/home/robin/Projects/Gwaspi/var/marius/libSvmTest";
//		String[] myArgs = new String[] {
//			"-t", "4",
//			"-c", "0.01",
//			workDir + "/kernel.txt",
//			workDir + "/model.txt"
//		};
//		// ./svm-train -t 4 -c 0.01 /home/robin/Projects/Gwaspi/var/marius/libSvmTest/kernel.txt model.txt

//		String workDir = "/home/robin/Projects/LibSVM/var/data";
//		String[] myArgs = new String[2];
//		myArgs[0] = workDir + "/gisette_scale.t";
//		myArgs[1] = workDir + "/gisette_scale.t-model.txt";

		t.run(myArgs);
	}

	private static double atof(String s)
	{
		double d = Double.valueOf(s).doubleValue();
		if (Double.isNaN(d) || Double.isInfinite(d))
		{
			LOG.severe("NaN or Infinity in input");
			System.exit(1);
		}
		return(d);
	}

	private static int atoi(String s)
	{
		return Integer.parseInt(s);
	}

	private void parse_command_line(String argv[])
	{
		int i = 0;

		param = new svm_parameter();
		// default values
		param.svm_type = svm_parameter.C_SVC;
		param.kernel_type = svm_parameter.RBF;
		param.degree = 3;
		param.gamma = 0;	// 1/num_features
		param.coef0 = 0;
		param.nu = 0.5;
		param.cache_size = 100;
		param.C = 1;
		param.eps = 1e-3;
		param.p = 0.1;
		param.shrinking = 1;
		param.probability = 0;
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];
		cross_validation = 0;

		// parse options
		try
		{
			for(i=0;i<argv.length;i++)
			{
				if(argv[i].charAt(0) != '-') break;
				i++;
				if ((i >= argv.length) && (argv[i-1].charAt(1) != '-'))
					throw new IllegalArgumentException("Missing argument for option " + argv[i]);
				switch(argv[i-1].charAt(1))
				{
					case 's':
						param.svm_type = atoi(argv[i]);
						break;
					case 't':
						param.kernel_type = atoi(argv[i]);
						break;
					case 'd':
						param.degree = atoi(argv[i]);
						break;
					case 'g':
						param.gamma = atof(argv[i]);
						break;
					case 'r':
						param.coef0 = atof(argv[i]);
						break;
					case 'n':
						param.nu = atof(argv[i]);
						break;
					case 'm':
						param.cache_size = atof(argv[i]);
						break;
					case 'c':
						param.C = atof(argv[i]);
						break;
					case 'e':
						param.eps = atof(argv[i]);
						break;
					case 'p':
						param.p = atof(argv[i]);
						break;
					case 'h':
						param.shrinking = atoi(argv[i]);
						break;
					case 'b':
						param.probability = atoi(argv[i]);
						break;
					case 'q':
						svm.svm_setLogLevel(Level.OFF);
						i--;
						break;
					case 'v':
						cross_validation = 1;
						nr_fold = atoi(argv[i]);
						if(nr_fold < 2)
							throw new IllegalArgumentException("n-fold cross validation: n must >= 2");
						break;
					case 'w':
						++param.nr_weight;
						{
							int[] old = param.weight_label;
							param.weight_label = new int[param.nr_weight];
							System.arraycopy(old,0,param.weight_label,0,param.nr_weight-1);
						}

						{
							double[] old = param.weight;
							param.weight = new double[param.nr_weight];
							System.arraycopy(old,0,param.weight,0,param.nr_weight-1);
						}

						param.weight_label[param.nr_weight-1] = atoi(argv[i-1].substring(2));
						param.weight[param.nr_weight-1] = atof(argv[i]);
						break;
					case '-':
						// long option
						String longOptName = argv[i-1].substring(2);
						if (longOptName.equals("help"))
						{
							logHelp();
							System.exit(0);
						}
						else if (longOptName.equals("version"))
						{
							LOG.log(Level.INFO, "{0} {1} {2}", new Object[] {"LibSVM", "svm-train", svm.getVersion()});
							System.exit(0);
						}
						else
						{
							throw new IllegalArgumentException("Unknown long option: " + argv[i-1]);
						}
						break;
					default:
						throw new IllegalArgumentException("Unknown option: " + argv[i-1]);
				}
			}

			if(i>=argv.length)
				throw new IllegalArgumentException("No model file-name given");
		}
		catch (IllegalArgumentException ex)
		{
			LOG.log(Level.SEVERE, "Failed parsing arguments", ex);
			logHelp();
			System.exit(1);
		}

		// determine filenames

		input_file_name = argv[i];

		if(i<argv.length-1)
			model_file_name = argv[i+1];
		else
		{
			int p = argv[i].lastIndexOf('/');
			++p;	// whew...
			model_file_name = argv[i].substring(p)+".model";
		}
	}

	/**
	 * Reads a problem from file (in SVM-light format).
	 */
	private void read_problem() throws IOException
	{
		BufferedReader fp = new BufferedReader(new FileReader(input_file_name));
		List<Double> vy = new LinkedList<Double>();
		List<svm_node[]> vx = new LinkedList<svm_node[]>();
		int max_index = 0;

		long begin = System.currentTimeMillis();
		try
		{
			while(true)
			{
				String line = fp.readLine();
				if(line == null) break;

				StringTokenizer st = new StringTokenizer(line," \t\f:");

				vy.add(atof(st.nextToken()));
				int m = st.countTokens()/2;
				svm_node[] x = new svm_node[m];
				for(int j=0;j<m;j++)
				{
					x[j] = new svm_node();
					x[j].index = atoi(st.nextToken());
					x[j].value = atof(st.nextToken());
				}
				if(m>0) max_index = Math.max(max_index, x[m-1].index);
				vx.add(x);

//				String[] parts = line.split("[ \t\f:]+");
//				int i = 0;
//				vy.add(atof(parts[i++]));
//				int m = parts.length / 2;
//				svm_node[] x = new svm_node[m];
//				for(int j=0;j<m;j++)
//				{
//					x[j] = new svm_node();
//					x[j].index = atoi(parts[i++]);
//					x[j].value = atof(parts[i++]);
//				}
//				if(m>0) max_index = Math.max(max_index, x[m-1].index);
//				vx.add(x);

//				String[] parts = line.split("[ \t\f:]+");
//				int i = 0;
//				vy.add(atof(parts[i++]));
//				int m = parts.length / 2;
//				svm_node[] x = new svm_node[m];
//				for(int j=0;j<m;j++)
//				{
//					x[j] = new svm_node();
//					x[j].index = atoi(parts[i++]);
//					x[j].value = atof(parts[i++]);
//				}
//				if(m>0) max_index = Math.max(max_index, x[m-1].index);
//				vx.add(x);
			}
		}
		finally
		{
			fp.close();
		}
		long end = System.currentTimeMillis();
		LOG.log(Level.INFO, "part: read (inner): {0}ms", (end - begin));

		prob = new svm_problem();
		prob.l = vy.size();
		prob.x = new svm_node[prob.l][];
		for(int i=0;i<prob.l;i++)
			prob.x[i] = vx.get(i);
		prob.y = new double[prob.l];
		for(int i=0;i<prob.l;i++)
			prob.y[i] = vy.get(i);

		if(param.gamma == 0 && max_index > 0)
			param.gamma = 1.0/max_index;

		if(param.kernel_type == svm_parameter.PRECOMPUTED)
		{
			for(int i=0;i<prob.l;i++)
			{
				if (prob.x[i][0].index != 0)
				{
					LOG.severe("Wrong kernel matrix: first column must be 0:sample_serial_number");
					System.exit(1);
				}
				if ((int)prob.x[i][0].value <= 0 || (int)prob.x[i][0].value > max_index)
				{
					LOG.severe("Wrong input format: sample_serial_number out of range");
					System.exit(1);
				}
			}
		}
	}
}

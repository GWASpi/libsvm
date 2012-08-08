import java.applet.Applet;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class svm_toy extends Applet
{
	private static final Logger LOG = Logger.getLogger(svm_toy.class.getName());

	private static final String DEFAULT_PARAM="-t 2 -c 100";
	private int XLEN;
	private int YLEN;

	// off-screen buffer

	private Image buffer;
	private Graphics buffer_gc;

	// pre-allocated colors

	private static final Color colors[] =
	{
		new Color(0,0,0),
		new Color(0,120,120),
		new Color(120,120,0),
		new Color(120,0,120),
		new Color(0,200,200),
		new Color(200,200,0),
		new Color(200,0,200)
	};

	private static class point
	{
		point(double x, double y, byte value)
		{
			this.x = x;
			this.y = y;
			this.value = value;
		}
		double x, y;
		byte value;
	}

	private List<point> point_list = new ArrayList<point>();
	private byte current_value = 1;

	@Override
	public void init()
	{
		setSize(getSize());

		final Button button_change = new Button("Change");
		Button button_run = new Button("Run");
		Button button_clear = new Button("Clear");
		Button button_save = new Button("Save");
		Button button_load = new Button("Load");
		final TextField input_line = new TextField(DEFAULT_PARAM);

		BorderLayout layout = new BorderLayout();
		this.setLayout(layout);

		Panel p = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridwidth = 1;
		gridbag.setConstraints(button_change,c);
		gridbag.setConstraints(button_run,c);
		gridbag.setConstraints(button_clear,c);
		gridbag.setConstraints(button_save,c);
		gridbag.setConstraints(button_load,c);
		c.weightx = 5;
		c.gridwidth = 5;
		gridbag.setConstraints(input_line,c);

		button_change.setBackground(colors[current_value]);

		p.add(button_change);
		p.add(button_run);
		p.add(button_clear);
		p.add(button_save);
		p.add(button_load);
		p.add(input_line);
		this.add(p,BorderLayout.SOUTH);

		button_change.addActionListener(new ActionListener()
		{ public void actionPerformed (ActionEvent e)
		  { button_change_clicked(); button_change.setBackground(colors[current_value]); }});

		button_run.addActionListener(new ActionListener()
		{ public void actionPerformed (ActionEvent e)
		  { button_run_clicked(input_line.getText()); }});

		button_clear.addActionListener(new ActionListener()
		{ public void actionPerformed (ActionEvent e)
		  { button_clear_clicked(); }});

		button_save.addActionListener(new ActionListener()
		{ public void actionPerformed (ActionEvent e)
		  { button_save_clicked(input_line.getText()); }});

		button_load.addActionListener(new ActionListener()
		{ public void actionPerformed (ActionEvent e)
		  { button_load_clicked(); }});

		input_line.addActionListener(new ActionListener()
		{ public void actionPerformed (ActionEvent e)
		  { button_run_clicked(input_line.getText()); }});

		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
	}

	void draw_point(point p)
	{
		Color c = colors[p.value+3];

		Graphics window_gc = getGraphics();
		buffer_gc.setColor(c);
		buffer_gc.fillRect((int)(p.x*XLEN),(int)(p.y*YLEN),4,4);
		window_gc.setColor(c);
		window_gc.fillRect((int)(p.x*XLEN),(int)(p.y*YLEN),4,4);
	}

	void clear_all()
	{
		point_list.clear();
		if(buffer != null)
		{
			buffer_gc.setColor(colors[0]);
			buffer_gc.fillRect(0,0,XLEN,YLEN);
		}
		repaint();
	}

	void draw_all_points()
	{
		int n = point_list.size();
		for(int i=0;i<n;i++)
			draw_point(point_list.get(i));
	}

	void button_change_clicked()
	{
		++current_value;
		if(current_value > 3) current_value = 1;
	}

	private static double atof(String s)
	{
		return Double.valueOf(s).doubleValue();
	}

	private static int atoi(String s)
	{
		return Integer.parseInt(s);
	}

	void button_run_clicked(String args)
	{
		// guard
		if(point_list.isEmpty()) return;

		svm_parameter param = new svm_parameter();

		// default values
		param.svm_type = svm_parameter.C_SVC;
		param.kernel_type = svm_parameter.RBF;
		param.degree = 3;
		param.gamma = 0;
		param.coef0 = 0;
		param.nu = 0.5;
		param.cache_size = 40;
		param.C = 1;
		param.eps = 1e-3;
		param.p = 0.1;
		param.shrinking = 1;
		param.probability = 0;
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];

		// parse options
		StringTokenizer st = new StringTokenizer(args);
		String[] argv = new String[st.countTokens()];
		for(int i=0;i<argv.length;i++)
			argv[i] = st.nextToken();

		for(int i=0;i<argv.length;i++)
		{
			if(argv[i].charAt(0) != '-') break;
			if(++i>=argv.length)
			{
				LOG.warning("unknown option");
				break;
			}
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
				default:
					LOG.log(Level.WARNING, "unknown option \"{0}\"", argv[i-1].charAt(1));
			}
		}

		// build problem
		svm_problem prob = new svm_problem();
		prob.l = point_list.size();
		prob.y = new double[prob.l];

		if(param.kernel_type == svm_parameter.PRECOMPUTED)
		{
		}
		else if(param.svm_type == svm_parameter.EPSILON_SVR ||
			param.svm_type == svm_parameter.NU_SVR)
		{
			if(param.gamma == 0) param.gamma = 1;
			prob.x = new svm_node[prob.l][1];
			for(int i=0;i<prob.l;i++)
			{
				point p = point_list.get(i);
				prob.x[i][0] = new svm_node();
				prob.x[i][0].index = 1;
				prob.x[i][0].value = p.x;
				prob.y[i] = p.y;
			}

			// build model & classify
			svm_model model = svm.svm_train(prob, param);
			svm_node[] x = new svm_node[1];
			x[0] = new svm_node();
			x[0].index = 1;
			int[] j = new int[XLEN];

			Graphics window_gc = getGraphics();
			for (int i = 0; i < XLEN; i++)
			{
				x[0].value = (double) i / XLEN;
				j[i] = (int)(YLEN*svm.svm_predict(model, x));
			}

			buffer_gc.setColor(colors[0]);
			buffer_gc.drawLine(0,0,0,YLEN-1);
			window_gc.setColor(colors[0]);
			window_gc.drawLine(0,0,0,YLEN-1);

			int p = (int)(param.p * YLEN);
			for(int i=1;i<XLEN;i++)
			{
				buffer_gc.setColor(colors[0]);
				buffer_gc.drawLine(i,0,i,YLEN-1);
				window_gc.setColor(colors[0]);
				window_gc.drawLine(i,0,i,YLEN-1);

				buffer_gc.setColor(colors[5]);
				window_gc.setColor(colors[5]);
				buffer_gc.drawLine(i-1,j[i-1],i,j[i]);
				window_gc.drawLine(i-1,j[i-1],i,j[i]);

				if(param.svm_type == svm_parameter.EPSILON_SVR)
				{
					buffer_gc.setColor(colors[2]);
					window_gc.setColor(colors[2]);
					buffer_gc.drawLine(i-1,j[i-1]+p,i,j[i]+p);
					window_gc.drawLine(i-1,j[i-1]+p,i,j[i]+p);

					buffer_gc.setColor(colors[2]);
					window_gc.setColor(colors[2]);
					buffer_gc.drawLine(i-1,j[i-1]-p,i,j[i]-p);
					window_gc.drawLine(i-1,j[i-1]-p,i,j[i]-p);
				}
			}
		}
		else
		{
			if(param.gamma == 0) param.gamma = 0.5;
			prob.x = new svm_node [prob.l][2];
			for(int i=0;i<prob.l;i++)
			{
				point p = point_list.get(i);
				prob.x[i][0] = new svm_node();
				prob.x[i][0].index = 1;
				prob.x[i][0].value = p.x;
				prob.x[i][1] = new svm_node();
				prob.x[i][1].index = 2;
				prob.x[i][1].value = p.y;
				prob.y[i] = p.value;
			}

			// build model & classify
			svm_model model = svm.svm_train(prob, param);
			svm_node[] x = new svm_node[2];
			x[0] = new svm_node();
			x[1] = new svm_node();
			x[0].index = 1;
			x[1].index = 2;

			Graphics window_gc = getGraphics();
			for (int i = 0; i < XLEN; i++)
				for (int j = 0; j < YLEN ; j++) {
					x[0].value = (double) i / XLEN;
					x[1].value = (double) j / YLEN;
					double d = svm.svm_predict(model, x);
					if (param.svm_type == svm_parameter.ONE_CLASS && d<0) d=2;
					buffer_gc.setColor(colors[(int)d]);
					window_gc.setColor(colors[(int)d]);
					buffer_gc.drawLine(i,j,i,j);
					window_gc.drawLine(i,j,i,j);
			}
		}

		draw_all_points();
	}

	void button_clear_clicked()
	{
		clear_all();
	}

	void button_save_clicked(String args)
	{
		FileDialog dialog = new FileDialog(new Frame(),"Save",FileDialog.SAVE);
		dialog.setVisible(true);
		String filename = dialog.getDirectory() + dialog.getFile();
		if (filename == null) return;
		try {
			DataOutputStream fp = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));

			int svm_type = svm_parameter.C_SVC;
			int svm_type_idx = args.indexOf("-s ");
			if(svm_type_idx != -1)
			{
				StringTokenizer svm_str_st = new StringTokenizer(args.substring(svm_type_idx+2).trim());
				svm_type = atoi(svm_str_st.nextToken());
			}

			int n = point_list.size();
			if(svm_type == svm_parameter.EPSILON_SVR || svm_type == svm_parameter.NU_SVR)
			{
				for(int i=0;i<n;i++)
				{
					point p = point_list.get(i);
					fp.writeBytes(p.y+" 1:"+p.x+"\n");
				}
			}
			else
			{
				for(int i=0;i<n;i++)
				{
					point p = point_list.get(i);
					fp.writeBytes(p.value+" 1:"+p.x+" 2:"+p.y+"\n");
				}
			}
			fp.close();
		} catch (IOException e) { LOG.log(Level.SEVERE, "", e); }
	}

	void button_load_clicked()
	{
		FileDialog dialog = new FileDialog(new Frame(),"Load",FileDialog.LOAD);
		dialog.setVisible(true);
		String filename = dialog.getDirectory() + dialog.getFile();
		if (filename == null) return;
		clear_all();
		try {
			BufferedReader fp = new BufferedReader(new FileReader(filename));
			String line;
			while((line = fp.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line," \t\f:");
				if(st.countTokens() == 5)
				{
					byte value = (byte)atoi(st.nextToken());
					st.nextToken();
					double x = atof(st.nextToken());
					st.nextToken();
					double y = atof(st.nextToken());
					point_list.add(new point(x,y,value));
				}
				else if(st.countTokens() == 3)
				{
					double y = atof(st.nextToken());
					st.nextToken();
					double x = atof(st.nextToken());
					point_list.add(new point(x,y,current_value));
				}else
					break;
			}
			fp.close();
		} catch (IOException e) { LOG.log(Level.SEVERE, "", e); }
		draw_all_points();
	}

	@Override
	protected void processMouseEvent(MouseEvent e)
	{
		if(e.getID() == MouseEvent.MOUSE_PRESSED)
		{
			if(e.getX() >= XLEN || e.getY() >= YLEN) return;
			point p = new point((double)e.getX()/XLEN,
					    (double)e.getY()/YLEN,
					    current_value);
			point_list.add(p);
			draw_point(p);
		}
	}

	@Override
	public void paint(Graphics g)
	{
		// create buffer first time
		if(buffer == null) {
			buffer = this.createImage(XLEN,YLEN);
			buffer_gc = buffer.getGraphics();
			buffer_gc.setColor(colors[0]);
			buffer_gc.fillRect(0,0,XLEN,YLEN);
		}
		g.drawImage(buffer,0,0,this);
	}

	@Override
	public Dimension getPreferredSize() { return new Dimension(XLEN,YLEN+50); }

	@Override
	public void setSize(Dimension d) { setSize(d.width,d.height); }
	@Override
	public void setSize(int w,int h) {
		super.setSize(w,h);
		XLEN = w;
		YLEN = h-50;
		clear_all();
	}

	private static void logHelp()
	{
		LOG.info("Usage: svm_toy [options]");
		LOG.info("");
		LOG.info("Starts a GUI that allows for easy, basic testing");
		LOG.info("of the libraries functions.");
		LOG.info("");
		LOG.info("Options:");
		LOG.info("--help : display this help and exit");
		LOG.info("--version : output version information and exit");
	}

	public static void main(String[] argv)
	{
		svm_train.setupLogging();

		try
		{
			// parse options
			for(int i=0;i<argv.length;i++)
			{
				if(argv[i].charAt(0) != '-') break;
				++i;
				switch(argv[i-1].charAt(1))
				{
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
							LOG.log(Level.INFO, "{0} {1} {2}", new Object[] {"LibSVM", "svm-toy", svm.getVersion()});
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
		}
		catch (IllegalArgumentException ex)
		{
			LOG.log(Level.SEVERE, "Failed parsing arguments", ex);
			logHelp();
			System.exit(1);
		}

		new AppletFrame("svm_toy",new svm_toy(),500,500+50);
	}
}

class AppletFrame extends Frame
{
	AppletFrame(String title, Applet applet, int width, int height)
	{
		super(title);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		applet.init();
		applet.setSize(width,height);
		applet.start();
		this.add(applet);
		this.pack();
		this.setVisible(true);
	}
}

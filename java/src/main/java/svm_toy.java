import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class svm_toy extends JApplet
{
	private static final Logger LOG = Logger.getLogger(svm_toy.class.getName());

	private static final String DEFAULT_PARAM = "-t 2 -c 100";

	/**
	 * Allows to start the JApplet as a desktop GUI application.
	 */
	private static class AppletFrame extends JFrame
	{
		AppletFrame(JApplet applet)
		{
			super(applet.getName());
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			applet.init();
			applet.start();
			add(applet);
		}
	}

	// pre-allocated colors
	private static final Color[] colors =
	{
		new Color(0, 0, 0),
		new Color(0, 120, 120),
		new Color(120, 120, 0),
		new Color(120, 0, 120),
		new Color(0, 200, 200),
		new Color(200, 200, 0),
		new Color(200, 0, 200)
	};

	private static class DataPoint
	{
		private Point2D location;
		private byte value;

		DataPoint(Point2D location, byte value)
		{
			this.location = location;
			this.value = value;
		}

		DataPoint(double x, double y, byte value)
		{
			this(new Point2D.Double(x, y), value);
		}

		public Point2D getLocation() {
			return location;
		}

		public byte getValue() {
			return value;
		}
	}

	private class DrawPanel extends JPanel
	{
		private final List<DataPoint> dataPoints;
		private svm_parameter param;
		private svm_model model;

		DrawPanel()
		{
			dataPoints = new ArrayList<DataPoint>();
			param = null;
			model = null;

			setBackground(Color.BLACK);
			enableEvents(AWTEvent.MOUSE_EVENT_MASK);
		}

		List<DataPoint> getDataPoints() {
			return dataPoints;
		}

		void addPoint(DataPoint dataPoint)
		{
			dataPoints.add(dataPoint);
		}

		void clearPoints()
		{
			dataPoints.clear();
			repaint();
		}

		void clear()
		{
			dataPoints.clear();
			param = null;
			model = null;
			repaint();
		}

		void setModel(svm_parameter param, svm_model model)
		{
			this.param = param;
			this.model = model;
		}

		private void predictAndDrawClassificationBoundaries(Graphics g)
		{
			int canvasWidth = drawPanel.getWidth();
			int canvasHeight = drawPanel.getHeight();

			// classify & draw
			if(param.svm_type == svm_parameter.EPSILON_SVR
					|| param.svm_type == svm_parameter.NU_SVR)
			{
				svm_node[] x = new svm_node[1];
				x[0] = new svm_node();
				x[0].index = 1;
				int[] j = new int[canvasWidth];

				for (int i = 0; i < canvasWidth; i++)
				{
					x[0].value = (double) i / canvasWidth;
					j[i] = (int)(canvasHeight * svm.svm_predict(model, x));
				}

				g.setColor(colors[0]);
				g.drawLine(0, 0, 0, canvasHeight-1);

				int p = (int)(param.p * canvasHeight);
				for (int i = 1; i < canvasWidth; i++)
				{
					g.setColor(colors[0]);
					g.drawLine(i, 0, i, canvasHeight-1);

					g.setColor(colors[5]);
					g.drawLine(i-1, j[i-1], i, j[i]);

					if (param.svm_type == svm_parameter.EPSILON_SVR)
					{
						g.setColor(colors[2]);
						g.drawLine(i-1, j[i-1]+p, i, j[i]+p);

						g.setColor(colors[2]);
						g.drawLine(i-1, j[i-1]-p, i, j[i]-p);
					}
				}
			}
			else if(param.kernel_type != svm_parameter.PRECOMPUTED)
			{
				svm_node[] x = new svm_node[2];
				x[0] = new svm_node();
				x[1] = new svm_node();
				x[0].index = 1;
				x[1].index = 2;

				for (int i = 0; i < canvasWidth; i++)
					for (int j = 0; j < canvasHeight ; j++) {
						x[0].value = (double) i / canvasWidth;
						x[1].value = (double) j / canvasHeight;
						double d = svm.svm_predict(model, x);
						if (param.svm_type == svm_parameter.ONE_CLASS && d < 0)
						{
							d = 2;
						}
						g.setColor(colors[(int)d]);
						g.drawLine(i, j, i, j);
				}
			}
		}

		private Point2D real2canvas(Point2D point)
		{
			return point; // TODO FIXME
		}

		private void drawPoint(Graphics g, DataPoint dataPoint)
		{
			Color c = colors[dataPoint.getValue()+3];

			g.setColor(c);
			g.fillRect((int)(dataPoint.getLocation().getX()*getWidth()),(int)(dataPoint.getLocation().getY()*getHeight()),4,4);
		}

		private void drawPoints(Graphics g)
		{
			for (DataPoint dataPoint : dataPoints)
			{
				drawPoint(g, dataPoint);
			}
		}

		@Override
		public void paint(Graphics g)
		{
			super.paint(g);

			if ((param != null) && (model != null))
			{
				predictAndDrawClassificationBoundaries(g);
			}

			drawPoints(g);
		}

		@Override
		protected void processMouseEvent(MouseEvent e)
		{
			if(e.getID() == MouseEvent.MOUSE_PRESSED)
			{

				if(e.getX() >= getWidth() || e.getY() >= getHeight()) return;
				DataPoint p = new DataPoint((double)e.getX()/getWidth(),
							(double)e.getY()/getHeight(),
							currentValue);
				addPoint(p);
				drawPoint(getGraphics(), p);
			}
		}
	}

	private class ChangeColorAction extends AbstractAction
	{
		private JPanel pCurrentColor;

		ChangeColorAction(JPanel pCurrentColor) {
			super("Change Color");

			this.pCurrentColor = pCurrentColor;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {

			++currentValue;
			if(currentValue > 3)
			{
				currentValue = 1;
			}
			pCurrentColor.setBackground(colors[currentValue]);
		}
	}

	private class RunAction extends AbstractAction
	{
		RunAction() {
			super("Run");
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			classify();
		}
	}

	private class ClearAction extends AbstractAction
	{
		ClearAction() {
			super("Clear");
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			drawPanel.clear();
		}
	}

	private class SaveAction extends AbstractAction
	{
		SaveAction() {
			super("Save");
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
//			button_save_clicked(input_line.getText());
		}
	}

	private class LoadAction extends AbstractAction
	{
		LoadAction() {
			super("Load");
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			button_load_clicked();
		}
	}

	private DrawPanel drawPanel;
	private JTextField tfInputLine;

	private Action changeColorAction;
	private Action runAction;
	private Action clearAction;
	private Action saveAction;
	private Action loadAction;

	private byte currentValue = 1;

	public svm_toy()
	{
		setName(String.format("%s %s %s", "LibSVM", "svm-toy", svm.getVersion()));
	}

	@Override
	public void init()
	{
		JPanel pCurrentColor = new JPanel();
		pCurrentColor.setBorder(new LineBorder(Color.BLACK));
		drawPanel = new DrawPanel();

		changeColorAction = new ChangeColorAction(pCurrentColor);
		runAction = new RunAction();
		clearAction = new ClearAction();
		saveAction = new SaveAction();
		loadAction = new LoadAction();

		JButton bChange = new JButton(changeColorAction);
		JButton bRun = new JButton(runAction);
		JButton bClear = new JButton(clearAction);
		JButton bSave = new JButton(saveAction);
		JButton bLoad = new JButton(loadAction);
		tfInputLine = new JTextField(DEFAULT_PARAM);

		BorderLayout layout = new BorderLayout();
		this.setLayout(layout);

		JPanel pControls = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		pControls.setLayout(gridbag);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(5, 5, 5, 5);
		c.weightx = 1;
		c.gridwidth = 1;
		gridbag.setConstraints(pCurrentColor,c);
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(bChange,c);
		gridbag.setConstraints(bRun,c);
		gridbag.setConstraints(bClear,c);
		gridbag.setConstraints(bSave,c);
		gridbag.setConstraints(bLoad,c);
		c.weightx = 5;
		c.gridwidth = 5;
		gridbag.setConstraints(tfInputLine,c);

		pCurrentColor.setBackground(colors[currentValue]);

		pControls.add(pCurrentColor);
		pControls.add(bChange);
		pControls.add(bRun);
		pControls.add(bClear);
		pControls.add(bSave);
		pControls.add(bLoad);
		pControls.add(tfInputLine);
		this.add(pControls, BorderLayout.SOUTH);

		this.add(drawPanel, BorderLayout.CENTER);
	}

	private static double atof(String s)
	{
		return Double.valueOf(s).doubleValue();
	}

	private static int atoi(String s)
	{
		return Integer.parseInt(s);
	}

	private svm_parameter parseParams(String args)
	{
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
		String[] argv = args.split("[ \t\n\r\f]+");

		int argi;
		for (argi = 0; argi < argv.length; argi++)
		{
			if(argv[argi].charAt(0) != '-') break;
			if(++argi>=argv.length)
			{
				LOG.warning("unknown option");
				break;
			}
			switch(argv[argi-1].charAt(1))
			{
				case 's':
					param.svm_type = atoi(argv[argi]);
					break;
				case 't':
					param.kernel_type = atoi(argv[argi]);
					break;
				case 'd':
					param.degree = atoi(argv[argi]);
					break;
				case 'g':
					param.gamma = atof(argv[argi]);
					break;
				case 'r':
					param.coef0 = atof(argv[argi]);
					break;
				case 'n':
					param.nu = atof(argv[argi]);
					break;
				case 'm':
					param.cache_size = atof(argv[argi]);
					break;
				case 'c':
					param.C = atof(argv[argi]);
					break;
				case 'e':
					param.eps = atof(argv[argi]);
					break;
				case 'p':
					param.p = atof(argv[argi]);
					break;
				case 'h':
					param.shrinking = atoi(argv[argi]);
					break;
				case 'b':
					param.probability = atoi(argv[argi]);
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

					param.weight_label[param.nr_weight-1] = atoi(argv[argi-1].substring(2));
					param.weight[param.nr_weight-1] = atof(argv[argi]);
					break;
				default:
					LOG.log(Level.WARNING, "unknown option \"{0}\"", argv[argi-1].charAt(1));
			}
		}

		return param;
	}

	void classify()
	{
		classify(tfInputLine.getText());
	}

	private svm_model train(svm_parameter param)
	{
		svm_model model = null;

		List<DataPoint> dataPoints = drawPanel.getDataPoints();

		// guard
		if (dataPoints.isEmpty())
		{
			return model;
		}

		// build problem
		svm_problem prob = new svm_problem();
		prob.l = dataPoints.size();
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
				DataPoint p = dataPoints.get(i);
				prob.x[i][0] = new svm_node();
				prob.x[i][0].index = 1;
				prob.x[i][0].value = p.getLocation().getX();
				prob.y[i] = p.getLocation().getY();
			}

			// build model
			model = svm.svm_train(prob, param);
		}
		else
		{
			if(param.gamma == 0) param.gamma = 0.5;
			prob.x = new svm_node [prob.l][2];
			for(int i=0;i<prob.l;i++)
			{
				DataPoint p = dataPoints.get(i);
				prob.x[i][0] = new svm_node();
				prob.x[i][0].index = 1;
				prob.x[i][0].value = p.getLocation().getX();
				prob.x[i][1] = new svm_node();
				prob.x[i][1].index = 2;
				prob.x[i][1].value = p.getLocation().getY();
				prob.y[i] = p.getValue();
			}

			// build model
			model = svm.svm_train(prob, param);
		}

		return model;
	}

	void classify(String args)
	{
		svm_parameter param = parseParams(args);

		svm_model model = train(param);

		drawPanel.setModel(param, model);
		drawPanel.repaint();
	}

	void button_save_clicked(String args)
	{
		JFileChooser dialog = new JFileChooser();
		dialog.setDialogType(JFileChooser.SAVE_DIALOG);
		dialog.setVisible(true);
		File file = dialog.getSelectedFile();
		if (file == null) return;
		try {
			DataOutputStream fp = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

			int svm_type = svm_parameter.C_SVC;
			int svm_type_idx = args.indexOf("-s ");
			if(svm_type_idx != -1)
			{
				StringTokenizer svm_str_st = new StringTokenizer(args.substring(svm_type_idx+2).trim());
				svm_type = atoi(svm_str_st.nextToken());
			}

			List<DataPoint> dataPoints = drawPanel.getDataPoints();
			int n = dataPoints.size();
			if(svm_type == svm_parameter.EPSILON_SVR || svm_type == svm_parameter.NU_SVR)
			{
				for(int i=0;i<n;i++)
				{
					DataPoint p = dataPoints.get(i);
					fp.writeBytes(p.getLocation().getY()+" 1:"+p.getLocation().getX()+"\n");
				}
			}
			else
			{
				for(int i=0;i<n;i++)
				{
					DataPoint p = dataPoints.get(i);
					fp.writeBytes(p.getValue()+" 1:"+p.getLocation().getX()+" 2:"+p.getLocation().getY()+"\n");
				}
			}
			fp.close();
		} catch (IOException e) { LOG.log(Level.SEVERE, "", e); }
	}

	void button_load_clicked()
	{
		JFileChooser dialog = new JFileChooser();
		dialog.setDialogType(JFileChooser.OPEN_DIALOG);
		dialog.setVisible(true);
		File file = dialog.getSelectedFile();
		if (file == null) return;
		drawPanel.clear();
		try {
			BufferedReader fp = new BufferedReader(new FileReader(file));
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
					drawPanel.addPoint(new DataPoint(x,y,value));
				}
				else if(st.countTokens() == 3)
				{
					double y = atof(st.nextToken());
					st.nextToken();
					double x = atof(st.nextToken());
					drawPanel.addPoint(new DataPoint(x,y,currentValue));
				}else
					break;
			}
			fp.close();
		} catch (IOException e) { LOG.log(Level.SEVERE, "", e); }
		drawPanel.repaint();
	}

//	@Override
//	public void paint(Graphics g)
//	{
//		// create buffer first time
//		if(buffer == null) {
//			buffer = this.createImage(XLEN,YLEN);
//			buffer_gc = buffer.getGraphics();
//			buffer_gc.setColor(colors[0]);
//			buffer_gc.fillRect(0,0,XLEN,YLEN);
//		}
//		g.drawImage(buffer,0,0,this);
//	}

//	@Override
//	public Dimension getPreferredSize() { return new Dimension(XLEN,YLEN+50); }
//
//	@Override
//	public void setSize(Dimension d) { setSize(d.width,d.height); }
//	@Override
//	public void setSize(int w,int h) {
//		super.setSize(w,h);
//		XLEN = w;
//		YLEN = h-50;
//		clear_all();
//	}

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
			int i;
			for(i=0;i<argv.length;i++)
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

		AppletFrame appletFrame = new AppletFrame(new svm_toy());
		appletFrame.setSize(640, 480);
		appletFrame.setVisible(true);
	}
}

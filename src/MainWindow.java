import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.*;
import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.SpringLayout.Constraints;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MainWindow implements Runnable, ActionListener {


	String im_location = "resources/model.png";
	private BufferedImage input;
	private BufferedImage model;
	private BufferedImage distance;
	private BufferedImage support;
	
	protected JFrame f;

    @Override
    public void run() {
        // Create the window
        f = new JFrame("Hello, !");
        // Sets the behavior for when the window is closed
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        // Add a layout manager so that the button is not placed on top of the label
        f.setLayout(new GridBagLayout());
        
        JPanel pica_panel = new JPanel(new FlowLayout());
        // add pica
        try {
            input = ImageIO.read(new File(im_location));
            Image resized = input.getScaledInstance(512, 512, Image.SCALE_FAST);
            model = getModel();
            distance = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
            processDistance();
            support = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
            processSupport();
            makeCompositeImage();
        	JLabel outLabel = new JLabel(new ImageIcon(resized));
        	pica_panel.add(outLabel);
        } catch (IOException e) { System.out.println("Couldn't open file!"); }
        f.add(pica_panel);
        
        JPanel options_panel = new JPanel(new BorderLayout());
        options_panel.add(new JLabel("Options"), BorderLayout.PAGE_START);
        { // get options
        	JPanel options = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
        	
	        // Add go button
            c.gridy = 0;
	        JButton button = new JButton("Go!");
	        button.setActionCommand("go");
	        button.addActionListener(this);
	        options.add(button);

	        // add single step button
            c.gridy = 1;
	        JButton step_button = new JButton("Step.");
	        button.setActionCommand("step");
	        button.addActionListener(this);
	        options.add(step_button);
	        
        	options_panel.add(options);
        }
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 2;
        f.add(options_panel, c);
        

        // Arrange the components inside the window
        f.pack();
        // By default, the window is not visible. Make it visible.
        f.setVisible(true);
    }

	public static void main(String[] args) {
		MainWindow se = new MainWindow();
        // Schedules the application to be run at the correct time in the event queue.
        SwingUtilities.invokeLater(se);
	}

	public BufferedImage getModel()
	{
		BufferedImage ret = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
		WritableRaster raster = ret.getRaster();
		for (int y = 0; y < input.getHeight(); y++)
			for (int x = 0; x < input.getWidth(); x++)
				if (input.getRGB(x, y) == new Color(0,0,0).getRGB())
					raster.setSample(x, y, 0, 1);
				else
					raster.setSample(x, y, 0, 0);
		for (int y = input.getHeight() - 2; y >= 0; y--)
			for (int x = 0; x < input.getWidth(); x++)
				if (raster.getSample(x, y, 0) != 0)
					for (int x_below = Math.max(0, x - 1); x_below <= Math.min(input.getWidth() - 1, x + 1); x_below++)
						if (raster.getSample(x_below, y + 1, 0) != 0)
						{
							raster.setSample(x, y, 0, 2);
							break;
						}
		return ret;
	}

	public void processDistance()
	{
		WritableRaster dist_raster = distance.getRaster();
		for (int x = 0; x < input.getWidth(); x++)
		{
			int y = distance.getHeight() - 1;
			if (model.getRaster().getSample(x, y, 0) == 0)
				dist_raster.setSample(x, y, 0, 0);
			else
				dist_raster.setSample(x, y, 0, 1);
		}
		for (int y = input.getHeight() - 2; y >= 0; y--)
		{
			for (int x = 0; x < input.getWidth(); x++)
			{
				if (model.getRaster().getSample(x, y, 0) != 0)
				{
					dist_raster.setSample(x, y, 0, 0);
				}
				else
				{
					int best_dist = 99999;
					for (int x_below = Math.max(0, x - 1); x_below <= Math.min(input.getWidth() - 1, x + 1); x_below++)
					{
						int dist_below = dist_raster.getSample(x_below, y + 1, 0);
						if (dist_below < best_dist)
						{
							best_dist = dist_below;
						}
					}
					dist_raster.setSample(x, y, 0, best_dist + 1);
				}
			}
		}
	}
	
	public void processSupport()
	{
		WritableRaster supp_raster = support.getRaster();

		for (int y = 0; y < input.getHeight(); y++)
		{
			for (int x = 0; x < input.getWidth(); x++)
			{
				int supported_count = 0;
				if (model.getRaster().getSample(x, y, 0) == 1)
					supported_count++;
				if (y > 0 && model.getRaster().getSample(x, y - 1, 0) == 1)
					supported_count++;
				
				int up = 0;
				if (y >= 2)
					up = supp_raster.getSample(x, y - 2, 0);

				int left = 0;
				if (x > 0)
				{
					if (y > 0)
						left = supp_raster.getSample(x - 1, y - 1, 0);
				}
				else
					up = 0;

				int right = 0;
				if (x < support.getWidth() - 1)
				{
					if (y > 0)
						right = supp_raster.getSample(x + 1, y - 1, 0);
				}
				else
					up = 0;
				
				supported_count += left + right - up;
				supp_raster.setSample(x, y, 0, supported_count);
			}
		}
	}
	
	public void makeCompositeImage()
	{
		int highest_dist = 0;
		for (int x = 0; x < input.getWidth(); x++)
			for (int y = 0; y < input.getHeight(); y++)
				highest_dist = Math.max(highest_dist, distance.getRaster().getSample(x, y, 0));

		int highest_support = 0;
		for (int x = 0; x < input.getWidth(); x++)
			for (int y = 0; y < input.getHeight(); y++)
				highest_support = Math.max(highest_support, support.getRaster().getSample(x, y, 0));
		
		float best_ratio = 0.0f;
		for (int x = 0; x < input.getWidth(); x++)
			for (int y = 0; y < input.getHeight(); y++)
			{
				if (model.getRaster().getSample(x, y, 0) == 0)
				{
					float ratio = (float)(support.getRaster().getSample(x, y, 0)) / (1 + distance.getRaster().getSample(x, y, 0));
					System.out.println(x +","+y+": "+ratio + " / "+(ratio/best_ratio));
					best_ratio = Math.max(best_ratio, ratio);
				}
			}
		System.out.println(best_ratio);
		
		WritableRaster raster = input.getRaster();
		for (int x = 0; x < input.getWidth(); x++)
		{
			for (int y = 0; y < input.getHeight(); y++)
			{
				if (model.getRaster().getSample(x, y, 0) == 1)
				{
					input.setRGB(x, y, new Color(255,255,255).getRGB());
					System.out.print("US ");
				}
				else if (model.getRaster().getSample(x, y, 0) == 2)
				{
					input.setRGB(x, y, new Color(0,0,0).getRGB());
					System.out.print("mm ");
				}
				else
				{ // change non-model pixels
					int dist = distance.getRaster().getSample(x, y, 0);
					int sup = support.getRaster().getSample(x, y, 0);
					raster.setSample(x, y, 0, 255 - dist * 255 / highest_dist);
					raster.setSample(x, y, 1, sup * 255 / highest_support);
					//raster.setSample(x, y, 0, 0);
					//raster.setSample(x, y, 1, 0);
					//raster.setSample(x, y, 1, dist * sup *255 / highest_dist / highest_support);
					float ratio = (float)(sup) / (1 + dist);
					//System.out.println(x +","+y+": "+ratio + " / "+(ratio/best_ratio));
					//raster.setSample(x, y, 2, (int)(255 * (1.0f - (ratio / best_ratio))));
					raster.setSample(x, y, 2, 0);
					System.out.print(sup+""+dist+" ");
				}
			}
			System.out.println("");
		}
	}
	
	public void step()
	{
		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("step"))
		{
			step();
			f.update(f.getGraphics());
		}
	}


}

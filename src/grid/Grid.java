package grid;
import java.awt.Color;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

public class Grid implements Cloneable {
	public static enum PixelType { Empty, Model, Overhang, Support };
	public static class Pixel implements Cloneable
	{
		PixelType type;
		int support_dir = -2; // -1, 0 or 1 if supported overhang; otherwise -2
		@Override
		public Pixel clone() {
			Pixel p = new Pixel();
			p.type = type;
			p.support_dir = support_dir;
		    return p;
		}
	}
	
	
	public BufferedImage input;
	public int w, h;
	private ArrayList<Pixel> grid;
	public Grid(BufferedImage _input) {
		input = _input;
		w = input.getWidth();
		h = input.getHeight();
		initializeGrid();
		computeOverhang();
	}
	
	public Grid() {
		// do nothing
	}

	@Override
	public Grid clone() {
		Grid clone = new Grid();
		clone.input = input;
		clone.w = w;
		clone.h = h;
		clone.grid = new ArrayList<Pixel>(grid.size());
		for (Pixel p : grid)
		{
			clone.grid.add(p.clone());
		}
		return clone;
	}
	
	public void set(Grid other)
	{
		assert(w == other.w);
		assert(h == other.h);
		for (int pos = 0; pos < grid.size(); pos++)
		{
			Pixel p = other.grid.get(pos);
			grid.set(pos, p.clone());
		}
	}

	public Pixel getPixel(int x, int y)
	{
		assert(x >=0 && x < w);
		assert(y >=0 && y < h);
		return grid.get(y * w + x);
	}
	
	public void initializeGrid()
	{
		grid = new ArrayList<Pixel>(h * w);
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				Pixel p = new Pixel();
				if (input.getRGB(x, h - 1 - y) == new Color(0, 0, 0).getRGB())
					p.type = PixelType.Model;
				else
					p.type = PixelType.Empty;
				Color c = new Color(input.getRGB(x, h - 1 - y));
				//System.out.println(x+", "+y+": "+p.type+"\t color:"+c.getRed()+","+c.getGreen()+","+c.getBlue());
				grid.add(p);
			}
		}
		assert(grid.size() == w * h);
	}

	public void computeOverhang()
	{
		for (int y = 1; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				Pixel p = getPixel(x, y); 
				if (p.type == PixelType.Model)
				{
					p.type = PixelType.Overhang;
					for (int x_below = Math.max(0, x - 1); x_below <= Math.min(w - 1, x + 1); x_below++)
					{
						if (getPixel(x_below, y - 1).type != PixelType.Empty)
						{
							p.type = PixelType.Model;
							p.support_dir = x_below - x;
							break;
						}
					}
				}
			}
		}
	}
	
	public BufferedImage toImage()
	{
		BufferedImage ret = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				Color color;
				switch (getPixel(x, y).type)
				{
				default:
				case Empty:
					color = new Color(255, 255, 255); break;
				case Model:
					color = new Color(0, 0, 0); break;
				case Overhang:
					color = new Color(255, 0, 0); break;
				case Support:
					color = new Color(0, 255, 255); break;
				}
				ret.setRGB(x, h - 1 - y, color.getRGB());
			}
		}
		return ret;
	}
	
	public void outputSupportDir()
	{

		for (int y = h-1; y >= 0; y--)
		{
			for (int x = 0; x < w; x++)
			{
				int dir = getPixel(x, y).support_dir;
				if (dir >= 0) System.out.print(" ");
				if (dir == -2)
					System.out.print("..");
				else
					System.out.print(dir);
				System.out.print(" ");
			}
			System.out.println("");
		}
	}
	public void output()
	{

		for (int y = h-1; y >= 0; y--)
		{
			for (int x = 0; x < w; x++)
			{
				Pixel p = getPixel(x, y);
				switch(p.type)
				{
				case Support:
				case Overhang:
				{
					int dir = p.support_dir;
					if (dir >= 0) System.out.print(" ");
					System.out.print(dir);
					System.out.print(" ");
				} break;
				case Model:
					System.out.print("## "); break;
				default:
				case Empty:
					System.out.print("   "); break;
				}
			}
			System.out.println("");
		}
	}
}

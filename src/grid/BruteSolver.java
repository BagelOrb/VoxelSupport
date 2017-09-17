package grid;
import java.util.*;

import grid.Grid.*;



public class BruteSolver {
	final Grid model;
	int w, h;
	int n_supports = 0;
	Grid currentSolution;
	Grid bestSolution;
	int best_score;
	int n_solutions = 1;
	public BruteSolver(Grid _model) {
		model = _model;
		w = model.w;
		h = model.h;
		currentSolution = model.clone();
		getInitialSolution();
		best_score = computeScore();
		bestSolution = currentSolution.clone();
	}
	
	public void findBestSolution()
	{
		while(true)
		{
			boolean done = nextSolution();
			if (done)
				return;
		}
	}

	private void getInitialSolution() {
		solveFrom(0, h - 1);
	}
	
	private void solveFrom(int start_x, int start_y)
	{
		for (int y = start_y; y >= 1; y--)
		{
			// clear row below
			for (int x = 0; x < w; x++)
			{
				Pixel p = currentSolution.getPixel(x, y - 1);
				p.support_dir = -2;
				if (p.type == PixelType.Support)
				{
					p.type = PixelType.Empty;
				}
			}
			// fill in start of row below from existing data (only in first iteration)
			for (int x = 0; x < start_x; x++)
			{
				Pixel p = currentSolution.getPixel(x, y);
				if (p.support_dir != -2)
				{
					assert(p.type == PixelType.Overhang || p.type == PixelType.Support);
					currentSolution.getPixel(x + p.support_dir, y - 1).type = PixelType.Support;
				}
			}
			for (int x = start_x; x < w; x++)
			{
				Pixel p = currentSolution.getPixel(x, y); 
				if (p.type == PixelType.Overhang || p.type == PixelType.Support)
				{
					boolean is_supported = false;
					for (int x_below = Math.max(0, x - 1); x_below <= Math.min(w - 1, x + 1); x_below++)
					{
						PixelType type_below = model.getPixel(x_below, y - 1).type; 
						//if (type_below == PixelType.Model || type_below == PixelType.Overhang)
						if (type_below != PixelType.Empty) // allow for support on layer below to support current pixels (only possible if support below is not invalidated)
						{
							is_supported = true;
							break;
						}
					}
					if (!is_supported)
					{
						int x_below = Math.max(0, x - 1);
						if (y == start_y && x == start_x
								&& (p.support_dir >= -1 && p.support_dir <= 1))
						{
							x_below = x + p.support_dir;
							assert (x_below >= 0 && x_below < w);
						}
						currentSolution.getPixel(x_below, y - 1).type = PixelType.Support;
						p.support_dir = x_below - x;
					}
					else
						p.support_dir = -2;
				}
				else
					assert(p.support_dir == -2);
			}
			start_x = 0;
		}
	}
	
	public boolean nextSolution()
	{
		boolean done = computeNextSolution();
		int score = computeScore();
		if (score < best_score)
		{
			best_score = score;
			bestSolution.set(currentSolution);
			//System.out.println("Best got updated");
		}
		n_solutions++;
		if (n_solutions % 10 == 0)
			System.out.println(n_solutions + " solutions");
		return done;
	}


	public boolean computeNextSolution()
	{
		for (int y = 1; y < h; y++)
		{
			for (int x = w - 1; x >= 0; x--)
			{
				Pixel p = currentSolution.getPixel(x, y);
				if (p.support_dir != -2)
				{
					p.support_dir++;
					if (x + p.support_dir <= Math.min(w - 1, x + 1))
					{
						//System.out.println("next solution at "+x+", "+y);
						solveFrom(x, y);
						//System.out.println("after solve");
						//currentSolution.outputSupportDir();
						return false;
					}
				}
			}
		}
		return true;
	}

	private int computeScore() {
		int ret = 0;
		for (int y = h - 1; y >= 0; y--)
			for (int x = 0; x < w; x++)
				if (currentSolution.getPixel(x, y).type == PixelType.Support)
					ret++;
		return ret;
	}

	public Grid getCurrentSolution()
	{
		return currentSolution;
	}
	public Grid getBestSolution()
	{
		return bestSolution;
	}
}

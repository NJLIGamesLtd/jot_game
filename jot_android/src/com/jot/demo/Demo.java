/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 */

package com.jot.demo;

import com.jot.crossword.*;
import com.jot.decomp.*;
import com.jot.simulation.*;
import com.jot.trace.Trace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.awt.*;

public class Demo extends Simulation
{
	private Game m_game;
	private int m_width;
	private int m_height;

	private int m_nForceExit = 10000;
	
	/***********************************************************************************************
	 * Runnable init method override.
	 */
	public void init()
	{
		Trace.startOf("Demo.init()");

		resize(800, 800);

		m_width = getSize().width;
		m_height = getSize().height;

		Trace.endOf("Demo.init()");
	}

	/***********************************************************************************************
	 * Demo initializer.
	 */
	public void initialize() throws Exception
	{
		Trace.startOf("Demo.initialize()");

		try
		{
			m_game = new Jot(null);
		}
		catch (Exception e)
		{
			Trace.fatal("** Demo **: failed to create game: " + e);
		}
		
		Trace.endOf("Demo.initialize()");
	}
	
	/***********************************************************************************************
	 * Default Demo constructor.
	 */
	public Demo() throws Exception
	{
		super((long)((24 * 40 * 365.25) /* 40 years */ + (6 * 60 * 60 * 1000) /* for 6am */));

		initialize();
	}

	/***********************************************************************************************
	 * Demo constructor that requires a starting time.
	 *
	 * @param nTimeStart
	 *		Initial time setting to base the start of the simulation.
	 */
	public Demo(long nTimeStart) throws Exception
	{
		super(nTimeStart);

		initialize();
	}
	
	/***********************************************************************************************
	 * Destructor.
	 */
	public void destroy()
	{
		Trace.critical("Demo.destroy()");
	}

	/***********************************************************************************************
	 * Paint the graphical representation of the current time state of the simulation.
	 *
	 * @param g
	 *		Graphics object needed for outputting visual simulation.
	 */
	public void paint(Graphics g)
	{
		Trace.trivial("Demo.paint()");
		Image image = m_game.getImage();
		g.drawImage(image, 0, 0, m_width, m_height,
			0, 0, image.getWidth(null), image.getHeight(null), null);
	}
	
	/***********************************************************************************************
	 * Time interval based update method for moving the environment a number of steps forward.
	 */
	public void update() throws Exception
	{
		Trace.startOf("Demo.update()");

		// If requested, cancel out of the simulation.
		if (m_nForceExit-- <= 0)
		{
			Trace.critical("Demo.update(): force exit.");
			System.exit(0);
			return;
		}

		// Try to begin a new update. If we're already in the middle of an update, skip over.
		if (updateBegin())
		{
			// Run an update of the game.
			try
			{
				m_game.update(this);
			}
			catch (Exception e)
			{
				Trace.fatal("** Demo **:" + e);
			}
			updateEnd();
		}

		Trace.endOf("Demo.update()");
	}
}

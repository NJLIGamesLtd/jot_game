/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * JotSim is a Simulation that simulates a city environment with at least one JotTower.
 */

package com.jot.jottower;

import com.jot.item.*;
import com.jot.simulation.*;
import com.jot.trace.Trace;
import com.jot.vector.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.awt.*;

public class JotSim extends Simulation
{
	private static final long serialVersionUID = 1L;
	private static int JOT_POPULATION = 10000;
	private static int JOT_TOWERS = 1;
	
	private int m_forceExit = 10000;

	private City m_city;
	private List<Structure> m_towers;
	private Queue<Drone> m_persons;
	private int m_width;
	private int m_height;
	
	/***********************************************************************************************
	 * Runnable init method override.
	 */
	public void init()
	{
		Trace.startOf("JotSim.init()");

		resize(800, 800);

		m_width = getSize().width;
		m_height = getSize().height;

		Trace.endOf("JotSim.init()");
	}

	/***********************************************************************************************
	 * JotSim initializer.
	 */
	public void initialize() throws Exception
	{
		Trace.startOf("JotSim.initialize()");

		try
		{
			m_towers = new ArrayList<Structure>();
			m_persons = new ArrayDeque<Drone>();
			for (int i = 0; i < JOT_POPULATION; ++i)
			{
				m_persons.add(new Person(new Position(0, 0, 0, new Orientation(0)), null));
			}

			for (int i = 0; i < JOT_TOWERS; ++i)
			{
				m_towers.add(new JotTower(new Position(100000, 0, 0, new Orientation(0))));
			}
			
			m_city = new City(m_persons, m_towers, this);	// As a test, create a city of 100K people working in 1 tower.
		}
		catch (Exception e)
		{
			Trace.fatal("** JotSim **: failed to create city: " + e);
		}
		
		Trace.endOf("JotSim.initialize()");
	}
	
	/***********************************************************************************************
	 * Default JotSim constructor.
	 */
	public JotSim() throws Exception
	{
		super((long)((24 * 40 * 365.25) /* 40 years */ + (6 * 60 * 60 * 1000) /* for 6am */));

		initialize();
	}

	/***********************************************************************************************
	 * JotSim constructor that requires a starting time.
	 *
	 * @param nTimeStart
	 *		Initial time setting to base the start of the simulation.
	 */
	public JotSim(long nTimeStart) throws Exception
	{
		super(nTimeStart);

		initialize();
	}
	
	/***********************************************************************************************
	 * Destructor.
	 */
	public void destroy()
	{
		Trace.critical("JotSim.destroy()");
	}

	/***********************************************************************************************
	 * Paint the graphical representation of the current time state of the simulation.
	 *
	 * @param g
	 *		Graphics object needed for outputting visual simulation.
	 */
	public void paint(Graphics g)
	{
		Trace.trivial("JotSim.paint()");
		Image image = m_city.getImage();
		g.drawImage(image, 0, 0, m_width, m_height,
			0, 0, image.getWidth(null), image.getHeight(null), null);
	}
	
	/***********************************************************************************************
	 * Time interval based update method for moving the environment a number of steps forward.
	 */
	public void update() throws Exception
	{
		Trace.startOf("JotSim.update()");

		// If requested, cancel out of the simulation.
		if (m_forceExit-- <= 0)
		{
			Trace.critical("JotSim.update(): force exit.");
			System.exit(0);
			return;
		}

		// Try to begin a new update. If we're already in the middle of an update, skip over.
		if (updateBegin())
		{
			// Run an update of the city.
			try
			{
				m_city.update(this);
			}
			catch (Exception e)
			{
				Trace.fatal("** JotSim **:" + e);
			}
			updateEnd();
		}

		Trace.endOf("JotSim.update()");
	}
}

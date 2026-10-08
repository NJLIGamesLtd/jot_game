/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * City provides a timed simulation framework for people entering structures and transportation devices. 
 */

package com.jot.item;

import com.jot.simulation.*;
import com.jot.trace.Trace;
import com.jot.vector.*;

import java.util.EmptyStackException;
import java.util.List;
import java.util.Queue;
import java.awt.Image;

public class City extends StructureImpl
{
	private List<Structure> m_lTowers;
	private float[] m_aPersonsToWork;

	/***********************************************************************************************
	 * City constructor based on a collection of people and structures, plus a simulation
	 * to control and display updates.
	 *
	 * @param persons
	 *		A queue of computer directed Drone items to simulation people.
	 * @param towers
	 *	    A list of structures for the people to enter and exit.
	 * @param sim
	 *	    A Simulation object to control and display updates.
	 */
	public City(Queue<Drone> qPersons, List<Structure> lTowers, Simulation sim) throws Exception
	{		
		super(0, new Size(), new Position());
		
		Trace.startOf("City()");
		
		int nPersons = qPersons.size();
		m_qPersonsExiting = qPersons;
		m_lTowers = lTowers;

		// Based on rush-hour patterns, create a distribution for people to arrive at their
		// office tower for work.
		m_aPersonsToWork = new float[24];
		m_aPersonsToWork[6] = (float)(0.10 * nPersons * Cycles.MILLISECONDS_PER_CYCLE / (1000 * 60 * 60));
		m_aPersonsToWork[7] = (float)(0.25 * nPersons * Cycles.MILLISECONDS_PER_CYCLE / (1000 * 60 * 60));
		m_aPersonsToWork[8] = (float)(0.35 * nPersons * Cycles.MILLISECONDS_PER_CYCLE / (1000 * 60 * 60));
		m_aPersonsToWork[9] = (float)(0.25 * nPersons * Cycles.MILLISECONDS_PER_CYCLE / (1000 * 60 * 60));
		m_aPersonsToWork[10] = (float)(0.1 * nPersons * Cycles.MILLISECONDS_PER_CYCLE / (1000 * 60 * 60));

		Trace.endOf("City()");
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Image getImage()
	{
		return m_lTowers.get(0).getImage();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getMass()
	{
		int mass = 0;
		
		for (Structure tower : m_lTowers)
		{
			mass += tower.getMass();
		}
		return mass;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("City.update()");

		int nCycles = sim.getUpdateCyclesCount();
		long nTime = sim.getTime();

		// Use an hourly distribution that provides the likelihood that a
		// person is going to work in a given time interval.
		int hour = (int)((nTime / (1000 * 60 * 60)) % 24);
		float rateToWork = m_aPersonsToWork[hour];
		
		Trace.trivial("City.update(" + nTime + ") Hour = " + hour);
		
		// For each tower, allow people to come and go to work.
		for (Structure tower : m_lTowers)
		{
			Drone person;

			if (rateToWork > 0.0)
			{
				for (int i = 0; i < nCycles; ++i)
				{
					if (1 + rateToWork > Math.random())
					{
						try
						{
							if ((person = m_qPersonsExiting.poll()) != null)
							{
								person.pushDestination(tower);
								person.update(sim);
							}
						}
						catch (EmptyStackException e) {}
					}
				}
			}
			
			// Check to see if there are people trying to leave the tower.
			while ((person = tower.poll()) != null)
			{
				m_qPersonsExiting.add(person);
				person.setLocation(new Location(0, 0, 0)); // For now, everyone lives here.
			}
			tower.update(sim);
		}

		Trace.endOf("City.update()");
	}
}

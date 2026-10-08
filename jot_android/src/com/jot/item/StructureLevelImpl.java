/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * An implentation of StructureLevel, which is a Place within a single level of a Structure.
 */

package com.jot.item;

import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.*;

import java.util.ArrayDeque;
import java.util.EmptyStackException;
import java.util.Queue;

public class StructureLevelImpl extends PlaceImpl implements StructureLevel
{
	protected Queue<Drone> m_qPersonsExiting;

	/***********************************************************************************************
	 * StructureLevel constructor based on its mass, size and position.
	 *
	 * @param mass
	 *	    The mass of the structure level.
	 * @param size
	 *	    The vector size of the structure level.
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 */
	public StructureLevelImpl(int mass, Size size, Position position) throws Exception
	{
		super(mass, size, position);
		m_qPersonsExiting = new ArrayDeque<Drone>();
	}

	/***********************************************************************************************
	 * Add a drone to for testing purposees.
	 */
	public void add(Drone drone)
	{
		m_qPersonsExiting.add(drone);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Drone poll()
	{
		try
		{
			return m_qPersonsExiting.poll();
		}
		catch (EmptyStackException e)
		{
			return null;
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setLiftConnections() throws Exception
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.trivial("StructureLevel.update();");	
	}
}

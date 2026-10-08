/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * An SpinnableItem is a stationary item that can spin based on an angular velocity.
 * A good example of this is merry-go-round.
 */

package com.jot.item;

import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;
import com.jot.vector.*;

public class SpinnableImpl extends PlaceImpl implements Spinnable
{
	protected Cycles m_cyclesStage;
	protected Cycles m_cyclesAngular;
	protected int m_velocityAngular;
	protected int m_originalHeading;

	/***********************************************************************************************
	 * Spinnable item constructor based on its mass, size and position.
	 *
	 * @param mass
	 *		The kg mass of this item.
	 * @param size
	 *	    The vector size of this item.
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 */
	public SpinnableImpl(int mass, Size size, Position position) throws Exception
	{
		super(mass, size, position);

		m_cyclesStage = new Cycles(0);
		m_cyclesAngular = new Cycles(0);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void addHeading(int delta)
	{
		m_cyclesAngular.set(0);
		m_position.addHeading(delta);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void directOrientation(Cycles cyclesAngular)
	{
		int nCycles = cyclesAngular.getInt();
		if (nCycles > 1)
		{
			Orientation orientation = m_position.getOrientation();
			m_velocityAngular = 360 / nCycles;
			m_originalHeading = orientation.m_z;
			m_cyclesStage.set(0);
			m_cyclesAngular.set(nCycles);
		}
	}
	
	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setOrientation(final Orientation orientation)
	{
		m_cyclesAngular.set(0);
		m_position.setOrientation(orientation);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		int nCyclesAngular = m_cyclesAngular.getInt();
		if (nCyclesAngular > 0)
		{
			Cycles cyclesUpdate = sim.getUpdateCycles();
			m_cyclesStage.incrementToMaxLoop(cyclesUpdate, m_cyclesAngular);
			m_position.setHeading(m_originalHeading + m_velocityAngular * m_cyclesStage.getInt());
		}
	}
}

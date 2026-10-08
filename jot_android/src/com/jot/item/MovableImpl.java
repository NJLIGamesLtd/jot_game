/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Movable item is a Spinnable item (change in orientation) that can move.
 */

package com.jot.item;

import com.jot.simulation.Simulation;
import com.jot.vector.*;

public class MovableImpl extends SpinnableImpl implements Movable
{
	public Velocity m_velocity;
	public Location m_locFinal;
	protected Orientation m_orientFinal;
	
	public MovableImpl(int mass, Size size, Position position) throws Exception
	{
		super(mass, size, position);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void addHeading(int deltaHeading, Location locOrigin, int distOrigin)
	{
		addHeading(deltaHeading);
		double heading = m_position.getHeadingInRadians();
		m_position.m_x = (int)(locOrigin.m_x + distOrigin * Math.sin(heading));
		m_position.m_y = (int)(locOrigin.m_y + distOrigin * Math.cos(heading));
		m_position.m_z = locOrigin.m_z;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void addLocation(final Location delta)
	{
		m_locFinal = null;
		m_position.add(delta);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void directLocation(int velocity, Location locFinal) throws Exception
	{
		m_velocity = new Velocity(velocity, m_position, locFinal);
		m_locFinal = locFinal;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void directLocation(Velocity velocity, Location locFinal) throws Exception
	{
		if (velocity == null || locFinal == null)
		{
			throw new Exception("MoveableItem.directLocation() requires a velocity and a location we are moving towards.");
		}
		m_velocity = velocity;
		m_locFinal = locFinal;
	}
	
	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void directOrientation(Orientation orientFinal)
	{
		m_orientFinal = orientFinal;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setOrientation(final Orientation orientation)
	{
		m_orientFinal = null;
		super.setOrientation(orientation);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		if (m_locFinal != null)
		{
			int nCycles = sim.getUpdateCyclesCount();

			m_position.add(m_velocity, nCycles);
			if ((m_velocity.m_x > 0 && m_position.m_x > m_locFinal.m_x) ||
				(m_velocity.m_x < 0 && m_position.m_x < m_locFinal.m_x) ||
				(m_velocity.m_y > 0 && m_position.m_y > m_locFinal.m_y) ||
				(m_velocity.m_y < 0 && m_position.m_y < m_locFinal.m_y) ||
				(m_velocity.m_z > 0 && m_position.m_z > m_locFinal.m_z) ||
				(m_velocity.m_z < 0 && m_position.m_z < m_locFinal.m_z))
			{
				m_position.m_x = m_locFinal.m_x;
				m_position.m_y = m_locFinal.m_y;
				m_position.m_z = m_locFinal.m_z;
				m_locFinal = null;
				m_velocity = null;
			}
		}
		else if (m_orientFinal != null)
		{
			Orientation orientation = m_position.getOrientation();
			int delta;
			
			orientation.m_x = m_orientFinal.m_x;
			orientation.m_y = m_orientFinal.m_y;
			if ((delta = orientation.m_z - m_orientFinal.m_z) < 0)
			{
				delta = -delta;
			}
			if (delta > 180)
			{
				delta = -15;
			}
			else if (delta >= 15)
			{
				delta = 15;
			}
			else
			{
				delta = 0;
			}
			orientation.m_z += delta;
			if (delta == 0)
			{
				orientation.m_z = m_orientFinal.m_z;
				m_orientFinal = null;
			}
		}
	}
}

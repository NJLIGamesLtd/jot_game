/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A train CarWay is a TrackCar that can be part of a line of cars.
 */

package com.jot.track;

import com.jot.trace.Trace;
import com.jot.vector.*;

public class CarWay extends CarReturn
{
	/***********************************************************************************************
	 * CarWay constructor based on its mass, size.
	 *
	 * @param mass
	 *		The kg mass of this car.
	 * @param size
	 *	    The vector size of this car.
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param nMaxVelocity
	 *	    The maximum number of steps a Car can take on the Track in one Tick.
	 */
	public CarWay(
		int mass,
		Size size,
		Position position,
		int nMaxVelocity
	) throws Exception
	{
		super(mass, size, position, nMaxVelocity);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean canMove(boolean recurse) throws Exception
	{
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean getDirection()
	{
		return true;
	}

	static int nMove = 0;
	
	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void move() throws Exception
	{
		if (++nMove == 295)
		{
			nMove = 295;
		}
		int nTicks;

		// Try to move this Car forward. If needed, allow it to decelerate by one.
		nTicks = m_nVelocity;
		while (--nTicks >= 0)
		{
			if (moveToNext())
			{
				if (collides(m_carLeader))
				{
					moveToPrevious();
				}
				else if (!m_position.equals(m_posDest))
				{
					continue;
				}
			}
			else
			{
				// We're at the end of our track, so our leader can no longer be present.
				m_carLeader = null;
			}
			if (--m_nVelocity > 0)
			{
				Trace.fatal("Car would have caused a collision.");
			}
			return;
		}

		// See if we need to maintain and want to accelerate our velocity.
		int nNeed = Section.s_aNumVelSteps[m_nVelocity];
		int nWant = Section.s_aNumVelSteps[m_nVelocity + 1];
		if (m_nVelocity >= m_nMaxVelocity)
		{
			nWant = 0;
		}

		// Loop through to see how many open sections we have.
		Section next = ((Section)m_position).getNext();
		while (next != null)
		{
			// If we're getting close to colliding with the leader, slow down.
			if (m_carLeader != null && m_carLeader.collides(next))
			{
				break;
			}

			// If we have the number of sections that we want to accelerate, do so.
			if (--nWant == 0)
			{
				++m_nVelocity;
				return;
			}

			// If we can't accelerate and have what we need to maintain this speed, break out.
			if (--nNeed <= 0 && nWant <= 0)
			{
				break;
			}

			// Don't overreach our destination.
			if (next.equals(m_posDest))
			{
				break;
			}
			next = next.getNext();
		}

		// If our velocity is too fast, decelerate.
		if (nNeed > 0)
		{
			--m_nVelocity;
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setDirection(boolean bMoveNext)
	{
	}
}

/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A CarReturn is TrackCar implementation which rides back and forth on a TrackReturn.
 */

package com.jot.track;

import java.util.List;

import com.jot.item.DroneImpl;
import com.jot.rollavator.RollaCar;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.*;

public class CarReturn extends DroneImpl implements TrackCar
{
	private static int s_nCar = 0;

	protected int m_nVelocity;		// Velocity of track motion based on steps forward or back.
	protected int m_nMaxVelocity;	// Maximum number of steps we can move forward/back per tick.
	protected Position m_posDest;	// Attempting to reach this position.
	protected TrackCar m_carLeader;
	protected int m_idCar;
	public boolean m_bMoveNext;	// True if we're trying to move forward.

	private long m_distSafe;		// Safe distance (squared) between this and the next LoopBox.

	/***********************************************************************************************
	 * CarReturn constructor based on its mass, size.
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
	public CarReturn(
		int mass,
		Size size,
		Position position,
		int nMaxVelocity
	) throws Exception
	{
		super(mass, size, position);
		m_idCar = ++s_nCar;

		// To avoid collisions between the cars, record a safe distance for them to stay.
		m_distSafe = (m_size.m_x * m_size.m_x) + (m_size.m_y * m_size.m_y);

		// Let the parent determine the maximum velocity.
		m_nMaxVelocity = nMaxVelocity;

		// By default, this Car is not moving, but is preparing to move forward.
		m_bMoveNext = true;
		m_nVelocity = 0;

		// By Default, we have no leader.
		m_carLeader = null;

		// No destination by default.
		m_posDest = null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	static int nSteps = 0;
	
	public boolean canMove(boolean recurse) throws Exception
	{
		if (m_idCar == 621)
		{
			++nSteps;
			if (!m_bMoveNext)
			{
				m_bMoveNext = false;
			}
			else
			{
				m_bMoveNext = true;
			}
		}
		
		
		if (m_bMoveNext)
		{
			return isNext();
		}
		return isPrevious();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean collides(Position position)
	{
		long x = m_position.m_x - position.m_x;
		long y = m_position.m_y - position.m_y;
		return ((x * x) + (y * y) < m_distSafe);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean collides(TrackCar car)
	{
		if (car != null)
		{
			return collides(car.getPosition());
		}
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Position getDestination()
	{
		return m_posDest;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean getDirection()
	{
		return m_bMoveNext;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public TrackCar getLeader()
	{
		return m_carLeader;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getQuadrantId()
	{
		return ((Section)m_position).getQuadrantId();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Section getSection()
	{
		return (Section)m_position;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getTickVelocity()
	{
		return m_nVelocity;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean isNext()
	{
		Section next = ((Section)m_position).getNext();
		return (next != null);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean isPrevious()
	{
		Section prev = ((Section)m_position).getPrevious();
		return (prev != null);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void move() throws Exception
	{
		int nTicks;

		// If requested to move forward, try to accelerate in this direction.
		if (m_bMoveNext)
		{
			// If we are trying to change direction, reduce our speed.
			if (m_nVelocity < 0)
			{
				if (++m_nVelocity < 0)
				{
					nTicks = - m_nVelocity;
					do
					{
						if (!moveToPrevious())
						{
							Trace.fatal("Car would have caused a collision.");
							throw new Exception("Car would have caused a collision.");
						}
					} while (--nTicks > 0);
				}
			}
			else
			{
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
						throw new Exception("Car would have caused a collision.");
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
		}
		else
		{
			// If we are trying to change direction, reduce our speed.
			if (m_nVelocity > 0)
			{
				if (--m_nVelocity > 0)
				{
					nTicks = m_nVelocity;
					do
					{
						if (!moveToNext())
						{
							Trace.fatal("Car would have caused a collision.");
							throw new Exception("Car would have caused a collision.");
						}
					} while (--nTicks > 0);
				}
			}
			else
			{
				// Try to move this Car backward. If needed, allow it to decelerate by one.
				nTicks = - m_nVelocity;
				while (--nTicks >= 0)
				{
					if (moveToPrevious())
					{
						if (collides(m_carLeader))
						{
							moveToNext();
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
					if (++m_nVelocity < 0)
					{
						Trace.fatal("Car would have caused a collision.");
						throw new Exception("Car would have caused a collision.");
					}
					return;
				}

				// See if we need to maintain and want to accelerate our velocity.
				int nNeed = Section.s_aNumVelSteps[-m_nVelocity];
				int nWant = Section.s_aNumVelSteps[-m_nVelocity + 1];
				if (-m_nVelocity >= m_nMaxVelocity)
				{
					nWant = 0;
				}

				// Loop through to see how many open sections we have.
				Section prev = ((Section)m_position).getPrevious();
				while (prev != null)
				{
					// If we have the number of sections that we want to accelerate, do so.
					if (--nWant == 0)
					{
						--m_nVelocity;
						return;
					}

					// If we can't accelerate and have what we need to maintain this speed, break out.
					if (--nNeed <= 0 && nWant <= 0)
					{
						break;
					}

					// Don't overreach our destination.
					if (prev.equals(m_posDest))
					{
						break;
					}
					prev = prev.getPrevious();
				}

				// If our velocity is too fast, decelerate.
				if (nNeed > 0)
				{
					++m_nVelocity;
				}
			}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean moveToNext()
	{
		Section next = ((Section)m_position).getNext();

		if (next == null)
		{
			return false;
		}
		m_position = next;
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean moveToPrevious()
	{
		Section prev = ((Section)m_position).getPrevious();

		if (prev == null)
		{
			return false;
		}
		m_position = prev;
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setDestination(Position posDestination)
	{
		m_posDest = posDestination;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setDirection(boolean bMoveNext)
	{
		m_bMoveNext = bMoveNext;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setLeader(TrackCar car)
	{
		m_carLeader = car;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setSection(Section section)
	{
		m_position = section;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return "CarReturn #" + m_idCar;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("CarReturn.update()");

		Trace.endOf("CarReturn.update()");
	}
}

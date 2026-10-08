/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator BoxReturn is a track CarReturn that contains an inner Track for the purpose of
 * holding and transferring a lift Car.
 */

package com.jot.rollavator;

import java.util.ArrayDeque;
import java.util.Queue;

import com.jot.item.Place;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.CarReturn;
import com.jot.track.Return;
import com.jot.track.Section;
import com.jot.track.Track;
import com.jot.track.TrackCar;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

public class BoxReturn extends Return implements Box
{
	protected static final int s_massBox = 1000;
    protected static final int s_massTrack = 100;
	protected static final int s_nMaxVelocity = 10;
	protected static final Size s_size = new Size(1700, 2000, 3000);
	
	private static final Orientation s_orientFixed = new Orientation(0);
	private static final Position[] s_aPosTrack =
	{
		new Position(-1700, 0, 0, s_orientFixed),
		new Position(-1650, 0, 0, s_orientFixed),
		new Position(-1600, 0, 0, s_orientFixed),
		new Position(-1550, 0, 0, s_orientFixed),
		new Position(-1500, 0, 0, s_orientFixed),
		new Position(-1450, 0, 0, s_orientFixed),
		new Position(-1400, 0, 0, s_orientFixed),
		new Position(-1350, 0, 0, s_orientFixed),
		new Position(-1300, 0, 0, s_orientFixed),
		new Position(-1250, 0, 0, s_orientFixed),
		new Position(-1200, 0, 0, s_orientFixed),
		new Position(-1150, 0, 0, s_orientFixed),
		new Position(-1100, 0, 0, s_orientFixed),
		new Position(-1050, 0, 0, s_orientFixed),
		new Position(-1000, 0, 0, s_orientFixed),
		new Position( -950, 0, 0, s_orientFixed),
		new Position( -900, 0, 0, s_orientFixed),
		new Position( -850, 0, 0, s_orientFixed),
		new Position( -800, 0, 0, s_orientFixed),
		new Position( -750, 0, 0, s_orientFixed),
		new Position( -700, 0, 0, s_orientFixed),
		new Position( -650, 0, 0, s_orientFixed),
		new Position( -600, 0, 0, s_orientFixed),
		new Position( -550, 0, 0, s_orientFixed),
		new Position( -500, 0, 0, s_orientFixed),
		new Position( -450, 0, 0, s_orientFixed),
		new Position( -400, 0, 0, s_orientFixed),
		new Position( -350, 0, 0, s_orientFixed),
		new Position( -300, 0, 0, s_orientFixed),
		new Position( -250, 0, 0, s_orientFixed),
		new Position( -200, 0, 0, s_orientFixed),
		new Position( -150, 0, 0, s_orientFixed),
		new Position( -100, 0, 0, s_orientFixed),
		new Position(  -50, 0, 0, s_orientFixed),
		new Position(    0, 0, 0, s_orientFixed),
	};

	public boolean m_bMoveNext;

	protected int m_zPosDefault;
	protected int m_zPosDestination;
	protected Queue<Integer> m_qzPosDestinations;

	private Box m_boxSpouse;		// Married to this Box to transfer a Car.
	private int m_idQuadLoaded;		// The Section Quadrant ID where our Car is fully loaded.
	private int m_nVelocity;		// Velocity of track motion based on steps forward or back.
	private LiftReturn m_liftParent;// The LiftReturn this is the child of.
	private Rollavator m_rollavator;
	private int m_iLane;

	/***********************************************************************************************
	 * BoxReturn constructor based on its position.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param rollavator
	 *	    The Rollavator this is owned by.
	 * @param liftParent
	 *	    The LiftReturn this is the child of.
	 */
	public BoxReturn(
		Position position,
		Rollavator rollavator,
		LiftReturn liftParent,
		int iLane
	) throws Exception
	{
		super(s_massBox, s_size, position, s_aPosTrack);

		m_rollavator = rollavator;
		m_liftParent = liftParent;
		m_iLane = iLane;

		// Mark the last section as a new quadrant to indicate when the car is full loaded.
		markQuadrant(0);
		m_idQuadLoaded = markQuadrant(s_aPosTrack.length - 1);

		// By default, we are not married to another box.
		m_boxSpouse = null;

		// By Default, we are moving towards the next track section.
		m_bMoveNext = true;

		// Internal velocity, by default, is zero.
		m_nVelocity = 0;

		// Stay at the bottom level unless directed else where.
		m_zPosDefault = m_zPosDestination = position.m_z;

		// Need a queue of destinations so we know to pick up waiting cars.
		m_qzPosDestinations = new ArrayDeque<Integer>();
	}

	/***********************************************************************************************
	 * Return the paired twin to this BoxLoop.
	 *
	 * @return
	 *	    The BoxLoop this is paired with.
	 */
	public boolean atDestination()
	{
		return m_position.m_z == m_zPosDestination;
	}

	/***********************************************************************************************
	 * Return true if the box can be moved.
	 *
	 * @param bRecurse
	 *		If true, recurse to check the twin as well.
	 * @return
	 *	    True if the box can move.
	 */
	public boolean canMove(boolean bRecurse) throws Exception
	{
		do
		{
			// Can't move if we have a stationary spouse.
			if (m_boxSpouse != null)
			{
				if (!bRecurse || !m_boxSpouse.canMove(false))
				{
					break;
				}
			}

			// Check to see if our car has reached its floor group.
			if (!atDestination())
			{
				return true;
			}
		} while (false);

		// If we can't move, make sure we have no velocity.
		if (m_nVelocity > 0)
		{
			if (--m_nVelocity != 0)
			{
				Trace.fatal("BoxLoop halted from moving when at speed.");
				throw new Exception("BoxLoop halted from moving when at speed.");
			}
		}
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void checkDestination() throws Exception
	{
		if (m_idItem == 5300)
		{
			m_idItem = 5300;
		}
		
		// If we're at our destination and we have yet to spouse up with a BoxPort,
		// see if it is ready for us to do so.
		if (m_boxSpouse == null && atDestination())
		{
			// Get the port at this location.
			BoxPort boxPort = m_rollavator.getBoxPort(m_position.m_z / 4000, m_iLane);
			if (boxPort != null)
			{
				// If we're trying to retrieve a car, make sure the port's car is in reverse.
				if (m_car == null)
				{
					RollaCar car = (RollaCar)boxPort.getTrackCar();
					if (car != null && !car.getDirection())
					{
						Trace.critical("BoxReturn (" + this + ") married up to BoxPort");

						setSpouse(boxPort);
						boxPort.setSpouse(this);

						// Try to grab ownership of the car.
						if (offer(car))
						{
							boxPort.removeTrackCar(car);
						}
					}
				}
				else if (m_car.getDirection())
				{
					Trace.critical("BoxReturn (" + this + ") married up to BoxPort");

					setSpouse(boxPort);
					boxPort.setSpouse(this);

					// Try to offer ownership of the Car over to the BoxReturn.
					if (boxPort.offer(m_car))
					{
						removeTrackCar(m_car);
					}
				}
			}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean collides(Position position)
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean collides(TrackCar car)
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getBaseLevel()
	{
		return m_position.m_z;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Position getDestination()
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean getDirection()
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getFloorGroupId()
	{
		return 0;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public TrackCar getLeader()
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Place getPlace()
	{
		return null;
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
	public Box getSpouse()
	{
		return m_boxSpouse;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getTickVelocity()
	{
		return 0;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Track getTrack()
	{
		return this;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public TrackCar getTrackCar(LiftLoop.PosEnum eDest)
	{
		if (m_car != null && !m_bMoveNext)
		{
			switch (eDest)
			{
				case UP_LEVEL_STACK:
					if (getFloorGroupId() > 0)
					{
						return m_car;
					}
					break;
				case DOWN_LEVEL_STACK:
					if (getFloorGroupId() == 0)
					{
						return m_car;
					}
					break;
				default:
					break;
			}
		}
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean hasDestination(int level)
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean isLoaded()
	{
		if (getTrackCar() != null)
		{
			return true;
		}
		return false;
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
						if (!atDestination())
						{
							continue;
						}
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
				if (m_nVelocity >= s_nMaxVelocity)
				{
					nWant = 0;
				}

				// Loop through to see how many open sections we have.
				Section next = ((Section)m_position).getNext();
				while (next != null)
				{
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
					if (next.m_z == m_zPosDestination)
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
						if (!atDestination())
						{
							continue;
						}
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
				if (-m_nVelocity >= s_nMaxVelocity)
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
					if (prev.m_z == m_zPosDestination)
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
	public boolean offer(TrackCar car) throws Exception
	{
		Trace.important("BoxReturn.offer(): Car (" + car + ") directed towards this.");

		// Make sure we have a track spouse that is married back to us.
		// SPK: If this is a Carousel to Loop track Box, then there could be two spouses.
		if (m_boxSpouse == null ||
			m_boxSpouse.getSpouse() != this)
		{
			return false;
		}

		// Make sure the Z axis positions are identical.
		if (getBaseLevel() != m_boxSpouse.getBaseLevel())
		{
			m_boxSpouse.setSpouse(null);
			setSpouse(null);
			return false;
		}

		// Assign our destination to the next floor we need to stop at.
		RollaCar rollacar = (RollaCar)car;
		int iFloor = rollacar.getNextFloorID(m_bMoveNext);
		if (iFloor <= 0)
		{
			iFloor = 0;
		}
		else
		{
			iFloor -= m_liftParent.getBaseLevel();
		}
		if ((iFloor /= 4000) >= m_rollavator.getLevelsPerStack())
		{
			Trace.fatal("BoxReturn.offer(): Car (" + car + ") requesting floor not on this stack.");
		}
		m_zPosDestination =
			m_liftParent.getSection(iFloor * m_rollavator.getTrackSectionsPerFloor()).getZ();

		// Try to offer the Car to our Track.
		m_car = car;

		// Assign us as the parent of the car.
		rollacar.setParent(this);

		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Place peekDestination()
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Place popDestination() throws Exception
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void pushDestination(Place destination) throws Exception
	{
		m_qzPosDestinations.add(destination.getPosition().getZ());
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setDestination(Position posDestination)
	{
		m_zPosDestination = posDestination.getZ();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setDirection(boolean bMoveNext)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setFloorGroupId(int idFloorGroup)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setLeader(TrackCar car)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setPlace(Place place)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Section getSection()
	{
		if (m_car != null)
		{
			return m_car.getSection();
		}
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setSpouse(Box spouse) throws Exception
	{
		if (spouse == null)
		{
			getSectionFirst().setPrevious(null);
			getSectionLast().setNext(null);
		}
		else if (spouse.getClass() == BoxLoop.class)
		{
			getSectionFirst().setPrevious(spouse.getSectionLast());
		}
		else
		{
			getSectionLast().setNext(spouse.getSectionFirst());
		}
		m_boxSpouse = spouse;
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
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("BoxReturn.update()");
		
		if (m_idItem == 5300)
		{
			m_idItem = 5300;
		}

		// Avoid redundant runs of the same update.
		long nTime = sim.getTime();
		if (nTime == m_nTime)
		{
			Trace.endOf("Track (" + this + ") already updated.");
			return;
		}
		m_nTime = nTime;

		// If we have a spouse, keep our updates in sync.
		if (m_boxSpouse != null)
		{
			m_boxSpouse.update(sim);
		}
		else if (
			m_car == null &&
			atDestination() &&
			!m_qzPosDestinations.isEmpty())
		{
			m_zPosDestination = m_qzPosDestinations.poll();
		}

		int nCycles = sim.getUpdateCyclesCount();

		// Try to move this box.
		for (int i = 0; i < nCycles; ++i)
		{
			if (canMove(false))
			{
				move();
			}

			// If we reached a destination, then act on it.
			checkDestination();
		}

		// If we have one, update and move our car.
		if (m_car != null)
		{
			boolean bClosed = ((RollaCar)m_car).isClosed();

			// Run an update internal to the car.
			m_car.update(sim);

			// If the door is closed, see if we have other updates.
			if (bClosed)
			{
				for (int i = 0; i < nCycles; ++i)
				{
					// Try to move the car.
					if (m_boxSpouse != null)
					{
						if (m_car.canMove(false))
						{
							m_car.move();
						}
						else
						{
							Box spouse = m_boxSpouse;
							setSpouse(null);
							spouse.setSpouse(null);

							RollaCar rollaCar = (RollaCar)m_car;
							int iFloor = rollaCar.getNextFloorID(m_bMoveNext);
							if (iFloor >= 0)
							{
								if (iFloor < m_position.m_z)
								{
									m_bMoveNext = false;
								}
								else
								{
									m_bMoveNext = true;
								}
								if (iFloor == 0)
								{
									rollaCar.setDirection(false);
								}
								else
								{
									rollaCar.setDirection(true);
									m_qzPosDestinations.add(iFloor);
								}
							}
						}
					}
				}
			}
		}
		else if (
			m_boxSpouse == null &&
			atDestination() &&
			m_position.m_z != m_zPosDefault &&
			m_qzPosDestinations.isEmpty())
		{
			m_zPosDestination = m_zPosDefault;
		}

		Trace.endOf("BoxReturn.update()");
	}
}

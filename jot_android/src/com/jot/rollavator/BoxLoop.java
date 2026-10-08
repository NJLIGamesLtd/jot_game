/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator BoxLoop is a Track that can transfer a rollavator Car between itself and another
 * Box or Track, while implementing motion as a train Car within a Loop.
 */

package com.jot.rollavator;

import com.jot.item.Place;
import com.jot.rollavator.LiftLoop.PosEnum;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.CarWay;
import com.jot.track.Return;
import com.jot.track.Section;
import com.jot.track.Track;
import com.jot.track.TrackCar;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

public class BoxLoop extends Return implements Box
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

	protected Track m_track;	// Track within our Box that accepts and holds a Car.
	protected LiftLoop.PosEnum m_eDest;
	protected Position[] m_aPosDests;	// Attempting to reach these destinations to swap cars.
	protected Position m_posDest;

	private Box m_boxSpouse;	// Married to this Box to transfer a Car.
	private BoxLoop m_boxTwin;	// Box that inversely mirrors the movement of this box.
	private int m_idQuadLoaded;	// The Section Quadrant ID where our Car is fully loaded.
	private Rollavator m_rollavator;
	private int m_nVelocity;		// Velocity of track motion based on steps forward or back.
	private TrackCar m_carLeader;
	private int m_timePause;

	/***********************************************************************************************
	 * BoxLoop constructor based on its position.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param aPosDests
	 *		Destinations that we need to marry with other Box objects.
	 * @param rollavator
	 *	    The Rollavator this is owned by.
	 */
	public BoxLoop(
		Position position,
		Position[] aPosDests,
		Rollavator rollavator
	) throws Exception
	{
		super(s_massBox, s_size, position, s_aPosTrack);

		// Store a reference to the parent rollavator.
		m_rollavator = rollavator;

		// Mark the last section as a new quadrant to indicate when the car is fully loaded.
		markQuadrant(0);
		m_idQuadLoaded = markQuadrant(s_aPosTrack.length - 1);

		// The parent will need to assign our twin after construction.
		m_boxTwin = null;

		// By default, we are not married to another box.
		m_boxSpouse = null;

		// Moving towards this destination.
		m_aPosDests = aPosDests.clone();
		setDestination(LiftLoop.PosEnum.FROM_LOBBY, false, 0);

		// Internal velocity, by default, is zero.
		m_nVelocity = 0;

		// By default, we have no pause setting.
		m_timePause = 0;
	}

	/***********************************************************************************************
	 * Return the paired twin to this BoxLoop.
	 *
	 * @return
	 *	    The BoxLoop this is paired with.
	 */
	public boolean atDestination()
	{
		return m_position.equals(m_posDest);
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
			if (m_boxSpouse != null && !m_boxSpouse.canMove(false))
			{
				break;
			}

			// If this is the top level call, then we might need to recurse down to check our spouse
			// or twin.
			if (bRecurse)
			{
				// If our twin or spouse can not move, then we can not.
				if (!m_boxTwin.canMove(false))
				{
					break;
				}
			}

			// Check to see if our car has reached its floor group.
			if (!atDestination())
			{
				return true;
			}

			// If we have been stuck here too long without a spouse, move towards
			// the next destination.
			if (m_boxSpouse == null && m_boxTwin.m_boxSpouse == null)
			{
				if (m_timePause > 0 && --m_timePause == 0)
				{
					setDestination(m_eDest.getNext(), true, 0);
				}
			}
		} while (false);

		// If we can't move, make sure we have no velocity.
		if (m_nVelocity > 0)
		{
			if ((m_boxTwin.m_nVelocity = --m_nVelocity) != 0)
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
		if (atDestination())
		{
			Box pair = m_rollavator.getLoopPair(m_eDest, m_position);

			if (pair != null)
			{
				RollaCar car = (RollaCar)getTrackCar();
				Box box;
				switch (m_eDest)
				{
					case FROM_LOBBY:
					case FROM_SKY:
						if (car != null)
						{
						    setDestination(m_eDest.getNext(), true, 0);
						}
						else if (m_boxSpouse == null)
						{
							if (pair.getTrackCar() != null)
							{
								Trace.critical("BoxLoop (" + this + ") married up from Carousel");

								setSpouse(pair);
								pair.setSpouse(this);
								break;
							}
							pair = m_rollavator.getLoopPair(m_boxTwin.m_eDest, m_boxTwin.m_position);
							if (pair == null || pair.getTrackCar() == null)
							{
								setDestination(m_eDest.getNext(), true, 0);
							}
						}
						break;

					case TO_SKY:
					case TO_LOBBY:
						if (m_boxSpouse == null)
						{
							if (car != null)
							{
								Trace.critical("BoxLoop (" + this + ") married up to Carousel");

								setSpouse(pair);
								pair.setSpouse(this);
							}
							else
							{
							    setDestination(m_eDest.getNext(), true, 0);
							}
						}
						break;

					case UP_LEVEL_STACK:
					case DOWN_LEVEL_STACK:
						// If we're married, wait for the car to transfer.
						// Otherwise, check to see if we should marry the pair.
						if (m_boxSpouse == null)
						{
							// If we have a car, we need to pair up and get rid of it.
							if (car != null)
							{
								// We can only pair up if it's empty.
								if (pair.getTrackCar() == null)
								{
									Trace.critical("BoxLoop (" + this + ") married up from LevelStack");

									setSpouse(pair);
									pair.setSpouse(this);

									// Try to offer ownership of the Car over to the BoxReturn.
									if (m_boxSpouse.offer(car))
									{
										removeTrackCar(car);
									}
								}
							}
							else
							{
								BoxReturn boxReturn = (BoxReturn)pair;
								car = (RollaCar)boxReturn.getTrackCar(m_eDest);
								if (car != null && !car.getDirection())
								{
									Trace.critical("BoxLoop (" + this + ") married up to LevelStack");

									setSpouse(pair);
									pair.setSpouse(this);

									// Try to offer ownership of the Car over to the BoxReturn.
									if (offer(car))
									{
										pair.removeTrackCar(car);
									}
								}
							}

							// SPK: We have to wait for a stack level to accept our car
							// and we need to make sure the eDest is moved once we get
							// rid of our car.
						}
						break;

					default:
						break;
				}
			}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean collides(Position position)
	{
		int idQuadrant = ((Section)m_position).getQuadrantId();
		if (idQuadrant != 0 &&
			idQuadrant == ((Section)position).getQuadrantId())
		{
			return true;
		}

		int y = m_position.m_y - position.m_y;
		if (y >= s_size.m_y || -y >= s_size.m_y)
		{
			return false;
		}

		long z = m_position.m_z - position.m_z;
		if (z >= s_size.m_z || -z >= s_size.m_z)
		{
			return false;
		}

		return true;
	}

	/***********************************************************************************************
	 * Return true if box collides with this.
	 *
	 * @param box
	 *	    The box moving towards this.
	 * @return
	 *	    True if the box would collide with this box if allowed to move to its new location.
	 */
	public boolean collides(BoxLoop box)
	{
		return collides(box.getPosition());
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
	public Position getDestination()
	{
		return m_posDest;
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
		return m_carLeader;
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
	public Section getSection()
	{
		return (Section)m_position;
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
	 * Return the paired twin to this BoxLoop.
	 *
	 * @return
	 *	    The BoxLoop this is paired with.
	 */
	public BoxLoop getTwin()
	{
		return m_boxTwin;
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
		// If it can, try to move the box.
		if (canMove(true))
		{
			// Move both this and its twin.
			moveTwins();
		}

		// If we reached a destination, then act on it.
		checkDestination();
		m_boxTwin.checkDestination();
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
	static int nth = 0;

	public void moveTwins() throws Exception
	{
		int nTicks;

		// Try to move this Car forward. If needed, allow it to decelerate by one.
		nTicks = m_nVelocity;
		while (--nTicks >= 0)
		{
			// We're in a loop, so there's always a next track section.
			moveToNext();
			m_boxTwin.moveToNext();

			if (collides(m_carLeader))
			{
				moveToPrevious();
				m_boxTwin.moveToPrevious();
				((BoxLoop)m_carLeader).nudge(true);
			}
			else if (!atDestination()
				&&  !m_boxTwin.atDestination())
			{
				continue;
			}
			if (--m_nVelocity > 0)
			{
				Trace.fatal("Car would have caused a collision.");
			}
			m_boxTwin.m_nVelocity = m_nVelocity;
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
		Section nextTwin = ((Section)m_boxTwin.m_position).getNext();
		while (true)
		{
			// If we're getting close to colliding with the leader, slow down.
			// Don't have to check the twin's leader, because it has to be the
			// same distance from this BoxLoop.
			if (m_carLeader != null && m_carLeader.collides(next))
			{
				((BoxLoop)m_carLeader).nudge(true);
				break;
			}

			// If we have the number of sections that we want to accelerate, do so.
			if (--nWant == 0)
			{
				++m_nVelocity;
				m_boxTwin.m_nVelocity = m_nVelocity;
				return;
			}

			// If we can't accelerate and have what we need to maintain this speed, break out.
			if (--nNeed <= 0 && nWant <= 0)
			{
				break;
			}

			// Don't overreach our destination.
			if (next.equals(m_posDest) ||
				nextTwin.equals(m_boxTwin.m_posDest))
			{
				break;
			}
			next = next.getNext();
			nextTwin = nextTwin.getNext();
		}

		// If our velocity is too fast, decelerate.
		if (nNeed > 0 && m_nVelocity > 1)
		{
			--m_nVelocity;
		}
		m_boxTwin.m_nVelocity = m_nVelocity;
	}

	/***********************************************************************************************
	 * If there is a lead car in our way, call this function to see if we can get it moving.
	 *
	 * @param bRecurse
	 *		If true, recurse to check the twin as well.
	 */
	public void nudge(boolean bRecurse) throws Exception
	{
		// Make sure both this and its twin is nudged.
		if (bRecurse)
		{
			m_boxTwin.nudge(false);
		}

		// If we're at our destination, try to move towards the next.
		if (atDestination())
		{
			if (m_boxSpouse != null)
			{
				if (getTrackCar() == null && m_boxSpouse.getTrackCar() == null)
				{
					Box spouse = m_boxSpouse;
					setSpouse(null);
					spouse.setSpouse(null);
				}
			}
			if (m_boxSpouse == null && m_boxTwin.m_boxSpouse != null)
			{
				if (m_boxTwin.getTrackCar() == null &&
					m_boxTwin.m_boxSpouse.getTrackCar() == null)
				{
					Box spouse = m_boxTwin.m_boxSpouse;
					m_boxTwin.setSpouse(null);
					spouse.setSpouse(null);
				}
			}
			if (m_boxSpouse == null && m_boxTwin.m_boxSpouse == null)
			{
				setDestination(m_eDest.getNext(), true, 0);
			}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(TrackCar car) throws Exception
	{
		Trace.important("BoxLoop.offer(): Car (" + car + ") directed towards this.");

		// Make sure we have a track spouse that is married back to us.
		// SPK: If this is a Carousel to Loop track Box, then there could be two spouses.
		if (m_boxSpouse == null ||
			m_boxSpouse.getSpouse() != this)
		{
			return false;
		}

		// Make sure the Z axis positions are identical.  If not, break the marriage.
		if (getBaseLevel() != m_boxSpouse.getBaseLevel())
		{
			Box spouse = m_boxSpouse;
			setSpouse(null);
			spouse.setSpouse(null);
			return false;
		}

		// Try to offer the Car to our Track.
		m_car = car;

		// Assign us as the parent of the car.
		RollaCar rollacar = (RollaCar)car;
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
	}

	/***********************************************************************************************
	 * Set the BoxLoop destination based on a LiftLoop position enumeration and a level stack
	 * where we need to stop at.
	 *
	 * @param eDest
	 *		LiftLoop track destination enumeration.
	 * @param bRecurse
	 *		If true, recurse to check the twin as well.
	 * @param nLevelStack
	 *		Nth level stack (starting with 1), we need to stop at.
	 */
	public void setDestination(LiftLoop.PosEnum eDest, boolean bRecurse, int nLevelStack)
	{
		// Move towards this destination.
		m_eDest = eDest;

		// Get the position from the array that we want to head towards.
		int iPos = eDest.m_value;

		// If needed, update the destination for the level stack.
		if (nLevelStack > 0)
		{
			if (eDest == LiftLoop.PosEnum.UP_LEVEL_STACK ||
				eDest == LiftLoop.PosEnum.DOWN_LEVEL_STACK)
			{
				iPos += 2 * (nLevelStack - 1);
			}
		}
		m_posDest = m_aPosDests[iPos];

		// If we need to recurse down and set the destination of our twin, do so.
		if (bRecurse)
		{
			m_boxTwin.setDestination(eDest.getTwin(), false, nLevelStack);
		}
			
		// Set a counter to avoid being stuck at a destination for more than 10 seconds.
		if (m_eDest == PosEnum.DOWN_LEVEL_STACK || m_eDest == PosEnum.UP_LEVEL_STACK)
		{
			m_timePause = 10 * 10;
		}
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
	public void setSection(Section section)
	{
		m_position = section;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setSpouse(Box spouse) throws Exception
	{
		if (m_boxSpouse != null && spouse == null)
		{
			if (m_boxSpouse.getClass() == BoxWay.class)
			{
				getSectionFirst().setPrevious(null);
			}
			else
			{
				getSectionLast().setNext(null);
			}
		}
		else if (spouse != null)
		{
			if (spouse.getClass() == BoxWay.class)
			{
				getSectionFirst().setPrevious(spouse.getSectionLast());
			}
			else
			{
				getSectionLast().setNext(spouse.getSectionFirst());
			}
		}
		m_boxSpouse = spouse;
	}

	/***********************************************************************************************
	 * Assigns a paired twin to this BoxLoop.
	 *
	 * @param boxTwin
	 *	    The BoxLoop this is will be paired with.
	 */
	public void setTwin(BoxLoop boxTwin)
	{
		m_boxTwin = boxTwin;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("BoxLoop.update()");

		// Avoid redundant runs of the same update.
		long nTime = sim.getTime();
		if (nTime == m_nTime)
		{
			Trace.endOf("Track (" + this + ") already updated.");
			return;
		}
		m_nTime = nTime;

		// The first of the twins updated is responsible for the update of both.
		m_boxTwin.m_nTime = nTime;

		// If we have spouses, update there positions.
		if (m_boxSpouse != null)
		{
			m_boxSpouse.update(sim);
		}
		if (m_boxTwin.m_boxSpouse != null)
		{
			m_boxTwin.m_boxSpouse.update(sim);
		}

		int nCycles = sim.getUpdateCyclesCount();

		// If we can, move the twins for each time cycle.
		for (int i = 0; i < nCycles; ++i)
		{
			move();
		}

		// Update our car.
		if (m_car != null)
		{
			// Run an update internal to the car.
			m_car.update(sim);

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
					}
				}
			}
		}

		// Update our twin's car.
		if (m_boxTwin.m_car != null)
		{
			// Run an update internal to the car.
			m_boxTwin.m_car.update(sim);

			for (int i = 0; i < nCycles; ++i)
			{
				// Try to move the car.
				if (m_boxTwin.m_boxSpouse != null)
				{
					if (m_boxTwin.m_car.canMove(false))
					{
						m_boxTwin.m_car.move();
					}
					else
					{
						Box spouse = m_boxTwin.m_boxSpouse;
						m_boxTwin.setSpouse(null);
						spouse.setSpouse(null);
					}
				}
			}
		}

		Trace.endOf("BoxLoop.update()");
	}
}

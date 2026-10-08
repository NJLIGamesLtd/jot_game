/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator BoxRing carries RollaCar objects around a TrackRing which circles around the
 * outside of the second level of the Carousel.  These can marry to BoxCarousel or BoxWay objects
 * for the purpose of transfering RollaCar objects to/from the LiftLoop.
 */

package com.jot.rollavator;

import com.jot.simulation.Cycles;
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

public class BoxRing extends CarWay implements Box
{
	protected static final int s_massBox = 1000;
    protected static final int s_massTrack = 100;
	protected static final int s_nMaxVelocity = 3;
	protected static final Size s_size = new Size(1600, 2000, 3000);
	
	private static final Orientation s_orientFixed = new Orientation(0);
	private static final Position[] s_aPosTrack =
	{
		new Position(    0, 0, 0, s_orientFixed),
		new Position(  -50, 0, 0, s_orientFixed),
		new Position( -100, 0, 0, s_orientFixed),
		new Position( -150, 0, 0, s_orientFixed),
		new Position( -200, 0, 0, s_orientFixed),
		new Position( -250, 0, 0, s_orientFixed),
		new Position( -300, 0, 0, s_orientFixed),
		new Position( -350, 0, 0, s_orientFixed),
		new Position( -400, 0, 0, s_orientFixed),
		new Position( -450, 0, 0, s_orientFixed),
		new Position( -500, 0, 0, s_orientFixed),
		new Position( -550, 0, 0, s_orientFixed),
		new Position( -600, 0, 0, s_orientFixed),
		new Position( -650, 0, 0, s_orientFixed),
		new Position( -700, 0, 0, s_orientFixed),
		new Position( -750, 0, 0, s_orientFixed),
		new Position( -800, 0, 0, s_orientFixed),
		new Position( -850, 0, 0, s_orientFixed),
		new Position( -900, 0, 0, s_orientFixed),
		new Position( -950, 0, 0, s_orientFixed),
		new Position(-1000, 0, 0, s_orientFixed),
		new Position(-1050, 0, 0, s_orientFixed),
		new Position(-1100, 0, 0, s_orientFixed),
		new Position(-1150, 0, 0, s_orientFixed),
		new Position(-1200, 0, 0, s_orientFixed),
		new Position(-1250, 0, 0, s_orientFixed),
		new Position(-1300, 0, 0, s_orientFixed),
		new Position(-1350, 0, 0, s_orientFixed),
		new Position(-1400, 0, 0, s_orientFixed),
		new Position(-1450, 0, 0, s_orientFixed),
		new Position(-1500, 0, 0, s_orientFixed),
		new Position(-1550, 0, 0, s_orientFixed),
		new Position(-1600, 0, 0, s_orientFixed),
	};

	private static final Cycles s_cyclesSlide = new Cycles((s_aPosTrack.length / 3) + 3);
	private static final Cycles s_cyclesEject = new Cycles((LiftCarousel.s_size.m_x / (3 * 50)) + 5);

	protected Track m_track;	// Track within our Box that accepts and holds a Car.
	protected long m_nTime;		// Record of last time to avoid redundant updates.
	protected TrackRing.PosEnum m_eDest;
	protected Position[] m_aPosDests;	// Attempting to reach this position to swap a car.
	protected Carousel m_carousel;
	protected BoxWay m_boxShaftFrom;
	protected BoxWay m_boxShaftTo;

	private long m_distSafe;	// Safe distance (squared) between this and the next Box.
	private Box m_boxSpouse;	// Married to this Box to transfer a Car.
	private int m_idQuadLoaded;	// Quadrant IDs for 0=entered, 1=transit, 2=loaded.

	/***********************************************************************************************
	 * Return the time needed to slide a car into the box in Cycles.
	 *
	 * @return
	 *	    The time needed to slide a car into the box in Cycles.
	 */
	public static Cycles GetSlideCycles()
	{
		return s_cyclesSlide;
	}

	/***********************************************************************************************
	 * Return the time needed to slide a car into the box in Cycles.
	 *
	 * @return
	 *	    The time needed to slide a car into the box in Cycles.
	 */
	public static Cycles GetEjectCycles()
	{
		return s_cyclesEject;
	}

	/***********************************************************************************************
	 * BoxRing constructor based on its position.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param posDests
	 *		Destinations that we need to marry with other Box objects.
	 * @param carousel
	 *		Carousel that this BoxRing circles and is owned by.
	 * @param boxShaftFrom
	 *		One directional track connecting the Shaft to the Carousel.
	 * @param boxShaftTo
	 *		One directional track connecting the Carousel to the Shaft.
	 */
	public BoxRing(
		Position position,
		Position[] aPosDests,
		Carousel carousel,
		BoxWay boxShaftFrom,
		BoxWay boxShaftTo
	) throws Exception
	{
		super(s_massBox, s_size, position, s_nMaxVelocity);

		// Create an inner Track for our Car to ride on.
		m_track = new Return(s_massTrack, s_size, position, s_aPosTrack);

		// Divide the track into three quadrants. 0 if entered, 1 for transit,
		// 2 for fully loaded.
		m_track.markQuadrant(0);
		m_track.markQuadrant(1);
		m_idQuadLoaded = m_track.markQuadrant(s_aPosTrack.length - 1);

		// To avoid collisions between the boxes, record a safe distance for them to stay.
		m_distSafe = (m_size.m_x * m_size.m_x) + (m_size.m_y * m_size.m_y);

		// By default, we are not married to another box.
		m_boxSpouse = null;

		// Used to avoid redundant updates.
		m_nTime = 0;

		// Moving towards this destination.
		m_aPosDests = aPosDests;
		m_eDest = TrackRing.PosEnum.FROM_CAROUSEL;
		m_posDest = aPosDests[m_eDest.m_value];

		// Get the connecting carousel.
		m_carousel = carousel;
		m_boxShaftFrom = boxShaftFrom;
		m_boxShaftTo = boxShaftTo;
	}

	/***********************************************************************************************
	 * Return true if the box can be moved.
	 *
	 * @return
	 *	    True if the box can move.
	 */
	public boolean canMove(boolean recurse) throws Exception
	{
		// If this is the top level call, then we might need to recurse down to check our spouse.
		if (recurse)
		{
			// If we have a spouse that can move, then we have to be able to move.
			if (m_boxSpouse != null)
			{
				if (m_boxSpouse.canMove(false))
				{
					return true;
				}

				// If our spouse can not move, halt our movement.
 				if (m_nVelocity > 0)
				{
					if (--m_nVelocity != 0)
					{
						Trace.fatal("BoxRing halted from moving when at speed.");
					}
				}
				return false;
			}

			// If we have reached our destination, try to marry up with another track.
			if (m_position.equals(m_posDest))
			{
				switch (m_eDest)
				{
					case FROM_CAROUSEL:	// Waiting to pick up a box from the carousel.
						if (((BoxRing)m_carLeader).m_eDest.m_value
							 <= TrackRing.PosEnum.FROM_SHAFT.m_value)
						{
							return false;
						}
						break;
					case TO_SHAFT:		// Leaving the to shaft connection after a drop off.
					case FROM_SHAFT:	// Leaving the to shaft connection after a pick up.
					default:
						break;
					case TO_CAROUSEL:	// Waiting to drop off a box to the carousel.
						return false;
				}
			}
		}

		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void checkDestination() throws Exception
	{
		if (m_position.equals(m_posDest))
		{
			TrackCar car;
			Box box;
			switch (m_eDest)
			{
				case FROM_CAROUSEL:
					// Waiting to marry up with a LiftCarousel and receive a TrackCar.
					if ((box = m_carousel.getBox(m_position)) != null)
					{
						if (box.getTrackCar() != null)
						{
							Trace.critical("BoxRing (" + this + ") married up from Carousel");

							setSpouse(box);
							box.setSpouse(this);
							m_eDest = m_eDest.getNext();
							m_posDest = m_aPosDests[m_eDest.m_value];
						}
					}
					break;

				case TO_SHAFT:		// Off load our car towards the shaft.
					// Check for a marriage with a the carousel or shaft.
					if (m_boxSpouse != null)
					{
						// If we're still married to the carousel, then try to break it off.
						if (m_boxSpouse.getClass() == BoxCarousel.class)
						{
							// SPK: might have to offer the car back to the carousel if we
							// missed the shaft.
							m_boxSpouse.setSpouse(null);
							setSpouse(null);
						}
						// Else see if the car we pushed over is fully loaded onto the BoxWay
						// so that we are clear of it.
						else if (m_boxSpouse.isLoaded())
						{
							// Since a BoxRing can have only one partner while a TrackRing can marry
							// from either end, nullify our connection so the TrackRing knows which
							// side disconnected.
							Box spouse = m_boxSpouse;
							setSpouse(null);
							spouse.setSpouse(null);
							m_eDest = m_eDest.getNext();
							m_posDest = m_aPosDests[m_eDest.m_value];
							break;
						}
					}
					if ((car = m_track.getTrackCar()) != null)
					{
						Trace.critical("BoxRing (" + this + ") married up to Shaft");

						m_boxShaftTo.setSpouse(this);
						setSpouse(m_boxShaftTo);

						// Try to offer ownership of the Car over to the BoxWay.
						if (m_boxSpouse.offer(car))
						{
							m_track.removeTrackCar(car);
						}
					}
					break;
					
				case FROM_SHAFT:	// Wait for the shaft track to give us a car.
					// If we're not married to the shaft track, do so.
					if (m_boxSpouse == null)
					{
						Trace.critical("BoxRing (" + this + ") married up from Shaft");

						m_boxShaftFrom.setSpouse(this);
						setSpouse(m_boxShaftFrom);
						break;
					}
					// If the car has been fully loaded, break the marriage.
					if ((car = m_track.getTrackCar()) != null &&
						m_track.getSectionFirst().equals(car.getPosition()))
					{
						// Since a BoxRing can have only one partner while a TrackRing can marry
						// from either end, nullify our connection so the TrackRing knows which
						// side disconnected.
						Box spouse = m_boxSpouse;
						setSpouse(null);
						spouse.setSpouse(null);
						m_eDest = m_eDest.getNext();
						m_posDest = m_aPosDests[m_eDest.m_value];
					}
					break;

				case TO_CAROUSEL:	// Waiting to drop off a box to the carousel.
					if ((box = m_carousel.getBox(m_position)) != null)
					{
						if (box.getTrackCar() != null)
						{
							Trace.critical("BoxRing (" + this + ") married up to Carousel");

							box.setSpouse(this);
							setSpouse(box);
							m_eDest = m_eDest.getNext();
							m_posDest = m_aPosDests[m_eDest.m_value];
						}
					}
					break;
				default:
					break;
			}
		}
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
	 * Return true if box collides with this.
	 *
	 * @param box
	 *	    The box moving towards this.
	 * @return
	 *	    True if the box would collide with this box if allowed to move to its new location.
	 */
	public boolean collides(Box box)
	{
		return collides(box.getPosition());
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
	public Section getSectionFirst()
	{
		return m_track.getSectionFirst();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Section getSectionLast()
	{
		return m_track.getSectionLast();
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
	public Track getTrack()
	{
		return m_track;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public TrackCar getTrackCar()
	{
		return m_track.getTrackCar();
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
	public void move() throws Exception
	{
		// If it can, try to move the box.
		if (canMove(true))
		{
			// Trigger CarWay.move().
			super.move();
		}

		// If we reached a destination, then act on it.
		checkDestination();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(TrackCar car) throws Exception
	{
		Trace.important("BoxRing.offer(): Car (" + car + ") directed towards this.");

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
			m_boxSpouse.setSpouse(null);
			m_boxSpouse = null;
			return false;
		}

		// Try to offer the Car to our Track.
		if (!m_track.offer(car))
		{
			return false;
		}

		// Assign us as the parent of the car.
		RollaCar rollacar = (RollaCar)car;
		rollacar.setParent(this);

		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void removeTrackCar(TrackCar car)
	{
		m_track.removeTrackCar(car);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setSpouse(Box spouse)
	{
		if ((m_boxSpouse = spouse) == null)
		{
			m_track.getSectionFirst().setPrevious(null);
			m_track.getSectionLast().setNext(null);
		}
		else if (spouse.getClass() == BoxCarousel.class)
		{
			m_track.getSectionFirst().setPrevious(spouse.getSectionLast());
		}
		else
		{
			m_track.getSectionLast().setNext(spouse.getSectionFirst());
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("BoxRing.update()");

		// Avoid redundant runs of the same update.
		long nTime = sim.getTime();
		if (nTime == m_nTime)
		{
			Trace.endOf("Track (" + this + ") already updated.");
			return;
		}
		m_nTime = nTime;

		// If we have a spouse, update its position.
		if (m_boxSpouse != null)
		{
			m_boxSpouse.update(sim);
		}

		// Lastly, update our track.
		m_track.update(sim);

		// If we were handed ownership of a car through a marriage, break the marriage
		// once the car reaches the end of our track.
		if (m_boxSpouse != null)
		{
			TrackCar car = getTrackCar();
			if (car != null)
			{
				if (!car.canMove(false))
				{
					m_boxSpouse.setSpouse(null);
					setSpouse(null);
				}
			}
		}

		Trace.endOf("BoxRing.update()");
	}
}

/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator LiftCarousel is a hydraulic ground to second floor track Return for the purpose of
 * sending/receiving a RollaCar via a BoxCarousel to/from a LiftLoop via a LiftTrack.
 */

package com.jot.rollavator;

import com.jot.item.Place;
import com.jot.lift.Door;
import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.Return;
import com.jot.track.Section;
import com.jot.track.Track;
import com.jot.track.TrackCar;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

public class LiftCarousel extends Return implements Box
{
	protected static final int s_mass = 1000;
	public static final Size s_size = new Size(1800, 2000, 6000);
	protected static final int s_nMaxVelocity = 3;
	
	private static final Orientation s_orientFixed = new Orientation(0);
	private static final Position[] s_aPosTrack =
	{
		new Position(0, 0,    0, s_orientFixed),
		new Position(0, 0,   50, s_orientFixed),
		new Position(0, 0,  100, s_orientFixed),
		new Position(0, 0,  150, s_orientFixed),
		new Position(0, 0,  200, s_orientFixed),
		new Position(0, 0,  250, s_orientFixed),
		new Position(0, 0,  300, s_orientFixed),
		new Position(0, 0,  350, s_orientFixed),
		new Position(0, 0,  400, s_orientFixed),
		new Position(0, 0,  450, s_orientFixed),
		new Position(0, 0,  500, s_orientFixed),
		new Position(0, 0,  550, s_orientFixed),
		new Position(0, 0,  600, s_orientFixed),
		new Position(0, 0,  650, s_orientFixed),
		new Position(0, 0,  700, s_orientFixed),
		new Position(0, 0,  750, s_orientFixed),
		new Position(0, 0,  800, s_orientFixed),
		new Position(0, 0,  850, s_orientFixed),
		new Position(0, 0,  900, s_orientFixed),
		new Position(0, 0,  950, s_orientFixed),
		new Position(0, 0, 1000, s_orientFixed),
		new Position(0, 0, 1050, s_orientFixed),
		new Position(0, 0, 1100, s_orientFixed),
		new Position(0, 0, 1150, s_orientFixed),
		new Position(0, 0, 1200, s_orientFixed),
		new Position(0, 0, 1250, s_orientFixed),
		new Position(0, 0, 1300, s_orientFixed),
		new Position(0, 0, 1350, s_orientFixed),
		new Position(0, 0, 1400, s_orientFixed),
		new Position(0, 0, 1450, s_orientFixed),
		new Position(0, 0, 1500, s_orientFixed),
		new Position(0, 0, 1550, s_orientFixed),
		new Position(0, 0, 1600, s_orientFixed),
		new Position(0, 0, 1650, s_orientFixed),
		new Position(0, 0, 1700, s_orientFixed),
		new Position(0, 0, 1750, s_orientFixed),
		new Position(0, 0, 1800, s_orientFixed),
		new Position(0, 0, 1850, s_orientFixed),
		new Position(0, 0, 1900, s_orientFixed),
		new Position(0, 0, 1950, s_orientFixed),
		new Position(0, 0, 2000, s_orientFixed),
		new Position(0, 0, 2050, s_orientFixed),
		new Position(0, 0, 2100, s_orientFixed),
		new Position(0, 0, 2150, s_orientFixed),
		new Position(0, 0, 2200, s_orientFixed),
		new Position(0, 0, 2250, s_orientFixed),
		new Position(0, 0, 2300, s_orientFixed),
		new Position(0, 0, 2350, s_orientFixed),
		new Position(0, 0, 2400, s_orientFixed),
		new Position(0, 0, 2450, s_orientFixed),
		new Position(0, 0, 2500, s_orientFixed),
		new Position(0, 0, 2550, s_orientFixed),
		new Position(0, 0, 2600, s_orientFixed),
		new Position(0, 0, 2650, s_orientFixed),
		new Position(0, 0, 2700, s_orientFixed),
		new Position(0, 0, 2750, s_orientFixed),
		new Position(0, 0, 2800, s_orientFixed),
		new Position(0, 0, 2850, s_orientFixed),
		new Position(0, 0, 2900, s_orientFixed),
		new Position(0, 0, 2950, s_orientFixed),
		new Position(0, 0, 3000, s_orientFixed),
	};

	private static final Cycles s_cyclesCloseLift =
		new Cycles(Door.GetDefaultCycles().getInt() + (s_aPosTrack.length / 3) + 5);

	protected Position m_posDest;	// Attempting to reach this position.
	protected TrackCar m_carLeader;

	private int m_idQuadLoaded;	// Quadrant IDs for 0=entered, 1=transit, 2=loaded.
	private Place m_place;			// Currently owned by this place.
	private Rollavator m_rollavator;	// Parent rollavator.

	/***********************************************************************************************
	 * Return the time needed to close the lift Door and raise the BoxCarousel to its destination
	 * in Cycles.
	 *
	 * @return
	 *	    The time needed to close the lift Door and raise the BoxCarousel to its destination
	 *		in Cycles.
	 */
	public static Cycles GetCloseLiftCycles()
	{
		return s_cyclesCloseLift;
	}

	/***********************************************************************************************
	 * LiftCarousel constructor based on its position.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param rollavator
	 *	    The Rollavator this is owned by.
	 */
	public LiftCarousel(
		Position position,
		Rollavator rollavator
	) throws Exception
	{
		super(s_mass, s_size, position, s_aPosTrack);

		Trace.startOf("rollavator.LiftCarousel()");

		// Store a reference to our parent.
		m_rollavator = rollavator;

		// Divide the track into three quadrants. 0 if at base level, 1 for transit,
		// 2 for top level.
		markQuadrant(0);
		markQuadrant(1);
		m_idQuadLoaded = markQuadrant(s_aPosTrack.length - 1);

		// Create a lift box and add it to our track.
		BoxCarousel box = new BoxCarousel(position, this, rollavator, s_nMaxVelocity);
		add(0, box);

		// No destination by default.
		m_posDest = null;

		// The leader of this lift will be provided.
		m_carLeader = null;

		Trace.endOf("rollavator.LiftCarousel()");
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
	public Position getDestination()
	{
		return m_posDest;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean getDirection()
	{
		return true;
	}

	/***********************************************************************************************
	 * Return the destination level group ID for this car based on a group being 10, 4 meter floors.
	 *
	 * @return
	 *		The destination level group ID of this car based on a group being 10, 4 meter floors.
	 */
	public int getFloorGroupDestinationID()
	{
		RollaCar car = (RollaCar)getTrackCar();
		if (car != null)
		{
			return car.getFloorGroupDestinationID();
		}
		return -1;
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
		return m_place;
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
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getTickVelocity()
	{
		return 1;
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
	public TrackCar getTrackCar()
	{
		BoxCarousel box = (BoxCarousel)m_car;
		return box.getTrackCar();
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
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean isPrevious()
	{
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void move() throws Exception
	{
		moveToNext();
		if (m_position.equals(m_posDest))
		{
			// If there is a person with a floor destination, then close the doors before
			// lifting the car to marry up with a BoxRing.
			RollaCar car = (RollaCar)getTrackCar();
			if (car != null)
			{
				int idFloor;
				if ((idFloor = car.getNextFloorID(true)) > 0)
				{
					Trace.important("Closing LiftCarousel Door.");
					car.close(false);

					// Request a car to be sent down to replace this.
					m_rollavator.requestNewCar(m_position.m_z / 4000, idFloor / 4000);
				}

				// Else, change the level destination up for the next cycle.
				else
				{
					int idFloorGroup = m_rollavator.getNextFloorGroupId(m_position.getZ());
					car.setFloorGroupDestinationID(idFloorGroup);
					Trace.important("Set LiftCarousel Car to FloorGroup " + idFloorGroup);
				}
			}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean moveToNext()
	{
		m_position = ((Section)m_position).getNext();
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean moveToPrevious()
	{
		m_position = ((Section)m_position).getPrevious();
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
	 * Assign the destination level group ID for this car based on a group being 10, 4 meter floors.
	 *
	 * @param idFloorGroup
	 *		The destination level group ID of this car based on a group being 10, 4 meter floors.
	 */
	public void setFloorGroupDestinationID(int idFloorGroup)
	{
		RollaCar car = (RollaCar)getTrackCar();
		if (car != null)
		{
			car.setFloorGroupDestinationID(idFloorGroup);
		}
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
		m_carLeader = car;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setPlace(Place place)
	{
		m_place = place;
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
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("rollavator.LiftCarousel.update()");

		// Run an update internal to the box.
		m_car.update(sim);

		// For each time cycle, put the items along the track one position.
		int nCycles = sim.getUpdateCyclesCount();
		do
		{
			if (m_car.canMove(true))
			{
				m_car.move();
			}
		} while (--nCycles > 0);

		Trace.endOf("rollavator.LiftCarousel.update()");
	}
}

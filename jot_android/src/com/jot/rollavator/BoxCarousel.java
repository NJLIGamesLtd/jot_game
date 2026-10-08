/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator BoxCarousel is a RollaCar carrier that is also a hydraulic ground to second floor
 * CarReturn for the purpose of sending/receiving a car to/from a LiftLoop via a LiftTrack.
 */

package com.jot.rollavator;

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

public class BoxCarousel extends CarReturn implements Box
{
	protected static final int s_massBox = 1000;
    protected static final int s_massTrack = 100;
	protected static final Size s_size = new Size(1600, 2000, 3000);
	
	private static final Orientation s_orientFixed = new Orientation(0);
	private static final Position[] s_aPosTrack =
	{
		new Position(0, 0, 0, s_orientFixed),
	};

	protected Track m_track;			// Track within our Box that accepts and holds a Car.
	protected long m_nTime;				// Record of last time to avoid redundant updates.

	private Box m_boxSpouse;				// Married to this Box to transfer a Car.
	private LiftCarousel m_liftParent;	// Parent lift hold this Box.
	private Rollavator m_rollavator;	// Parent rollavator.

	/***********************************************************************************************
	 * BoxCarousel constructor based on its position.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param rollavator
	 *	    The parent rollavator.
	 */
	public BoxCarousel(
		Position position,
		LiftCarousel liftParent,
		Rollavator rollavator,
		int nMaxVelocity
	) throws Exception
	{
		super(s_massBox, s_size, position, nMaxVelocity);

		// Store a reference to our parents.
		m_liftParent = liftParent;
		m_rollavator = rollavator;

		// Create an inner Track for our Car to ride on.
		m_track = new Return(s_massTrack, s_size, position, s_aPosTrack);

		// By default, each carousel lift box track has a Car.
		RollaCar car = new RollaCar(position, rollavator);
		m_track.add(0, car, null);

		// Assign us as the parent of the car.
		car.setParent(this);

		// Used to avoid redundant updates.
		m_nTime = 0;

		// By default, we are not married to another box.
		m_boxSpouse = null;
	}

	/***********************************************************************************************
	 * Return true if the box can be moved.
	 *
	 * @return
	 *	    True if the box can move.
	 */
	public boolean canMove(boolean recurse) throws Exception
	{
		// If we are married to a box in the TrackRing, don't move up or down.
		if (m_boxSpouse != null)
		{
			return false;
		}

		// If our car door is not closed, don't move either.
		RollaCar car = (RollaCar)getTrackCar();
		if (car != null && !car.isClosed())
		{
			return false;
		}
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
	 * Return true if box collides with this.
	 *
	 * @param box
	 *	    The box moving towards this.
	 * @return
	 *	    True if the box would collide with this box if allowed to move to its new location.
	 */
	public boolean collides(TrackCar box)
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
	public Position getPosition()
	{
		Position posParent = m_liftParent.getPosition();
		Position posCombine = new Position(m_position);
		posCombine.m_x = posParent.m_x;
		posCombine.m_y = posParent.m_y;

		return posCombine;
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
	public boolean offer(TrackCar car) throws Exception
	{
		Trace.important("BoxCarousel.offer(): Car (" + car + ") directed towards this.");

		// Make sure we have a track spouse that is married back to us.
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

		// Give this car a new destination floor group.
		LiftCarousel liftLeader = (LiftCarousel)m_liftParent.getLeader();
		int idFloorGroup;
		while ((idFloorGroup = liftLeader.getFloorGroupDestinationID()) <= 0)
		{
			if ((liftLeader = (LiftCarousel)liftLeader.getLeader()) == m_liftParent)
			{
				idFloorGroup = 0;
				break;
			}
		}
		if (++idFloorGroup >= m_rollavator.getMaxFloorGroupID())
		{
			idFloorGroup = 1;
		}
		RollaCar rollacar = (RollaCar)car;
		rollacar.setFloorGroupDestinationID(idFloorGroup);

		// Assign us as the parent of the car.
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
	public void setSpouse(Box spouse) throws Exception
	{
		if ((m_boxSpouse = spouse) == null)
		{
			m_track.getSectionLast().setNext(null);
		}
		else
		{
			m_track.getSectionLast().setNext(spouse.getSectionFirst());
			TrackCar car = getTrackCar();
			if (car != null)
			{
				if (spouse.offer(car))
				{
				    removeTrackCar(car);
				}
			}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("BoxCarousel.update()");

		// Avoid redundant runs of the same update.
		long nTime = sim.getTime();
		if (nTime == m_nTime)
		{
			Trace.endOf("Track (" + this + ") already updated.");
			return;
		}
		m_nTime = nTime;

		// Update our spouse, if married.
		if (m_boxSpouse != null)
		{
			m_boxSpouse.update(sim);
		}

		// Lastly, update our track.
		m_track.update(sim);

		Trace.endOf("BoxCarousel.update()");
	}
}

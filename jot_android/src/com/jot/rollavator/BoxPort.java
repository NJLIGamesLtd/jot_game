/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator BoxPort is a stationary place for a lift Car to park for StructureLevel access.
 */

package com.jot.rollavator;

import com.jot.item.*;
import com.jot.lift.Door;
import com.jot.lift.LiftCar;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.Return;
import com.jot.track.Section;
import com.jot.track.Track;
import com.jot.track.TrackCar;
import com.jot.vector.*;

import java.util.ArrayDeque;
import java.util.Queue;

public class BoxPort extends DroneImpl implements Box
{
	private static final int s_mass = 0;
    protected static final int s_massTrack = 100;
	private static final Size s_size = new Size(1600, 2000, 3000);

	private static final Orientation s_orientFixed = new Orientation(0);
	private static final Position[] s_aPosTrack =
	{
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

	protected Return m_track;	// Track within our Box that accepts and holds a Car.

	private Queue<Drone> m_qDroneEntering;	// Persons entering the port from the structure level.
	private StructureLevel m_towerLevel;
	private Box m_boxSpouse;		// Married to this Box to transfer a Car.
	private int m_idQuadLoaded;	// Quadrant IDs for 0=entered, 1=transit, 2=loaded.
	private boolean m_bMoveNext;
	private long m_nTime;

	/***********************************************************************************************
	 * BoxPort constructor based on its position and a StructureLevel it connects with.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param towerLevel
	 *	    The structure level this BoxPort connects with.
	 * @param rollavator
	 *	    The Rollavator this is owned by.
	 */
	public BoxPort(
		Position position,
		StructureLevel towerLevel,
		Rollavator rollavator
	) throws Exception
	{
		super(s_mass, s_size, position);
		m_towerLevel = towerLevel;

		// Need a queue of people trying to enter from the structure level.
		m_qDroneEntering = new ArrayDeque<Drone>();

		// Create an inner Track for our Car to ride on.
		m_track = new Return(s_massTrack, s_size, position, s_aPosTrack);

		// Divide the track into three quadrants. 0 if entered, 1 for transit,
		// 2 for fully loaded.
		m_track.markQuadrant(0);
		m_track.markQuadrant(1);
		m_idQuadLoaded = m_track.markQuadrant(s_aPosTrack.length - 1);

		// By default, we are not married to another box.
		m_boxSpouse = null;

		// By default, we should be trying to move next towards the docked position.
		m_bMoveNext = true;
		
		m_nTime = 0;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean add(TrackCar car) throws Exception
	{
		// Try to offer the Car to our Track.
		if (!m_track.offer(car))
		{
			return false;
		}

		// Place our car at the last "docked" section.
		car.setSection(m_track.getSectionLast());

		// Assign us as the parent of the car.
		RollaCar rollacar = (RollaCar)car;
		rollacar.setParent(this);

		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean canMove(boolean recurse) throws Exception
	{
		return false;
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
		return true;
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
	public int getTickVelocity()
	{
		return 0;
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
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean isPrevious()
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void move() throws Exception
	{
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
	public boolean offer(Drone person) throws Exception
	{
		// Tell the drone we control it.
		person.setPlace(this);

		m_qDroneEntering.add(person);
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(TrackCar car) throws Exception
	{
		Trace.important("BoxPort.offer(): Car (" + car + ") directed towards this.");

		// Make sure we have a track spouse that is married back to us.
		if (m_boxSpouse == null ||
			m_boxSpouse.getSpouse() != this)
		{
			return false;
		}

		// Make sure the Z axis positions are identical.
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
	public void setDestination(Position posDestination)
	{
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
		if (spouse == null)
		{
			m_track.getSectionFirst().setPrevious(null);
		}
		else
		{
			m_track.getSectionFirst().setPrevious(spouse.getSectionLast());
		}
		m_boxSpouse = spouse;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("rollavator.BoxPort.update()");
		
		// Only need to update if we have a car.
		RollaCar car = (RollaCar)m_track.getTrackCar();
		if (car != null)
		{
			// Avoid redundant runs of the same update.
			long nTime = sim.getTime();
			if (nTime == m_nTime)
			{
				Trace.endOf("Track (" + this + ") already updated.");
				return;
			}
			m_nTime = nTime;

			if (m_idDrone == 1112)
			{
				m_idDrone = 1112;
			}

			// Run an update of the track.
			m_track.update(sim);

			// SPK: Need to figure out how to gather people returning to the lobby.

			// If we're connected to the center lane BoxReturn, see if we need to disconnect.
			if (m_boxSpouse != null)
			{
				// If we're move back to the BoxReturn, then give it ownership.
				if (!car.getDirection())
				{
					// Wait until we started moving away from the office level door.
					if (car.isNext())
					{
						if (m_boxSpouse.offer(car))
						{
							removeTrackCar(car);
						}
					}
				}

				// If we're docked, let people on and off.
				else if (!car.isNext())
				{
					// If this is the last stop, break the marriage with the BoxReturn.
					if (car.getNextFloorID(true) == -1)
					{
						m_boxSpouse.setSpouse(null);
						setSpouse(null);
					}
					// Else if the door is now closed again, allow it to return to the BoxReturn.
					else if (car.isClosing())
					{
						car.setDirection(false);
					}
				}
			}
		}

		Trace.endOf("rollavator.BoxPort.update()");
	}
}

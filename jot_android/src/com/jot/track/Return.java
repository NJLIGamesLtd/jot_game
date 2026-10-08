/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A track Return is a Place that implements a two-way, single lane Track which supports at most
 * one Car.
 */

package com.jot.track;

import java.util.ArrayList;
import java.util.List;

import com.jot.item.Place;
import com.jot.item.PlaceImpl;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.Location;
import com.jot.vector.Position;
import com.jot.vector.Size;

public class Return extends PlaceImpl implements Track
{
	public TrackCar m_car;			// TrackCar, if present, traversing the Track.

	protected Section[] m_aSections;	// Sections that define the Track path.
	protected Section m_sectFirst;	// First Section in the Track.
	protected Section m_sectLast;	// Last Section in the Track.	
	protected long m_nTime;			// Record of last time to avoid redundant updates.

	private Track m_trackStart;		// Track accessible at the start of the Track.
	private Track m_trackEnd;		// Track accessible at the end of the Track.
	private Track m_trackDest;		// Track the car is trying to get to.

	/***********************************************************************************************
	 * Track constructor based on its array of sections.
	 *
	 * @param mass
	 *		The kg mass of this item.
	 * @param size
	 *	    The vector size of this item.
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param aPosTrack
	 *	    An array of positions that defines the path of the track.
	 */
	public Return(
		int mass,
		Size size,
		Position position,
		Position[] aPosTrack
	) throws Exception
	{
		super(mass, size, position);

		// If we have the track positions at construction time, lay the track.
		m_sectFirst = m_sectLast = null;
		if (aPosTrack != null)
		{
			setTrack(aPosTrack, position);
		}

		// A TrackReturn can hold at most one Car.
		m_car = null;

		// By default, there are no destinations off of this track.
		m_trackStart = m_trackEnd = m_trackDest = null;

		// Initialize the time when the last update was made so as to avoid duplicate updates.
		m_nTime = 0;
	}

	/***********************************************************************************************
	 * Force add the given car to our track at the specified section. This can be used to provide
	 * an initial location for the car.
	 *
	 * @param iPos
	 *		The 0-based index into our Track where the car should be added.
	 * @param car
	 *		The TrackCar to be added.
	 */
	public void add(int iPos, TrackCar car)
	{
		// Set the position of the car.  This position is not controlled by the track.
		car.setSection(m_aSections[iPos]);
		m_car = car;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void add(int iPos, TrackCar car, TrackCar leader)
	{
		add(iPos, car);
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
	public Section getSection(int iSection) throws Exception
	{
		if (iSection < 0 || iSection >= m_aSections.length)
		{
			Trace.fatal("Request for a section (" + iSection + ") beyond the track's range.");
		}
		return m_aSections[iSection];
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Section getSectionFirst()
	{
		return m_sectFirst;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Section getSectionLast()
	{
		return m_sectLast;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public TrackCar getTrackCar()
	{
		return m_car;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public TrackCar getTrackCarLast()
	{
		return m_car;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int markQuadrant(int iPos) throws Exception
	{
		// Error checking.
		if (m_aSections == null)
		{
			throw new Exception("markQuadrant(): Track sections have not been created.");
		}
		if (iPos < 0 || iPos >= m_aSections.length)
		{
			throw new Exception("markQuadrant(): Requested position is beyond list of sections.");
		}

		// Assign the rest of the sections with the new quadrant number.
		int idQuadrant = Section.GetNextQuadrantId();
		while (iPos < m_aSections.length)
		{
			m_aSections[iPos++].setQuadrantId(idQuadrant);
		}

		return idQuadrant;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(TrackCar car) throws Exception
	{
		// This Track can handle at most one Car.
		if (m_car != null)
		{
			return false;
		}

		m_car = car;
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void removeTrackCar(TrackCar car)
	{
		m_car = null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setDestinationEnd(Track destination)
	{
		m_trackEnd = destination;
		if (m_car != null && m_car.getDirection())
		{
			m_car.setDestination(destination.getPosition());
			m_trackDest = destination;
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setDestinationStart(Track destination)
	{
		m_trackStart = destination;
		if (m_car != null && !m_car.getDirection())
		{
			m_car.setDestination(destination.getPosition());
			m_trackDest = destination;
		}
	}

	/***********************************************************************************************
	 * Allow post construction of the track.
	 *
	 * @param aPosTrack
	 *	    An array of positions that defines the path of the track.
	 * @param posBase
	 *		The base position that our x,y,z is an offset of.
	 */
	public void setTrack(Position[] aPosTrack, Position posBase) throws Exception
	{
		// Verify we have at least one position to track.
		int nPos;
		if ((nPos = aPosTrack.length) <= 0)
		{
			throw new Exception("Track() requires non-empty position array.");
		}

		// Create a section array, providing a link from one position to the next.
		m_aSections = new Section[nPos];
		Section prev = null;
		for (int iPos = 0; iPos < nPos; ++iPos)
		{
			prev = m_aSections[iPos] = new Section(aPosTrack[iPos], posBase, prev, null);
			m_aSections[iPos].setIndex(iPos);
		}
		Section next = null;
		for (int iPos = nPos; iPos-- > 0; )
		{
			m_aSections[iPos].setNext(next);
			next = m_aSections[iPos];
		}

		// Record the start and end sections.
		m_sectFirst = m_aSections[0];
		m_sectLast = m_aSections[nPos - 1];
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("track.Return(" + this + ").update()");

		// Avoid redundant runs of the same update.
		long nTime = sim.getTime();
		if (nTime != m_nTime)
		{
			m_nTime = nTime;

			if (m_car != null)
			{
				// Run an update internal to the car.
				m_car.update(sim);

				// For each time cycle, put the cars along the track one section.
				int nCycles = sim.getUpdateCyclesCount();
				do
				{
					// If the car reached and was accepted by it's destination, remove it
					// from our track.
					//if (m_trackDest != null && m_trackDest.offer(m_car))
					//{
					//	m_car = null;
					//	break;
					//}

					// Try to move the car.
					if (m_car.canMove(false))
					{
						m_car.move();
					}
				} while (--nCycles > 0);
			}
		}

		Trace.endOf("track.Return(" + this + ").update()");
	}
}

/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A track Way is a Place that implements a one-way Track that supports multiple Cars.
 */

package com.jot.track;

import com.jot.item.Place;
import com.jot.item.PlaceImpl;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.Position;
import com.jot.vector.Size;

import java.util.*;

public class Way extends PlaceImpl implements Track
{
	protected List<TrackCar> m_lCars;	// The Car List traveling on this path.
	protected Section[] m_aSections;	// The train Section path.
	protected Section m_sectFirst;		// First Section in the Track.
	protected Section m_sectLast;		// Last Section in the Track.
	protected long m_nTime;				// Record of last time to avoid redundant updates.
	private Track m_trackDest;			// Destination at the end of our path.

	/***********************************************************************************************
	 * Way constructor based on its array of sections.
	 *
	 * @param mass
	 *		The kg mass of this item.
	 * @param size
	 *	    The vector size of this item.
	 * @param position
	 *	    The vector position (location and orientation) of this.
	 * @param aPosTrack
	 *	    An array of positions that defines the path of the track.
	 */
	public Way(
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
			setTrack(aPosTrack, position, false);
		}

		// Create an array of train cars that will course through our track.
		m_lCars = new ArrayList<TrackCar>();

		// Initialize the time when the last update was made so as to avoid duplicate updates.
		m_nTime = 0;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void add(int iPos, TrackCar car, TrackCar leader)
	{
		// Need to know the car's leader, so it doesn't collide with it.
		car.setLeader(leader);
		
		// Set the position of the car.  This position is not controlled by the track.
		car.setSection(m_aSections[iPos]);
		m_lCars.add(car);
	}

	/***********************************************************************************************
	 * Return the z-axis level of this, accounting for the level of the parent base.
	 *
	 * @return
	 *	    The overall z-axis level.
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
		if (m_lCars.size() > 0)
		{
			return m_lCars.get(0);
		}
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public TrackCar getTrackCarLast()
	{
		int nCars;
		if ((nCars = m_lCars.size()) > 0)
		{
			return m_lCars.get(nCars - 1);
		}
		return null;
	}

	/***********************************************************************************************
	 * Return the list of track cars within this track.
	 *
	 * @return
	 *		The List of TrackCar objects within this Track.
	 */
	public List<TrackCar> getTrackCars()
	{
		return m_lCars;
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
		Section sectStart = m_aSections[0];

		// To be accepted onto the track, the car should already be in the same position as the
		// start of our track.
		if (!sectStart.equals(car.getLocation()))
		{
			return false;
		}

		TrackCar carLast = null;
		int nCars;
		// Check to make sure we don't collide with the car added last to the list.
		if ((nCars = m_lCars.size()) > 0)
		{
			carLast = m_lCars.get(nCars - 1);
			if (carLast.collides(sectStart))
			{
				return false;
			}
		}

		// Assign the first second in the track to our car as a path to follow.
		car.setSection(sectStart);

		// Mark this last car as leading our new car.
		car.setLeader(carLast);

		// Add the car to our list.
		m_lCars.add(car);

		Trace.important("Way.offer(): accepted car:" + car + " at " + car.getPosition());
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void removeTrackCar(TrackCar car)
	{
		m_lCars.remove(car);
	}

	/***********************************************************************************************
	 * Assign a Track object has the destination at the end of a Track.
	 *
	 * @param destination
	 *		A Track which our Track leads to.
	 */
	public void setDestination(Track destination)
	{
		m_trackDest = destination;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setDestinationEnd(Track destination)
	{
		setDestination(destination);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setDestinationStart(Track destination)
	{
		setDestination(destination);
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
		setTrack(aPosTrack, posBase, false);
	}

	/***********************************************************************************************
	 * Allow post construction of the track.
	 *
	 * @param aPosTrack
	 *	    An array of positions that defines the path of the track.
	 * @param posBase
	 *		The base position that our x,y,z is an offset of.
	 * @param bReverse
	 *	    True if we want to assign the track in the reverse order provided.
	 */
	public void setTrack(
		Position[] aPosTrack,
		Position posBase,
		boolean bReverse
	) throws Exception
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
		if (bReverse)
		{
			for (int iPos = 0; iPos < nPos; ++iPos)
			{
				prev = m_aSections[iPos]
					= new Section(aPosTrack[nPos - iPos - 1], posBase, prev, null);
				m_aSections[iPos].setIndex(iPos);
			}
		}
		else
		{
			for (int iPos = 0; iPos < nPos; ++iPos)
			{
				prev = m_aSections[iPos] = new Section(aPosTrack[iPos], posBase, prev, null);
				m_aSections[iPos].setIndex(iPos);
			}
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
		Trace.startOf("track.Way(" + this + ").update()");

		// Avoid redundant runs of the same update.
		long nTime = sim.getTime();
		if (nTime != m_nTime)
		{
			m_nTime = nTime;

			// Need to define the car count outside the tick method.
			int nCars = m_lCars.size();

			// Update each of the cars.
			for (int i = 0; i < nCars; ++i)
			{
				TrackCar car = m_lCars.get(i);

				// Run an update internal to the car.
				car.update(sim);
			}

			// For each time cycle, move the cars along the track one section.
			int nCycles = sim.getUpdateCyclesCount();
			do
			{
				// Starting with the first car added to the track, try to move it
				// forward one section.
				for (int i = 0; i < nCars; ++i)
				{
					TrackCar car = m_lCars.get(i);

					// If the car reached and was accepted by it's destination, remove it
					// from our track.
					if (m_trackDest != null && m_trackDest.offer(car))
					{
						m_lCars.remove(car);
						--nCars;
						--i;
						Trace.important("track.Way.update(): car reached destination:" + car);
						continue;
					}

					// Try to move the car.
					car.move();

					Trace.trivial(
						"track.Way.updateTick(): Car *(" + car + ") moved to position " + car.getLocation());
				}
			} while (--nCycles > 0);
		}

		Trace.endOf("track.Way(" + this + ").update()");
	}
}

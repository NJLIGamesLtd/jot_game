/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A track Loop is a Way that loops back onto its start.
 */

package com.jot.track;

import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.Position;
import com.jot.vector.Size;

public class Loop extends Way
{
	/***********************************************************************************************
	 * Loop constructor based on its array of track positions.
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
	public Loop(
		int mass,
		Size size,
		Position position,
		Position[] aPosTrack
	) throws Exception
	{
		super(mass, size, position, null);

		if (aPosTrack != null)
		{
			setTrack(aPosTrack, position);
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(TrackCar car) throws Exception
	{
		TrackCar carFirst = null;
		if (m_lCars.size() > 0)
		{
			// Make sure the first car added to the list hasn't looped back around
			// too near the starting location.
			carFirst = m_lCars.get(0);
			if (carFirst.collides(m_aSections[0]))
			{
				return false;
			}
		}

		// Utilize the offer logic from our super.
		if (!super.offer(car))
		{
			return false;
		}

		// Set this new car as our first car's leader.
		if (carFirst != null)
		{
			carFirst.setLeader(car);
		}

		return true;
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

		// Set the last section element to loop back to the first as its next and vise-versa.
		m_aSections[m_aSections.length - 1].setNext(m_aSections[0]);
		m_aSections[0].setPrevious(m_aSections[m_aSections.length - 1]);
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
		super.setTrack(aPosTrack, posBase, bReverse);

		// Set the last section element to loop back to the first as its next and vise-versa.
		m_aSections[m_aSections.length - 1].setNext(m_aSections[0]);
		m_aSections[0].setPrevious(m_aSections[m_aSections.length - 1]);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("track.Loop.update()");

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

					// Try to move the car.
					car.move();

					Trace.trivial(
						"track.Loop.update(): Car *(" + car + ") moved to position " + car.getLocation());
				}
			} while (--nCycles > 0);
		}

		Trace.endOf("track.Loop.update()");
	}
}

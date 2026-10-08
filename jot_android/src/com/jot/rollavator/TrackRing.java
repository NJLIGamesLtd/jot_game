/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator TrackRing carries BoxRing objects around its Track which circles around the
 * outside of the second level of the Carousel.
 */

package com.jot.rollavator;

import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.Loop;
import com.jot.track.TrackCar;
import com.jot.vector.Position;
import com.jot.vector.Size;

public class TrackRing extends Loop
{
	public enum PosEnum
	{
		FROM_CAROUSEL(0),
		TO_SHAFT(1),
		FROM_SHAFT(2),
		TO_CAROUSEL(3),
		NPOS(4);

		public final int m_value;

		PosEnum(int value)
		{
			m_value = value;
		}

		public PosEnum
		getNext()
		{
			switch (this)
			{
				case FROM_CAROUSEL:
					return TO_SHAFT;
				case TO_SHAFT:
					return FROM_SHAFT;
				case FROM_SHAFT:
					return TO_CAROUSEL;
				case TO_CAROUSEL:
				default:
					return FROM_CAROUSEL;
			}
		}
	}

	protected static final int s_mass = 1000;
	protected static final Size s_size
	    = new Size(2 * Carousel.s_radPlatform, 2 * Carousel.s_radPlatform, 3000);

	public Position m_aPosDests[];	// Attempting to reach this position to swap a car.

	private TrackCar m_carEnter;

	/***********************************************************************************************
	 * Loop constructor based on its position and structure levels it connects with.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param bReverse
	 *	    True if we want the Carousel to spin counter-clockwise.
	 */
	public TrackRing(
		Position position,
		boolean bReverse,
		Carousel carousel,
		BoxWay boxShaftFrom,
		BoxWay boxShaftTo
	) throws Exception
	{
		super(s_mass, s_size, position, null);
		
		Trace.startOf("rollavator.TrackRing()");

		// Assign the same track as the carousel.
		Position posRing = new Position(position);
		posRing.addZ(3000);
		setTrack(Carousel.s_aPosTrack, posRing, bReverse);

		// Set out destinations.
		m_aPosDests = new Position[PosEnum.NPOS.m_value];

		// The first stage requires the LiftCarousel Door to close and raise its
		// BoxCarousel into position.
		int iCycle = LiftCarousel.GetCloseLiftCycles().getInt();
		m_aPosDests[PosEnum.FROM_CAROUSEL.m_value] = m_aSections[iCycle];
		iCycle += BoxRing.GetSlideCycles().getInt() + BoxRing.GetEjectCycles().getInt();
		m_aPosDests[PosEnum.TO_SHAFT.m_value] = m_aSections[iCycle];
		iCycle += BoxRing.GetSlideCycles().getInt();
		m_aPosDests[PosEnum.FROM_SHAFT.m_value] = m_aSections[iCycle];
		iCycle += BoxRing.GetSlideCycles().getInt();
		m_aPosDests[PosEnum.TO_CAROUSEL.m_value] = m_aSections[iCycle];

		// Need only half as many car carriers as the Carousel.
		int nBoxes = Carousel.s_nElevatorSlots / 2;

		// Fill our array with the lift boxes.
		int iPos = 0;
		int nNextPos = Carousel.s_aPosTrack.length / nBoxes;
		BoxRing box = new BoxRing(position, m_aPosDests, carousel, boxShaftFrom, boxShaftTo);
		for (int iBox = 0; iBox < (nBoxes - 1); ++iBox)
		{
			// Create a leader in front of our next lift box so we
			// can check to make sure a collision doesn't occur.
			BoxRing leader = new BoxRing(position, m_aPosDests, carousel, boxShaftFrom, boxShaftTo);
			add(iPos, box, leader);
			box = leader;

			// Update to the next track position.
			iPos += nNextPos;
		}
		// Add the last loop box, using the first as it's leader.
		add(iPos, box, m_lCars.get(0));

		// By default, no car is trying to enter the ring of car carriers.
		m_carEnter = null;

		Trace.endOf("rollavator.TrackRing()");
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(TrackCar car)
	{
		// If there is already a car waiting to enter, refuse the offer for a new one.
		if (m_carEnter != null)
		{
			return false;
		}

		// Accept the car, put still force it to wait until it can be pulled into a box.
		m_carEnter = car;
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("rollavator.TrackRing.update()");

		// Need to define the car count outside the tick method.
		int nCars = m_lCars.size();

		// Update each of the box cars.
		for (int i = 0; i < nCars; ++i)
		{
			BoxRing box = (BoxRing)m_lCars.get(i);

			// Run an update internal to the box.
			box.update(sim);
		}

		Trace.endOf("rollavator.TrackRing.update()");
	}
}

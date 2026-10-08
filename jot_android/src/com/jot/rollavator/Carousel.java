/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator Carousel is a slow, spinning platform with numerous RollaCar access points for
 * Robot control persons to enter and exit. Inside this platform, a Carousel is also a circular
 * track Loop of LiftCarousel lifts.  Each RollaCar is held within a BoxCarousel carrier
 * which itself is carried by the two-story LiftCarousel Return tracks.
 */

package com.jot.rollavator;

import java.util.ArrayList;
import java.util.List;

import com.jot.item.*;
import com.jot.lift.Door;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.Loop;
import com.jot.track.Section;
import com.jot.track.TrackCar;
import com.jot.vector.*;

public class Carousel extends Loop
{
	public static final int s_nElevatorSlots = 16;
	public static final int s_nTrackPositions = 1024;
	public static final int s_radPlatform = 10000;
	private static final int s_radSlot = 8500;
	protected static final int s_mass = 10000;
	protected static final Size s_size = new Size(2 * s_radPlatform, 2 * s_radPlatform, 6000);
	protected static Position[] s_aPosTrack = null;

	protected List<Door> m_lDoors;		// The Door objects associated with each LiftCarousel.
	public TrackRing m_trackRing;
	protected BoxWay m_boxShaftFrom;
	protected BoxWay m_boxShaftTo;

	private Rollavator m_rollavator;
	private int m_idNextFloorGroup;

	/***********************************************************************************************
	 * Carousel constructor based on its position and the Rollavator it is owned by.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param rollavator
	 *	    The Rollavator this is owned by.
	 * @param bReverse
	 *	    True if we want the Carousel to spin counter-clockwise.
	 * @param boxShaftFrom
	 *		One directional track connecting the Shaft to the Carousel.
	 * @param boxShaftTo
	 *		One directional track connecting the Carousel to the Shaft.
	 */
	Carousel(
		Position position,
		Rollavator rollavator,
		boolean bReverse,
		BoxWay boxShaftFrom,
		BoxWay boxShaftTo
	) throws Exception
	{
		super(s_mass, s_size, position, null);
		
		Trace.startOf("Rollavator.Carousel()");

		m_boxShaftFrom = boxShaftFrom;
		m_boxShaftTo = boxShaftTo;

		// If needed, estantiate our static circular position track.
		if (s_aPosTrack == null)
		{
			double angle = 0.0;
			s_aPosTrack = new Position[s_nTrackPositions];
			for (int iPos = 0;
				 iPos < s_nTrackPositions;
				 ++iPos, angle += (2.0 * Math.PI) / s_nTrackPositions)
			{
				int heading = (int)(360.0 * angle / (2.0 * Math.PI));
				int x = (int)(s_radSlot * Math.sin(angle));
				int y = (int)(s_radSlot * Math.cos(angle));
				s_aPosTrack[iPos] = new Position(x, y, 0, heading);
			}
		}

		// Assign our track.
		setTrack(s_aPosTrack, position, bReverse);

		// Save a copy of the parent rollavator.
		m_rollavator = rollavator;

		// Create an array of doors that will follow the course of our LiftCarousels.
		m_lDoors = new ArrayList<Door>();

		// Fill our array with the two-story lifts.
		int iPos = 0;
		int iLift;
		int iSection = 0;
		RollaCar car;
		m_idNextFloorGroup = 0;
		LiftCarousel lift = new LiftCarousel(m_aSections[iSection], rollavator);
		for (iLift = 0; iLift < (s_nElevatorSlots - 1); ++iLift)
		{
			iSection += (s_nTrackPositions / s_nElevatorSlots);

			// Once a car reaches the end of the track, it should try to close its doors
			// and make its way up the shaft.
			lift.setDestination(m_aSections[s_nTrackPositions - 1]);

			// Create a leader in front of our next lift box so we
			// can check to make sure a collision doesn't occur.
			LiftCarousel leader = new LiftCarousel(m_aSections[iSection], rollavator);
			add(iPos, lift, leader);

			// Get a reference to the that will traverse to a specific level group.
			car = (RollaCar)lift.getTrackCar();

			// Assign floor group to direct the initial car within this lift towards.
			car.setFloorGroupDestinationID(getNextFloorGroupId());

			// Create a Door to match this Lift.
			Door door = new Door(lift.getPosition(), null);
			m_lDoors.add(door);

			// Marry our new carousel door to our track car door and have them open.
			car.getDoor().setSpouse(door);
			door.open();

			// Update to the next track position.
			iPos += (s_nTrackPositions / s_nElevatorSlots);
			lift = leader;
		}

		// Once a car reaches the end of the track, it should try to close its doors
		// and make its way up the shaft.
		lift.setDestination(m_aSections[s_nTrackPositions - 1]);

		// Add the last loop lift, using the first as it's leader.
		add(iPos, lift, m_lCars.get(0));

		// Get a reference to the that will traverse to a specific level group.
		car = (RollaCar)lift.getTrackCar();

		// Assign floor group to direct the initial car within this lift towards.
		car.setFloorGroupDestinationID(getNextFloorGroupId());

		// Create a Door to match this Lift.
		Door door = new Door(lift.getPosition(), null);
		m_lDoors.add(door);

		// Marry our new carousel door to our track car door and have them open.
		car.getDoor().setSpouse(door);
		door.open();

		// Create a second-floor TrackRing to control boxes to marry with our LiftCarousel boxes.
		m_trackRing = new TrackRing(position, bReverse, this, boxShaftFrom, boxShaftTo);

		Trace.endOf("Rollavator.Carousel()");
	}

	/***********************************************************************************************
	 * If found, return the rollavator Box that is at the given position.
	 */
	public Box getBox(int iPosition)
	{
		if (iPosition < s_aPosTrack.length)
		{
			return getBox(m_aSections[iPosition]);
		}
		return null;
	}

	/***********************************************************************************************
	 * If found, return the rollavator Box that is at the given position.
	 */
	public Box getBox(Position position)
	{
		for (TrackCar car: m_lCars)
		{
			LiftCarousel lift = (LiftCarousel)car;
			Box box = (Box)lift.m_car;
			if (position.equals(box.getPosition()))
			{
				return box;
			}
		}
		return null;
	}

	/***********************************************************************************************
	 * Return the nth door position.
	 *
	 * @param iDoor
	 *		The door index on this level for this lift (starting from 0).
	 * @return
	 *		The door position.
	 */
	public Position getDoorPosition(int iDoor)
	{
		if (iDoor < 0 || iDoor >= m_lDoors.size())
		{
			return null;
		}
		return m_lDoors.get(iDoor).getPosition();
	}

	/***********************************************************************************************
	 */
	public int getNextFloorGroupId()
	{
		if (m_idNextFloorGroup > m_rollavator.getLevelStackCount())
		{
			m_idNextFloorGroup = 0;
		}
		return ++m_idNextFloorGroup;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public TrackCar getTrackCar()
	{
		LiftCarousel lift = (LiftCarousel)m_lCars.get(0);
		return lift.getTrackCar();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(Drone drone) throws Exception
	{
		Person person = (Person)drone;

		// Pop off the carousel from the destination stack and find out where this person
		// needs to take a lift to.
		person.popDestination();

		Place destLevel = person.peekDestination();
		int idFloorGroup = destLevel.getFloorGroupID();

		// Save this floor group id for later.
		drone.setFloorGroupId(idFloorGroup);

		for (TrackCar trackCar : m_lCars)
		{
			LiftCarousel lift = (LiftCarousel)trackCar;
			RollaCar car = (RollaCar)lift.getTrackCar();

			if (car != null && idFloorGroup == car.getFloorGroupDestinationID())
			{
				// Get an estimate as to where our carousel door will be when the person
				// reaches it.
				Section section = trackCar.getSection();
				int nTickDistance = person.getTickDistance(section);
				int iSection = section.getIndex() + nTickDistance;

				// Make sure the person can reach in time.
				if (iSection < s_nTrackPositions)
				{
					person.pushDestination(car);
					person.directLocation(m_aSections[iSection]);

					// Tell the drone we control it.
					person.setPlace(this);
					return true;
				}
			}
		}

		// No cars for this floor group are present on the rollavator.  Direct the person towards
		// the start of the carousel.
		person.pushDestination(this);
		person.directLocation(m_aSections[0]);
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(TrackCar car)
	{
		return m_trackRing.offer(car);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("rollavator.Carousel.update()");

		// Update our TrackRing.
		m_trackRing.update(sim);

		// Update our tracks to and from the shaft.
		m_boxShaftFrom.update(sim);
		m_boxShaftTo.update(sim);

		// Need to define the car count outside the tick method.
		List<TrackCar> ringCars = m_trackRing.getTrackCars();
		int nRingCars = ringCars.size();

		// Update each of the box cars.
		for (int i = 0; i < s_nElevatorSlots; ++i)
		{
			TrackCar lift = m_lCars.get(i);

			// Run an update of each of our internal lifts.
			lift.update(sim);
		}

		// For each time cycle, put the items along the track one position.
		int nCycles = sim.getUpdateCyclesCount();
		do
		{
			// Starting with the first car added to the track, try to move it
			// forward one section.
			for (int i = 0; i < s_nElevatorSlots; ++i)
			{
				TrackCar lift = m_lCars.get(i);
				
				// Move the lift around the carousel.
				lift.move();

				// Move the doors to sync with the lifts.
				Door door = m_lDoors.get(i);
				door.setPosition(lift.getPosition());
			}

			// Starting with the first car added to the track, try to move it
			// forward one section.
			for (int i = 0; i < nRingCars; ++i)
			{
				// Move the box and try to marry up for box transfers.
				BoxRing box = (BoxRing)ringCars.get(i);
				box.move();
			}
		} while (--nCycles > 0);

		Trace.endOf("rollavator.Carousel.update()");
	}
}

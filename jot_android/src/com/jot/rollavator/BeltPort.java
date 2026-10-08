/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Belt is a Track that loops around in a tight vertical fashion, as with an escalator or
 * assisted walk where only top or bottom side is used to carry something.
 *
 * A rollavator BeltPort carries a set of BoxPort car carriers to the left or right in a loop.
 * This allows a port to have an up car or a down car, while pushing an empty car to the side
 * to make room for a second.
 */

package com.jot.rollavator;

import com.jot.item.StructureLevel;
import com.jot.lift.Door;
import com.jot.lift.LiftCar;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.Loop;
import com.jot.track.Return;
import com.jot.track.Section;
import com.jot.track.TrackCar;
import com.jot.vector.Location;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

public class BeltPort extends Loop
{
	protected static final int s_mass = 1000;
	protected static final Size s_size = new Size(1800, 4000, 4000);
	protected static final int s_nMaxVelocity = 3;

	private static final Orientation s_orientFixed = new Orientation(0);
	private static final Position[] s_aPosTrack =
	{
		new Position(0, -1000,    0, s_orientFixed),
		new Position(0,  -950,    0, s_orientFixed),
		new Position(0,  -900,    0, s_orientFixed),
		new Position(0,  -850,    0, s_orientFixed),
		new Position(0,  -800,    0, s_orientFixed),
		new Position(0,  -750,    0, s_orientFixed),
		new Position(0,  -700,    0, s_orientFixed),
		new Position(0,  -650,    0, s_orientFixed),
		new Position(0,  -600,    0, s_orientFixed),
		new Position(0,  -550,    0, s_orientFixed),
		new Position(0,  -500,    0, s_orientFixed),
		new Position(0,  -450,    0, s_orientFixed),
		new Position(0,  -400,    0, s_orientFixed),
		new Position(0,  -350,    0, s_orientFixed),
		new Position(0,  -300,    0, s_orientFixed),
		new Position(0,  -250,    0, s_orientFixed),
		new Position(0,  -200,    0, s_orientFixed),
		new Position(0,  -150,    0, s_orientFixed),
		new Position(0,  -100,    0, s_orientFixed),
		new Position(0,   -50,    0, s_orientFixed),
		new Position(0,     0,    0, s_orientFixed),
		new Position(0,    50,    0, s_orientFixed),
		new Position(0,   100,    0, s_orientFixed),
		new Position(0,   150,    0, s_orientFixed),
		new Position(0,   200,    0, s_orientFixed),
		new Position(0,   250,    0, s_orientFixed),
		new Position(0,   300,    0, s_orientFixed),
		new Position(0,   350,    0, s_orientFixed),
		new Position(0,   400,    0, s_orientFixed),
		new Position(0,   450,    0, s_orientFixed),
		new Position(0,   500,    0, s_orientFixed),
		new Position(0,   550,    0, s_orientFixed),
		new Position(0,   600,    0, s_orientFixed),
		new Position(0,   650,    0, s_orientFixed),
		new Position(0,   700,    0, s_orientFixed),
		new Position(0,   750,    0, s_orientFixed),
		new Position(0,   800,    0, s_orientFixed),
		new Position(0,   850,    0, s_orientFixed),
		new Position(0,   900,    0, s_orientFixed),
		new Position(0,   950,    0, s_orientFixed),

		new Position(0,  1000,    0, s_orientFixed),
		new Position(0,  1050,    0, s_orientFixed),
		new Position(0,  1100,    0, s_orientFixed),
		new Position(0,  1150,    0, s_orientFixed),
		new Position(0,  1200,    0, s_orientFixed),
		new Position(0,  1250,    0, s_orientFixed),
		new Position(0,  1300,    0, s_orientFixed),
		new Position(0,  1350,    0, s_orientFixed),
		new Position(0,  1400,    0, s_orientFixed),
		new Position(0,  1450,    0, s_orientFixed),
		new Position(0,  1500,    0, s_orientFixed),
		new Position(0,  1550,    0, s_orientFixed),
		new Position(0,  1600,    0, s_orientFixed),
		new Position(0,  1650,    0, s_orientFixed),
		new Position(0,  1700,    0, s_orientFixed),
		new Position(0,  1750,    0, s_orientFixed),
		new Position(0,  1800,    0, s_orientFixed),
		new Position(0,  1850,    0, s_orientFixed),
		new Position(0,  1900,    0, s_orientFixed),
		new Position(0,  1950,    0, s_orientFixed),
		new Position(0,  2000, 3000, s_orientFixed),
		new Position(0,  1950, 6000, s_orientFixed),
		new Position(0,  1900, 6000, s_orientFixed),
		new Position(0,  1850, 6000, s_orientFixed),
		new Position(0,  1800, 6000, s_orientFixed),
		new Position(0,  1750, 6000, s_orientFixed),
		new Position(0,  1700, 6000, s_orientFixed),
		new Position(0,  1650, 6000, s_orientFixed),
		new Position(0,  1600, 6000, s_orientFixed),
		new Position(0,  1550, 6000, s_orientFixed),
		new Position(0,  1500, 6000, s_orientFixed),
		new Position(0,  1450, 6000, s_orientFixed),
		new Position(0,  1400, 6000, s_orientFixed),
		new Position(0,  1350, 6000, s_orientFixed),
		new Position(0,  1300, 6000, s_orientFixed),
		new Position(0,  1250, 6000, s_orientFixed),
		new Position(0,  1200, 6000, s_orientFixed),
		new Position(0,  1150, 6000, s_orientFixed),
		new Position(0,  1100, 6000, s_orientFixed),
		new Position(0,  1050, 6000, s_orientFixed),

		new Position(0,  1000, 6000, s_orientFixed),
		new Position(0,   950, 6000, s_orientFixed),
		new Position(0,   900, 6000, s_orientFixed),
		new Position(0,   850, 6000, s_orientFixed),
		new Position(0,   800, 6000, s_orientFixed),
		new Position(0,   750, 6000, s_orientFixed),
		new Position(0,   700, 6000, s_orientFixed),
		new Position(0,   650, 6000, s_orientFixed),
		new Position(0,   600, 6000, s_orientFixed),
		new Position(0,   550, 6000, s_orientFixed),
		new Position(0,   500, 6000, s_orientFixed),
		new Position(0,   450, 6000, s_orientFixed),
		new Position(0,   400, 6000, s_orientFixed),
		new Position(0,   350, 6000, s_orientFixed),
		new Position(0,   300, 6000, s_orientFixed),
		new Position(0,   250, 6000, s_orientFixed),
		new Position(0,   200, 6000, s_orientFixed),
		new Position(0,   150, 6000, s_orientFixed),
		new Position(0,   100, 6000, s_orientFixed),
		new Position(0,    50, 6000, s_orientFixed),
		new Position(0,     0, 6000, s_orientFixed),
		new Position(0,   -50, 6000, s_orientFixed),
		new Position(0,  -100, 6000, s_orientFixed),
		new Position(0,  -150, 6000, s_orientFixed),
		new Position(0,  -200, 6000, s_orientFixed),
		new Position(0,  -250, 6000, s_orientFixed),
		new Position(0,  -300, 6000, s_orientFixed),
		new Position(0,  -350, 6000, s_orientFixed),
		new Position(0,  -400, 6000, s_orientFixed),
		new Position(0,  -450, 6000, s_orientFixed),
		new Position(0,  -500, 6000, s_orientFixed),
		new Position(0,  -550, 6000, s_orientFixed),
		new Position(0,  -600, 6000, s_orientFixed),
		new Position(0,  -650, 6000, s_orientFixed),
		new Position(0,  -700, 6000, s_orientFixed),
		new Position(0,  -750, 6000, s_orientFixed),
		new Position(0,  -800, 6000, s_orientFixed),
		new Position(0,  -850, 6000, s_orientFixed),
		new Position(0,  -900, 6000, s_orientFixed),
		new Position(0,  -950, 6000, s_orientFixed),

		new Position(0, -1000, 6000, s_orientFixed),
		new Position(0, -1050, 6000, s_orientFixed),
		new Position(0, -1100, 6000, s_orientFixed),
		new Position(0, -1150, 6000, s_orientFixed),
		new Position(0, -1200, 6000, s_orientFixed),
		new Position(0, -1250, 6000, s_orientFixed),
		new Position(0, -1300, 6000, s_orientFixed),
		new Position(0, -1350, 6000, s_orientFixed),
		new Position(0, -1400, 6000, s_orientFixed),
		new Position(0, -1450, 6000, s_orientFixed),
		new Position(0, -1500, 6000, s_orientFixed),
		new Position(0, -1550, 6000, s_orientFixed),
		new Position(0, -1600, 6000, s_orientFixed),
		new Position(0, -1650, 6000, s_orientFixed),
		new Position(0, -1700, 6000, s_orientFixed),
		new Position(0, -1750, 6000, s_orientFixed),
		new Position(0, -1800, 6000, s_orientFixed),
		new Position(0, -1850, 6000, s_orientFixed),
		new Position(0, -1900, 6000, s_orientFixed),
		new Position(0, -1950, 6000, s_orientFixed),
		new Position(0, -2000, 3000, s_orientFixed),
		new Position(0, -1950,    0, s_orientFixed),
		new Position(0, -1900,    0, s_orientFixed),
		new Position(0, -1850,    0, s_orientFixed),
		new Position(0, -1800,    0, s_orientFixed),
		new Position(0, -1750,    0, s_orientFixed),
		new Position(0, -1700,    0, s_orientFixed),
		new Position(0, -1650,    0, s_orientFixed),
		new Position(0, -1600,    0, s_orientFixed),
		new Position(0, -1550,    0, s_orientFixed),
		new Position(0, -1500,    0, s_orientFixed),
		new Position(0, -1450,    0, s_orientFixed),
		new Position(0, -1400,    0, s_orientFixed),
		new Position(0, -1350,    0, s_orientFixed),
		new Position(0, -1300,    0, s_orientFixed),
		new Position(0, -1250,    0, s_orientFixed),
		new Position(0, -1200,    0, s_orientFixed),
		new Position(0, -1150,    0, s_orientFixed),
		new Position(0, -1100,    0, s_orientFixed),
		new Position(0, -1050,    0, s_orientFixed),
	};

	private StructureLevel m_level;
	private boolean m_bDirectionRight;	// True to move the belt right.
	private int m_iSections[];			// Section index of the ports.
	private int m_nVelocity;		// Velocity of track motion based on steps forward or back.

	/***********************************************************************************************
	 * BeltPort constructor based on its position and structure levels it connects with.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param aLevels
	 *	    The structure levels this lift connects with.
	 * @param rollavator
	 *	    The Rollavator this is owned by.
	 */
	public BeltPort(
		Position position,
		StructureLevel towerLevel,
		Rollavator rollavator
	) throws Exception
	{
		super(s_mass, s_size, position, null);
		
		Trace.startOf("rollavator.BeltPort()");

		m_size = s_size;

		// Save a reference to our tower levels.
		m_level = towerLevel;

		// Lay the return track.
		setTrack(s_aPosTrack, position);

		// Mark the section indexes where our port doors reside.
		m_iSections = new int[2];
		m_iSections[0] = 0;
		m_iSections[1] = 40;

		// By default, try to move the car towards the down side.
		m_bDirectionRight = true;

		// Create four BoxPort objects to cycle around our BeltPort.
		int iPos = 0;
		for (int i = 0; i < 4; ++i)
		{
			BoxPort box = new BoxPort(position, towerLevel, rollavator);
			add(iPos, box, null);
			iPos += s_aPosTrack.length / 4;

			if (i == 1)
			{
				// By default, each BeltPort has one RollaCar on its first BoxPort slot.
				RollaCar car = new RollaCar(position, rollavator);
				box.add(car);
			}
		}

		Trace.endOf("rollavator.BeltPort()");
	}

	/***********************************************************************************************
	 * Return true if the belt can be moved.
	 *
	 * @return
	 *	    True if the box can move.
	 */
	public boolean canMove() throws Exception
	{
		boolean bCanMove = true;
		boolean bHaveCar = false;

		for (TrackCar trackCar : m_lCars)
		{
			BoxPort box = (BoxPort)trackCar;
			RollaCar car = (RollaCar)box.getTrackCar();
			if (car != null)
			{
				// If the lift door isn't completely closed, don't move.
				if (!car.isClosed())
				{
					bCanMove = false;
					break;
				}

				// Can't move sideways if we're married to a LiftReturn.
				if (box.getSpouse() != null)
				{
					bCanMove = false;
					break;
				}

				// If there are occupants in the car, don't move sideways.
				if (!car.isEmpty())
				{
					bCanMove = false;
					break;
				}

				// Can't rotate up around if we're carrying a car at the shaft wall.
				int iSection = box.getSection().getIndex();
				if ((iSection == m_iSections[0] && !m_bDirectionRight) ||
					(iSection == m_iSections[1] && m_bDirectionRight))
				{
					bCanMove = false;
					break;
				}
				bHaveCar = true;
			}
		}

		// If we have no cars, we shouldn't be trying to move them.
		if (!bHaveCar)
		{
			bCanMove = false;
		}

		// If we can't move, make sure we're not at speed.
		if (!bCanMove)
		{
			if (m_nVelocity > 0)
			{
				if (--m_nVelocity != 0)
				{
					Trace.fatal("BeltPort halted from moving when at speed.");
				}
			}
			return false;
		}

		return true;
	}

	/***********************************************************************************************
	 * Return, if available, the BoxPort which is located at the given box index slot, else null.
	 *
	 * @param iBox
	 *		The box index on this port.
	 * @return
	 *	    If available, the BoxPort which is located at the given box index slot, else null.
	 */
	public BoxPort getBoxPort(int iBox)
	{
		// Verify our box index.
		if (iBox < 0 || iBox >= m_iSections.length)
		{
			return null;
		}

		for (TrackCar trackCar : m_lCars)
		{
			BoxPort box = (BoxPort)trackCar;
			if (box.getSection().getIndex() == m_iSections[iBox])
			{
				return box;
			}
		}
		return null;
	}

	/***********************************************************************************************
	 * Return the nth door, if present.
	 *
	 * @param iDoor
	 *		The door index on this level for this lift (starting from 0).
	 * @return
	 *		The door.
	 */
	public Door getDoor(int iDoor)
	{
		// Get the section index where this door would have to be.
		if (iDoor < 0 || iDoor >= m_iSections.length)
		{
			return null;
		}
		int iSection = m_iSections[iDoor];

		// Check to see if we have a track car at this section.  If so, return its door.
		for (TrackCar trackCar : m_lCars)
		{
			BoxPort box = (BoxPort)trackCar;
			if (box.getSection().getIndex() == iSection)
			{
				RollaCar car = (RollaCar)box.getTrackCar();
				if (car != null)
				{
					return car.getDoor();
				}
				break;
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
		// Get the section index where this door would have to be.
		if (iDoor < 0 || iDoor >= m_iSections.length)
		{
			return null;
		}
		int iSection = m_iSections[iDoor];

		return m_aSections[iSection];
	}

	/***********************************************************************************************
	 * Return, if available, the TrackCar which is located at the given box index slot, else null.
	 *
	 * @param iBox
	 *		The box index on this port.
	 * @return
	 *	    If available, the TrackCar which is located at the given box index slot, else null.
	 */
	public TrackCar getTrackCar(int iBox)
	{
		// Verify our box index.
		if (iBox < 0 || iBox >= m_iSections.length)
		{
			return null;
		}

		for (TrackCar trackCar : m_lCars)
		{
			BoxPort box = (BoxPort)trackCar;
			if (box.getSection().getIndex() == m_iSections[iBox])
			{
				return box.getTrackCar();
			}
		}
		return null;
	}

	/***********************************************************************************************
	 * Return true if the belt has a box that is carrying a car that is at the left door slot.
	 *
	 * @return
	 *	    True if the belt has a box that is carrying a car that is at the left door slot.
	 */
	public boolean isLeft()
	{
		for (TrackCar trackCar : m_lCars)
		{
			BoxPort box = (BoxPort)trackCar;
			if (box.getTrackCar() != null &&
				box.getSection().getIndex() == m_iSections[0])
			{
				return true;
			}
		}
		return false;
	}

	/***********************************************************************************************
	 * Return true if the belt has a box that is carrying a car that is at the right door slot.
	 *
	 * @return
	 *	    True if the belt has a box that is carrying a car that is at the right door slot.
	 */
	public boolean isRight()
	{
		for (TrackCar trackCar : m_lCars)
		{
			BoxPort box = (BoxPort)trackCar;
			if (box.getTrackCar() != null &&
				box.getSection().getIndex() == m_iSections[1])
			{
				return true;
			}
		}
		return false;
	}

	/***********************************************************************************************
	 * Move the belt, and thus the BoxPort which resides under to the left or right.
	 * @throws Exception 
	 */
	public void move() throws Exception
	{
		// If we can move, then there can be only one car we have to worry about.
		BoxPort boxCarrier = null;
		for (TrackCar trackCar : m_lCars)
		{
			boxCarrier = (BoxPort)trackCar;
			if (boxCarrier.getTrackCar() != null)
			{
				break;
			}
		}
		if (boxCarrier != null)
		{
			boolean bReachedDest = false;

			if (m_bDirectionRight)
			{
				// Before we potentially change our velocity, remember that for this
				// step so we can use it with the empty carriers.
				int nVelocity = m_nVelocity;

				// Try to move this Car. If needed, allow it to decelerate by one.
				int nTicks = m_nVelocity;
				while (--nTicks >= 0)
				{
					boxCarrier.moveToNext();
					if (boxCarrier.getSection().getIndex() == m_iSections[1])
					{
						bReachedDest = true;
						break;
					}
				}
				if (nTicks >= 0)
				{
					if (--m_nVelocity > 0)
					{
						Trace.fatal("BoxPort would have caused a collision.");
					}
				}

				// If we haven't reached out destination, then we can change our speed.
				if (!bReachedDest)
				{
					// See if we need to maintain, and want to accelerate our velocity.
					int nNeed = Section.s_aNumVelSteps[m_nVelocity];
					int nWant = Section.s_aNumVelSteps[m_nVelocity + 1];
					if (m_nVelocity >= s_nMaxVelocity)
					{
						nWant = 0;
					}

					// Loop through to see how many open sections we have.
					Section next = boxCarrier.getSection();
					while (true)
					{
						next = next.getNext();

						// If we have the number of sections that we want to accelerate, do so.
						if (--nWant == 0)
						{
							++m_nVelocity;
							break;
						}

						// If we can't accelerate and have what we need to maintain this speed, break out.
						if (--nNeed <= 0 && nWant <= 0)
						{
							break;
						}

						// Don't overreach our destination.
						if (next.getIndex() == m_iSections[1])
						{
							break;
						}
					}

					// If our velocity is too fast, decelerate.
					if (nNeed > 0)
					{
						--m_nVelocity;
					}
				}

				// Move each box together, but make sure our carrier doesn't crash.
				if (nVelocity > 0)
				{
					for (TrackCar trackCar : m_lCars)
					{
						BoxPort box = (BoxPort)trackCar;
						if (box != boxCarrier)
						{
							for (int i = 0; i < nVelocity; ++i)
							{
								box.moveToNext();
							}
						}
					}
				}
			}
			else
			{
				// Before we potentally change our velocity, remember that for this
				// step so we can use it with the empty carriers.
				int nVelocity = m_nVelocity;

				// Try to move this Car. If needed, allow it to decelerate by one.
				int nTicks = m_nVelocity;
				while (--nTicks >= 0)
				{
					boxCarrier.moveToPrevious();
					if (boxCarrier.getSection().getIndex() == m_iSections[0])
					{
						bReachedDest = true;
						break;
					}
				}
				if (nTicks >= 0)
				{
					if (--m_nVelocity > 0)
					{
						Trace.fatal("BoxPort would have caused a collision.");
					}
				}

				// If we haven't reached out destination, then we can change our speed.
				if (!bReachedDest)
				{
					// See if we need to maintain, and want to accelerate our velocity.
					int nNeed = Section.s_aNumVelSteps[m_nVelocity];
					int nWant = Section.s_aNumVelSteps[m_nVelocity + 1];
					if (m_nVelocity >= s_nMaxVelocity)
					{
						nWant = 0;
					}

					// Loop through to see how many open sections we have.
					Section prev = boxCarrier.getSection();
					while (true)
					{
						prev = prev.getPrevious();

						// If we have the number of sections that we want to accelerate, do so.
						if (--nWant == 0)
						{
							++m_nVelocity;
							break;
						}

						// If we can't accelerate and have what we need to maintain this speed, break out.
						if (--nNeed <= 0 && nWant <= 0)
						{
							break;
						}

						// Don't overreach our destination.
						if (prev.getIndex() == m_iSections[0])
						{
							break;
						}
					}

					// If our velocity is too fast, decelerate.
					if (nNeed > 0)
					{
						--m_nVelocity;
					}
				}

				// Move each box together, but make sure our carrier doesn't crash.
				if (nVelocity > 0)
				{
					for (TrackCar trackCar : m_lCars)
					{
						BoxPort box = (BoxPort)trackCar;
						if (box != boxCarrier)
						{
							for (int i = 0; i < nVelocity; ++i)
							{
								box.moveToPrevious();
							}
						}
					}
				}
			}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(TrackCar car) throws Exception
	{
		for (TrackCar trackCar : m_lCars)
		{
			BoxPort box = (BoxPort)trackCar;

			if (box.offer(car))
			{
				m_bDirectionRight = false;
				if (box.getSection().getIndex() == m_iSections[0])
				{
					m_bDirectionRight = true;
				}

				LiftCar liftCar = (LiftCar)car;
				liftCar.removeFloorID();
				return true;
			}
		}
		return false;
	}

	/***********************************************************************************************
	 * Request a car to be sent down to replace one leaving a carousel.
	 *
	 * @param idFloorDest
	 *		Try to direct the car towards this destination level.
	 */
	public void requestNewCar(int idFloorDest)
	{
		// Look for an available car.
		for (TrackCar trackCar : m_lCars)
		{
			BoxPort box = (BoxPort)trackCar;
			RollaCar car = (RollaCar)box.getTrackCar();
			if (car != null)
			{
				// Only give this car a destination if is doesn't already have one.
				if (car.getNextFloorID(box.getSection().getIndex() == m_iSections[0]) < 0)
				{
					car.setNextFloorID(idFloorDest);
					car.close(false);

					Trace.important("Requested car (" + car + ") to replace another");
				}
			}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("rollavator.BeltPort.update()");

		// Run an update internal to the box.
		for (TrackCar car : m_lCars)
		{
			car.update(sim);
		}

		// For each time cycle, put the items along the track one position.
		int nCycles = sim.getUpdateCyclesCount();
		do
		{
			if (canMove())
			{
				move();
			}
		} while (--nCycles > 0);

		Trace.endOf("rollavator.BeltPort.update()");
	}
}

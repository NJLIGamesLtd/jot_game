/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A LevelOffice is a TowerLevel that holds rented office space.
 */

package com.jot.jottower;

import com.jot.item.*;
import com.jot.lift.*;
import com.jot.random.Random;
import com.jot.rollavator.BeltPort;
import com.jot.rollavator.RollaCar;
import com.jot.rollavator.Rollavator;
import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.*;

import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/***************************************************************************************************
 * An LevelOffice is a StructureLevel where offices are housed.
 */
public class LevelOffice extends StructureLevelImpl
{
	private static final int s_mass = 0;
	private static final Size s_size = new Size(106000, 122400, 4000);

	private static final CollisionGrid.GridItem s_gridItems[] =
	{
		// Support pillars from center out.
		new CollisionGrid.GridCircle(0, -60000, 1000),
		new CollisionGrid.GridCircle(0, -45000, 1000),
		new CollisionGrid.GridCircle(0, -30000, 1000),
		new CollisionGrid.GridCircle(0, -15000, 1000),
		new CollisionGrid.GridCircle(0,      0, 1000),
		new CollisionGrid.GridCircle(0,  30000, 1000),
		new CollisionGrid.GridCircle(0,  45000, 1000),
		new CollisionGrid.GridCircle(0,  60000, 1000),
		
		new CollisionGrid.GridCircle(-13000, -52500, 1000),	new CollisionGrid.GridCircle(13000, -52500, 1000),
		new CollisionGrid.GridCircle(-13000, -37500, 1000),	new CollisionGrid.GridCircle(13000, -37500, 1000),
		new CollisionGrid.GridCircle(-13000, -22500, 1000),	new CollisionGrid.GridCircle(13000, -22500, 1000),
		new CollisionGrid.GridCircle(-13000,   7500, 1000),	new CollisionGrid.GridCircle(13000,   7500, 1000),
		new CollisionGrid.GridCircle(-13000,  22500, 1000),	new CollisionGrid.GridCircle(13000,  22500, 1000),
		new CollisionGrid.GridCircle(-13000,  37500, 1000),	new CollisionGrid.GridCircle(13000,  37500, 1000),
		new CollisionGrid.GridCircle(-13000,  52500, 1000),	new CollisionGrid.GridCircle(13000,  52500, 1000),
		
		new CollisionGrid.GridCircle(-26000, -45000, 1000),	new CollisionGrid.GridCircle(26000, -45000, 1000),
		new CollisionGrid.GridCircle(-26000, -30000, 1000),	new CollisionGrid.GridCircle(26000, -30000, 1000),
		new CollisionGrid.GridCircle(-26000, -15000, 1000),	new CollisionGrid.GridCircle(26000, -15000, 1000),
		new CollisionGrid.GridCircle(-26000,      0, 1000),	new CollisionGrid.GridCircle(26000,      0, 1000),
		new CollisionGrid.GridCircle(-26000,  15000, 1000),	new CollisionGrid.GridCircle(26000,  15000, 1000),
		new CollisionGrid.GridCircle(-26000,  30000, 1000),	new CollisionGrid.GridCircle(26000,  30000, 1000),
		new CollisionGrid.GridCircle(-26000,  45000, 1000),	new CollisionGrid.GridCircle(26000,  45000, 1000),
		
		new CollisionGrid.GridCircle(-39000, -37500, 1000),	new CollisionGrid.GridCircle(39000, -37500, 1000),
		new CollisionGrid.GridCircle(-39000, -22500, 1000),	new CollisionGrid.GridCircle(39000, -22500, 1000),
		new CollisionGrid.GridCircle(-39000,  -7500, 1000),	new CollisionGrid.GridCircle(39000,  -7500, 1000),
		new CollisionGrid.GridCircle(-39000,   7500, 1000),	new CollisionGrid.GridCircle(39000,   7500, 1000),
		new CollisionGrid.GridCircle(-39000,  22500, 1000),	new CollisionGrid.GridCircle(39000,  22500, 1000),
		new CollisionGrid.GridCircle(-39000,  37500, 1000),	new CollisionGrid.GridCircle(39000,  37500, 1000),
		
		new CollisionGrid.GridCircle(-52000, -30000, 1000),	new CollisionGrid.GridCircle(52000, -30000, 1000),
		new CollisionGrid.GridCircle(-52000, -15000, 1000),	new CollisionGrid.GridCircle(52000, -15000, 1000),
		new CollisionGrid.GridCircle(-52000,      0, 1000),	new CollisionGrid.GridCircle(52000,      0, 1000),
		new CollisionGrid.GridCircle(-52000,  15000, 1000),	new CollisionGrid.GridCircle(52000,  15000, 1000),
		new CollisionGrid.GridCircle(-52000,  30000, 1000),	new CollisionGrid.GridCircle(52000,  30000, 1000),
		
		// Suite dividing walls.
		new CollisionGrid.GridRectangle(     0,  21130,      0,  60000, 1000),
		new CollisionGrid.GridRectangle( 18300, -10570,  52000, -30000, 1000),
		new CollisionGrid.GridRectangle(-18300, -10570, -52000, -30000, 1000),
		
		// Core walls
		new CollisionGrid.GridRectangle(-16800, -11430,  -1000, -11430, 1000),
		new CollisionGrid.GridRectangle( 16800, -11430,   1000, -11430, 1000),
		new CollisionGrid.GridRectangle(-18300,  -8340, -10460,   4880, 1000),
		new CollisionGrid.GridRectangle( 18300,  -8340,  10460,   4880, 1000),
		new CollisionGrid.GridRectangle( -9460,   6620,  -1500,  20270, 1000),
		new CollisionGrid.GridRectangle(  9460,   6620,   1500,  20270, 1000),
	};

	private static final Location[] s_aLocCubes =
	{
		new Location(     0,  60000, 0),
		new Location( 13000, -52500, 0),
		new Location(-13000, -52500, 0),
	};

	private static final Size s_sizeCube = new Size(2000, 2000, 3000);

	private List<PlaceImpl> m_lCubicles;
	private List<Drone> m_lPersonsDirected;
	private Queue<Drone>[] m_aqPersonsExiting;
	private Door[][] m_aaDoors;		// The Door objects associated with this level.
	public BeltPort[] m_aPorts;
	private Lift[] m_aLifts;
	private CollisionGrid m_collisionGrid;

	/***********************************************************************************************
	 * LevelOffice constructor based on its position and lifts.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param towerLifts
	 *	    An array of lifts that can be used to access this level.
	 */
	public LevelOffice(Position position, Lift[] towerLifts) throws Exception
	{
		super(s_mass, s_size, position);

		Trace.startOf("LevelOffice()");

		m_aLifts = towerLifts;

		// Create a list object to manage people on this level.
		m_lPersonsDirected = new ArrayList<Drone>();

		// Create list objects to manage people coming and going.
		m_aqPersonsExiting = new Queue[2];
		m_aqPersonsExiting[0] = new ArrayDeque<Drone>();
		m_aqPersonsExiting[1] = new ArrayDeque<Drone>();

		// Create cubicles for people to work at.
		m_lCubicles = new ArrayList<PlaceImpl>();
		for (Location locCube: s_aLocCubes)
		{
			PlaceImpl cube = new PlaceImpl(0, s_sizeCube, new Position(locCube.m_x, locCube.m_y, position.m_z, 0));
			m_lCubicles.add(cube);
		}

		// Create an array of port references.
		m_aPorts = new BeltPort[towerLifts.length];

		// Create the Door objects associated with each Lift on this Level.
		m_aaDoors = new Door[towerLifts.length][2];

		m_collisionGrid = new CollisionGrid(110000, 124000);
		m_collisionGrid.addBlock(s_gridItems);

		Trace.endOf("LevelOffice()");
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void add(Drone person)
	{
		// Add the drone to the exit queue.
		m_aqPersonsExiting[1].add(person);

		// Place it near the port door.
		Position position = m_aaDoors[0][1].getPosition();
		person.setPosition(position);

		// Tell the drone it is in the control of the lobby;
		person.setPlace(this);

		// Tell the lift to add a destination to pick up this person.
		m_aLifts[0].pushDestination(person);
	}

	/***********************************************************************************************
	 * Return a two dimensional image of the collision grid that represents the walls and people.
	 * If needed, update the image to include the latest changes.
	 *
	 * @return
	 *		A two dimensional item of this item.
	 */
	public Image getImage()
	{
		return m_collisionGrid.getImage();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(Drone drone) throws Exception
	{
		Robot person = (Robot)drone;
		
		Trace.important("LevelOffice: person (" + person + ") arrived at position: " + person.getLocation());

		// If there is a vacant location at the office's egress, place our person there.
		// SPK: Force people to get out of the way of those exiting the elevator.
		if (!m_collisionGrid.offer(person))
		{
			Trace.critical("LevelOffice.arrive(): Collision occured. Person rejected.");
			return false;
		}
		person.setCollisionGrid(m_collisionGrid);

		// Pop off the office from the destination stack and direct the person its cubicle.
		person.popDestination();
		person.pushDestination(m_lCubicles.get(Random.GetInt(m_lCubicles.size())));

		// Tell the drone it is in the control of the lobby;
		person.setPlace(this);

		return m_lPersonsDirected.add(person);
	}

	/***********************************************************************************************
	 * Paint the graphical representation of the current time state of the simulation.
	 */
	public void paint(Graphics g)
	{
		m_collisionGrid.paint(g);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setLiftConnections() throws Exception
	{
		// Need our office level index.
		int iLevel = m_position.getZ() / 4000;

		// Populate these arrays.
		for (int iLift = 0; iLift < m_aLifts.length; ++iLift)
		{
			Rollavator lift = (Rollavator)m_aLifts[iLift];

			BeltPort port = m_aPorts[iLift] = lift.getLevelPort(iLevel);

			m_aaDoors[iLift][0] = new Door(port.getDoorPosition(0), null);
			m_aaDoors[iLift][1] = new Door(port.getDoorPosition(1), null);			
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return m_collisionGrid.toString();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("LevelOffice.update()");

		// Update each person on the level.
		for (Drone person : m_lPersonsDirected)
		{
			Trace.trivial("OfficeLobby.update(): Update " + person);
			person.update(sim);
		}

		// Update the door connection with the ports.
		for (int iPort = 0; iPort < m_aPorts.length; ++iPort)
		{
			BeltPort port = m_aPorts[iPort];
			Door[] aDoors = m_aaDoors[iPort];
			for (int iDoor = 0; iDoor < aDoors.length; ++iDoor)
			{
				RollaCar car = (RollaCar)port.getTrackCar(iDoor);
				if (car == null)
				{
					continue;
				}

				// If we have a car that we have are not married to, see if we need to.
				Door door = aDoors[iDoor];
				if (door.getSpouse() == null && !car.isNext() && car.getDirection())
				{
					if (!car.isEmpty() ||
						!m_aqPersonsExiting[iDoor].isEmpty())
					{
						door.setSpouse(car.getDoor());
					}
				}

				// Update the operation of the door.
				door.update(sim);

				// If our door is opening, see if we have people trying to exit and/or enter.
				if (door.isOpenEnough())
				{
					boolean bExchanging = door.isOpening();

					// For each time cycle, put the cars along the track one section.
					int nCycles = sim.getUpdateCyclesCount();
					do
					{
						// Allow people to exit the car.
						Drone person;
						if ((person = car.poll()) == null)
						{
							break;
						}

						// Change the person's location to that of the door.
						person.setLocation(door.getLocation());

						// Offer the person to the office level.
						if (!offer(person))
						{
							Trace.fatal("LevelOffice.update(): Person rejected by CollisionGrid.");
						}

						bExchanging = true;
					} while (--nCycles > 0);

					// For the remaining time, see if people want to enter the car.
					while (nCycles-- > 0)
					{
						// Allow people to enter the car.
						Drone person;
						if ((person = m_aqPersonsExiting[iDoor].poll()) == null)
						{
							break;
						}

						// If the person doesn't fit, force him to wait for another.
						if (!car.offer(person))
						{
							m_aqPersonsExiting[iDoor].offer(car);

							// Tell the lift to send another car.
							m_aLifts[0].pushDestination(person);
							break;
						}
						bExchanging = true;
					}

					// If we're no longer exchanging people, close the doors.
					if (!bExchanging)
					{
						door.close(true);
					}
				}
			}
		}

		Trace.endOf("LevelOffice.update()");
	}
}

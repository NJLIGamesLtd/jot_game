/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A LiftCar is an Item that is directed on a Track and carries people within an elevator system.
 */

package com.jot.lift;

import com.jot.item.Drone;
import com.jot.item.Person;
import com.jot.item.Place;
import com.jot.item.Robot;
import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.CarReturn;
import com.jot.track.Section;
import com.jot.vector.Location;
import com.jot.vector.Position;
import com.jot.vector.Size;

import java.util.*;

/***************************************************************************************************
 * A LiftCar supports the tightly packing of robot persons and is directed itself as a robot.
 */
public class LiftCar extends CarReturn
{	
	private static final Cycles s_cyclesDoorDef = new Cycles(2.0);
	private static final int s_massDef = 300;
	private static final int s_massDoorDef = 50;
	private static final int s_nMaxVelocity = 3;	// Number of motion steps per tick.
	private static final Size s_sizeOuterDef = new Size(1760, 1500, 3000);
	private static final Size s_sizeInnerDef = new Size(1600, 1200, 2500);
	private static final Size s_sizeDoorDef = new Size(1200, 200, 2300);

	private static int s_idNew = 1000;

	protected Lift m_lift;
	protected Set<Integer> m_sFloors;		// Set of floors we are traveling towards.
	protected List<Drone> m_lDrones;
	protected Queue<Drone> m_qDroneExiting;
	protected Door m_door;
	protected boolean m_bIsFull;
	protected Size m_sizeInner;
	protected int m_id;

	/***********************************************************************************************
	 * LiftCar initializer.
	 *
	 * @param massDoor
	 *		The kg mass of the door.
	 * @param lift
	 *		The parent Lift object that this LiftCar belongs to.
	 * @param sizeInner
	 *	    The vector size of the occupiable space within the LiftCar.
	 * @param sizeDoor
	 *	    The vector size of the LiftCar Door.
	 * @param cyclesDoor
	 *		The number of Cycles to open or close the Door.
	 */
	private void initialize(
		int massDoor,
		Lift lift,
		Size sizeInner,
		Size sizeDoor,
		Cycles cyclesDoor
	) throws Exception
	{
		Trace.startOf("LiftCar.initialize()");
		
		m_id = ++s_idNew;

		// A lift car has to belong to a lift.
		if ((m_lift = lift) == null)
		{
			throw new Exception("Must provide a non-null Lift for this LiftCar.");
		}

		m_sizeInner = sizeInner;
		m_door = new Door(massDoor, sizeDoor, m_position, cyclesDoor);
		m_lDrones = new ArrayList<Drone>();
		m_qDroneExiting = new ArrayDeque<Drone>();
		m_bIsFull = false;

		// Create a set of floor destinations for us to hit.
		m_sFloors = new TreeSet<Integer>();

		// Verify the inside is not larger than the outside.
		if (m_sizeInner.m_x > m_size.m_x ||
			m_sizeInner.m_y > m_size.m_y ||
			m_sizeInner.m_z > m_size.m_z)
		{
			throw new Exception("Inside of LiftCar is larger than the outside.");
		}

		// Verify the door is not larger than the inside.
		if (sizeDoor.m_x > m_sizeInner.m_x)
		{
			throw new Exception("Door is wider than the inside of the LiftCar.");
		}
		if (sizeDoor.m_x > m_sizeInner.m_x)
		{
			throw new Exception("Door is taller than the inside of the LiftCar.");
		}
		if (sizeDoor.m_y > m_size.m_y - m_sizeInner.m_y)
		{
			throw new Exception("Door is thicker than the walls of the LiftCar.");
		}

		Trace.endOf("LiftCar.initialize()");
	}

	/***********************************************************************************************
	 * LiftCar constructor based on its parent Lift. Other measures based on defaults.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param lift
	 *		The parent Lift object that this LiftCar belongs to.
	 */
	public LiftCar(Position position, Lift lift) throws Exception
	{
		super(s_massDef, s_sizeOuterDef, position, s_nMaxVelocity);

		Trace.startOf("LiftCar()");
		initialize(
			s_massDoorDef, lift,
			s_sizeInnerDef, s_sizeDoorDef,
			s_cyclesDoorDef);
		Trace.endOf("LiftCar()");
	}

	/***********************************************************************************************
	 * LiftCar constructor based on its mass, size and position.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param lift
	 *		The parent Lift object that this LiftCar belongs to.
	 * @param massCar
	 *		The total kg mass of this LiftCar.
	 * @param massDoor
	 *		The kg mass of the door used by this LiftCar.
	 * @param sizeOuter
	 *	    The exterior vector Size of the total LiftCar.
	 * @param sizeInner
	 *	    The interior vector Size of the occupied space within the LiftCar.
	 * @param sizeDoor
	 *	    The vector Size of LiftCar Door.
	 * @param cyclesDoor
	 *		The number of Cycles to open or close the Door.
	 * @param nMaxVelocity
	 *		The maximum number of motion steps per tick.
	 */
	public LiftCar(
		Position position,
		Lift lift,
		int massCar,
		int massDoor,
		Size sizeOuter,
		Size sizeInner,
		Size sizeDoor,
		Cycles cyclesDoor,
		int nMaxVelocity
	) throws Exception
	{
		super(massCar, sizeOuter, position, nMaxVelocity);

		Trace.startOf("LiftCar()");
		initialize(massDoor, lift, sizeInner, sizeDoor, cyclesDoor);
		Trace.endOf("LiftCar()");
	}

	/***********************************************************************************************
	 * If there is room, add this person to the lift car and return true.  Else, return false
	 * and leave the person waiting for another.
	 *
	 * @param person
	 *		The Item to be added.
	 */
	private boolean add(Robot person) throws Exception
	{
		// If car is full or the door is closed, reject this person.
		if (m_bIsFull || m_door.isClosed() == true)
		{
			return false;
		}

		Position posOpen = new Position();
		posOpen.setOrientation(getOrientation());
		if (isRoom(person, posOpen))
		{
			// If the door is closing, force it back open.
			if (m_door.isClosing())
			{
				m_door.open();
			}

			// If the person is walking on a collision grid, remove it.
			person.removeFromGrid();

			m_lDrones.add(person);
			person.setPosition(posOpen);
			person.m_velocity = null;
			person.m_locFinal = null;

			// Peek at the floor destination to see what floor we need to stop at.
			Place placeDest = person.peekDestination();
			m_sFloors.add(placeDest.getLocation().getZ());
			return true;
		}
		return false;
	}

	/***********************************************************************************************
	 * If there is room, add this person to the lift car and return true.  Else, return false
	 * and leave the person waiting for another.
	 *
	 * @param person
	 *		The Item to be added.
	 */
	public boolean addTest(Robot person) throws Exception
	{
		// If car is full, reject this person.
		if (m_bIsFull)
		{
			return false;
		}
		
		Position posOpen = new Position();
		posOpen.setOrientation(getOrientation());
		if (isRoom(person, posOpen))
		{
			// If the person is walking on a collision grid, remove it.
			person.removeFromGrid();

			m_lDrones.add(person);
			person.setPosition(posOpen);
			person.m_velocity = null;
			person.m_locFinal = null;

			// Peek at the floor destination to see what floor we need to stop at.
			Place placeDest = person.peekDestination();
			m_sFloors.add(new Integer(placeDest.getLocation().getZ()));
			return true;
		}
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean canMove(boolean recurse) throws Exception
	{
		if (m_door.isClosed())
		{
			return super.canMove(recurse);
		}
		return false;
	}

	/***********************************************************************************************
	 * Return the door object for this car.
	 *
	 * @return
	 *		The Door object that controls entry and exiting.
	 */
	public Door getDoor()
	{
		return m_door;
	}

	/***********************************************************************************************
	 * Return the number of remaining destination floors.
	 *
	 * @return
	 *		The number of remaining destination floors.
	 */
	public int getFloorCount()
	{
		return m_sFloors.size();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getMass()
	{
		int totalMass = m_mass;
		for (Drone person : m_lDrones) totalMass += person.getMass();
		return totalMass;
	}

	/***********************************************************************************************
	 * Return the next floor mm ID we want to travel to. Return -1 if we have no more destinations.
	 *
	 * @param bMoveUp
	 *		True if we are moving up.
	 * @return
	 *		The next floor ID we want to travel to.
	 */
	public int getNextFloorID(boolean bMoveUp)
	{
		// Search for the next highest floor to travel to.
		int zPos = m_position.m_z;
		int zFloorDest = -1;
		for (int zFloor : m_sFloors)
		{
			if (zFloor >= zPos)
			{
				if (bMoveUp)
				{
					zFloorDest = zFloor;
				}
				break;
			}
			else
			{
				zFloorDest = zFloor;
			}
		}

		return zFloorDest;
	}

	/***********************************************************************************************
	 * Return the parent Lift.
	 *
	 * @return
	 *	    The parent Lift
	 */
	public Lift getParentLift()
	{
		return m_lift;
	}

	/***********************************************************************************************
	 * Return true if this Door is closed.
	 *
	 * @return
	 *	    True if closed.
	 */
	public boolean isClosed()
	{
		return m_door.isClosed();
	}

	/***********************************************************************************************
	 * Return true if this Door is in the process of closing.
	 *
	 * @return
	 *	    True if this Door is closing.
	 */
	public boolean isClosing()
	{
		return m_door.isClosing();
	}

	/***********************************************************************************************
	 * Return true if this LiftCar has no occupants.
	 *
	 * @return
	 *	    True if this LiftCar has no occupants.
	 */
	public boolean isEmpty()
	{
		return m_lDrones.size() == 0;
	}

	/***********************************************************************************************
	 * Return true if this Door is open.
	 *
	 * @return
	 *	    True if open.
	 */
	public boolean isOpen()
	{
		return m_door.isOpen();
	}

	/***********************************************************************************************
	 * Return true if this Door is open enough to exit or enter.
	 *
	 * @return
	 *	    True if open enough to exit or enter.
	 */
	public boolean isOpenEnough()
	{
		return m_door.isOpenEnough();
	}

	/***********************************************************************************************
	 * Return true if this Door is opening.
	 *
	 * @return
	 *	    True if opening.
	 */
	public boolean isOpening()
	{
		return m_door.isOpening();
	}

	/***********************************************************************************************
	 * Tell this Door to close.
	 */
	public void close(boolean bDelay)
	{
		m_door.close(bDelay);
	}

	/***********************************************************************************************
	 * Tell this Door to open.
	 */
	public void open()
	{
		m_door.open();
	}

	/***********************************************************************************************
	 * If there is room available, return true along with an open location for the person to stand.
	 *
	 * @param person
	 *		The Drone person looking for entry into the LiftCar.
	 * @param position
	 *		The return Position where this Drone potentially fits within the LiftCar.
	 * @return
	 *		True if the person fits within the LiftCar.
	 */
	private boolean isRoom(Drone person, Position position)
	{
		// Return a vacant position within the car.  Default to the center.
		position.m_x = 0;
		position.m_y = 0;
		position.m_z = 0;

		// Don't let anyone enter until those that are trying to exit are able to.
		if (m_qDroneExiting.size() != 0)
		{
			return false;
		}

		// If the car is empty, allow a person with a package of any size
		// to crouch in.
		if (m_lDrones.size() == 0)
		{
			return true;
		}

		Iterator<Drone> itPerson = m_lDrones.iterator();
		int totalY = 0;
		
		while (person != null)
		{
			Size personSize = person.getSize();
			if ((position.m_x += personSize.m_x) > m_sizeInner.m_x)
			{
				position.m_x = personSize.m_x;
				totalY += position.m_y;
				position.m_y = 0;
			}
			if (position.m_y < personSize.m_y)
			{
				position.m_y = personSize.m_y;
				if (position.m_y + totalY > m_sizeInner.m_y)
				{
					m_bIsFull = true;
					return false;
				}
			}
			if (!itPerson.hasNext())
			{
				break;
			}
			person = itPerson.next();
		}
		
		// Reposition to the interior of the car.
		position.m_x -= m_sizeInner.m_x / 2;
		position.m_y -= m_sizeInner.m_y / 2;
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(Drone drone) throws Exception
	{
		Person person = (Person)drone;
		
		Trace.trivial("LiftCar.arrive(): Person (" + person + ") directed towards this.");

		// Don't accept any new people in if the doors are closed.
		Door door;
		if (isClosed() || (door = m_door.getSpouse()) == null)
		{
			return false;
		}

		Location lociNext = person.getNextLocation();

		// If the car is no longer on the same level of the person, redirect the person back
		// towards the parent lift to wait for another car.
		if (person.getPosition().getZ() != m_position.m_z)
		{
			Trace.important(
				"LiftCar.offer(): LiftCar moved off level. Person (" +
				person +
				") directed back to parent lift.");
			person.popDestination();
			person.pushDestination(m_lift);
			return false;
		}

		// If the person is in range of the car door, allow it to be offered into the car.		
		Position posDoor = door.getPosition();

		int nTickDistance = person.getTickDistance(posDoor);
		if (nTickDistance < 3)
		{
			// If there are persons exiting, have this person wait.
			if (m_qDroneExiting.size() > 0)
			{
				Trace.critical("Person offered to car while people are still exiting.");
			}
			person.popDestination();
			if (add(person))
			{
				// Tell the drone it is in the control of the LiftCar.
				person.setPlace(this);

				Trace.critical("LiftCar.offer(): Person (" + person + ") accepted into car.");
				return true;
			}
			Trace.important("LiftCar.offer(): Person arrived at car with no room left.");
			person.pushDestination(m_lift);
		}
		else if ((nTickDistance & 0x1F) == 0 && posDoor.getClass() == Section.class)
		{
			Section sectDoor = (Section)posDoor;
			while (--nTickDistance > 10)
			{
				sectDoor = sectDoor.getNext();
			}
			person.directLocation(sectDoor);

			Trace.important(
				"LiftCar.offer(): Person (" + person +
				") redirected towards this at (" + sectDoor + ").");
		}

		return false;
	}
	
	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Drone poll()
	{
		try
		{
			return m_qDroneExiting.poll();
		}
		catch (EmptyStackException e)
		{
			return null;
		}
	}

	/***********************************************************************************************
	 * Remove the current floor destination from the next floor destination list.
	 */
	public void removeFloorID()
	{
		m_sFloors.remove(m_lift.getBaseLevel() + m_position.m_z);
	}

	/***********************************************************************************************
	 * Set a floor ID we want to travel to.
	 *
	 * @param idFloor
	 *		The id of the floor that we want to direct our car towards.
	 */
	public void setNextFloorID(int idFloor)
	{
		m_sFloors.add(4000 * idFloor);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return new String("LiftCar" + m_id);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("LiftCar.update()");

		super.update(sim);

		// If our door is open enough for people to come or go, check to see if they need to.
		if (m_door.isOpenEnough())
		{
			// Since we're not moving, people might be moving in and out or around.
			int iDrone = 0;
			int nDrones = m_lDrones.size();
			while (iDrone < nDrones)
			{
				Drone person = m_lDrones.get(iDrone);

				person.update(sim);

				// If this person reached its floor, add it to the exit list and remove
				// this floor as a destination.
				int idFloor = m_lift.getBaseLevel() + m_position.m_z;
				if (person.hasDestination(idFloor))
				{
					removeFloorID();
					m_qDroneExiting.add(person);
					m_lDrones.remove(person);
					--nDrones;
					continue;
				}
				++iDrone;
			}
		}
		// In case the door is opening or closing, run its update.
		m_door.update(sim);

		Trace.endOf("LiftCar.update()");
	}
}

/**************************************************************************************************/

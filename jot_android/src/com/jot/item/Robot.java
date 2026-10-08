/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Robot is an implementation of a Drone which is a Movable item that can be directed towards a
 * queue of Place destinations.
 */

package com.jot.item;

import com.jot.rollavator.RollaCar;
import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.*;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

import javax.imageio.ImageIO;

public class Robot extends MovableImpl implements Drone
{
	// A 5x5 circle is used to provide the image of an moving item at a grid location.
	private static byte[]	s_bmpItem =
	{
		0x42, 0x4d, (byte)0x86, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x36, 0x00, 0x00, 0x00, 0x28, 0x00,
		0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x01, 0x00, 0x18, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x50, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x00, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
		(byte)0xFF, (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
		(byte)0xFF, (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
		(byte)0xFF, (byte)0xFF, 0x00, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x00,
	};
	private static Image s_image;
	private static int s_nRobots = 0;
	
	public CollisionGrid m_collisionGrid;
	private Location m_locNext;
	private Place m_place;		// Currently owned by this place.
	private Stack<Place> m_destinations;
	private int m_idRobot;
	private int m_idFloorGroup;

	/***********************************************************************************************
	 * Robot constructor based on its mass, size and position.
	 *
	 * @param mass
	 *		The kg mass of this item.
	 * @param size
	 *	    The vector size of this item.
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 */
	public Robot(int mass, Size size, Position position) throws Exception
	{
		super(mass, size, position);
		
		m_locNext = new Location(getLocation());
		
		m_destinations = new Stack<Place>();
		if (s_image == null)
		{
			try
			{
				InputStream inputStream = new ByteArrayInputStream(s_bmpItem);
				s_image = ImageIO.read(inputStream);
			}
			catch (Exception e)
			{}
		}

		m_idRobot = ++s_nRobots;

		m_idFloorGroup = 0;
	}

	/***********************************************************************************************
	 * Set a new location that we want to move this Robot towards.
	 *
	 * @param locFinal
	 *		The location we want to move towards.
	 */
	public void directLocation(Location locFinal) throws Exception
	{
		directLocation(1000, locFinal);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getFloorGroupId()
	{
		return m_idFloorGroup;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Image getImage()
	{
		return s_image;
	}

	/***********************************************************************************************
	 * If available, return a reference to the location we plan on moving to in this iteration if it
	 * available to us.
	 *
	 * @return
	 *		The location we plan on moving to in this iteration if it available to us, else null.
	 */
	public Location getNextLocation()
	{
		return m_locNext;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Place getPlace()
	{
		return m_place;
	}

	/***********************************************************************************************
	 * Return true if this robot's destination is on the same z-axis level as provided.
	 *
	 * @param level
	 *		The z-axis location to compare against.
	 * @return
	 *		true if this robot's destination is on the same z-axis level as provided.
	 */
	public boolean hasDestination(int level)
	{
		Place destination = peekDestination();
		if (destination != null)
		{
			Location lociDestination = destination.getLocation(); 
			if (lociDestination != null)
			{
				return ((int)lociDestination.m_z == level);
			}
		}
		return false;
	}

	/***********************************************************************************************
	 * If available, return a reference to the active destination.
	 *
	 * @return
	 *		The active destination or null if not available.
	 */
	public Place peekDestination()
	{
		try
		{
			return m_destinations.peek();
		}
		catch (EmptyStackException e)
		{
			return null;
		}
	}

	/***********************************************************************************************
	 * If available, pop off and return a reference to the active destination.  If there is a
	 * next destination, make this the new active direction.
	 *
	 * @return
	 *		The active destination or null if not available.
	 */
	public Place popDestination() throws Exception
	{
		Place activeDestination;
		try
		{
			activeDestination = m_destinations.pop();
		}
		catch (EmptyStackException e)
		{
			return null;
		}

		Place nextDestination = peekDestination();
		if (nextDestination != null)
		{
			directLocation(nextDestination.getLocation());
		}
			
		return activeDestination;
	}

	/***********************************************************************************************
	 * Push a new destination to the stack.  Make this new destination active.
	 *
	 * @param destination
	 *		The next place in the queue that we want to move towards.
	 */
	public void pushDestination(Place destination) throws Exception
	{
		if (destination != null)
		{
			m_destinations.push(destination);
			directLocation(destination.getLocation());
		}
	}

	/***********************************************************************************************
	 * Remove this robot from the collision grid.
	 */
	public void removeFromGrid()
	{
		if (m_collisionGrid != null)
		{
			Location location = getLocation();
			m_collisionGrid.removeItem(location.m_x, location.m_y);
			m_collisionGrid = null;
		}
	}

	/***********************************************************************************************
	 * Assign a collision avoidance grid system for this Robot to traverse with other Robots.
	 *
	 * @param collisionGrid
	 *		The floor grid that controls the movement of robots so as not to have them collide.
	 */
	public void setCollisionGrid(CollisionGrid collisionGrid)
	{
		m_collisionGrid = collisionGrid;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setFloorGroupId(int idFloorGroup)
	{
		m_idFloorGroup = idFloorGroup;
	}

	/***********************************************************************************************
	 * Set the location we would like to move this Robot to, if is offered as available.
	 *
	 * @param location
	 *		A location to move towards.
	 */
	public void setNextLocation(Location location)
	{
		m_locNext = location;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setPlace(Place place)
	{
		m_place = place;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return "Robot #" + m_idRobot;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("Robot.update()");

		Place currentDestination = peekDestination();
		if (m_velocity == null || currentDestination == null)
		{
			Trace.trivial("Robot.update(): no velocity or destination.");
			super.update(sim);
		}
		else
		{
			// For each time cycle, put the items along the track one position.
			int nCycles = sim.getUpdateCyclesCount();
			do
			{
				Location location = getLocation();
				int x = location.m_x;
				int y = location.m_y;
				int dx = m_velocity.m_x;
				int dy = m_velocity.m_y;
			
				m_locNext.m_x = x + dx;
				m_locNext.m_y = y + dy;
		
				Trace.trivial("Robot.update(): " + this + ": attempt to move by (" + dx + ", " + dy + ").");

				// If we're headed towards a RollaCar, make sure it is still directed towards
				// our floor group.
				if (currentDestination.getClass() == RollaCar.class)
				{
					RollaCar car = (RollaCar)currentDestination;
					if (m_idFloorGroup != car.getFloorGroupId())
					{
						popDestination();
						currentDestination = car.getParentLift();
						pushDestination(currentDestination);
					}
				}

				// If the person reached the its destination and is now in its care, remove this person from
				// the previously controlled collision avoidance grid.
				if (currentDestination.offer(this))
				{
					Trace.important("Robot.update(): " + this + ": rearched destination: " + currentDestination + ".");
				}
				else
				{
					// Try to move to a new location.
					if (m_collisionGrid != null && !m_collisionGrid.moveItem(this, m_locNext.m_x, m_locNext.m_y))
					{
						Trace.trivial("Robot.update(): " + this + ": changing direction.");

						// We were redirected.  Update our new direction.
						//directLocation(currentDestination.getLocation());
					}
				}
			} while (--nCycles > 0);
		}

		Trace.endOf("Robot.update()");
	}
}

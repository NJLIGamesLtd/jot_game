/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Person is a directed item or Robot that can carry baggage.
 */

package com.jot.item;

import com.jot.random.Random;
import com.jot.simulation.Cycles;
import com.jot.vector.*;

public class Person extends Robot
{
	private static final Size s_sizeSafe = new Size(2000, 2000, 2000);  // Different per person?
	private static final Size s_size = new Size(700, 400, 1800); // Make random.
	private static final int s_mass = 70; // Make random.
	private static int s_nPersons = 0;

	private Movable m_baggage;
	private Size m_sizeTotal;
	private final int m_velWalk;
	private int m_idPerson;

	/***********************************************************************************************
	 * Person constructor based on its position and baggage.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param baggage
	 *	    Movable baggage that might be carried by the Person.
	 */
	public Person(Position position, Movable baggage) throws Exception
	{
		super(s_mass, s_size, position);
		m_baggage = baggage;
		
		switch (Random.GetInt(10))
		{
			case 0: m_velWalk = 500;		break;
			case 1: m_velWalk = 750;		break;
			case 2: m_velWalk = 1000;		break;
			case 3: m_velWalk = 1000;		break;
			case 4: m_velWalk = 1500;		break;
			case 5: m_velWalk = 1500;		break;
			case 6: m_velWalk = 1500;		break;
			case 7: m_velWalk = 2000;		break;
			case 8: m_velWalk = 2000;		break;
			default:
			case 9: m_velWalk = 2250;		break;
		}

		m_idPerson = ++s_nPersons;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getMass()
	{
		if (m_baggage != null)
		{
			return m_mass + m_baggage.getMass();
		}
		return m_mass;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Size getSize()
	{
		if (m_baggage != null)
		{
			if (m_sizeTotal == null)
			{
				Size baggageSize = m_baggage.getSize();
				m_sizeTotal = new Size(m_size.m_x, m_size.m_y, m_size.m_z);
				m_sizeTotal.m_x += baggageSize.m_x;
				if (baggageSize.m_y > m_sizeTotal.m_y) m_sizeTotal.m_y = baggageSize.m_y;
				if (baggageSize.m_z > m_sizeTotal.m_z) m_sizeTotal.m_z = baggageSize.m_z;
			}
			return m_sizeTotal;
		}
		return m_size;
	}

	/***********************************************************************************************
	 * Return the vector size around this person that might be considered his safe zone.
	 *
	 * @return
	 *		The vector size of this item's safe zone.
	 */
	public Size getSizeSafe()
	{
		return s_sizeSafe;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void addLocation(final Location delta)
	{
		super.addLocation(delta);
		if (m_baggage != null)
		{
			m_baggage.addLocation(delta);
		}
	}

	/***********************************************************************************************
	 * Return the number of ticks it would take for this drone to traverse the given destination.
	 *
	 * @param destination
	 *		The location we want to measure our tick distance from.
	 * @return
	 *		The number of ticks needed to traverse to the destination.
	 */
	public int getTickDistance(Location destination)
	{
		Location location = getLocation();
		int x = location.m_x - destination.m_x;
		int y = location.m_y - destination.m_y;
		int distance = (int)Math.sqrt((double)x * x + y * y);
		int nTime = distance / m_velWalk;
		int nTicks = nTime * Cycles.GetCyclesPerSecond();
		return nTicks;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void directLocation(Location locFinal) throws Exception
	{
		directLocation(m_velWalk, locFinal);
		if (m_baggage != null)
		{
			m_baggage.directLocation(m_velWalk, locFinal);
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void directOrientation(Orientation orientFinal)
	{
		super.directOrientation(orientFinal);
		if (m_baggage != null)
		{
			m_baggage.directOrientation(orientFinal);
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setOrientation(final Orientation orientation)
	{
		super.setOrientation(orientation);
		if (m_baggage != null)
		{
			m_baggage.setOrientation(orientation);
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return "Person #" + m_idPerson;
	}
}

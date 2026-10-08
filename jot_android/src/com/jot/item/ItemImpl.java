/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * An ItemImpl is a default implementation of an Item.
 */

package com.jot.item;

import com.jot.vector.*;
import java.awt.*;

public class ItemImpl implements Item
{
	private static int s_idItemNew = 0;

	protected int m_idItem;
	protected int m_mass;
	protected int m_radius;
	protected Size m_size;
	protected Position m_position;

	/***********************************************************************************************
	 * Item constructor based on its mass, size and position.
	 *
	 * @param mass
	 *		The kg mass of this item.
	 * @param size
	 *	    The vector size of this item.
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 */
	public ItemImpl(int mass, Size size, Position position) throws Exception
	{
		m_idItem = ++s_idItemNew;

		m_mass = mass;
		if ((m_size = size) == null)
		{
			m_size = new Size(1000, 1000, 1000);
		}
		m_radius = (int)(Math.sqrt
			(((m_size.m_x / 2) * (m_size.m_x / 2)) + ((m_size.m_y / 2) * (m_size.m_y / 2))) / 2);

		if ((m_position = position) == null)
		{
			m_position = new Position();
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getHeading()
	{
		return m_position.getHeading();
	}
	
	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Image getImage()
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Location getLocation()
	{
		return m_position;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getMass()
	{
		return m_mass;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Orientation getOrientation()
	{
		return m_position.getOrientation();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getPitch()
	{
		return m_position.getPitch();
	}
	
	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Position getPosition()
	{
		return m_position;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getRadius()
	{
		return m_radius;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getRoll()
	{
		return m_position.getRoll();
	}
	
	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Size getSize()
	{
		return m_size;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setLocation(int x, int y)
	{
		m_position.set(x, y);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setLocation(int x, int y, int z)
	{
		m_position.set(x, y, z);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setLocation(final Location location)
	{
		m_position.set(location);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setMass(int mass)
	{
		m_mass = mass;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setOrientation(final Orientation orientation)
	{
		m_position.setOrientation(orientation);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setPosition(Position position)
	{
		m_position = position;
	}
}

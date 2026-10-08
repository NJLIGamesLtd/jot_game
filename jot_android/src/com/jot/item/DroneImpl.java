/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Drone is a directed Item. This can be by any means of transport.
 */

package com.jot.item;

import com.jot.vector.*;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

import javax.imageio.ImageIO;

public class DroneImpl extends PlaceImpl implements Drone
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
	private static int s_nDrones = 0;
	
	protected int m_idDrone;
	private Place m_place;		// Currently owned by this place.
	private Stack<Place> m_destinations;	// Places yet to go.

	/***********************************************************************************************
	 * Drone constructor based on its mass, size and position.
	 *
	 * @param mass
	 *		The kg mass of this item.
	 * @param size
	 *	    The vector size of this item.
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 */
	public DroneImpl(int mass, Size size, Position position) throws Exception
	{
		super(mass, size, position);

		// Create a stack of destinations.
		m_destinations = new Stack<Place>();

		// Create an image of this Drone.
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

		// Record an ID for our Drone
		m_idDrone = ++s_nDrones;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Place getPlace()
	{
		return m_place;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Image getImage()
	{
		return s_image;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getFloorGroupId()
	{
		return 0;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
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
	 * {@inheritDoc}
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
	 * {@inheritDoc}
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

		return activeDestination;
	}
	
	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void pushDestination(Place destination) throws Exception
	{
		if (destination != null)
		{
			m_destinations.push(destination);
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setFloorGroupId(int idFloorGroup)
	{
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
		return "Drone #" + m_idDrone;
	}
}

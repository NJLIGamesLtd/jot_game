/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Block item is a place holder on the CollisionGrid to block any other items from going there.
 */

package com.jot.item;

import com.jot.vector.*;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class Block implements Item
{
	private static final double COS_45 = Math.cos(Math.PI * 45 / 180);
	
	// This simple filled in 5x5 square is used to provide the image of a wall or other item that blocks a grid location.
	private static final byte[]	bmpBlock =
	{
			0x42, 0x4d, (byte)0x86, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x36, 0x00, 0x00, 0x00, 0x28, 0x00,
			0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x01, 0x00, 0x18, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x50, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
	};
	private static Image s_image;

	/***********************************************************************************************
	 * Default constructor.
	 */
	public Block()
	{
		if (s_image == null)
		{
			try
			{
				InputStream inputStream = new ByteArrayInputStream(bmpBlock);
				s_image = ImageIO.read(inputStream);
			}
			catch (Exception e)
			{}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getHeading()
	{
		return 0;
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
	public Location getLocation()
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getMass()
	{
		return 0;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Orientation getOrientation()
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getPitch()
	{
		return 0;
	}
	
	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Position getPosition()
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getRadius()
	{
		return (int)(COS_45 * CollisionGrid.GRID_WIDTH);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getRoll()
	{
		return 0;
	}
	
	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Size getSize()
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setLocation(int x, int y)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setLocation(int x, int y, int z)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setLocation(final Location location)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setMass(int mass)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setOrientation(final Orientation orientation)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setPosition(Position position)
	{
	}
}

/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * CollisionGrid provides a grid of squares in 2D that can be occupied only by a single item.
 * These items can either be stationary blocks, such as a wall, or a directed item, trying to
 * find it's way through a crowd.
 */

package com.jot.item;

import com.jot.random.Random;
import com.jot.trace.Trace;
import com.jot.vector.*;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class CollisionGrid implements ImageObserver
{
	// A GridItem can be used a as a block for directed items to walk around.
	public interface GridItem
	{
	}
	
	// A GridCircle is a GridItem that is shaped as a 2D circle.
	public static class GridCircle implements GridItem
	{
		protected final int m_x;
		protected final int m_y;
		protected final int m_radius;
		
		public GridCircle(final int x, final int y, final int radius)
		{
			m_x = x;
			m_y = y;
			m_radius = radius;
		}
	}
	
	// A GridRectangle is a GridItem that can be placed in any 2D orientation.
	public static class GridRectangle implements GridItem
	{
		protected final int m_x1;
		protected final int m_y1;
		protected final int m_x2;
		protected final int m_y2;
		protected final int m_width;
		
		public GridRectangle(final int x1, final int y1, final int x2, final int y2, final int width)
		{
			m_x1 = x1;
			m_y1 = y1;
			m_x2 = x2;
			m_y2 = y2;
			m_width = width;
		}
	}
	
	// This defines how big each grid location is.  It is assumed that directed items can squeeze through
	// a blockage of this size.
	public static final int GRID_WIDTH = 512;
	public static final int GRID_WIDTH_SHIFT = 9;
	
	// This provides alternate courses around a blocked direction.  Directions defined by getCourse() as:
	//  3 6 0
	//  5 + 2
	//  4 7 1
	private static final int s_gridCourses[][][][] =
	{
		{	// 0: Tried to go { 1, 1 }
			{ { 1,  0 }, {  0, 1 } },
			{ { 2,  0 }, {  0, 2 } },
			{ { 2,  1 }, {  1, 2 } },
			{ { 1, -1 }, { -1, 1 } },
		},
		{	// 1: Tried to go { 1, -1 }
			{ { 0, -1 }, {  1, 0 } },
			{ { 0, -2 }, {  2, 0 } },
			{ { 1, -2 }, {  2, -1 } },
			{ { 1,  1 }, { -1, -1 } },
		},
		{	// 2: Tried to go { 1, 0 }
			{ { 1, -1 }, { 1, 1 } },
			{ { 1, -2 }, { 1, 2 } },
			{ { 2, -1 }, { 2, 1 } },
			{ { 0, -1 }, { 0, 1 } },
		},
		{	// 3: Tried to go { -1, 1 }
			{ {  0, 1 }, { -1,  0 } },
			{ {  0, 2 }, { -2,  0 } },
			{ { -1, 2 }, { -2,  1 } },
			{ {  1, 1 }, { -1, -1 } },
		},
		{	// 4: Tried to go { -1, -1 }
			{ { -1,  0 }, {  0, -1 } },
			{ { -2,  0 }, {  0, -2 } },
			{ { -2, -1 }, { -1, -2 } },
			{ { -1,  1 }, {  1, -1 } },
		},
		{	// 5: Tried to go { -1, 0 }
			{ { -1, 1 }, { -1, -1 } },
			{ { -1, 2 }, { -1, -2 } },
			{ { -2, 1 }, { -2, -1 } },
			{ {  0, 1 }, {  0, -1 } },
		},
		{	// 6: Tried to go { 0, 1 }
			{ { 1, 1 }, { -1, 1 } },
			{ { 2, 1 }, { -2, 2 } },
			{ { 1, 2 }, { -1, 2 } },
			{ { 1, 0 }, { -1, 0 } },
		},
		{	// 7: Tried to go { 0, -1 }
			{ { -1, -1 }, { 1, -1 } },
			{ { -2, -1 }, { 2, -1 } },
			{ { -1, -2 }, { 1, -2 } },
			{ { -1, 0 }, { 1, 0 } },
		},
	};

	// A 5x5 circle is used to provide the image of an moving item at a grid location.
	private static byte[]	s_bmpEmpty =
	{
			0x42, 0x4d, (byte)0x86, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x36, 0x00, 0x00, 0x00, 0x28, 0x00,
			0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x01, 0x00, 0x18, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x50, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
			(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
			(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
			(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
			(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
			(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x00,
	};
	private static Image s_imgEmpty;
	
	private BufferedImage m_image;
	private boolean m_bUpdateImage;
	private Graphics2D m_graphImage;
	private Item[][] m_aaCollisionGrid;
	private Item[][] m_aaCollisionGridDrawn;
	private Item m_itemBlock;
	private int m_originX;
	private int m_originY;

	/***********************************************************************************************
	 * CollisionGrid constructor based on the width and length of the traversed region.
	 *
	 * @param width
	 *		Width in meters of a the traversed region.
	 * @param length
	 *		Length in meters of a the traversed region.
	 */
	public CollisionGrid(int width, int length) throws Exception
	{
		Trace.startOf("CollisionGrid()");

		int x = (width + GRID_WIDTH - 1) >> GRID_WIDTH_SHIFT;
		int y = (length + GRID_WIDTH - 1) >> GRID_WIDTH_SHIFT;
		
		if (x <= 1 || y <= 1)
		{
			throw new Exception("CollisionGrid() requires positive width and length.");
		}
		m_aaCollisionGrid = new Item[y][x];
		m_aaCollisionGridDrawn = new Item[y][x];
		m_originX = (x * GRID_WIDTH) >> 1;
		m_originY = (y * GRID_WIDTH) >> 1;
		
		// Fill the drawn collision grid with an unknown blocker item, to tell us it needs to be updated.
		m_itemBlock = new Block();
		for (int ix = 0; ix < x; ++ix)
		{
			for (int iy = 0; iy < y; ++iy)
			{
				m_aaCollisionGridDrawn[iy][ix] = m_itemBlock;
			}
		}

		// Create the final blocker item that will we use to build walls and such on our building floor.
		m_itemBlock = new Block();
		
		// Create a 2D image of our floor that we can update as the people move around.
		m_image = new BufferedImage(5 * x, 5 * y, BufferedImage.TYPE_3BYTE_BGR);
		m_graphImage = m_image.createGraphics();
		
		// If needed,  create an image to represent an empty or unoccupied grid location.
		if (s_imgEmpty == null)
		{
			try
			{
				InputStream inputStream = new ByteArrayInputStream(s_bmpEmpty);
				s_imgEmpty = ImageIO.read(inputStream);
			}
			catch (Exception e)
			{}
		}
		m_bUpdateImage = true;

		Trace.endOf("CollisionGrid()");
	}

	/***********************************************************************************************
	 * Add this stationary item as a block on the grid.
	 *
	 * @param item
	 *		An item to add to the traversed region.
	 */
	public void addBlock(final GridItem item)
	{
		if (item.getClass() == GridCircle.class)
		{
			addBlock((GridCircle)item);
		}
		else if (item.getClass() == GridRectangle.class)
		{
			addBlock((GridRectangle)item);
		}
	}
	
	/***********************************************************************************************
	 * Add a collection of stationary item blocks to the grid.
	 *
	 * @param aGridItems
	 *		An array of items to add to the traversed region.
	 */
	public void addBlock(final GridItem[] aGridItems)
	{
		for (int i = 0; i < aGridItems.length; ++i)
		{
			addBlock(aGridItems[i]);
		}
	}
	
	/***********************************************************************************************
	 * Add a stationary circular item as a block on the grid.
	 *
	 * @param circle
	 *		A stationary circular item as a block on the grid.
	 */
	public void addBlock(final GridCircle circle)
	{
		int radiusSquared = circle.m_radius * circle.m_radius;
		int startX = getGridIndexX(circle.m_x - circle.m_radius);
		int startY = getGridIndexY(circle.m_y - circle.m_radius);
		int endX = getGridIndexX(circle.m_x + circle.m_radius);
		int endY = getGridIndexY(circle.m_y + circle.m_radius);
		int dX = (GRID_WIDTH / 2) + (GRID_WIDTH * startX) - (m_originX + circle.m_x);
		for (int iX = startX; iX <= endX; ++iX, dX += GRID_WIDTH)
		{
			int dXsquared = dX * dX;
			int dY = (GRID_WIDTH / 2) + (GRID_WIDTH * startY) - (m_originY + circle.m_y);
			for (int iY = startY; iY <= endY; ++iY, dY += GRID_WIDTH)
			{
				if (radiusSquared >= dXsquared + (dY * dY))
				{
					m_aaCollisionGrid[iY][iX] = m_itemBlock;
					m_bUpdateImage = true;
				}
			}
		}
	}
	
	/***********************************************************************************************
	 * Add a stationary rectangular item as a block on the grid.
	 *
	 * @param circle
	 *		A stationary rectangular item as a block on the grid.
	 */
	public void addBlock(final GridRectangle rectangle)
	{
		int x1 = rectangle.m_x1;
		int y1 = rectangle.m_y1;
		int x2 = rectangle.m_x2;
		int y2 = rectangle.m_y2;
		
		// Turn the widthHalf into a distance from the line.
		int widthHalf = rectangle.m_width >> 1;
		
		// Make sure x1 comes before x2.
		if (x1 > x2)
		{
			int temp = x1;
			x1 = x2;
			x2 = temp;
			temp = y1;
			y1 = y2;
			y2 = temp;
		}
		
		// Determine the center point of the rectangle, and half it's length.
		int centerX = (x1 + x2) >> 1;
		int centerY = (y1 + y2) >> 1;
		int length = y1 - centerY;

		// Iterate through all of the grid squares that might fall within our
		// rectangle.  Block each that do.
		int dX = x1 - widthHalf;
		int startX = getGridIndexX(dX);
		int startY = getGridIndexY((y1 < y2 ? y1 : y2) - widthHalf);
		int endX = getGridIndexX(x2 + widthHalf);
		int endY = getGridIndexY((y1 > y2 ? y1 : y2) + widthHalf);

		// If we have a vertical rectangle, no need for angles.
		if (x1 == x2)
		{
			length = Math.abs(length);
			dX = (GRID_WIDTH / 2) + (GRID_WIDTH * startX) - (m_originX + centerX);
			for (int iX = startX; iX <= endX; ++iX, dX += GRID_WIDTH)
			{
				int dY = (GRID_WIDTH / 2) + (GRID_WIDTH * startY) - (m_originY + centerY);
				for (int iY = startY; iY <= endY; ++iY, dY += GRID_WIDTH)
				{
					if (dX < widthHalf && dX >= -widthHalf && dY < length && dY >= -length)
					{
						m_aaCollisionGrid[iY][iX] = m_itemBlock;
						m_bUpdateImage = true;
					}
				}
			}
		}
		// Else, if we have a vertical rectangle, no need for angles.
		else if (y1 == y2)
		{
			length = Math.abs(x1 - centerX);
			int dY = (GRID_WIDTH / 2) + (GRID_WIDTH * startY) - (m_originY + centerY);
			for (int iY = startY; iY <= endY; ++iY, dY += GRID_WIDTH)
			{
				dX = (GRID_WIDTH / 2) + (GRID_WIDTH * startX) - (m_originX + centerX);
				for (int iX = startX; iX <= endX; ++iX, dX += GRID_WIDTH)
				{
					if (dY < widthHalf && dY >= -widthHalf && dX < length && dX >= -length)
					{
						m_aaCollisionGrid[iY][iX] = m_itemBlock;
						m_bUpdateImage = true;
					}
				}
			}
		}
		else
		{
			// Calculate the tilt angle of the rectangle.
			double rectAngle = Math.atan((double)(y1 - y2) / (x1 - x2));
			length = (int)Math.sqrt(((centerX - x1) * (centerX - x1)) + (length * length));
		
			dX = (GRID_WIDTH / 2) + (GRID_WIDTH * startX) - (m_originX + centerX);
			for (int iX = startX; iX <= endX; ++iX, dX += GRID_WIDTH)
			{
				int dXsquared = dX * dX;
				int dY = (GRID_WIDTH / 2) + (GRID_WIDTH * startY) - (m_originY + centerY);
				for (int iY = startY; iY <= endY; ++iY, dY += GRID_WIDTH)
				{
					double gridAngle = dX == 0 ? (Math.PI / 2) : Math.atan((double)dY / dX);
					double angle = rectAngle - gridAngle;
					int r = (int)Math.sqrt(dXsquared + (dY * dY));
					if (Math.abs(Math.sin(angle)) * r <= widthHalf && Math.abs(Math.cos(angle)) * r <= length)
					{
						m_aaCollisionGrid[iY][iX] = m_itemBlock;
						m_bUpdateImage = true;
					}
				}
			}
		}
	}

	/***********************************************************************************************/
	// Return the course direction from location 1 to location 2.  This is defined as:
	//  3 6 0
	//  5 + 2
	//  4 7 1
	private int getCourse(int x1, int y1, int x2, int y2)
	{
		int course;
		switch ((x2 - x1) >> GRID_WIDTH_SHIFT)
		{
			case  1:	course = 2; 	break;
			case -1:	course = 5; 	break;
			default:	course = 8; 	break;
		}
		switch ((y2 - y1) >> GRID_WIDTH_SHIFT)
		{
			case  1:	course -= 2;	break;
			case -1:	course -= 1;	break;
			default:
		}
		if (course == 8)
		{
			course = 0;
		}
		return course;
	}

	/***********************************************************************************************/
	// Return the X grid index based on its vector X position.
	private int getGridIndexX(int x)
	{
		if ((x += m_originX) < 0)
		{
			return -1;
		}
		return (x >> GRID_WIDTH_SHIFT);
	}

	/***********************************************************************************************/
	// Return the Y grid index based on its vector Y position.
	private int getGridIndexY(int y)
	{
		if ((y += m_originY) < 0)
		{
			return -1;
		}
		return (y >> GRID_WIDTH_SHIFT);
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
		if (m_bUpdateImage)
		{
			m_bUpdateImage = false;
			for (int y = m_aaCollisionGrid.length - 1; y >= 0; --y)
			{
				for (int x = 0; x < m_aaCollisionGrid[y].length; ++x)
				{
					Item item = m_aaCollisionGrid[y][x];
					if (true || item != m_aaCollisionGridDrawn[y][x])
					{
						m_aaCollisionGridDrawn[y][x] = item;
						Image imgGrid = null;
						if (item != null)
						{
							imgGrid = item.getImage();
						}
						if (imgGrid == null)
						{
							imgGrid = s_imgEmpty;
						}
						m_graphImage.drawImage(imgGrid, 5 * x, 5 * y, this);
					}
				}
			}
			Trace.trivial("CollisionGrid.getImage(): updated.");
		}
		return m_image;
	}

	/***********************************************************************************************
	 * Return an item, if any, located on the grid at this location.
	 *
	 * @param x
	 *		x location vector of the collision item.
	 * @param y
	 *	    x location vector of the collision item.
	 * @return
	 *		An item, if any, located on the grid at this location.
	 */
	public Item getItem(int x, int y)
	{
		if ((x = getGridIndexX(x)) < 0 ||
			(y = getGridIndexY(y)) < 0)
		{
			return null;
		}
		if (x >= m_aaCollisionGrid[0].length ||
			y >= m_aaCollisionGrid.length)
		{
			return null;
		}
		
		return m_aaCollisionGrid[y][x];
	}

	/***********************************************************************************************
	 * Update our image based on the provided image as defined by its position and size.
	 * Return true for success.
	 *
	 * @param img
	 *		The image to superimpose over the object image.
	 * @param infoflags
	 *		Flags to determine overlay options.
	 * @param x
	 *		X location vector of the collision item.
	 * @param y
	 *		Y location vector of the collision item.
	 * @param width
	 *		Width of the overlay image.
	 * @param height
	 *		Height of the overlay image.
	 * @return
	 *		true for success.
	 */
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
	{
		return true;
	}
	
	/***********************************************************************************************/
	// Move the item at location 1 to location 2.
	private boolean moveItem(int x1, int y1, int x2, int y2)
	{
		if ((x1 = getGridIndexX(x1)) < 0 ||
			(y1 = getGridIndexY(y1)) < 0)
		{
			return false;
		}

		if (x1 >= m_aaCollisionGrid[0].length ||
			y1 >= m_aaCollisionGrid.length)
		{
			return false;
		}

		if (setItem(x2, y2, m_aaCollisionGrid[y1][x1]))
		{
			m_aaCollisionGrid[y1][x1] = null;
			m_bUpdateImage = true;
			return true;
		}
		return false;
	}

	/***********************************************************************************************
	 * Attempt to move an item from its current location to the new location.
	 * Return true if the move occurred without having to alter the course.
	 *
	 * @param item
	 *		Item to move on the collection grid.
	 * @param newx
	 *		X location vector of the collision item.
	 * @param newy
	 *		Y location vector of the collision item.
	 * @return
	 *		true if the move occurred without having to alter the course.
	 */
	public boolean moveItem(Item item, int newx, int newy)
	{
		Location location = item.getLocation();
		Item it = getItem(newx, newy);
		
		if (it == item)
		{
			Trace.trivial("CollisionGrid.moveItem(): " + item + "(" + newx + ", " + newy + ") " + " stayed on same grid.");
		}

		// If the new square is vacant, move our object there.
		if (it == null)
		{
			if (moveItem(location.m_x, location.m_y, newx, newy))
			{
				Trace.trivial(
					"CollisionGrid.moveItem(): " + item + "(" + newx + ", " + newy + ") "
					+ " moved to grid (" + getGridIndexX(newx) + ", " + getGridIndexX(newy) + ").");
				it = item;
			}
		}

		// If we're still in the same square, just update our location
		// within the square.
		if (it == item)
		{
			location.m_x = newx;
			location.m_y = newy;
			return true;
		}

		int course = getCourse(location.m_x, location.m_y, newx, newy);
		int iX = getGridIndexX(newx);
		int iY = getGridIndexY(newy);

		// Loop around the blockage to see if we can find a passage.
		for (int alter = 0; alter < 4; ++alter)
		{
			int i = Random.GetInt(2);
			for (int attempt = 0; attempt < 2; ++attempt, i ^= 1)
			{
				int attemptX = iX + s_gridCourses[course][alter][i][0];
				int attemptY = iY + s_gridCourses[course][alter][i][1];
				if (attemptY >= 0 && attemptY < m_aaCollisionGrid.length &&
					attemptX >= 0 && attemptX < m_aaCollisionGrid[0].length &&
					m_aaCollisionGrid[attemptY][attemptX] == null)
				{
					Trace.important(
						"CollisionGrid.moveItem(): " + item + "(" + newx + ", " + newy + ") "
						+ " diverted to grid (" + attemptX + ", " + attemptY + ") from grid ("
						+ iX + ", " + iY + ").");
					newx = (attemptX * GRID_WIDTH) - m_originX;
					newy = (attemptY * GRID_WIDTH) - m_originY;
					moveItem(location.m_x, location.m_y, newx, newy);
					location.m_x = newx;
					location.m_y = newy;
					return false;
				}
			}
		}
		Trace.important("CollisionGrid.moveItem(): " + item + "(" + newx + ", " + newy + ") " + " blocked from moving.");
		
		return false;
	}

	/***********************************************************************************************
	 * Check to see if the item can be moved to the new location.
	 *
	 * @param item
	 *		An item to be moved onto the collision grid.
	 * @return
	 *		true if the item can be moved to the new location.
	 */
	public boolean offer(Item item)
	{
		Location location = item.getLocation();
		int x, y;

		if ((x = getGridIndexX(location.m_x)) < 0 ||
			(y = getGridIndexY(location.m_y)) < 0)
		{
			return false;
		}

		if (x >= m_aaCollisionGrid[0].length ||
			y >= m_aaCollisionGrid.length ||
			m_aaCollisionGrid[y][x] != null)
		{
			return false;
		}
		
		m_aaCollisionGrid[y][x] = item;
		m_bUpdateImage = true;
		return true;
	}

	/***********************************************************************************************
	 * Paint the latest image of our collision grid on the provided graphics object.
	 *
	 * @param g
	 *		The graphics object for displaying the simulation.
	 */
	public void paint(Graphics g)
	{
		Image image = getImage();
		g.drawImage(image, 0, 0,  m_aaCollisionGrid.length * 5,  m_aaCollisionGrid.length * 5, this);
	}

	/***********************************************************************************************
	 * Remove the item from the grid at the location provided;
	 *
	 * @param x
	 *		X location vector of the collision item.
	 * @param y
	 *		Y location vector of the collision item.
	 * @return
	 *		true for success.
	 */
	public boolean removeItem(int x, int y)
	{
		if ((x = getGridIndexX(x)) < 0 ||
			(y = getGridIndexY(y)) < 0)
		{
			return false;
		}

		if (x >= m_aaCollisionGrid[0].length ||
			y >= m_aaCollisionGrid.length)
		{
			return false;
		}
		
		m_aaCollisionGrid[y][x] = null;
		m_bUpdateImage = true;
		return true;
	}

	/***********************************************************************************************
	 * Set the given item to the location provided on the collision grid.
	 *
	 * @param x
	 *		X location vector of the collision item.
	 * @param y
	 *		Y location vector of the collision item.
	 * @param item
	 *		An item to be moved onto the collision grid.
	 * @return
	 *		true for success.
	 */
	public boolean setItem(int x, int y, Item item)
	{
		if ((x = getGridIndexX(x)) < 0 ||
			(y = getGridIndexY(y)) < 0)
		{
			return false;
		}

		if (x >= m_aaCollisionGrid[0].length ||
			y >= m_aaCollisionGrid.length ||
			m_aaCollisionGrid[y][x] != null)
		{
			return false;
		}
		
		m_aaCollisionGrid[y][x] = item;
		m_bUpdateImage = true;
		return true;
	}

	/***********************************************************************************************
	 * Return the String version of the collision grid.
	 *
	 * @return
	 *		The String version of the collision grid.
	 */
	public String toString()
	{
		String str = new String();

		str += "  ";
		for (int x = 0; x < m_aaCollisionGrid[0].length; ++x)
		{
			char ch = (char)('0' + (x % 10));
			str += ch;
		}
		str += '\n';
		
		for (int y = m_aaCollisionGrid.length - 1; y >= 0; --y)
		{
			char ch = (char)('0' + (y % 10));
			str += ch;
			str += ' ';

			for (int x = 0; x < m_aaCollisionGrid[y].length; ++x)
			{
				str += m_aaCollisionGrid[y][x] == null ? 'O' : '#';
			}
			str += '\n';
		}
		return str;
	}
}

/***********************************************************************************************/
// Author: Jot Russell (copyright 2014, all rights reserved)

/***********************************************************************************************/

package com.jot.item.test;

/***********************************************************************************************/

import org.junit.Test;

import com.jot.item.CollisionGrid;
import com.jot.item.CollisionGrid.GridRectangle;
import com.jot.item.CollisionGrid.GridCircle;

import static org.junit.Assert.*;

/***********************************************************************************************/

public class CollisionGridTest
{
	static final int GRID_HALF = CollisionGrid.GRID_WIDTH / 2;

	/***********************************************************************************************/
	// Use a small square as a blocker.
	@Test
	public void testBlockSquareSmall()
	{
		CollisionGrid collisionGrid = null;
		int width = 4 * GRID_HALF;
		int height = 4 * GRID_HALF;
		try
		{
			collisionGrid = new CollisionGrid(width, height);
		}
		catch (Exception e)
		{
			fail("Unable to create CollisionGrid object: " + e.getMessage());
			return;
		}
		// Add the blocker object.
		collisionGrid.addBlock(new GridRectangle(0, 0, 2 * GRID_HALF, 2 * GRID_HALF, 2 * GRID_HALF));
		System.out.print(collisionGrid.toString());

		// Verify these locations within the blocker object are blocked.
		assertEquals(collisionGrid.getItem(GRID_HALF, GRID_HALF) != null, true);

		// Verify the locations around the blocker object are empty.
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF,  1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF, -1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF,  1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF,  3 * GRID_HALF) == null, true);
	}
	
	/***********************************************************************************************/
	// Use a large square as a blocker.
	@Test
	public void testBlockSquareLarge()
	{
		CollisionGrid collisionGrid = null;
		int width = 8 * GRID_HALF;
		int height = 8 * GRID_HALF;
		try
		{
			collisionGrid = new CollisionGrid(width, height);
		}
		catch (Exception e)
		{
			fail("Unable to create CollisionGrid object: " + e.getMessage());
			return;
		}
		// Add the blocker object.
		collisionGrid.addBlock(new GridRectangle(-2 * GRID_HALF, 0, 2 * GRID_HALF, 0, 4 * GRID_HALF));
		System.out.print(collisionGrid.toString());

		// Verify these locations within the blocker object are blocked.
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF,  1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF, -1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF, -1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF,  1 * GRID_HALF) != null, true);

		// Verify the locations around the blocker object are empty.
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF,  1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF, -1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF, -3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF, -3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF, -1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF,  1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF,  3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF,  3 * GRID_HALF) == null, true);
	}
	
	/***********************************************************************************************/
	// Use a large square as a blocker.
	@Test
	public void testBlockSquareDiagonal()
	{
		CollisionGrid collisionGrid = null;
		int width = 8 * GRID_HALF;
		int height = 8 * GRID_HALF;
		try
		{
			collisionGrid = new CollisionGrid(width, height);
		}
		catch (Exception e)
		{
			fail("Unable to create CollisionGrid object: " + e.getMessage());
			return;
		}
		// Add the blocker object.
		collisionGrid.addBlock(
			new GridRectangle(-2 * GRID_HALF, -2 * GRID_HALF, 2 * GRID_HALF, 2 * GRID_HALF, 4 * GRID_HALF));
		System.out.print(collisionGrid.toString());

		// Verify these locations within the blocker object are blocked.
		assertEquals(collisionGrid.getItem(             0,              0) != null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF,  1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF, -1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF, -3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF, -1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF,  1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF,  3 * GRID_HALF) != null, true);

		// Verify the locations around the blocker object are empty.
		assertEquals(collisionGrid.getItem( 5 * GRID_HALF,  1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF, -1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF, -3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF, -5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF, -3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF, -1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF,  1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF,  3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF,  5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF,  3 * GRID_HALF) == null, true);
	}
	
	/***********************************************************************************************/
	// Use a horizontal rectangle as a blocker.
	@Test
	public void testBlockRectangleHorizontal()
	{
		CollisionGrid collisionGrid = null;
		int width = 16 * GRID_HALF;
		int height = 16 * GRID_HALF;
		try
		{
			collisionGrid = new CollisionGrid(width, height);
		}
		catch (Exception e)
		{
			fail("Unable to create CollisionGrid object: " + e.getMessage());
			return;
		}
		// Add the blocker object.
		collisionGrid.addBlock(new GridRectangle(-8 * GRID_HALF, 0, 8 * GRID_HALF, 0, 8 * GRID_HALF));
		System.out.print(collisionGrid.toString());

		// Verify these locations within the blocker object are blocked.
		assertEquals(collisionGrid.getItem(             0,              0) != null, true);
		assertEquals(collisionGrid.getItem( 7 * GRID_HALF,  3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 7 * GRID_HALF, -3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-7 * GRID_HALF, -3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-7 * GRID_HALF,  3 * GRID_HALF) != null, true);

		// Verify the locations around the blocker object are empty.
		assertEquals(collisionGrid.getItem( 7 * GRID_HALF,  5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 7 * GRID_HALF, -5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 9 * GRID_HALF, -3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-9 * GRID_HALF, -3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-7 * GRID_HALF, -5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-7 * GRID_HALF,  5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-9 * GRID_HALF,  3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 9 * GRID_HALF,  3 * GRID_HALF) == null, true);
	}
	
	/***********************************************************************************************/
	// Use a vertical rectangle as a blocker.
	@Test
	public void testBlockRectangleVertical()
	{
		CollisionGrid collisionGrid = null;
		int width = 16 * GRID_HALF;
		int height = 16 * GRID_HALF;
		try
		{
			collisionGrid = new CollisionGrid(width, height);
		}
		catch (Exception e)
		{
			fail("Unable to create CollisionGrid object" + e.getMessage());
			return;
		}
		// Add the blocker object.
		collisionGrid.addBlock(new GridRectangle(0, -8 * GRID_HALF, 0, 8 * GRID_HALF, 8 * GRID_HALF));
		System.out.print(collisionGrid.toString());

		// Verify these locations within the blocker object are blocked.
		assertEquals(collisionGrid.getItem(             0,              0) != null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF,  7 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF, -7 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF, -7 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF,  7 * GRID_HALF) != null, true);

		// Verify the locations around the blocker object are empty.
		assertEquals(collisionGrid.getItem( 5 * GRID_HALF,  7 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 5 * GRID_HALF, -7 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF, -9 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF, -9 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF, -7 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF,  7 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF,  9 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF,  9 * GRID_HALF) == null, true);
	}

	/***********************************************************************************************/
	// Use a horizontal rectangle as a blocker.
	@Test
	public void testBlockRectangleDiagonalSmall()
	{
		CollisionGrid collisionGrid = null;
		int width = 16 * GRID_HALF;
		int height = 16 * GRID_HALF;
		try
		{
			collisionGrid = new CollisionGrid(width, height);
		}
		catch (Exception e)
		{
			fail("Unable to create CollisionGrid object" + e.getMessage());
			return;
		}
		// Add the blocker object.
		collisionGrid.addBlock(
			new GridRectangle(-4 * GRID_HALF, -4 * GRID_HALF, 4 * GRID_HALF, 4 * GRID_HALF, 4 * GRID_HALF));

		System.out.print(collisionGrid.toString());

		// Verify these locations within the blocker object are blocked.
		assertEquals(collisionGrid.getItem(    0,     0) != null, true);
		assertEquals(collisionGrid.getItem( 5 * GRID_HALF,  3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF,  1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF, -1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF, -3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF, -5 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF, -3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF, -1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF,  1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF,  3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF,  5 * GRID_HALF) != null, true);

		// Verify the locations around the blocker object are empty.
		assertEquals(collisionGrid.getItem( 5 * GRID_HALF,  5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 7 * GRID_HALF,  3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 5 * GRID_HALF,  1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF, -1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF, -3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF, -5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF, -7 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF, -5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-7 * GRID_HALF, -3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF, -1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF,  1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF,  3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF,  5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF,  7 * GRID_HALF) == null, true);
	}

	/***********************************************************************************************/
	// Use a horizontal rectangle as a blocker.
	@Test
	public void testBlockRectangleDiagonalLarge()
	{
		CollisionGrid collisionGrid = null;
		int width = 28 * GRID_HALF;
		int height = 28 * GRID_HALF;
		try
		{
			collisionGrid = new CollisionGrid(width, height);
		}
		catch (Exception e)
		{
			fail("Unable to create CollisionGrid object" + e.getMessage());
			return;
		}
		// Add the blocker object.
		collisionGrid.addBlock(
			new GridRectangle(-8 * GRID_HALF, -8 * GRID_HALF, 8 * GRID_HALF, 8 * GRID_HALF, 8 * GRID_HALF));
		System.out.print(collisionGrid.toString());

		// Verify these locations within the blocker object are blocked.
		assertEquals(collisionGrid.getItem(             0,              0) != null, true);
		assertEquals(collisionGrid.getItem( 9 * GRID_HALF,  7 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 9 * GRID_HALF,  5 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 7 * GRID_HALF,  3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 5 * GRID_HALF,  1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF, -1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF, -3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF, -5 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF, -7 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF, -9 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-7 * GRID_HALF, -9 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-9 * GRID_HALF, -7 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-9 * GRID_HALF, -5 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-7 * GRID_HALF, -3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF, -1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF,  1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF,  3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 1 * GRID_HALF,  5 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF,  7 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 5 * GRID_HALF,  9 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem( 7 * GRID_HALF,  9 * GRID_HALF) != null, true);

		// Verify the locations around the blocker object are empty.
		assertEquals(collisionGrid.getItem( 11 * GRID_HALF,   7 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 11 * GRID_HALF,   5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(  9 * GRID_HALF,   3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(  7 * GRID_HALF,   1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(  5 * GRID_HALF,  -1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(  3 * GRID_HALF,  -3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(  1 * GRID_HALF,  -5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( -1 * GRID_HALF,  -7 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( -3 * GRID_HALF,  -9 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( -5 * GRID_HALF, -11 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( -7 * GRID_HALF, -11 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( -9 * GRID_HALF,  -9 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-11 * GRID_HALF,  -7 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-11 * GRID_HALF,  -5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( -9 * GRID_HALF,  -3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( -7 * GRID_HALF,  -1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( -5 * GRID_HALF,   1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( -3 * GRID_HALF,   3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( -1 * GRID_HALF,   5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(  1 * GRID_HALF,   7 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(  3 * GRID_HALF,   9 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(  5 * GRID_HALF,  11 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(  7 * GRID_HALF,  11 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(  9 * GRID_HALF,   9 * GRID_HALF) == null, true);
	}

	/***********************************************************************************************/
	@Test
	public void testBlockCircle()
	{
		CollisionGrid collisionGrid = null;
		try
		{
			collisionGrid = new CollisionGrid(16 * GRID_HALF, 16 * GRID_HALF);
		}
		catch (Exception e)
		{
			fail("Unable to create CollisionGrid object" + e.getMessage());
			return;
		}
		collisionGrid.addBlock(new GridCircle(-2 * GRID_HALF, -2 * GRID_HALF, 4 * GRID_HALF));

		System.out.print(collisionGrid.toString());

		// Verify these locations within the blocker object are blocked.
		assertEquals(collisionGrid.getItem(     GRID_HALF, -1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(     GRID_HALF, -3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF, -5 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF, -5 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF, -3 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF, -1 * GRID_HALF) != null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF,      GRID_HALF) != null, true);

		// Verify the locations around the blocker object are empty.
		assertEquals(collisionGrid.getItem(     GRID_HALF,      GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF, -1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem( 3 * GRID_HALF, -2 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(     GRID_HALF, -5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF, -7 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF, -7 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF, -5 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-7 * GRID_HALF, -3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-7 * GRID_HALF, -1 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-5 * GRID_HALF,      GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-3 * GRID_HALF,  3 * GRID_HALF) == null, true);
		assertEquals(collisionGrid.getItem(-1 * GRID_HALF,  3 * GRID_HALF) == null, true);
	}
}

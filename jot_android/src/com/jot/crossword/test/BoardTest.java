//**************************************************************************************************
// Author: Jot Russell (copyright 2017, all rights reserved)

package com.jot.crossword.test;

import com.jot.crossword.Board;
import com.jot.crossword.Bonus;
import com.jot.crossword.Tile;

import org.junit.Test;
import static org.junit.Assert.*;

//**************************************************************************************************

public class BoardTest
{
	// An enumeration set of bonus squares.
	private static final byte NA = (byte)Bonus.NA.ordinal();
	private static final byte L2 = (byte)Bonus.L2.ordinal();
	private static final byte L3 = (byte)Bonus.L3.ordinal();
	private static final byte L4 = (byte)Bonus.L4.ordinal();
	private static final byte W2 = (byte)Bonus.W2.ordinal();
	private static final byte W3 = (byte)Bonus.W3.ordinal();
	private static final byte W4 = (byte)Bonus.W4.ordinal();

	// Location of bonus scores on board.
	private static final byte s_aaBoardBonuses1[][] =
	{
		{ W2,  0,  0,  0, L2,  0,  0,  0, W2 },
		{  0, W2,  0,  0,  0,  0,  0, W2,  0 },
		{  0,  0, L3,  0,  0,  0, L3,  0,  0 },
		{  0,  0,  0, L2,  0, L2,  0,  0,  0 },
		{ L2,  0,  0,  0, W2,  0,  0,  0, L2 },
		{  0,  0,  0, L2,  0, L2,  0,  0,  0 },
		{  0,  0, L3,  0,  0,  0, L3,  0,  0 },
		{  0, W2,  0,  0,  0,  0,  0, W2,  0 },
		{ W2,  0,  0,  0, L2,  0,  0,  0, W2 },
	};

	// Location of bonus scores on board.
	private static final byte s_aaBoardBonuses2[][] =
	{
		{ L3,  0, L2,  0,  0, L2,  0, L3 },
		{  0, W2,  0,  0,  0,  0, W2,  0 },
		{ L2,  0, L2,  0,  0, L2,  0, L2 },
		{  0,  0,  0, L2, L2,  0,  0,  0 },
		{  0,  0,  0, L2, L2,  0,  0,  0 },
		{ L2,  0, L2,  0,  0, L2,  0, L2 },
		{  0, W2,  0,  0,  0,  0, W2,  0 },
		{ L3,  0, L2,  0,  0, L2,  0, L3 },
	};

	// Sample boards in play for testing.
	private static final char s_aaBoardDefault1[][] =
	{
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0, 'K', 0 },
		{ 0,  0,  0,  0, 'W','H','E','N', 0 },
		{ 0, 'R','U','N','E', 0,  0, 'O', 0 },
		{ 0,  0,  0,  0,  0,  0,  0, 'C', 0 },
		{ 0,  0,  0,  0,  0,  0,  0, '@', 0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0 },
	};
	private static final char s_aaBoardDefault2[][] =
	{
		{  0,  0,  0,  0,  0,  0,  0,  0 },
		{  0,  0,  0,  0,  0,  0,  0,  0 },
		{  0,  0,  0,  0,  0,  0, 'K', 0 },
		{  0,  0,  0, 'W','H','E','N', 0 },
		{ 'R','U','N','E', 0,  0, 'O', 0 },
		{  0,  0,  0,  0,  0,  0, 'C', 0 },
		{  0,  0,  0,  0,  0,  0, '@', 0 },
		{  0,  0,  0,  0,  0,  0,  0,  0 },
	};

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	private static final byte s_aTileScores[] =
	{
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 1, 3, 3, 2, 1, 4, 2, 4, 1, 8, 5, 1, 3, 1, 1,
		3,10, 1, 1, 1, 1, 4, 4, 8, 4,10, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	};

	private Board m_board;

	//**********************************************************************************************
	// Check invalid status of a lone tile.
	private void
	testCreateBoardEmpty1()
	{
		try
		{
			m_board = new Board(s_aaBoardBonuses1, 25, 6);
		}
		catch (Exception e)
		{
			fail("Unable to create objects: " + e.getMessage());
		}
	}

	//**********************************************************************************************
	// Check invalid status of a lone tile.
	private void
	testCreateBoardEmpty2()
	{
		try
		{
			m_board = new Board(s_aaBoardBonuses2, 25, 6);
		}
		catch (Exception e)
		{
			fail("Unable to create objects: " + e.getMessage());
		}
	}

	//**********************************************************************************************
	// Check invalid status of a lone tile.
	private void
	testCreateBoardSample1()
	{
		try
		{
			m_board =
				new Board(s_aaBoardBonuses1, 25, 6, null, s_aaBoardDefault1, s_aTileScores, null);
		}
		catch (Exception e)
		{
			fail("Unable to create objects: " + e.getMessage());
		}
	}

	//**********************************************************************************************
	// Check invalid status of a lone tile.
	private void
	testCreateBoardSample2()
	{
		try
		{
			m_board =
				new Board(s_aaBoardBonuses2, 25, 6, null, s_aaBoardDefault2, s_aTileScores, null);
		}
		catch (Exception e)
		{
			fail("Unable to create objects: " + e.getMessage());
		}
	}

	//**********************************************************************************************
	// Check invalid status of a lone tile.
	private void
	testTilePlacement(
		char cTile,
		int iRow,
		int iColumn
	)
	{
		try
		{
			// Create a tile to place on the board.
			Tile tile = new Tile(cTile, s_aTileScores[cTile], null);
			m_board.setTile(iRow, iColumn, tile);

			// The tile should now have a state of PLACED.
			assertEquals("Tile state mismatch: ", Tile.State.PLACED, tile.getState());
		}
		catch (Exception e)
		{
			fail("Unable to create objects: " + e.getMessage());
		}
	}

	//**********************************************************************************************
	// Check invalid status of a lone tile.
	@Test
	public void
	testIsValidMove_Alone()
	{
		testCreateBoardSample1();
		testTilePlacement('A', 1, 1);

		// Should not be able to put down a single tile without crossing another.
		assertFalse("Placed tile wrongly allowed by itself.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check invalid status of a lone tile down on the far side of the board.
	@Test
	public void
	testIsValidMove_AloneBottom()
	{
		testCreateBoardSample1();
		testTilePlacement('A', 7, 5);

		// Should not be able to put down a single tile without crossing another.
		assertFalse("Placed tile wrongly allowed by itself.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check invalid status of a lone tile on the center
	@Test
	public void
	testIsValidMove_AloneCenter1()
	{
		testCreateBoardEmpty1();
		testTilePlacement('A', 4, 4);

		assertFalse("Lone tile allowed on center.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check invalid status of a lone tile on the center
	@Test
	public void
	testIsValidMove_AloneCenter2()
	{
		testCreateBoardEmpty2();
		testTilePlacement('A', 4, 4);

		assertFalse("Lone tile allowed on center.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check invalid status of a diagonal word from the center
	@Test
	public void
	testIsValidMove_Diagonal()
	{
		testCreateBoardEmpty1();
		testTilePlacement('A', 4, 4);
		testTilePlacement('B', 5, 5);

		assertFalse("Diagonal placement allowed from center.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check valid status of a lone couple tiles on the center.
	@Test
	public void
	testIsValidMove_Center1()
	{
		testCreateBoardEmpty1();
		testTilePlacement('A', 4, 4);
		testTilePlacement('B', 5, 4);

		assertTrue("Could not place lone word on center.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check valid status of a lone couple tiles on the center.
	@Test
	public void
	testIsValidMove_Center2()
	{
		testCreateBoardEmpty2();
		testTilePlacement('A', 4, 4);
		testTilePlacement('B', 5, 4);

		assertTrue("Could not place lone word on center.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check valid status of a tile placed infront of a word.
	@Test
	public void
	testIsValidMove_Front()
	{
		testCreateBoardSample1();
		testTilePlacement('A', 5, 0);

		assertTrue("Tile could not be placed in front of a word.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check valid status of a tile placed infront of a word.
	@Test
	public void
	testIsValidMove_Back()
	{
		testCreateBoardSample1();
		testTilePlacement('A', 4, 8);

		assertTrue("Tile could not be placed behind a word.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check valid status of a tile placed under a word.
	@Test
	public void
	testIsValidMove_Bottom()
	{
		testCreateBoardSample1();
		testTilePlacement('A', 8, 7);

		assertTrue("Tile could not be placed under of a word.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check valid status of a tile placed on top of a word.
	@Test
	public void
	testIsValidMove_Top()
	{
		testCreateBoardSample1();
		testTilePlacement('A', 2, 7);

		assertTrue("Tile could not be placed on top of a word.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check valid status of a few tiles place inline.
	@Test
	public void
	testIsValidMove_Horizontal()
	{
		testCreateBoardSample1();
		testTilePlacement('A', 6, 5);
		testTilePlacement('B', 6, 6);
		testTilePlacement('C', 6, 8);

		assertTrue("Tile could not place a horizontal word.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check valid status of a few tiles place inline.
	@Test
	public void
	testIsValidMove_Vertical()
	{
		testCreateBoardSample1();
		testTilePlacement('A', 4, 1);
		testTilePlacement('B', 6, 1);
		testTilePlacement('C', 7, 1);

		assertTrue("Tile could not place a horizontal word.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check invalid status of a word placed with a gap.
	@Test
	public void
	testIsValidMove_BrokenWordHorizontal()
	{
		testCreateBoardSample1();
		testTilePlacement('A', 6, 4);
		testTilePlacement('B', 6, 6);
		testTilePlacement('C', 6, 8);

		assertFalse("Allowed placement of a broken horizontal word.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check invalid status of a word placed with a gap.
	@Test
	public void
	testIsValidMove_BrokenWordVertical()
	{
		testCreateBoardSample1();
		testTilePlacement('A', 3, 2);
		testTilePlacement('B', 4, 2);
		testTilePlacement('C', 7, 2);

		assertFalse("Allowed placement of a broken vertical word.", m_board.isValidMove());
	}

	//**********************************************************************************************
	// Check invalid status of a word placed with a gap.
	@Test
	public void
	testScoreWord_DoubleWord()
	{
		testCreateBoardSample1();
		testTilePlacement('A', 4, 1);
		testTilePlacement('B', 6, 1);
		testTilePlacement('C', 7, 1);
		m_board.playWord();

		try
		{
			assertEquals("Score mismatch: ", 2 * (1 + 1 + 3 + 3), m_board.scoreWord());
		}
		catch (Exception e)
		{
			fail("Unable to create objects: " + e.getMessage());
		}
	}
}

//**************************************************************************************************

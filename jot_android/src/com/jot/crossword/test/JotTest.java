//**************************************************************************************************
// Author: Jot Russell (copyright 2017, all rights reserved)

package com.jot.crossword.test;

import com.jot.crossword.BestWord;
import com.jot.crossword.Game;
import com.jot.crossword.Jot;
import com.jot.decomp.TreeWords;
import com.jot.decomp.Words;

import org.junit.Test;
import static org.junit.Assert.*;

//**************************************************************************************************

public class JotTest
{
	private static String s_strTreeFile = new String("data/crosswords.jot");

	//**********************************************************************************************
	@Test
	public void
	testJot()
	{
		BestWord bestWord;
		Game game;
		Words words;

		try
		{
			words = new TreeWords(s_strTreeFile);
			game = new Jot(words, 2);

			int iPlayer = 0;
			while (!game.isOver())
			{
				game.moveRobot(iPlayer);
				System.out.println(game.toString());

				if (!game.areValidAllWords())
				{
					throw new Exception("Invalid words placed.");
				}
						
				iPlayer ^= 1;
			}
		}
		catch (Exception e)
		{
			fail("Unable to create objects: " + e.getMessage());
		}
	}
}

//**************************************************************************************************

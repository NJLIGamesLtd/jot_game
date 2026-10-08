/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * JotSim Unit Test.
 */

package com.jot.jottower.test;

import com.jot.jottower.JotSim;
import com.jot.trace.Trace;

import org.junit.Test;

import static org.junit.Assert.*;

public class JotSimTest
{
	@Test
	public void testJotTower()
	{
		// Create a Simulation, but don't start it. We will manually update it.
		JotSim jotSim;
		try
		{
			jotSim = new JotSim((long)((24 * 40 * 365.25) /* 40 years */ + (6 * 60 * 60 * 1000) /* for 6am */));
		}
		catch (Exception e)
		{
			fail("Failed to create JotSim: " + e.getMessage());
			return;
		}

		// For each track, run an update to move the item from start to finish.
		for (int i = 0; i < 1000; ++i)
		{
			Trace.important("JotTower Testing [" + i + "]");
			if (i == 15)
			{
				i = 15;
			}
			try
			{
				jotSim.update();
			}
			catch (Exception e)
			{
				fail("Failed to update JotSim: " + e.getMessage());
				return;
			}
		}
	}
}

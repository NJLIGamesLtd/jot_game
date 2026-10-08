/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Unit Test for a lift Door.
 */

package com.jot.lift.test;

import org.junit.Test;

import com.jot.lift.Door;
import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;

import static org.junit.Assert.*;

public class DoorTest
{
	@Test
	public void testDoorOpen()
	{
		// Create a Simulation, but don't start it. We will manually update it.
		Simulation sim = new Simulation(0);

		Door door1, door2;	
		try
		{
			door1 = new Door(null, new Cycles(4.0));
			door2 = new Door(null, new Cycles(4.0));
		}
		catch (Exception e)
		{
			fail("Unable to create Door objects" + e.getMessage());
			door1 = door2 = null;
		}
		if (door1 != null && door2 != null)
		{
			try
			{
				Cycles twoSeconds = new Cycles(2.0);
				assertTrue(door1.isClosed());
				assertTrue(door2.isClosed());
				door1.open();
				assertTrue(door1.isClosed());
				assertTrue(door2.isClosed());
				door1.setSpouse(door2);
				door1.open();
				assertTrue(door1.isOpening());
				assertTrue(door2.isOpening());
				sim.updateTime(twoSeconds);
				door1.update(sim);
				assertTrue(door1.isOpening());
				assertTrue(door2.isOpening());
				sim.updateTime(twoSeconds);
				door1.update(sim);
				assertTrue(door1.isOpen());
				assertTrue(door2.isOpen());
			}
			catch (Exception e)
			{
				fail("Unable to update Door objects" + e.getMessage());
			}
		}
	}

	@Test
	public void testDoorClose()
	{
		// Create a Simulation, but don't start it. We will manually update it.
		Simulation sim = new Simulation(0);

		Door door1, door2;	
		try
		{
			door1 = new Door(null, new Cycles(4.0));
			door2 = new Door(null, new Cycles(4.0));
		}
		catch (Exception e)
		{
			fail("Unable to create Door objects" + e.getMessage());
			door1 = door2 = null;
		}
		if (door1 != null && door2 != null)
		{
			try
			{
				Cycles threeSeconds = new Cycles(3.0);
				assertTrue(door1.isClosed());
				door1.open();
				assertTrue(door1.isClosed());
				door1.setSpouse(door2);
				door1.open();
				assertTrue(door1.isOpening());
				sim.updateTime(threeSeconds);
				door1.update(sim);
				assertTrue(door1.isOpening());
				sim.updateTime(threeSeconds);
				door1.update(sim);
				assertTrue(door1.isOpen());
				door1.close(false);
				assertTrue(door1.isClosing());
				sim.updateTime(threeSeconds);
				door1.update(sim);
				assertTrue(door1.isClosing());
				sim.updateTime(threeSeconds);
				door1.update(sim);
				assertTrue(door1.isClosed());
			}
			catch (Exception e)
			{
				fail("Unable to update Door object" + e.getMessage());
			}
		}
	}
}

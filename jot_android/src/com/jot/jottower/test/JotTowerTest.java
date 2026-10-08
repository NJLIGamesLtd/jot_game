/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * JotTower Unit Test.
 */

package com.jot.jottower.test;

import com.jot.item.*;
import com.jot.jottower.JotTower;
import com.jot.jottower.LevelOffice;
import com.jot.rollavator.RollaCar;
import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.*;

import org.junit.Test;

import static org.junit.Assert.*;

public class JotTowerTest
{
	//private static final double EPSILON = 1e-10;

	// Define two tracks of positions.
	final Position posFixed = new Position(0, 0, 0, 0);

	@Test
	public void testJotTowerConstruct()
	{
		// Create our tower.
		Structure tower;
		try
		{
			tower = new JotTower(posFixed);
		}
		catch (Exception e)
		{
			fail("Failed to create JotTower: " + e.getMessage());
			return;
		}
	}

	@Test
	public void testJotTowerOfferPerson()
	{
		// Create our tower.
		Structure tower;
		Drone person;
		try
		{
			tower = new JotTower(posFixed);
			person = new Person(new Position(100000, 0, 0, 0), null);
		}
		catch (Exception e)
		{
			fail("Failed to create JotTower: " + e.getMessage());
			return;
		}

		Trace.important("JotTower Testing: offering person.");
		try
		{
			// Place our robot on the start of the first track.
			assertEquals(tower.offer(person), true);
		}
		catch (Exception e)
		{
			fail("Failed to offer person to JotTower: " + e.getMessage());
			return;
		}
	}

	public void testJotTowerDeliverPersonToRollavator(int iFloor)
	{
		// Create our tower.
		Structure tower;
		Robot person;
		try
		{
			tower = new JotTower(posFixed);
			person = new Person(new Position(100000, 0, 0, 0), null);
		}
		catch (Exception e)
		{
			fail("Failed to create JotTower: " + e.getMessage());
			return;
		}

		try
		{
			// Set the person destination stack to the tower and office level.
			person.pushDestination(tower.getStructureLevel(iFloor));
			person.pushDestination(tower);
		}
		catch (Exception e)
		{
			fail("Failed to direct person towards tower and office: " + e.getMessage());
			return;
		}

		Trace.important("JotTower Testing: offering person.");
		try
		{
			// Place our robot on the start of the first track.
			assertEquals(tower.offer(person), true);
		}
		catch (Exception e)
		{
			fail("Failed to offer person to JotTower: " + e.getMessage());
			return;
		}

		// Our person should now belong to a Place.
		assertTrue(person.getPlace() != null);

		// Create a Simulation, but don't start it. We will manually update it.
		Simulation sim = new Simulation(0);

		// Run through updates one clock cycle at a time.
		Cycles oneTick = new Cycles(1);

		while (person.getPlace().getClass() != RollaCar.class)
		{
			try
			{
				sim.updateTime(oneTick);
				tower.update(sim);
			}
			catch (Exception e)
			{
				fail("Failed to update JotTower: " + e.getMessage());
				return;
			}
		}
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor10()
	{
		testJotTowerDeliverPersonToRollavator(10);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor21()
	{
		testJotTowerDeliverPersonToRollavator(21);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor32()
	{
		testJotTowerDeliverPersonToRollavator(32);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor43()
	{
		testJotTowerDeliverPersonToRollavator(43);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor54()
	{
		testJotTowerDeliverPersonToRollavator(54);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor65()
	{
		testJotTowerDeliverPersonToRollavator(65);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor76()
	{
		testJotTowerDeliverPersonToRollavator(76);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor87()
	{
		testJotTowerDeliverPersonToRollavator(87);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor98()
	{
		testJotTowerDeliverPersonToRollavator(98);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor101()
	{
		testJotTowerDeliverPersonToRollavator(101);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor112()
	{
		testJotTowerDeliverPersonToRollavator(112);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor123()
	{
		testJotTowerDeliverPersonToRollavator(123);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor134()
	{
		testJotTowerDeliverPersonToRollavator(134);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor145()
	{
		testJotTowerDeliverPersonToRollavator(145);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor156()
	{
		testJotTowerDeliverPersonToRollavator(156);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor167()
	{
		testJotTowerDeliverPersonToRollavator(167);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor178()
	{
		testJotTowerDeliverPersonToRollavator(178);
	}

	@Test
	public void testJotTowerDeliverPersonToRollavatorFloor188()
	{
		testJotTowerDeliverPersonToRollavator(188);
	}

	@Test
	public void testJotTowerDeliverTwoPeopleToRollavator()
	{
		// Create our tower.
		Structure tower;
		Drone person1;
		Drone person2;
		try
		{
			tower = new JotTower(posFixed);
			person1 = new Person(new Position(100000, 0, 0, 0), null);
			person2 = new Person(new Position(0, 100000, 0, 0), null);
		}
		catch (Exception e)
		{
			fail("Failed to create JotTower: " + e.getMessage());
			return;
		}

		try
		{
			// Set the person destination stack to the tower and office level.
			person1.pushDestination(tower.getStructureLevel(10));
			person1.pushDestination(tower);
			person2.pushDestination(tower.getStructureLevel(160));
			person2.pushDestination(tower);
		}
		catch (Exception e)
		{
			fail("Failed to direct person towards tower and office: " + e.getMessage());
			return;
		}

		Trace.important("JotTower Testing: offering person.");
		try
		{
			// Place our robots on the start of the first track.
			assertEquals(tower.offer(person1), true);
			assertEquals(tower.offer(person2), true);
		}
		catch (Exception e)
		{
			fail("Failed to offer people to JotTower: " + e.getMessage());
			return;
		}

		// Our people should now belong to a Place.
		assertTrue(person1.getPlace() != null);
		assertTrue(person2.getPlace() != null);

		// Create a Simulation, but don't start it. We will manually update it.
		Simulation sim = new Simulation(0);

		// Run through updates one clock cycle at a time.
		Cycles oneTick = new Cycles(1);

		int nArrived = 0;
		while (nArrived < 3)
		{
			if ((nArrived & 1) == 0)
			{
				if (person1.getPlace().getClass() == RollaCar.class)
				{
					nArrived |= 1;
				}
			}
			if ((nArrived & 2) == 0)
			{
				if (person2.getPlace().getClass() == RollaCar.class)
				{
					nArrived |= 2;
				}
			}

			try
			{
				sim.updateTime(oneTick);
				tower.update(sim);
			}
			catch (Exception e)
			{
				fail("Failed to update JotTower: " + e.getMessage());
				return;
			}
		}
	}

	@Test
	public void testJotTowerDeliverPersonToOfficeLevel()
	{
		int iFloor = 10;

		// Create our tower.
		Structure tower;
		Robot person;
		try
		{
			tower = new JotTower(posFixed);
			person = new Person(new Position(100000, 0, 0, 0), null);
		}
		catch (Exception e)
		{
			fail("Failed to create JotTower: " + e.getMessage());
			return;
		}

		try
		{
			// Set the person destination stack to the tower and office level.
			person.pushDestination(tower.getStructureLevel(iFloor));
			person.pushDestination(tower);
		}
		catch (Exception e)
		{
			fail("Failed to direct person towards tower and office: " + e.getMessage());
			return;
		}

		Trace.important("JotTower Testing: offering person.");
		try
		{
			// Place our robot on the start of the first track.
			assertEquals(tower.offer(person), true);
		}
		catch (Exception e)
		{
			fail("Failed to offer person to JotTower: " + e.getMessage());
			return;
		}

		// Our person should now belong to a Place.
		assertTrue(person.getPlace() != null);

		// Create a Simulation, but don't start it. We will manually update it.
		Simulation sim = new Simulation(0);

		// Run through updates one clock cycle at a time.
		Cycles oneTick = new Cycles(1);

		// Wait for the person to make it into a rollavator car.
		while (person.getPlace().getClass() != RollaCar.class)
		{
			try
			{
				sim.updateTime(oneTick);
				tower.update(sim);
			}
			catch (Exception e)
			{
				fail("Failed to update JotTower: " + e.getMessage());
				return;
			}
		}

		// Wait for the person to make it onto the office level.
		while (person.getPlace().getClass() != LevelOffice.class)
		{
			try
			{
				sim.updateTime(oneTick);
				tower.update(sim);
			}
			catch (Exception e)
			{
				fail("Failed to update JotTower: " + e.getMessage());
				return;
			}
		}
	}
}

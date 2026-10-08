/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Unit Test for a lift Car.
 */

package com.jot.lift.test;

import org.junit.Test;

import com.jot.item.Person;
import com.jot.item.Place;
import com.jot.item.PlaceImpl;
import com.jot.lift.Door;
import com.jot.lift.Lift;
import com.jot.lift.LiftCar;
import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;
import com.jot.vector.Position;
import com.jot.vector.Size;

import static org.junit.Assert.*;

public class CarTest
{
	// Good construction test.
	@Test
	public void testCarConstructGood1()
	{
		Position position = new Position();
		try
		{
			Lift lift = new Lift(0, null, position, null, null);
			LiftCar car = new LiftCar(position, lift);
		}
		catch (Exception e)
		{
			fail("Unable to construct LiftCar object" + e.getMessage());
		}
	}

	// Good construction test.
	@Test
	public void testCarConstructGood2()
	{
		final Size sizeOuter = new Size(1760, 1500, 3000);
		final Size sizeInner = new Size(1600, 1200, 2500);
		final Size sizeDoor = new Size(1200, 200, 2300);
		final Cycles cyclesDoor = new Cycles(4.0);
		Position position = new Position();

		try
		{
			Lift lift = new Lift(0, null, position, null, null);
			LiftCar car = new LiftCar(
				position, lift, 100, 10, sizeOuter, sizeInner, sizeDoor, cyclesDoor, 3);
		}
		catch (Exception e)
		{
			fail("Unable to construct LiftCar object" + e.getMessage());
		}
	}

	// Verify construction fails when the inside is bigger than the outside.
	@Test
	public void testCarConstructInsideOut()
	{
		final Size sizeOuter = new Size(1760, 1500, 3000);
		final Size sizeInner = new Size(1600, 1200, 2500);
		final Size sizeDoor = new Size(1200, 200, 2300);
		final Cycles cyclesDoor = new Cycles(4.0);
		Position position = new Position();
		LiftCar car;

		try
		{
			Lift lift = new Lift(0, null, position, null, null);
			car = new LiftCar(
				position, lift, 100, 10, sizeInner, sizeOuter, sizeDoor, cyclesDoor, 3);
		}
		catch (Exception e)
		{
			car = null;
		}
		if (car != null)
		{
			fail("LiftCar object constructed with insides bigger than outsides.");
		}
	}

	// Verify construction fails when the door is larger than the car.
	@Test
	public void testCarConstructOversizedDoor()
	{
		final Size sizeOuter = new Size(1760, 1500, 3000);
		final Size sizeInner = new Size(1600, 1200, 2500);
		final Size sizeDoorTall = new Size(1200, 200, 3300);
		final Size sizeDoorWide = new Size(1800, 200, 2300);
		final Size sizeDoorThick = new Size(1200, 500, 2300);
		final Cycles cyclesDoor = new Cycles(4.0);
		Position position = new Position();
		LiftCar car;

		try
		{
			Lift lift = new Lift(0, null, position, null, null);
			car = new LiftCar(
				position, lift, 100, 10, sizeInner, sizeOuter, sizeDoorTall, cyclesDoor, 3);
		}
		catch (Exception e)
		{
			car = null;
		}
		if (car != null)
		{
			fail("LiftCar object constructed with Door taller than the car.");
		}

		try
		{
			Lift lift = new Lift(0, null, position, null, null);
			car = new LiftCar(
				position, lift, 100, 10, sizeInner, sizeOuter, sizeDoorWide, cyclesDoor, 3);
		}
		catch (Exception e)
		{
			car = null;
		}
		if (car != null)
		{
			fail("LiftCar object constructed with Door wider than the car.");
		}

		try
		{
			Lift lift = new Lift(0, null, position, null, null);
			car = new LiftCar(
				position, lift, 100, 10, sizeInner, sizeOuter, sizeDoorThick, cyclesDoor, 3);
		}
		catch (Exception e)
		{
			car = null;
		}
		if (car != null)
		{
			fail("LiftCar object constructed with Door thicker than the car walls.");
		}
	}

	// Test the ability to add people to our LiftCar.
	@Test
	public void testCarAdd()
	{
		// Create a Simulation, but don't start it. We will manually update it.
		Simulation sim = new Simulation(0);
		Position position = new Position();

		LiftCar car;
		Door doorPort;
		try
		{
			Lift lift = new Lift(0, null, position, null, null);
			car = new LiftCar(position, lift);
			doorPort = new Door(null, new Cycles(4.0));
		}
		catch (Exception e)
		{
			fail("Unable to create LiftCar object" + e.getMessage());
			car = null;
			doorPort = null;
		}
		if (car != null && doorPort != null)
		{
			try
			{
				boolean status = false;

				// Create a person to add to our car.
				Person person = new Person(new Position(), null);
				assertFalse(car.offer(person));

				// Give the person a floor destination.
				Place placeDest = new PlaceImpl(0, null, new Position(0, 0, 40 * 4000, 0));
				person.pushDestination(placeDest);

				Cycles twoSeconds = new Cycles(2.0);
				assertTrue(car.isClosed());
				car.open();
				assertTrue(car.isClosed());
				car.getDoor().setSpouse(doorPort);
				car.open();
				assertTrue(car.isOpening());
				sim.updateTime(twoSeconds);
				car.update(sim);
				assertTrue(car.isOpening());
				sim.updateTime(twoSeconds);
				car.update(sim);
				assertTrue(car.isOpen());

				// Should be able to fit only four people into the Car.
				for (int i = 0; i < 6; ++i)
				{
					status = car.offer(person);
					assertTrue(status);
					sim.updateTime(twoSeconds);
					car.update(sim);
					person = new Person(new Position(), null);

					// Give the person a floor destination.
					placeDest = new PlaceImpl(0, null, new Position(0, 0, (i + 41) * 4000, 0));
					person.pushDestination(placeDest);
				}

				status = car.offer(person);
				assertTrue(!status);
				sim.updateTime(twoSeconds);
				car.update(sim);

				Cycles fiveSeconds = new Cycles(5.0);
				assertTrue(car.isOpen());
				car.close(false);
				assertTrue(car.isClosing());
				sim.updateTime(fiveSeconds);
				car.update(sim);
				assertTrue(car.isClosed());
			}
			catch (Exception e)
			{
				fail("Unable to update Door object" + e.getMessage());
			}
		}
	}
}

/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Unit Test for a BeltPort.
 */

package com.jot.rollavator.test;

import org.junit.Test;

import com.jot.item.Person;
import com.jot.item.StructureLevel;
import com.jot.jottower.LevelOffice;
import com.jot.lift.Door;
import com.jot.lift.Lift;
import com.jot.rollavator.BeltPort;
import com.jot.rollavator.Box;
import com.jot.rollavator.BoxPort;
import com.jot.rollavator.BoxRing;
import com.jot.rollavator.Carousel;
import com.jot.rollavator.RollaCar;
import com.jot.rollavator.Rollavator;
import com.jot.rollavator.TrackRing;
import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.TrackCar;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

import static org.junit.Assert.*;

public class BeltPortTest
{
	private static final int s_nLevels = 20;
	private static final Size s_size = new Size(6000, 8000, 4000 * s_nLevels);
	private static final Position s_aPosRollavators[] =
	{
		new Position(6560, 7380, 0, new Orientation(150)),
		new Position(12980, 22500, 0, new Orientation(120)),
		new Position(0, 18180, 4000 * (s_nLevels - 1), new Orientation(90)),
	};

	// Construction test.
	@Test
	public void testBeltPortConstruct()
	{
		try
		{
			Rollavator lift = new Rollavator();
			BeltPort beltPort = new BeltPort(new Position(), null, lift);
		}
		catch (Exception e)
		{
			fail("Unable to construct BeltPort object" + e.getMessage());
		}
	}

	// Insert car test
	@Test
	public void testBeltPortInsertCar()
	{
		try
		{
			Rollavator lift = new Rollavator();
			Position position = new Position();
			BeltPort beltPort = new BeltPort(position, null, lift);

			// Without any cars, we should not be able to move.
			assertFalse(beltPort.canMove());

			// Get the left most BoxPort.
			BoxPort boxPort = beltPort.getBoxPort(0);
			assertTrue(boxPort != null);

			// Need a car to add to one of the BeltPort objects to give it reason
			// to move.
			TrackCar car = new RollaCar(position, lift);
			assertTrue(car != null);

			// Marry the boxPort to itself to allow us to offer it the car.
			boxPort.setSpouse(boxPort);

			// Try to offer the car onto its track.
			assertTrue(beltPort.offer(car));
		}
		catch (Exception e)
		{
			fail("Unable to construct BeltPort object" + e.getMessage());
		}
	}

	// Move car right test.
	@Test
	public void testBeltPortMoveRight()
	{
		try
		{
			// Create a Simulation, but don't start it. We will manually update it.
			Simulation sim = new Simulation(0);

			Rollavator lift = new Rollavator();
			Position position = new Position();
			BeltPort beltPort = new BeltPort(position, null, lift);

			// Without any cars, we should not be able to move.
			assertFalse(beltPort.canMove());

			// Get the left most BoxPort.
			BoxPort boxPort = beltPort.getBoxPort(0);
			assertTrue(boxPort != null);

			// Need a car to add to one of the BeltPort objects to give it reason
			// to move.
			TrackCar car = new RollaCar(position, lift);
			assertTrue(car != null);

			// Marry the boxPort to itself to allow us to offer it the car.
			boxPort.setSpouse(boxPort);

			// Try to offer the car onto its track.
			assertTrue(beltPort.offer(car));

			// Need to assign the car's position onto the track, because in the program,
			// it would be at the edge of an adjoining track.
			car.setSection(boxPort.getSectionFirst());

			// Run through updates to circle the carousel around and transition
			// cars over to the shaft.
			Cycles oneTick = new Cycles(1);

			// Run updates until the car is fully loaded.
			while (car.getSection() != boxPort.getSectionLast())
			{
				sim.updateTime(oneTick);
				beltPort.update(sim);
			}

			// Need to break the marriage before we can shift sideways.
			boxPort.setSpouse(null);

			// Confirm we can move towards the right.
			assertTrue(beltPort.canMove());
			do
			{
				beltPort.move();
			} while (beltPort.canMove());

			// The belt should have moved the car to right slot.
			assertTrue(beltPort.isRight());
		}
		catch (Exception e)
		{
			fail("Unable to construct BeltPort object" + e.getMessage());
		}
	}

	// Move car right test.
	@Test
	public void testBeltPortDeliverPerson()
	{
		try
		{
			// Create a Simulation, but don't start it. We will manually update it.
			Simulation sim = new Simulation(0);

			// Need structure levels as a destination for the person.
			StructureLevel[] aLevels = new StructureLevel[10];

			// Need positions for the two carousels and shaft of our rollavator.
			Position aPosRollavator[] =
			{
				new Position(6560, 7380, 0, new Orientation(150)),
				new Position(12980, 22500, 0, new Orientation(120)),
				new Position(0, 18180, 4000 * s_nLevels, new Orientation(90)),
			};

			// Create a rollavator to access these levels.
			Rollavator lift = new Rollavator(aPosRollavator, aLevels, 2, false);

			// Create an array of these for the office level.
			Lift[] aLifts = new Lift[1];
			aLifts[0] = lift;

			// Create each of our office levels as destinations for our lift.
			for (int iLevel = 2; iLevel < 10; ++iLevel)
			{
				StructureLevel level = new LevelOffice(new Position(0, 0, iLevel * 4000, 0), aLifts);
				aLevels[iLevel] = level;
			}

			// To complete the Rollavator's instantiation, we need to set connections to this levels.
			lift.setLevelConnections();
			for (int iLevel = 2; iLevel < 10; ++iLevel)
			{
			    aLevels[iLevel].setLiftConnections();
			}

			// Now that we have a destination for our port, provide its connection.
			LevelOffice levelOffice = (LevelOffice)aLevels[9];
			BeltPort beltPort = levelOffice.m_aPorts[0];
			Position position = beltPort.getPosition();

			// Without any cars, we should not be able to move.
			assertFalse(beltPort.canMove());

			// Get the left most BoxPort.
			BoxPort boxPort = beltPort.getBoxPort(0);
			assertTrue(boxPort != null);

			// Need a car to add to one of the BeltPort objects to give it reason
			// to move.
			RollaCar car = new RollaCar(position, lift);
			assertTrue(car != null);

			// Create a person to add to the car.
			Person person = new Person(position, null);

			// Give the person an office destination.
			person.pushDestination(aLevels[9]);

			// Assert that we can add this person to the car.
			assertTrue(car.addTest(person));

			// Marry the boxPort to itself to allow us to offer it the car.
			boxPort.setSpouse(boxPort);

			// Try to offer the car onto its track.
			assertTrue(beltPort.offer(car));

			// Need to assign the car's position onto the track, because in the program,
			// it would be at the edge of an adjoining track.
			car.setSection(boxPort.getSectionFirst());

			// Run through updates to circle the carousel around and transition
			// cars over to the shaft.
			Cycles oneTick = new Cycles(1);

			// Run updates until the car is fully loaded.
			while (car.getSection() != boxPort.getSectionLast())
			{
				sim.updateTime(oneTick);
				car.update(sim);
				beltPort.update(sim);
				aLevels[9].update(sim);
			}

			// Need to break the marriage before we can shift sideways.
			boxPort.setSpouse(null);

			// Confirm we can't move sideways until we off load the person.
			assertFalse(beltPort.canMove());

			// Loop until the person is delivered.
			while (!car.isEmpty())
			{
				// The car door should be married to the office level door.
				sim.updateTime(oneTick);
				car.update(sim);
				beltPort.update(sim);
				aLevels[9].update(sim);
			}

			// Loop until the doors are closed.
			while (!car.isClosed())
			{
				sim.updateTime(oneTick);
				car.update(sim);
				beltPort.update(sim);
				aLevels[9].update(sim);
			}

			// Confirm we can move towards the right.
			assertTrue(beltPort.canMove());
			do
			{
				beltPort.move();
			} while (beltPort.canMove());

			// The belt should have moved the car to right slot.
			assertTrue(beltPort.isRight());
		}
		catch (Exception e)
		{
			fail("Unable to construct BeltPort object" + e.getMessage());
		}
	}

	// Move car right and load a second car.
	@Test
	public void testBeltPortInsertTwoCars()
	{
		try
		{
			// Create a Simulation, but don't start it. We will manually update it.
			Simulation sim = new Simulation(0);

			Rollavator lift = new Rollavator();
			Position position = new Position();
			BeltPort beltPort = new BeltPort(position, null, lift);

			// Without any cars, we should not be able to move.
			assertFalse(beltPort.canMove());

			// Get the left most BoxPort.
			BoxPort boxPort = beltPort.getBoxPort(0);
			assertTrue(boxPort != null);

			// Need a car to add to one of the BeltPort objects to give it reason
			// to move.
			TrackCar car = new RollaCar(position, lift);
			assertTrue(car != null);

			// Marry the boxPort to itself to allow us to offer it the car.
			boxPort.setSpouse(boxPort);

			// Try to offer the car onto its track.
			assertTrue(beltPort.offer(car));

			// Need to assign the car's position onto the track, because in the program,
			// it would be at the edge of an adjoining track.
			car.setSection(boxPort.getSectionFirst());

			// Run through updates to circle the carousel around and transition
			// cars over to the shaft.
			Cycles oneTick = new Cycles(1);

			// Run updates until the car is fully loaded.
			while (car.getSection() != boxPort.getSectionLast())
			{
				sim.updateTime(oneTick);
				beltPort.update(sim);
			}

			// Need to break the marriage before we can shift sideways.
			boxPort.setSpouse(null);

			// Confirm we can move towards the right.
			assertTrue(beltPort.canMove());
			do
			{
				beltPort.move();
			} while (beltPort.canMove());

			// The belt should have moved the car to right slot.
			assertTrue(beltPort.isRight());

			// Get the left most BoxPort now.
			boxPort = beltPort.getBoxPort(0);
			assertTrue(boxPort != null);

			// Should be able to add a second car now.
			car = new RollaCar(position, lift);
			assertTrue(car != null);

			// Marry the boxPort to itself to allow us to offer it the car.
			boxPort.setSpouse(boxPort);

			// Try to offer the car onto its track.
			assertTrue(beltPort.offer(car));

			// Need to assign the car's position onto the track, because in the program,
			// it would be at the edge of an adjoining track.
			car.setSection(boxPort.getSectionFirst());

			// Run updates until the car is fully loaded.
			while (car.getSection() != boxPort.getSectionLast())
			{
				sim.updateTime(oneTick);
				beltPort.update(sim);
			}

			// Need to break the marriage before we can shift sideways.
			boxPort.setSpouse(null);

			// With two cars, we should not be able to move.
			assertFalse(beltPort.canMove());
		}
		catch (Exception e)
		{
			fail("Unable to construct BeltPort object" + e.getMessage());
		}
	}
}

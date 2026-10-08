/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Unit Test for a LiftReturn.
 */

package com.jot.rollavator.test;

import org.junit.Test;

import com.jot.item.Person;
import com.jot.item.StructureLevel;
import com.jot.jottower.LevelOffice;
import com.jot.lift.Door;
import com.jot.lift.Lift;
import com.jot.rollavator.Box;
import com.jot.rollavator.BoxRing;
import com.jot.rollavator.Carousel;
import com.jot.rollavator.RollaCar;
import com.jot.rollavator.Rollavator;
import com.jot.rollavator.TrackRing;
import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

import static org.junit.Assert.*;

public class LiftReturnTest
{
	private static final int s_nLevels = 20;
	private static final Size s_size = new Size(6000, 8000, 4000 * s_nLevels);
	private static final Position s_aPosRollavators[] =
	{
		new Position(6560, 7380, 0, new Orientation(150)),
		new Position(12980, 22500, 0, new Orientation(120)),
		new Position(0, 18180, 4000 * (s_nLevels - 1), new Orientation(90)),
	};

	// Good construction test.
	@Test
	public void testLiftReturnConstruct()
	{
		StructureLevel[] aLevels = new StructureLevel[s_nLevels];

		try
		{
			Lift lift = new Rollavator();
		}
		catch (Exception e)
		{
			fail("Unable to construct Rollavator object" + e.getMessage());
		}
	}

	// Add a person to a carousel car.
	@Test
	public void testLiftReturnAddPerson()
	{
		try
		{
			// Create a Simulation, but don't start it. We will manually update it.
			Simulation sim = new Simulation(0);

			// Create the array of levels and add our office level.
			StructureLevel[] aLevels = new StructureLevel[s_nLevels];
			for (int iLevel = 10; iLevel < 20; ++iLevel)
			{
				StructureLevel level = new LevelOffice(new Position(0, 0, iLevel * 4000, 0), null);
				aLevels[iLevel] = level;
			}
			Rollavator lift = new Rollavator();

			// Get the BoxCarousel at position zero.
			Carousel carousel = lift.getLobbyCarousel();
			Box box = carousel.getBox(0);
			assertTrue(box != null);

			// Get a reference to the first car in the carousel.
			RollaCar car = (RollaCar)box.getTrackCar();
			assertTrue(car != null);

			// Create a person to add to the car.
			Person person = new Person(car.getPosition(), null);

			// Give the person an office destination.
			person.pushDestination(aLevels[10]);
			person.pushDestination(lift);

			// Assert that we can add this person to the car.
			assertTrue(car.offer(person));
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Add two persons to a carousel car.
	@Test
	public void testLiftReturnAddTwoPersons()
	{
		try
		{
			// Create a Simulation, but don't start it. We will manually update it.
			Simulation sim = new Simulation(0);

			// Create the array of levels and add our office level.
			StructureLevel[] aLevels = new StructureLevel[s_nLevels];
			for (int iLevel = 10; iLevel < 20; ++iLevel)
			{
				StructureLevel level = new LevelOffice(new Position(0, 0, iLevel * 4000, 0), null);
				aLevels[iLevel] = level;
			}
			Rollavator lift = new Rollavator();

			// Get the BoxCarousel at position zero.
			Carousel carousel = lift.getLobbyCarousel();
			Box box = carousel.getBox(0);
			assertTrue(box != null);

			// Get a reference to the first car in the carousel.
			RollaCar car = (RollaCar)box.getTrackCar();
			assertTrue(car != null);

			// Create a person to add to the car.
			Person person = new Person(car.getPosition(), null);

			// Give the person an office destination.
			person.pushDestination(aLevels[10]);
			person.pushDestination(lift);

			// Assert that we can add this person to the car.
			assertTrue(car.offer(person));

			// Run through updates to circle the carousel around and transition
			// cars over to the shaft.
			Cycles oneTick = new Cycles(1);

			// Should take two seconds for the doors to fully open
			int iTick = 0;
			int nTicksDoor = Door.GetDefaultCycles().getInt();
			while (iTick++ < nTicksDoor)
			{
				assertTrue(car.isOpening());
				sim.updateTime(oneTick);
				lift.update(sim);
			}
			assertTrue(car.isOpen());

			// Create another person to add to the car.
			person = new Person(car.getPosition(), null);

			// Give the second person the same office destination.
			person.pushDestination(aLevels[10]);
			person.pushDestination(lift);

			// Assert that we can add this person to the car.
			assertTrue(car.offer(person));
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Test of Carousel to BoxRing.
	@Test
	public void testLiftReturnToBoxRing()
	{
		try
		{
			// Create a Simulation, but don't start it. We will manually update it.
			Simulation sim = new Simulation(0);

			// Create the array of levels and add our office level.
			StructureLevel[] aLevels = new StructureLevel[s_nLevels];
			for (int iLevel = 10; iLevel < 20; ++iLevel)
			{
				StructureLevel level = new LevelOffice(new Position(0, 0, iLevel * 4000, 0), null);
				aLevels[iLevel] = level;
			}
			Rollavator lift = new Rollavator();

			// Get the BoxCarousel at position zero.
			Carousel carousel = lift.getLobbyCarousel();
			Box box = carousel.getBox(0);
			assertTrue(box != null);

			// Get a reference to the first car in the carousel.
			RollaCar car = (RollaCar)box.getTrackCar();
			assertTrue(car != null);

			// Create a person to add to the car.
			Person person = new Person(car.getPosition(), null);

			// Give the person an office destination.
			person.pushDestination(aLevels[10]);
			person.pushDestination(lift);

			// Assert that we can add this person to the car.
			assertTrue(car.offer(person));

			// Run through updates to circle the carousel around and transition
			// cars over to the shaft.
			Cycles oneTick = new Cycles(1);

			// Should take two seconds for the doors to fully open
			int iTick = 0;
			int nTicksDoor = Door.GetDefaultCycles().getInt();
			while (iTick++ < nTicksDoor)
			{
				assertTrue(car.isOpening());
				sim.updateTime(oneTick);
				lift.update(sim);
			}
			assertTrue(car.isOpen());

			// Create another person to add to the car.
			person = new Person(car.getPosition(), null);

			// Give the second person the same office destination.
			person.pushDestination(aLevels[10]);
			person.pushDestination(lift);

			// Assert that we can add this person to the car.
			assertTrue(car.offer(person));

			// Have the car rotate around the carousel until it reaches the launch location.
			while (iTick++ < Carousel.s_nTrackPositions)
			{
				assertTrue(car.isOpen());
				sim.updateTime(oneTick);
				lift.update(sim);
			}

			// Should take two seconds for the doors to close.
			iTick = 0;
			while (iTick++ < nTicksDoor)
			{
				assertTrue(car.isClosing());
				sim.updateTime(oneTick);
				lift.update(sim);
			}
			assertTrue(car.isClosed());

			// Continue until we reach the destination where the TrackRing can receive the car.
			TrackRing trackRing = carousel.m_trackRing;
			while (!box.getPosition().equals(
				trackRing.m_aPosDests[TrackRing.PosEnum.FROM_CAROUSEL.m_value]))
			{
				assertTrue(box.getSpouse() == null);
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;
			}

			// We should now be married up with a BoxRing.
			Box boxSpouse = box.getSpouse();
			assertTrue(boxSpouse != null);

			// Continue until the car is slid over to the BoxRing.
			int nTicksSlide = BoxRing.GetSlideCycles().getInt();
			for (int i = 0; i < nTicksSlide; ++i)
			{
				assertTrue(box.getSpouse() == boxSpouse);
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;
			}

			// The BoxRing should own the Car now.
			assertTrue(boxSpouse.getTrackCar() == car);
			assertTrue(box.getTrackCar() == null);
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Test of Carousel to BoxWay
	@Test
	public void testLiftReturnToBoxWay()
	{
		try
		{
			// Create a Simulation, but don't start it. We will manually update it.
			Simulation sim = new Simulation(0);

			// Create the array of levels and add our office level.
			StructureLevel[] aLevels = new StructureLevel[s_nLevels];
			for (int iLevel = 10; iLevel < 20; ++iLevel)
			{
				StructureLevel level = new LevelOffice(new Position(0, 0, iLevel * 4000, 0), null);
				aLevels[iLevel] = level;
			}
			Rollavator lift = new Rollavator();

			// Get the BoxCarousel at position zero.
			Carousel carousel = lift.getLobbyCarousel();
			Box box = carousel.getBox(0);
			assertTrue(box != null);

			// Get a reference to the first car in the carousel.
			RollaCar car = (RollaCar)box.getTrackCar();
			assertTrue(car != null);

			// Create a person to add to the car.
			Person person = new Person(car.getPosition(), null);

			// Give the person an office destination.
			person.pushDestination(aLevels[10]);
			person.pushDestination(lift);

			// Assert that we can add this person to the car.
			assertTrue(car.offer(person));

			// Run through updates to circle the carousel around and transition
			// cars over to the shaft.
			Cycles oneTick = new Cycles(1);

			// Should take two seconds for the doors to fully open
			int iTick = 0;
			int nTicksDoor = Door.GetDefaultCycles().getInt();
			while (iTick++ < nTicksDoor)
			{
				assertTrue(car.isOpening());
				sim.updateTime(oneTick);
				lift.update(sim);
			}
			assertTrue(car.isOpen());

			// Create another person to add to the car.
			person = new Person(car.getPosition(), null);

			// Give the second person the same office destination.
			person.pushDestination(aLevels[10]);
			person.pushDestination(lift);

			// Assert that we can add this person to the car.
			assertTrue(car.offer(person));

			// Have the car rotate around the carousel until it reaches the launch location.
			while (iTick++ < Carousel.s_nTrackPositions)
			{
				assertTrue(car.isOpen());
				sim.updateTime(oneTick);
				lift.update(sim);
			}

			// Should take two seconds for the doors to close.
			iTick = 0;
			while (iTick++ < nTicksDoor)
			{
				assertTrue(car.isClosing());
				sim.updateTime(oneTick);
				lift.update(sim);
			}
			assertTrue(car.isClosed());

			// Continue until we reach the destination where the TrackRing can receive the car.
			TrackRing trackRing = carousel.m_trackRing;
			while (!box.getPosition().equals(
				trackRing.m_aPosDests[TrackRing.PosEnum.FROM_CAROUSEL.m_value]))
			{
				assertTrue(box.getSpouse() == null);
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;
			}

			// We should now be married up with a BoxRing.
			Box boxSpouse = box.getSpouse();
			assertTrue(boxSpouse != null);

			// Continue until the car is slid over to the BoxRing.
			int nTicksSlide = BoxRing.GetSlideCycles().getInt();
			for (int i = 0; i < nTicksSlide; ++i)
			{
				assertTrue(box.getSpouse() == boxSpouse);
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;
			}

			// The BoxRing should own the Car now.
			assertTrue(boxSpouse.getTrackCar() == car);
			assertTrue(box.getTrackCar() == null);
			box = boxSpouse;

			// Continue until we reach the destination where the TrackRing can unload
			// the TrackCar to the BoxWay which will bring it to the Shaft.
			while (!box.getPosition().equals(
				trackRing.m_aPosDests[TrackRing.PosEnum.TO_SHAFT.m_value]))
			{
				assertTrue(box.getSpouse() == null);
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;
			}

			// We should now be married up with a BoxWay which leads to the Shaft.
			boxSpouse = box.getSpouse();
			assertTrue(boxSpouse != null);

			// Continue until the car is slid over to the BoxWay.
			for (int i = 0; i < nTicksSlide; ++i)
			{
				assertTrue(box.getSpouse() == boxSpouse);
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;
			}

			// The BoxWay should now own the Car.
			assertTrue(boxSpouse.getTrackCar() == car);
			assertTrue(box.getTrackCar() == null);
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Full test.
	@Test
	public void testLiftReturnToShaft()
	{
		try
		{
			// Create a Simulation, but don't start it. We will manually update it.
			Simulation sim = new Simulation(0);

			// Create the array of levels and add our office level.
			StructureLevel[] aLevels = new StructureLevel[s_nLevels];
			for (int iLevel = 10; iLevel < 20; ++iLevel)
			{
				StructureLevel level = new LevelOffice(new Position(0, 0, iLevel * 4000, 0), null);
				aLevels[iLevel] = level;
			}
			Rollavator lift = new Rollavator();

			// Get the BoxCarousel at position zero.
			Carousel carousel = lift.getLobbyCarousel();
			Box box = carousel.getBox(0);
			assertTrue(box != null);

			Trace.critical("Box at (" + box.getPosition() + ")");

			// Get a reference to the first car in the carousel.
			RollaCar car = (RollaCar)box.getTrackCar();
			assertTrue(car != null);

			// Create a person to add to the car.
			Person person = new Person(car.getPosition(), null);

			// Give the person an office destination.
			person.pushDestination(aLevels[10]);
			person.pushDestination(lift);

			// Assert that we can add this person to the car.
			assertTrue(car.offer(person));

			// Run through updates to circle the carousel around and transition
			// cars over to the shaft.
			Cycles oneTick = new Cycles(1);

			// Should take two seconds for the doors to fully open
			int iTick = 0;
			int nTicksDoor = Door.GetDefaultCycles().getInt();
			while (iTick++ < nTicksDoor)
			{
				assertTrue(car.isOpening());
				sim.updateTime(oneTick);
				lift.update(sim);

				Trace.critical("Box at (" + box.getPosition() + ")");
			}
			assertTrue(car.isOpen());

			// Create another person to add to the car.
			person = new Person(car.getPosition(), null);

			// Give the second person the same office destination.
			person.pushDestination(aLevels[10]);
			person.pushDestination(lift);

			// Assert that we can add this person to the car.
			assertTrue(car.offer(person));

			// Have the car rotate around the carousel until it reaches the launch location.
			while (iTick++ < Carousel.s_nTrackPositions)
			{
				assertTrue(car.isOpen());
				sim.updateTime(oneTick);
				lift.update(sim);

				Trace.critical("Box at (" + box.getPosition() + ")");
			}

			// Should take two seconds for the doors to close.
			iTick = 0;
			while (iTick++ < nTicksDoor)
			{
				assertTrue(car.isClosing());
				sim.updateTime(oneTick);
				lift.update(sim);

				Trace.critical("Box at (" + box.getPosition() + ")");
			}
			assertTrue(car.isClosed());

			// Continue until we reach the destination where the TrackRing can receive the car.
			TrackRing trackRing = carousel.m_trackRing;
			while (!box.getPosition().equals(
				trackRing.m_aPosDests[TrackRing.PosEnum.FROM_CAROUSEL.m_value]))
			{
				assertTrue(box.getSpouse() == null);
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;

				Trace.critical("Box at (" + box.getPosition() + ")");
			}

			// We should now be married up with a BoxRing.
			Box boxSpouse = box.getSpouse();
			assertTrue(boxSpouse != null);

			// Continue until the car is slid over to the BoxRing.
			int nTicksSlide = BoxRing.GetSlideCycles().getInt();
			for (int i = 0; i < nTicksSlide; ++i)
			{
				assertTrue(box.getSpouse() == boxSpouse);
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;

				Trace.critical("Box at (" + box.getPosition() + ")");
			}

			// The BoxRing should own the Car now.
			assertTrue(boxSpouse.getTrackCar() == car);
			assertTrue(box.getTrackCar() == null);
			box = boxSpouse;

			Trace.critical("Box at (" + box.getPosition() + ")");

			// Continue until we reach the destination where the TrackRing can unload
			// the TrackCar to the BoxWay which will bring it to the Shaft.
			while (!box.getPosition().equals(
				trackRing.m_aPosDests[TrackRing.PosEnum.TO_SHAFT.m_value]))
			{
				assertTrue(box.getSpouse() == null);
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;

				Trace.critical("Box at (" + box.getPosition() + ")");
			}

			// We should now be married up with a BoxWay which leads to the Shaft.
			boxSpouse = box.getSpouse();
			assertTrue(boxSpouse != null);

			// Continue until the car is slid over to the BoxWay.
			for (int i = 0; i < nTicksSlide; ++i)
			{
				assertTrue(box.getSpouse() == boxSpouse);
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;

				Trace.critical("Box at (" + box.getPosition() + ")");
			}

			// The BoxWay should now own the Car.
			assertTrue(boxSpouse.getTrackCar() == car);
			assertTrue(box.getTrackCar() == null);
			box = boxSpouse;

			Trace.critical("Box at (" + box.getPosition() + ")");

			// Continue until we marry up with a BoxLoop from the Shaft.
			while ((boxSpouse = box.getSpouse()) == null)
			{
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;

				Trace.critical("Box at (" + box.getPosition() + ")");
			}

			// Continue until the car is slid over into the BoxLoop.
			for (int i = 0; i < nTicksSlide; ++i)
			{
				assertTrue(box.getSpouse() == boxSpouse);
				sim.updateTime(oneTick);
				lift.update(sim);
				++iTick;

				Trace.critical("Box at (" + box.getPosition() + ")");
			}
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}
}

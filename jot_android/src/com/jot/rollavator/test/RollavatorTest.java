/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Unit Test for a Rollavator.
 */

package com.jot.rollavator.test;

import org.junit.Test;

import com.jot.item.Person;
import com.jot.item.StructureLevel;
import com.jot.jottower.LevelOffice;
import com.jot.lift.Door;
import com.jot.lift.Lift;
import com.jot.rollavator.Box;
import com.jot.rollavator.BoxLoop;
import com.jot.rollavator.BoxPort;
import com.jot.rollavator.BoxReturn;
import com.jot.rollavator.BoxRing;
import com.jot.rollavator.Carousel;
import com.jot.rollavator.LiftLoop;
import com.jot.rollavator.RollaCar;
import com.jot.rollavator.Rollavator;
import com.jot.rollavator.Shaft;
import com.jot.rollavator.TrackRing;
import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

import static org.junit.Assert.*;

public class RollavatorTest
{
	private static final int s_nLevels = 200;
	private static final int s_nLevelsPerStack = 10;
	private static final Size s_size = new Size(106000, 122400, 4000 * s_nLevels);
	private static final Position s_aPosRollavators[] =
	{
		new Position(6560, 7380, 0, new Orientation(150)),
		new Position(12980, 22500, 0, new Orientation(120)),
		new Position(0, 18180, 752000, new Orientation(90)),
	};

	private int m_iTick;
	private int m_nTicksDoor;
	private Box m_box;
	private Box m_boxSpouse;
	private Carousel m_carousel;
	private Cycles m_oneTick;
	private Person m_person;
	private RollaCar m_car;
	private Rollavator m_lift;
	private Rollavator[] m_aLifts;
	private Simulation m_sim;
	private StructureLevel[] m_aLevels;
	private TrackRing m_trackRing;

	private void createLiftsAndLevels() throws Exception
	{
		// Create a Simulation, but don't start it. We will manually update it.
		m_sim = new Simulation(0);

		// Run through updates to circle the carousel around and transition
		// cars over to the shaft.
		m_oneTick = new Cycles(1);

		// Create the array of levels and add our office level.
		m_iTick = 0;
		m_aLifts = new Rollavator[1];
		m_aLevels = new StructureLevel[s_nLevels];
		m_lift = m_aLifts[0]
			= new Rollavator(s_aPosRollavators, m_aLevels, s_nLevelsPerStack, false);

		for (int iLevel = 10; iLevel < 20; ++iLevel)
		{
			StructureLevel level = new LevelOffice(new Position(0, 0, iLevel * 4000, 0), m_aLifts);
			m_aLevels[iLevel] = level;
		}

		// After all the structure levels have been created, we need to assign connections from
		// the lifts to these levels.
		for (Rollavator lift : m_aLifts)
		{
			lift.setLevelConnections();
		}
		for (StructureLevel level : m_aLevels)
		{
			if (level != null)
			{
				level.setLiftConnections();
			}
		}
	}

	private void update() throws Exception
	{
		m_sim.updateTime(m_oneTick);
		m_lift.update(m_sim);
		for (int iLevel = 10; iLevel < 20; ++iLevel)
		{
			m_aLevels[iLevel].update(m_sim);
		}

		++m_iTick;

		if (m_car != null)
		{
			Trace.critical(
				"Box at (" + m_box.getPosition() + "); Car at (" + m_car.getPosition() + ")");
		}
		else if (m_box != null)
		{
			Trace.critical(
				"Box at (" + m_box.getPosition() + ")");
		}
	}

	// Good construction test.
	@Test
	public void testRollavatorConstruct()
	{
		try
		{
			createLiftsAndLevels();
		}
		catch (Exception e)
		{
			fail("Unable to construct Rollavator objects" + e.getMessage());
		}
	}

	// Add a person to a carousel car.
	@Test
	public void testRollavatorAddPerson()
	{
		try
		{
			// Create the rollavator and connecting level objects.
			createLiftsAndLevels();

			// Get the BoxCarousel at position zero.
			m_carousel = m_lift.getLobbyCarousel();
			m_box = m_carousel.getBox(0);
			assertTrue(m_box != null);

			Trace.critical("Box at (" + m_box.getPosition() + ")");

			// Get a reference to the first car in the carousel.
			m_car = (RollaCar)m_box.getTrackCar();
			assertTrue(m_car != null);

			// Create a person to add to the car.
			m_person = new Person(m_car.getPosition(), null);

			// Give the person an office destination.
			m_person.pushDestination(m_aLevels[11]);
			m_person.pushDestination(m_lift);

			// Assert that we can add this person to the car.
			assertTrue(m_car.offer(m_person));
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Add two persons to a carousel car.
	@Test
	public void testRollavatorAddTwoPersons()
	{
		// Run the dependent unit test to this next phase.
		testRollavatorAddPerson();

		try
		{
			// Should take two seconds for the doors to fully open
			while (m_car.isOpening())
			{
				update();
			}
			assertTrue(m_car.isOpen());

			// Create another person to add to the car.
			m_person = new Person(m_car.getPosition(), null);

			// Give the second person the same office destination.
			m_person.pushDestination(m_aLevels[13]);
			m_person.pushDestination(m_lift);

			// Assert that we can add this person to the car.
			assertTrue(m_car.offer(m_person));
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Test of Carousel to BoxRing.
	@Test
	public void testRollavatorToBoxRing()
	{
		// Run the dependent unit test to this next phase.
		testRollavatorAddTwoPersons();

		try
		{
			// Have the car rotate around the carousel until it reaches the launch location.
			while (m_iTick < Carousel.s_nTrackPositions - 1)
			{
				assertTrue(m_car.isOpen());
				update();
			}

			// Should take two seconds for the doors to close.
			while (m_car.isClosing())
			{
				update();
			}
			assertTrue(m_car.isClosed());

			// Continue until we reach the destination where the TrackRing can receive the car.
			m_trackRing = m_carousel.m_trackRing;
			while (!m_box.getPosition().equals(
				m_trackRing.m_aPosDests[TrackRing.PosEnum.FROM_CAROUSEL.m_value]))
			{
				assertTrue(m_box.getSpouse() == null);
				update();
			}

			// We should now be married up with a BoxRing.
			m_boxSpouse = m_box.getSpouse();
			assertTrue(m_boxSpouse != null);

			// Continue until the car is slid over to the BoxRing.
			while (m_box.getSpouse() == m_boxSpouse)
			{
				update();
			}

			// The BoxRing should own the Car now.
			assertTrue(m_boxSpouse.getTrackCar() == m_car);
			assertTrue(m_box.getTrackCar() == null);
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Test of Carousel to BoxWay
	@Test
	public void testRollavatorToBoxWay()
	{
		// Run the dependent unit test to this next phase.
		testRollavatorToBoxRing();
		m_box = m_boxSpouse;

		try
		{
			// Continue until we reach the destination where the TrackRing can unload
			// the TrackCar to the BoxWay which will bring it to the Shaft.
			while (!m_box.getPosition().equals(
				m_trackRing.m_aPosDests[TrackRing.PosEnum.TO_SHAFT.m_value]))
			{
				assertTrue(m_box.getSpouse() == null);
				update();
			}

			// We should now be married up with a BoxWay which leads to the Shaft.
			m_boxSpouse = m_box.getSpouse();
			assertTrue(m_boxSpouse != null);

			// Continue until the car is slid over to the BoxWay.
			while (m_box.getSpouse() == m_boxSpouse)
			{
				update();
			}

			// The BoxWay should now own the Car.
			assertTrue(m_boxSpouse.getTrackCar() == m_car);
			assertTrue(m_box.getTrackCar() == null);
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Full test.
	@Test
	public void testRollavatorToShaft()
	{
		// Run the dependent unit test to this next phase.
		testRollavatorToBoxRing();
		m_box = m_boxSpouse;

		try
		{
			// Continue until we reach the destination where the TrackRing can unload
			// the TrackCar to the BoxWay which will bring it to the Shaft.
			while (!m_box.getPosition().equals(
				m_trackRing.m_aPosDests[TrackRing.PosEnum.TO_SHAFT.m_value]))
			{
				assertTrue(m_box.getSpouse() == null);
				update();
			}

			// We should now be married up with a BoxWay which leads to the Shaft.
			m_boxSpouse = m_box.getSpouse();
			assertTrue(m_boxSpouse != null);

			// Continue until the car is slid over to the BoxWay.
			while (m_box.getSpouse() == m_boxSpouse)
			{
				update();
			}

			// The BoxWay should now own the Car.
			assertTrue(m_boxSpouse.getTrackCar() == m_car);
			assertTrue(m_box.getTrackCar() == null);
			m_box = m_boxSpouse;

			// Continue until we marry up with a BoxLoop from the Shaft.
			while ((m_boxSpouse = m_box.getSpouse()) == null)
			{
				update();
			}

			assertTrue(m_car == m_box.getTrackCar());

			// Continue until the car is slid over into the BoxLoop.
			while (m_car == m_box.getTrackCar())
			{
				assertTrue(m_box.getSpouse() == m_boxSpouse);
				update();
			}

			assertTrue(m_car == m_boxSpouse.getTrackCar());
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Full test.
	@Test
	public void testRollavatorToFloorGroup()
	{
		// Run the dependent unit test to this next phase.
		testRollavatorToShaft();

		try
		{
			// Loop until the car is fully loaded on the BoxLoop.
			while (m_box.getSpouse() == m_boxSpouse)
			{
				update();
			}
			m_box = m_boxSpouse;

			// Continue until we reach the floor group.
			while (m_box.getSpouse() == null)
			{
				update();
			}
			m_boxSpouse = m_box.getSpouse();
			assertTrue(m_boxSpouse.getClass() == BoxReturn.class);

			// Loop until the car is fully loaded on the BoxReturn.
			while (m_box.getSpouse() == m_boxSpouse)
			{
				update();
			}
			m_box = m_boxSpouse;
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Full test.
	@Test
	public void testRollavatorToFloor()
	{
		// Run the dependent unit test to this next phase.
		testRollavatorToFloorGroup();

		try
		{
			// Our car should now be in a BoxReturn.
			assertTrue(m_box.getClass() == BoxReturn.class);

			// Continue until we reach a BoxPort for our floor.
			while (m_box.getSpouse() == null)
			{
				update();
			}
			m_boxSpouse = m_box.getSpouse();

			// We should now be married to a BoxPort.
			assertTrue(m_boxSpouse.getClass() == BoxPort.class);

			// Loop until the car is fully loaded on the BoxPort.
			while (m_box.getSpouse() == m_boxSpouse)
			{
				update();
			}
			m_box = m_boxSpouse;

			// Loop until the people have left the car.
			while (!m_car.isEmpty())
			{
				update();
			}
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Full test.
	@Test
	public void testRollavatorFloorToBoxPort()
	{
		// Run the dependent unit test to this next phase.
		testRollavatorConstruct();

		try
		{
			// Create a person to add to the car.
			m_person = new Person(m_aLevels[10].getPosition(), null);

			// Give the person destination towards the lobby.
			m_person.pushDestination(m_lift);
			m_person.pushDestination(m_lift);

			// Add it to the office level.
			m_aLevels[10].add(m_person);

			// Loop until the person is off the floor.
			assertTrue(m_person.getPlace().getClass() == LevelOffice.class);

			do
			{
				update();
			} while (m_person.getPlace().getClass() != RollaCar.class);

			m_car = (RollaCar)m_person.getPlace();
			m_box = m_car.getParent();

			assertTrue(m_box.getClass() == BoxPort.class);
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Full test.
	@Test
	public void testRollavatorFloorToBoxReturn()
	{
		// Run the dependent unit test to this next phase.
		testRollavatorFloorToBoxPort();

		try
		{
			while (m_box.getClass() != BoxReturn.class)
			{
				update();
				m_box = m_car.getParent();
			}
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}

	// Full test.
	@Test
	public void testRollavatorFloorToBoxLoop()
	{
		// Run the dependent unit test to this next phase.
		testRollavatorFloorToBoxReturn();

		try
		{
			while (m_box.getClass() != BoxLoop.class)
			{
				update();
				m_box = m_car.getParent();
			}
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}
}

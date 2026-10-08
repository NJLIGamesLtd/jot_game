/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Unit Test for a Shaft.
 */

package com.jot.rollavator.test;

import org.junit.Test;

import com.jot.item.Person;
import com.jot.item.StructureLevel;
import com.jot.jottower.LevelOffice;
import com.jot.lift.Door;
import com.jot.lift.Lift;
import com.jot.rollavator.Box;
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

public class ShaftTest
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
		else
		{
			Trace.critical(
				"Box at (" + m_box.getPosition() + ")");
		}
	}

	// Shaft test
	@Test
	public void testShaft()
	{
		try
		{
			// Create the rollavator and connecting level objects.
			createLiftsAndLevels();

			// Get a box from the Shaft.
			Shaft shaft = m_lift.getShaft();
			LiftLoop loop = shaft.getLiftLoop();
			m_box = loop.getBox(0);
			assertTrue(m_box != null);

			Trace.critical("Box at (" + m_box.getPosition() + ")");

			// Create a Simulation, but don't start it. We will manually update it.
			m_sim = new Simulation(0);

			// Run through updates one clock cycle at a time.
			m_oneTick = new Cycles(1);

			// Without people in the system, the main loop should seek destinations
			// and then get nudged towards the next by a trailing loop car.
			for (int i = 0; i < LiftLoop.PosEnum.NPOS.m_value; ++i)
			{
				while (m_box.canMove(true))
				{
					update();
				}

				// After reaching a destination, it should get nudged towards the next.
				while (!m_box.canMove(true))
				{
					update();
				}
			}
		}
		catch (Exception e)
		{
			fail("Exception thrown: " + e.getMessage());
		}
	}
}

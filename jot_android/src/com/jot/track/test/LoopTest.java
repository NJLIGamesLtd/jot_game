/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Track Loop Unit Test.
 */

package com.jot.track.test;

import com.jot.track.CarWay;
import com.jot.track.Track;
import com.jot.track.TrackCar;
import com.jot.track.Loop;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

import org.junit.Test;

import static org.junit.Assert.*;

public class LoopTest
{
	// Define two tracks of positions.
	final Orientation orientFixed = new Orientation(0);
	final Position posTrack[] =
	{
		new Position(4000, 18000, 0, orientFixed),
		new Position(5000, 18000, 0, orientFixed),
		new Position(6000, 17700, 0, orientFixed),
		new Position(7000, 17400, 0, orientFixed),
		new Position(8000, 17000, 0, orientFixed),
		new Position(9000, 16000, 0, orientFixed),
		new Position(9200, 15000, 0, orientFixed),
		new Position(9200, 14000, 0, orientFixed),
		new Position(9000, 13000, 0, orientFixed),
		new Position(8500, 12000, 0, orientFixed),
		new Position(8000, 11000, 0, orientFixed),
		new Position(7600, 10000, 0, orientFixed),
		new Position(7300,  9000, 0, orientFixed),
		new Position(7300,  8000, 0, orientFixed),
		new Position(7700,  7000, 0, orientFixed),
		new Position(8200,  6000, 0, orientFixed),
		new Position(8700,  5000, 0, orientFixed),
		new Position(8900,  4000, 0, orientFixed),
		new Position(8600,  3000, 0, orientFixed),
		new Position(8200,  2000, 0, orientFixed),
		new Position(7300,  1000, 0, orientFixed),
		new Position(6200,     0, 0, orientFixed),
		new Position(5100,   200, 0, orientFixed),
		new Position(4100,   800, 0, orientFixed),
		new Position(3100,  1800, 0, orientFixed),
		new Position(2100,  3000, 0, orientFixed),
		new Position(1500,  4000, 0, orientFixed),
		new Position(1100,  5000, 0, orientFixed),
		new Position( 800,  6000, 0, orientFixed),
		new Position( 500,  7000, 0, orientFixed),
		new Position( 200,  8000, 0, orientFixed),
		new Position( 100,  9000, 0, orientFixed),
		new Position(   0, 10000, 0, orientFixed),
		new Position( 100, 11000, 0, orientFixed),
		new Position( 300, 12000, 0, orientFixed),
		new Position( 500, 13000, 0, orientFixed),
		new Position( 800, 14000, 0, orientFixed),
		new Position(1200, 15000, 0, orientFixed),
		new Position(1700, 16000, 0, orientFixed),
		new Position(2500, 17000, 0, orientFixed),
		new Position(3200, 17500, 0, orientFixed),
	};

	@Test
	public void testLoopOneCar()
	{
		// Create a Simulation, but don't start it. We will manually update it.
		Simulation sim = new Simulation(0);
		Position position = new Position();

		// Create our track based on these positions.
		Track track;
		try
		{
			track = new Loop(0, new Size(), position, posTrack);
		}
		catch (Exception e)
		{
			fail("Failed to create Track: " + e.getMessage());
			return;
		}

		// Create a car to be directed around the track.
		TrackCar car = null;
		try
		{
			car = new CarWay(0, new Size(700, 400, 200), position, 3);
		}
		catch (Exception e)
		{
			fail("Failed to create Car: " + e.getMessage());
			return;
		}
		car.setPosition(posTrack[0]);

		// Place our car on the start of the first track.
		try
		{
			assertEquals(track.offer(car), true);
		}
		catch (Exception e)
		{
			fail("Failed to offer Car: " + e.getMessage());
			return;
		}

		// For our track, run an update to move the car from start to finish.
		int iPos = 0;
		boolean bDone = false;
		while (!bDone)
		{
			if ((iPos += car.getTickVelocity()) >= posTrack.length)
			{
				iPos -= posTrack.length;
				bDone = true;
			}

			try
			{
				sim.updateBegin();
				track.update(sim);
				sim.updateEnd();
			}
			catch (Exception e)
			{
				fail("Failed to update Track: " + e.getMessage());
				return;				
			}

			Trace.important("TrackLoop Testing [" + iPos + "]");
			assertEquals(posTrack[iPos].m_x, car.getPosition().m_x);
			assertEquals(posTrack[iPos].m_y, car.getPosition().m_y);
			assertEquals(posTrack[iPos].m_z, car.getPosition().m_z);
		}
	}
}

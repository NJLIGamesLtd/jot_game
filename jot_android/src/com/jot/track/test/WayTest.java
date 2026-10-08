/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Track Way Unit Test.
 */

package com.jot.track.test;

import com.jot.track.CarWay;
import com.jot.track.TrackCar;
import com.jot.track.Way;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

import org.junit.Test;

import static org.junit.Assert.*;

public class WayTest
{
	// Define two tracks of positions.
	final Orientation orientFixed = new Orientation(0);
	final Position posTracks[][] =
	{
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
		},
		{
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
		}
	};

	@Test
	public void testWayOneCar()
	{
		// Create a Simulation, but don't start it. We will manually update it.
		Simulation sim = new Simulation(0);
		Position position = new Position();

		// Create our two tracks based on these positions.
		Way track1, track2;
		try
		{
			track1 = new Way(0, new Size(), position, posTracks[0]);
			track2 = new Way(0, new Size(), position, posTracks[1]);
		}
		catch (Exception e)
		{
			fail("Failed to create Track: " + e.getMessage());
			return;
		}

		// Join these two tracks into a circular loop.
		track1.setDestination(track2);

		// Create a car to be directed around the track.
		TrackCar car = null;
		try
		{
			car = new CarWay(0, new Size(700, 400, 2000), position, 3);
			car.setPosition(posTracks[0][0]);
		
			// Place our car on the start of the first track.
			assertEquals(track1.offer(car), true);
		}
		catch (Exception e)
		{
			fail("Failed to create Car: " + e.getMessage());
			return;
		}

		// For each track, run an update to move the car from start to finish.
		for (int iTrack = 0; iTrack < posTracks.length; ++iTrack)
		{
			int iPos = 0;
			int nVelocity = 0;
			boolean bDone = false;
			int nSections = posTracks[iTrack].length;
			while (!bDone)
			{
				Trace.important("Track Testing [" + iTrack + "][" + iPos + "]");
				assertEquals(car.getPosition().m_x, posTracks[iTrack][iPos].m_x);
				assertEquals(car.getPosition().m_y, posTracks[iTrack][iPos].m_y);
				assertEquals(car.getPosition().m_z, posTracks[iTrack][iPos].m_z);

				if ((iPos += nVelocity) >= nSections)
				{
					iPos -= nSections;
					bDone = true;
				}

				try
				{
					sim.updateBegin();
					track1.update(sim);
					track2.update(sim);
					sim.updateEnd();
				}
				catch (Exception e)
				{
					fail("Failed to update Track: " + e.getMessage());
					return;				
				}

				if ((nVelocity = car.getTickVelocity()) == 0)
				{
					break;
				}
			}
		}

		// The car should end up back at the start of the first track.
		assertEquals(car.getPosition().m_x, posTracks[1][19].m_x);
		assertEquals(car.getPosition().m_y, posTracks[1][19].m_y);
		assertEquals(car.getPosition().m_z, posTracks[1][19].m_z);
	}
}

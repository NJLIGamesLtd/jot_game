/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Track Return Unit Test.
 */

package com.jot.track.test;

import com.jot.track.CarReturn;
import com.jot.track.Return;
import com.jot.track.Track;
import com.jot.track.TrackCar;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReturnTest
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
	public void testReturnForward()
	{
		// Create a Simulation, but don't start it. We will manually update it.
		Simulation sim = new Simulation(0);
		Position position = new Position();

		// Create our two tracks based on these positions.
		Return track1, track2;
		try
		{
			track1 = new Return(0, new Size(), position, posTracks[0]);
			track2 = new Return(0, new Size(), position, posTracks[1]);
		}
		catch (Exception e)
		{
			fail("Failed to create Track: " + e.getMessage());
			return;
		}

		// Have the first track feed to the second track.
		track1.setDestinationEnd(track2);
		track2.setDestinationStart(track1);

		// Create a car to be directed around the track.
		TrackCar car = null;
		try
		{
			car = new CarReturn(0, new Size(700, 400, 2000), position, 3);
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
		int iPos = 0;
		int iTrack;
		for (iTrack = 0; iTrack < posTracks.length; ++iTrack)
		{
			int nVelocity = 0;
			boolean bDone = false;
			int nSections = posTracks[iTrack].length;

			iPos = 0;
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
		--iTrack;

		// The car should end up back at the start of the first track.
		assertEquals(car.getPosition().m_x, posTracks[iTrack][iPos].m_x);
		assertEquals(car.getPosition().m_y, posTracks[iTrack][iPos].m_y);
		assertEquals(car.getPosition().m_z, posTracks[iTrack][iPos].m_z);
	}

	@Test
	public void testReturnBackward()
	{
		// Create a Simulation, but don't start it. We will manually update it.
		Simulation sim = new Simulation(0);
		Position position = new Position();

		// Create our two tracks based on these positions.
		Return track1, track2;
		try
		{
			track1 = new Return(0, new Size(), position, posTracks[0]);
			track2 = new Return(0, new Size(), position, posTracks[1]);
		}
		catch (Exception e)
		{
			fail("Failed to create Track: " + e.getMessage());
			return;
		}

		// Have the first track feed to the second track.
		track1.setDestinationEnd(track2);
		track2.setDestinationStart(track1);

		// Create a car to be directed around the track.
		TrackCar car = null;
		int iTrack;
		int iPos;
		try
		{
			car = new CarReturn(0, new Size(700, 400, 2000), position, 3);
			iTrack = posTracks.length - 1;
			iPos = posTracks[iTrack].length - 1;
			car.setPosition(posTracks[iTrack][iPos]);
		
			// Place our car on the start of the first track.
			assertEquals(track2.offer(car), true);
		}
		catch (Exception e)
		{
			fail("Failed to create Car: " + e.getMessage());
			return;
		}

		// For each track, run an update to move the car from start to finish.
		do
		{
			int nVelocity = 0;

			iPos = posTracks[iTrack].length - 1;
			while (true)
			{
				Trace.important("Track Testing [" + iTrack + "][" + iPos + "]");
				assertEquals(car.getPosition().m_x, posTracks[iTrack][iPos].m_x);
				assertEquals(car.getPosition().m_y, posTracks[iTrack][iPos].m_y);
				assertEquals(car.getPosition().m_z, posTracks[iTrack][iPos].m_z);

				iPos += nVelocity;

				try
				{
					sim.updateBegin();
					track2.update(sim);
					track1.update(sim);
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
		} while (iTrack-- > 0);
		++iTrack;

		// The car should end up back at the start of the first track.
		assertEquals(car.getPosition().m_x, posTracks[iTrack][iPos].m_x);
		assertEquals(car.getPosition().m_y, posTracks[iTrack][iPos].m_y);
		assertEquals(car.getPosition().m_z, posTracks[iTrack][iPos].m_z);
	}
}

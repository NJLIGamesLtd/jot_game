/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator BoxWay is a lateral Track that joins a Carousel to the Shaft.
 */

package com.jot.rollavator;

import com.jot.item.Place;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.Section;
import com.jot.track.Track;
import com.jot.track.TrackCar;
import com.jot.track.Way;
import com.jot.vector.Position;
import com.jot.vector.Size;

public class BoxWay extends Way implements Box
{
	protected static final int s_mass = 1000;
	protected static final Size s_size = new Size(2000, 1800, 3000);
	
	private static final Position[] s_aPosTrackToShaft =
	{
		new Position( 9500, -2500, 3000,  0),
		new Position( 9550, -2525, 3000,  0),
		new Position( 9600, -2550, 3000,  0),
		new Position( 9650, -2500, 3000,  0),
		new Position( 9700, -2575, 3000,  0),
		new Position( 9750, -2600, 3000,  0),
		new Position( 9800, -2620, 3000,  0),
		new Position( 9850, -2640, 3000,  0),
		new Position( 9900, -2660, 3000,  0),
		new Position( 9950, -2680, 3000,  0),
		new Position(10000, -2700, 3000,  1),
		new Position(10050, -2710, 3000,  1),
		new Position(10100, -2720, 3000,  1),
		new Position(10150, -2730, 3000,  1),
		new Position(10200, -2740, 3000,  1),
		new Position(10250, -2750, 3000,  1),
		new Position(10300, -2760, 3000,  1),
		new Position(10350, -2770, 3000,  1),
		new Position(10400, -2780, 3000,  1),
		new Position(10450, -2790, 3000,  1),
		new Position(10500, -2800, 3000,  2),
		new Position(10550, -2800, 3000,  2),
		new Position(10600, -2800, 3000,  2),
		new Position(10650, -2800, 3000,  2),
		new Position(10700, -2800, 3000,  2),
		new Position(10750, -2800, 3000,  2),
		new Position(10800, -2800, 3000,  2),
		new Position(10850, -2800, 3000,  2),
		new Position(10900, -2800, 3000,  2),
		new Position(10950, -2800, 3000,  2),
		new Position(11000, -2800, 3000,  4),
		new Position(11050, -2800, 3000,  4),
		new Position(11100, -2800, 3000,  4),
		new Position(11150, -2800, 3000,  4),
		new Position(11200, -2800, 3000,  4),
		new Position(11250, -2800, 3000,  4),
		new Position(11300, -2800, 3000,  4),
		new Position(11350, -2800, 3000,  4),
		new Position(11400, -2800, 3000,  4),
		new Position(11450, -2800, 3000,  4),
		new Position(11500, -2790, 3000,  5),
		new Position(11550, -2780, 3000,  5),
		new Position(11600, -2770, 3000,  5),
		new Position(11650, -2760, 3000,  5),
		new Position(11700, -2750, 3000,  5),
		new Position(11750, -2740, 3000,  5),
		new Position(11800, -2730, 3000,  5),
		new Position(11850, -2720, 3000,  5),
		new Position(11900, -2710, 3000,  5),
		new Position(11950, -2700, 3000,  5),
		new Position(12000, -2690, 3000,  7),
		new Position(12050, -2680, 3000,  7),
		new Position(12100, -2670, 3000,  7),
		new Position(12150, -2660, 3000,  7),
		new Position(12200, -2640, 3000,  7),
		new Position(12250, -2620, 3000,  7),
		new Position(12300, -2600, 3000,  7),
		new Position(12350, -2575, 3000,  7),
		new Position(12400, -2550, 3000,  7),
		new Position(12450, -2525, 3000,  7),
		new Position(12500, -2500, 3000,  8),
		new Position(12550, -2490, 3000,  8),
		new Position(12600, -2480, 3000,  8),
		new Position(12650, -2470, 3000,  8),
		new Position(12700, -2460, 3000,  9),
	};

	private static final Position[] s_aPosTrackToCarousel =
	{
		new Position( 8200, -4700, 3000,  0),
		new Position( 8250, -4730, 3000,  0),
		new Position( 8300, -4760, 3000,  0),
		new Position( 8350, -4800, 3000,  0),
		new Position( 8400, -4840, 3000,  0),
		new Position( 8450, -4870, 3000,  0),
		new Position( 8500, -4900, 3000,  1),
		new Position( 8550, -4920, 3000,  1),
		new Position( 8600, -4940, 3000,  1),
		new Position( 8650, -4960, 3000,  1),
		new Position( 8700, -4980, 3000,  1),
		new Position( 8750, -5000, 3000,  1),
		new Position( 8800, -5020, 3000,  1),
		new Position( 8850, -5040, 3000,  1),
		new Position( 8900, -5060, 3000,  1),
		new Position( 8950, -5080, 3000,  1),
		new Position( 9000, -5100, 3000,  2),
		new Position( 9050, -5110, 3000,  2),
		new Position( 9100, -5120, 3000,  2),
		new Position( 9150, -5130, 3000,  2),
		new Position( 9200, -5140, 3000,  2),
		new Position( 9250, -5150, 3000,  2),
		new Position( 9300, -5160, 3000,  2),
		new Position( 9350, -5170, 3000,  2),
		new Position( 9400, -5180, 3000,  2),
		new Position( 9450, -5190, 3000,  2),
		new Position( 9500, -5200, 3000,  2),
		new Position( 9550, -5205, 3000,  2),
		new Position( 9600, -5210, 3000,  2),
		new Position( 9650, -5215, 3000,  2),
		new Position( 9700, -5220, 3000,  2),
		new Position( 9750, -5225, 3000,  2),
		new Position( 9800, -5230, 3000,  2),
		new Position( 9850, -5235, 3000,  2),
		new Position( 9900, -5240, 3000,  2),
		new Position( 9950, -5245, 3000,  2),
		new Position(10000, -5250, 3000,  2),
		new Position(10050, -5255, 3000,  2),
		new Position(10100, -5260, 3000,  2),
		new Position(10150, -5265, 3000,  2),
		new Position(10200, -5270, 3000,  2),
		new Position(10250, -5275, 3000,  2),
		new Position(10300, -5280, 3000,  2),
		new Position(10350, -5285, 3000,  2),
		new Position(10400, -5290, 3000,  2),
		new Position(10450, -5292, 3000,  2),
		new Position(10500, -5294, 3000,  2),
		new Position(10550, -5296, 3000,  2),
		new Position(10600, -5298, 3000,  2),
		new Position(10650, -5299, 3000,  2),
		new Position(10700, -5300, 3000,  4),
		new Position(10750, -5280, 3000,  4),
		new Position(10800, -5240, 3000,  4),
		new Position(10850, -5200, 3000,  4),
		new Position(10900, -5160, 3000,  4),
		new Position(10950, -5130, 3000,  4),
		new Position(11000, -5100, 3000,  5),
		new Position(11050, -5060, 3000,  5),
		new Position(11100, -5030, 3000,  5),
		new Position(11150, -5000, 3000,  5),
		new Position(11200, -4960, 3000,  5),
		new Position(11250, -4930, 3000,  5),
		new Position(11300, -4900, 3000,  5),
		new Position(11350, -4860, 3000,  5),
		new Position(11400, -4830, 3000,  5),
		new Position(11450, -4800, 3000,  5),
		new Position(11500, -4760, 3000,  7),
		new Position(11550, -4730, 3000,  7),
		new Position(11600, -4700, 3000,  7),
		new Position(11650, -4660, 3000,  7),
		new Position(11700, -4630, 3000,  7),
		new Position(11750, -4600, 3000,  7),
		new Position(11800, -4560, 3000,  7),
		new Position(11850, -4530, 3000,  7),
		new Position(11900, -4500, 3000,  7),
		new Position(11950, -4460, 3000,  7),
		new Position(12000, -4430, 3000,  8),
		new Position(12050, -4400, 3000,  8),
		new Position(12100, -4360, 3000,  8),
		new Position(12150, -4330, 3000,  8),
		new Position(12200, -4300, 3000,  8),
		new Position(12250, -4260, 3000,  8),
		new Position(12300, -4230, 3000,  8),
		new Position(12350, -4200, 3000,  8),
		new Position(12400, -4160, 3000,  8),
		new Position(12450, -4130, 3000,  8),
		new Position(12500, -4100, 3000,  9),
		new Position(12550, -4060, 3000,  9),
		new Position(12600, -4030, 3000,  9),
		new Position(12650, -4000, 3000,  9),
		new Position(12700, -3975, 3000,  9),
		new Position(12750, -3950, 3000,  9),
		new Position(12800, -3925, 3000,  9),
		new Position(12850, -3900, 3000, 10),
		new Position(12900, -3875, 3000, 10),
		new Position(12950, -3850, 3000, 10),
		new Position(13000, -3825, 3000, 10),
		new Position(13050, -3800, 3000, 10),
		new Position(13100, -3780, 3000, 10),
		new Position(13150, -3760, 3000, 10),
		new Position(13200, -3740, 3000, 10),
		new Position(13250, -3720, 3000, 10),
		new Position(13300, -3700, 3000, 11),
	};

	protected Position m_posDest;	// Attempting to reach this position.

	private boolean m_bMoveNext;	// True if we're trying to move forward towards shaft.
	private Box m_boxSpouseCarousel;// Married to this Box to transfer a Car.
	private Box m_boxSpouseShaft;	// Married to this Box to transfer a Car.
	private int m_idQuadArrived;	// Section Quadrant ID where a car is arrived to our track.
	private int m_idQuadLoaded;		// Section Quadrant ID where a car is fully loaded.
	private Place m_place;			// Currently owned by this place.

	/***********************************************************************************************
	 * BoxWay constructor based on its position.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param rollavator
	 *	    The parent rollavator.
	 * @param bMoveNext
	 *		True if this BoxWay is moving next towards the Shaft.
	 */
	public BoxWay(
		Position position,
		Rollavator rollavator,
		boolean bMoveNext
	) throws Exception
	{
		super(s_mass, s_size, position, null);

		Trace.startOf("rollavator.TrackArc()");

		m_bMoveNext = bMoveNext;

		// Add the track.
		if (bMoveNext)
		{
			setTrack(s_aPosTrackToShaft, position);
		}
		else
		{
			setTrack(s_aPosTrackToCarousel, position);
		}

		// Divide our track into quadrants so we know when a car is fully loaded.
		m_idQuadArrived = markQuadrant(0);
		m_idQuadLoaded = markQuadrant(35);

		// By default, we are not married to another box.
		m_boxSpouseCarousel = null;
		m_boxSpouseShaft = null;

		// No destination by default.
		m_posDest = null;

		Trace.endOf("rollavator.TrackArc()");
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean canMove(boolean recurse) throws Exception
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void checkDestination() throws Exception
	{
		// Check to make sure we have a car to even reach a destination.
		if (m_lCars.size() > 0)
		{
			TrackCar car = m_lCars.get(0);

			if (m_bMoveNext)
			{
				if (m_boxSpouseShaft != null &&
					m_boxSpouseShaft.getTrackCar() == null)
				{
					// Try to offer ownership of the Car over to the BoxLoop.
					if (m_boxSpouseShaft.offer(car))
					{
						removeTrackCar(car);
					}
				}
			}
			else
			{
				if (m_boxSpouseCarousel != null &&
					m_boxSpouseCarousel.getTrackCar() == null)
				{
					// Try to offer ownership of the Car over to the BoxRing.
					if (m_boxSpouseCarousel.offer(car))
					{
						removeTrackCar(car);
					}
				}
			}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean collides(Position position)
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean collides(TrackCar car)
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getBaseLevel()
	{
		return 3000 + m_position.m_z;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Position getDestination()
	{
		return m_posDest;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean getDirection()
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getFloorGroupId()
	{
		return 0;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public TrackCar getLeader()
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Place getPlace()
	{
		return m_place;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getQuadrantId()
	{
		return ((Section)m_position).getQuadrantId();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Section getSection()
	{
		return (Section)m_position;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Box getSpouse()
	{
		if (m_boxSpouseCarousel != null)
		{
			return m_boxSpouseCarousel;
		}
		if (m_boxSpouseShaft != null)
		{
			return m_boxSpouseShaft;
		}
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getTickVelocity()
	{
		return 0;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Track getTrack()
	{
		return this;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean hasDestination(int level)
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean isLoaded()
	{
		TrackCar car = getTrackCarLast();

		if (car != null && car.getQuadrantId() == m_idQuadLoaded)
		{
			return true;
		}
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean isNext()
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean isPrevious()
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void move() throws Exception
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean moveToNext()
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean moveToPrevious()
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(TrackCar car) throws Exception
	{
		TrackCar carLast = null;
		int nCars;

		// Check to make sure we don't collide with the last car added to the list.
		if ((nCars = m_lCars.size()) > 0)
		{
			carLast = m_lCars.get(nCars - 1);
		}

		// SPK: Make sure we don't fill up the track ways to cause a dead-lock.

		// Mark this last car as leading our new car.
		car.setLeader(carLast);

		// Add the car to our list.
		m_lCars.add(car);

		// Assign us as the parent of the car.
		RollaCar rollacar = (RollaCar)car;
		rollacar.setParent(this);

		Trace.important("BoxWay.offer(): accepted car:" + car + " at " + car.getPosition());
		return true;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Place peekDestination()
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Place popDestination() throws Exception
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void pushDestination(Place destination) throws Exception
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setDestination(Position posDestination)
	{
		m_posDest = posDestination;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setDirection(boolean bMoveNext)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setFloorGroupId(int idFloorGroup)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setLeader(TrackCar car)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setPlace(Place place)
	{
		m_place = place;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setSection(Section section)
	{
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void setSpouse(Box spouse) throws Exception
	{
		if (spouse == null)
		{
			if (m_boxSpouseCarousel != null && m_boxSpouseCarousel.getSpouse() == null)
			{
				if (!m_bMoveNext)
				{
					getSectionFirst().setPrevious(null);
				}
				m_boxSpouseCarousel = null;
			}
			else if (m_boxSpouseShaft != null && m_boxSpouseShaft.getSpouse() == null)
			{
				if (m_bMoveNext)
				{
					getSectionLast().setNext(null);
				}
				m_boxSpouseShaft = null;
			}
		}
		else if (spouse.getClass() == BoxRing.class)
		{
			m_boxSpouseCarousel = spouse;
			if (!m_bMoveNext)
			{
				getSectionFirst().setPrevious(spouse.getSectionLast());
			}
		}
		else
		{
			m_boxSpouseShaft = spouse;
			if (m_bMoveNext)
			{
				getSectionLast().setNext(spouse.getSectionFirst());
			}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("BoxWay(" + this + ").update()");

		// Avoid redundant runs of the same update.
		long nTime = sim.getTime();
		if (nTime != m_nTime)
		{
			m_nTime = nTime;

			// Need to define the car count outside the tick method.
			int nCars = m_lCars.size();

			// Update each of the cars.
			for (int i = 0; i < nCars; ++i)
			{
				TrackCar car = m_lCars.get(i);

				// Run an update internal to the car.
				car.update(sim);
			}

			// For each time cycle, move the cars along the track one section.
			int nCycles = sim.getUpdateCyclesCount();
			do
			{
				// Starting with the first car added to the track, try to move it
				// forward one section.
				for (int i = 0; i < nCars; ++i)
				{
					TrackCar car = m_lCars.get(i);

					// Try to move the car.
					car.move();

					Trace.trivial(
						"BoxWay.updateTick(): Car *(" + car + ") moved to position " + car.getLocation());
				}
			} while (--nCycles > 0);

			// If a car reached a destination, then act on it.
			checkDestination();
		}

		Trace.endOf("BoxWay(" + this + ").update()");
	}
}

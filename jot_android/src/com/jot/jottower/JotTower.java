/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A JotTower is a Structure that utilizes Rollavator lifts to allow for more tower levels.
 */

package com.jot.jottower;

import com.jot.item.*;
import com.jot.lift.Lift;
import com.jot.random.Random;
import com.jot.rollavator.*;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.*;

import java.awt.Image;

/***************************************************************************************************
 * JotTower is a 200 story office tower that is hexagonal in shape and has a proprietary elevator
 * design that allows the core to remain reasonably small compared with the office space.
 */
public class JotTower extends StructureImpl
{
	private static final int s_nSides = 6;
	private static final int s_nLevels = 200;
	private static final int s_nLevelsPerStack = 10;
	private static final int s_nElevators = s_nSides;
	private static final Size s_size = new Size(106000, 122400, 4000 * s_nLevels);
	private static final int s_aLevelEngineer[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 189, 191, 193, 195, 197, 199 };
	private static final int s_aLevelSky[] = { 188, 190, 192, 194, 196, 198 };

	private static final Position s_aPosRollavators[][] =
	{
		{
			new Position(6560, 7380, 0, new Orientation(150)),
			new Position(12980, 22500, 0, new Orientation(120)),
			new Position(0, 18180, 752000, new Orientation(90)),
		},
		{
			new Position(8950, 1590, 0, new Orientation(150)),
			new Position(25980, 0, 0, new Orientation(180)),
			new Position(15770, -9050, 760000, new Orientation(210)),
		},
		{
			new Position(3100, -9370, 0, new Orientation(270)),
			new Position(12980, -22500, 0, new Orientation(240)),
			new Position(15770, -9050, 768000, new Orientation(210)),
		},
		{
			new Position(-3100, -9130, 0, new Orientation(270)),
			new Position(-12980, -22500, 0, new Orientation(300)),
			new Position(-15770, -9050, 776000, new Orientation(330)),
		},
		{
			new Position(-8950, 1590, 0, new Orientation(30)),
			new Position(-25980, 0, 0, new Orientation(0)),
			new Position(-15770, -9050, 784000, new Orientation(330)),
		},
		{
			new Position(-6560, 7380, 0, new Orientation(30)),
			new Position(-12980, 22500, 0, new Orientation(60)),
			new Position(0, 18180, 792000, new Orientation(270))
		},
	};

	private StructureLevel[] m_aLevels;
	private Rollavator[] m_aLifts;
	private int[] m_aLevelOffice;
	//private Queue<Drone> personsEntering;

	/***********************************************************************************************
	 * Constructor requires a position (location and orientation) within the city.
	 */
	public JotTower(Position position) throws Exception
	{
		super(0, s_size, position);

		Trace.startOf("JotTower()");

		//personsEntering = new LinkedList<Drone>();

		// Create the m_aLevels object and fill it in later.  We need to pass a reference of this to the elevators.
		m_aLevels = new StructureLevel[s_nLevels];

		// Create the list of tower lifts.  An elevator is a specific type of lift, as is a rollavator.
		m_aLifts = new Rollavator[s_nElevators];
		for (int i = 0; i < s_nElevators; ++i)
		{
			m_aLifts[i] = new Rollavator(s_aPosRollavators[i], m_aLevels, s_nLevelsPerStack, (i & 1) == 1);
		}
		
		// Fill in the lobby, engineering, and sky levels.
		m_aLevels[0] = new LevelLobby(new Position(0, 0, 0, m_position.getOrientation()), m_aLifts);
		for (int i = 0; i < s_aLevelEngineer.length; ++i)
		{
			Position levelPosition = new Position(0, 0, 4000 * s_aLevelEngineer[i], m_position.getOrientation());
			m_aLevels[s_aLevelEngineer[i]] = new LevelEngineering(levelPosition, m_aLifts);
		}
		for (int i = 0; i < s_aLevelSky.length; ++i)
		{
			Position levelPosition = new Position(0, 0, 4000 * s_aLevelSky[i], m_position.getOrientation());
			m_aLevels[s_aLevelSky[i]] = new LevelSky(levelPosition, m_aLifts);
		}
		
		// The remaining levels are for office space.
		m_aLevelOffice = new int[s_nLevels - s_aLevelEngineer.length - s_aLevelSky.length - 1];
		for (int i = 0, j = 0; i < s_nLevels; ++i)
		{
			if (m_aLevels[i] == null)
			{
				m_aLevelOffice[j++] = i;
				Position levelPosition = new Position(0, 0, 4000 * i, m_position.getOrientation());
				m_aLevels[i] = new LevelOffice(levelPosition, m_aLifts);
			}
		}

		// After all the structure levels have been created, we need to assign connections from the
		// lifts to these levels.
		for (int i = 0; i < m_aLifts.length; ++i)
		{
			m_aLifts[i].setLevelConnections();
		}
		for (int i = 0, j = 0; i < s_nLevels; ++i)
		{
			if (m_aLevels[i] != null)
			{
				m_aLevels[i].setLiftConnections();
			}
		}

		Trace.endOf("JotTower()");
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Image getImage()
	{
		return m_aLevels[0].getImage();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public StructureLevel getStructureLevel(int iLevel)
	{
		if (iLevel < m_aLevels.length)
		{
			return m_aLevels[iLevel];
		}
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getMass()
	{
		int mass = 0;
		
		for (int i = 0; i < s_nLevels; ++i)
		{
			mass += m_aLevels[i].getMass();
		}
		return mass;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(Drone person) throws Exception
	{
		// CURRENTLY ACCEPTING ALL PEOPLE, NO MANNER THEIR LOCATION.
		Trace.important("JotTower: person (" + person + ") arrived");

		// Pop off the tower as a destination and see if the person has already been assigned
		// an office.
		Place place;
		if ((place = person.popDestination()) != null)
		{
			place = person.peekDestination();
		}


		// UNTIL PEOPLE HAVE SET OFFICES, ASSIGN A RANDOM OFFICE LEVEL AS A DESTINATION.
		if (place == null || place.getClass() != LevelOffice.class)
		{
			person.pushDestination(m_aLevels[m_aLevelOffice[Random.GetInt(m_aLevelOffice.length)]]);
		}

		// Push the next destination to be the building lobby.
		person.pushDestination(m_aLevels[0]);

		// Tell the drone it is in the control of the building.
		person.setPlace(this);

		// Include this person on a temporary queue until it can be pulled into the lobby.
		return m_qPersonsExiting.offer(person);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Drone poll()
	{
		try
		{
			return m_aLevels[0].poll();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("JotTower.update(" + sim + ");");

		for (int i = 0; i < s_nLevels; ++i)
		{
			m_aLevels[i].update(sim);
		}
		for (int i = 0; i < s_nElevators; ++i)
		{
			m_aLifts[i].update(sim);
		}
		for (Drone person : m_qPersonsExiting)
		{
			if (m_aLevels[0].offer(person))
			{
				m_qPersonsExiting.remove(person);
			}
		}

		Trace.endOf("JotTower.update(" + sim + ");");
	}
}

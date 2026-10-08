/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Rollavator is a multilane Lift system.
 */

package com.jot.rollavator;

import com.jot.item.*;
import com.jot.jottower.LevelLobby;
import com.jot.jottower.LevelSky;
import com.jot.lift.*;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.TrackCar;
import com.jot.vector.*;

public class Rollavator extends Lift
{
	private static final Size s_size = Shaft.s_size;

	private static int s_nLevelsDef = 10;
	private static final int s_nLevelsPerStackDef = 2;
	private static final int s_nTrackSectionsPerFloor = 80;
	private static StructureLevel[] s_aLevelsDef = new StructureLevel[s_nLevelsDef];

	private static final Position s_aPosRollavatorDef[] =
	{
		new Position(6560, 7380, 0, new Orientation(150)),
		new Position(12980, 22500, 0, new Orientation(120)),
		new Position(0, 18180, 4000 * s_nLevelsDef, new Orientation(90)),
	};

	private Carousel m_carouselLobby;
	private Carousel m_carouselSky;
	private Shaft m_shaft;
	private int m_nLevelsPerStack;

	/***********************************************************************************************
	 * Rollavator initializer based on its position and structure levels it connects with.
	 *
	 * @param aPosRollavator
	 *	    The Position set of our new rollavator.
	 * @param aTowerLevels
	 *	    The structure levels this lift connects with.
	 * @param nLevelsPerStack
	 *		The number of levels for each stack of floors connected by the same elevator.
	 * @param bReverse
	 *	    True if we want the Carousel to spin counter-clockwise.
	 */
	private void initialize(
		Position[] aPosRollavator,
		StructureLevel[] aTowerLevels,
		int nLevelsPerStack,
		boolean bReverse
	) throws Exception
	{
		Trace.startOf("Rollavator.initialize()");

		// Create two, one-directional tracks for connecting the Carousel to/from
		// the Shaft.
		BoxWay boxShaftFrom = new BoxWay(aPosRollavator[1], this, false);
		BoxWay boxShaftTo = new BoxWay(aPosRollavator[1], this, true);

		m_nLevelsPerStack = nLevelsPerStack;

		// Create the shaft that runs the height of the building.
		m_shaft = new Shaft(
			aPosRollavator[0],
			aTowerLevels,
			nLevelsPerStack,
			this);

		// Create carousels at the lobby and sky levels, plus the tracks which join them
		// to the shaft.
		m_carouselLobby = new Carousel(
			aPosRollavator[1],
			this,
			bReverse,
			boxShaftFrom,
			boxShaftTo);

		boxShaftFrom = new BoxWay(aPosRollavator[2], this, false);
		boxShaftTo = new BoxWay(aPosRollavator[2], this, true);
		m_carouselSky = new Carousel(
			aPosRollavator[2],
			this,
			false,
			boxShaftFrom,
			boxShaftTo);

		// Sum up the masses.
		m_mass = m_shaft.getMass() + 2 * Carousel.s_mass;

		Trace.endOf("Rollavator.initialize()");
	}

	/***********************************************************************************************
	 * Default Rollavator constructor needed for unit tests.
	 */
	public Rollavator() throws Exception
	{
		super(
			0,
			s_size,
			s_aPosRollavatorDef[0],
			null,
			s_aLevelsDef);
		
		Trace.startOf("Rollavator()");
		initialize(s_aPosRollavatorDef, s_aLevelsDef, s_nLevelsPerStackDef, false);
		Trace.endOf("Rollavator()");
	}

	/***********************************************************************************************
	 * Rollavator constructor based on its position and structure levels it connects with.
	 *
	 * @param aPosRollavator
	 *	    The Position set of our new rollavator.
	 * @param aTowerLevels
	 *	    The structure levels this lift connects with.
	 * @param nLevelsPerStack
	 *		The number of levels for each stack of floors connected by the same elevator.
	 * @param bReverse
	 *	    True if we want the Carousel to spin counter-clockwise.
	 */
	public Rollavator(
		Position[] aPosRollavator,
		StructureLevel[] aTowerLevels,
		int nLevelsPerStack,
		boolean bReverse
	) throws Exception
	{
		super(
			0,
			s_size,
			aPosRollavator[0],
			null,
			aTowerLevels);
		
		Trace.startOf("Rollavator()");
		initialize(aPosRollavator, aTowerLevels, nLevelsPerStack, bReverse);
		Trace.endOf("Rollavator()");
	}

	/***********************************************************************************************
	 * Return the BoxPort for the given vertical structure level location.
	 *
	 * @param iFloor
	 *		The floor where the port is located.
	 * @param iLane
	 *		The port lane number on this floor.
	 * @return
	 *		The BoxPort at this location.
	 */
	public BoxPort getBoxPort(int iFloor, int iLane)
	{
		return m_shaft.getBoxPort(iFloor, iLane);
	}

	/***********************************************************************************************
	 * Return the floor group ID for the given vertical structure level location.
	 *
	 * @return
	 *		The floor group ID for this location.
	 */
	public int getFloorGroupID(Location location)
	{
		return location.getZ() / (4000 * m_nLevelsPerStack);
	}

	/***********************************************************************************************
	 * Return the Carousel object for the Lobby.
	 *
	 * @return
	 *		The Carousel object for the Lobby.
	 */
	public Carousel getLobbyCarousel()
	{
		return m_carouselLobby;
	}

	/***********************************************************************************************
	 * Return the maximum floor group ID for this rollavator.
	 *
	 * @return
	 *		The maximum floor group ID.
	 */
	public int getMaxFloorGroupID()
	{
		return m_shaft.m_aaLiftReturns.length;
	}

	/***********************************************************************************************
	 * Return the nth door position on the nth structure level.
	 *
	 * @param iLevel
	 *		The nth structure level (starting with 0 for the lobby).
	 * @param iDoor
	 *		The door index from the list on the given level for this lift.
	 * @return
	 *		The door position.
	 */
	public Position getLevelDoorPosition(int iLevel, int iDoor)
	{
		if (iLevel < 0 || iLevel >= m_aLevels.length)
		{
			return null;
		}

		// If we're on a carousel level, get its door position.
		if (m_aLevels[iLevel].getClass() == LevelLobby.class)
		{
			return m_carouselLobby.getDoorPosition(iDoor);
		}
		if (m_aLevels[iLevel].getClass() == LevelSky.class)
		{
			return m_carouselSky.getDoorPosition(iDoor);
		}

		// Otherwise, get this from the shaft.
		return m_shaft.getLevelDoorPosition(iLevel, iDoor);
	}

	/***********************************************************************************************
	 * Return the port, if available, located on the nth tower level.
	 *
	 * @param iLevel
	 *		The nth structure level (starting with 0 for the lobby).
	 * @return
	 *		The port located on the nth tower level.
	 */
	public BeltPort getLevelPort(int iLevel)
	{
		return m_shaft.getLevelPort(iLevel);
	}

	/***********************************************************************************************
	 * Return the number of structure level floors per elevator stack.
	 *
	 * @return
	 *		The number of structure level floors per elevator stack.
	 */
	public int getLevelsPerStack()
	{
		return m_nLevelsPerStack;
	}

	/***********************************************************************************************
	 * Return the number of structure level floors per elevator stack.
	 *
	 * @return
	 *		The number of structure level floors per elevator stack.
	 */
	public int getLevelStackCount()
	{
		return m_shaft.getLevelStackCount();
	}

	/***********************************************************************************************
	 * Return the LiftLoop object.
	 *
	 * @return
	 *		The LiftLoop object.
	 */
	public LiftLoop getLiftLoop()
	{
		return m_shaft.getLiftLoop();
	}

	/***********************************************************************************************
	 * Return a lift Box that we can pair up with depending upon the position enumeration.
	 *
	 * @param ePosDest
	 *		The LiftLook destination position enumeration.
	 * @param position
	 *		The position we are trying to pair up from.
	 * @return
	 *		The Box, if available, waiting at this location to be paired up with.
	 */
	public Box getLoopPair(LiftLoop.PosEnum ePosDest, Position position)
	{
		switch (ePosDest)
		{
			case FROM_LOBBY:
				return m_carouselLobby.m_boxShaftTo;

			case TO_LOBBY:
				return m_carouselLobby.m_boxShaftFrom;

			case FROM_SKY:
				return m_carouselSky.m_boxShaftTo;

			case TO_SKY:
				return m_carouselSky.m_boxShaftFrom;

			case UP_LEVEL_STACK:
			case DOWN_LEVEL_STACK:
				return m_shaft.getLoopPair(ePosDest, position);

			default:
				break;
		}
		return null;
	}

	/***********************************************************************************************
	 */
	public int getNextFloorGroupId(int zPos)
	{
		if (zPos == 0)
		{
			return m_carouselLobby.getNextFloorGroupId();
		}

		return m_carouselSky.getNextFloorGroupId();
	}

	/***********************************************************************************************
	 * Return the Shaft object.
	 *
	 * @return
	 *		The Shaft object.
	 */
	public Shaft getShaft()
	{
		return m_shaft;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public TrackCar getTrackCar()
	{
		return m_carouselLobby.getTrackCar();
	}

	/***********************************************************************************************
	 * Return the number of track sections per floor.
	 *
	 * @return
	 *		The number of track sections per floor.
	 */
	public int getTrackSectionsPerFloor()
	{
		return s_nTrackSectionsPerFloor;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(Drone drone) throws Exception
	{
		// Redirect the drone towards a rollavator carousel or port.
		int idLevel = drone.getFloorLevelID();
		if (idLevel == m_carouselLobby.getFloorLevelID())
		{
			return m_carouselLobby.offer(drone);
		}
		if (idLevel == m_carouselSky.getFloorLevelID())
		{
			return m_carouselSky.offer(drone);
		}
		return m_shaft.offer(drone);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void pushDestination(Drone person)
	{
		m_shaft.pushDestination(person);
	}

	/***********************************************************************************************
	 * Request a car to be sent down to replace one leaving a carousel.
	 *
	 * @param idFloorDest
	 *		Try to direct the car towards this destination level.
	 * @param idFloorSrc
	 *		Try to retrieve the car from this source floor or its group.
	 */
	public void requestNewCar(int idFloorSrc, int idFloorDest)
	{
		m_shaft.requestNewCar(idFloorDest, idFloorSrc);
	}

	/***********************************************************************************************
	 * Build connections to the structure levels after they have been estansiated.
	 */
	public void setLevelConnections() throws Exception
	{
		m_shaft.setLevelConnections();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("Rollavator.update()");

		m_carouselLobby.update(sim);
		m_carouselSky.update(sim);
		m_shaft.update(sim);

		Trace.endOf("Rollavator.update()");
	}
}

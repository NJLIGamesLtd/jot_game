/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator Shaft is the verticle part of a multilane lift system.
 */

package com.jot.rollavator;

import com.jot.item.*;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.*;

public class Shaft extends PlaceImpl
{
	protected static final Size s_size = new Size(5000, 4000, 800000);
	protected static final Size s_sizeLane = new Size(1600, 2000, 4000);

	protected LiftReturn[][] m_aaLiftReturns;
	private LiftLoop m_liftLoop;
	private BeltPort[] m_aPorts;
	private int m_nLevelStacks;
	private int m_nLevelsPerStack;
	private StructureLevel[] m_aLevels;
	private Rollavator m_rollavator;

	/***********************************************************************************************
	 * Belt constructor based on its position and structure levels it connects with.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param aLevels
	 *	    The structure levels this lift connects with.
	 * @param nLevelsPerStack
	 *		The number of levels for each stack of floors connected by the same elevator.
	 * @param rollavator
	 *	    The Rollavator this is owned by.
	 */
	public Shaft(
		Position position,
		StructureLevel[] aLevels,
		int nLevelsPerStack,
		Rollavator rollavator
	) throws Exception
	{
		super(0, s_size, position);
		
		Trace.startOf("Rollavator.Shaft()");

		m_aLevels = aLevels;
		m_rollavator = rollavator;

		// Get the number of level stacks needed to accommodate all the structure levels.
		m_nLevelsPerStack = nLevelsPerStack;
		m_nLevelStacks = (aLevels.length - 3) / nLevelsPerStack;

		setMass(LiftLoop.s_mass + 2 * m_nLevelStacks /* * Lift.MASS */);

		Orientation orientation = position.getOrientation();
		double heading = orientation.getHeadingInRadians();
		int xBack = - s_size.m_x + s_sizeLane.m_x;

		Position posLoop = new Position(
			(int)(position.m_x + (xBack * Math.cos(heading))),
			(int)(position.m_y + (xBack * Math.sin(heading))),
			position.m_z,
			orientation);
		m_liftLoop = new LiftLoop(posLoop, aLevels, rollavator, m_nLevelStacks, nLevelsPerStack);
		
		m_aaLiftReturns = new LiftReturn[2][aLevels.length / nLevelsPerStack];
		m_aPorts = new BeltPort[aLevels.length];

		Trace.endOf("Rollavator.Shaft()");
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
		if (iFloor >= m_aPorts.length || m_aPorts[iFloor] == null)
		{
			return null;
		}
		return m_aPorts[iFloor].getBoxPort(iLane);
	}

	/***********************************************************************************************
	 * Return the nth door position.
	 *
	 * @param iLevel
	 *		The nth structure level (starting with 0 for the lobby).
	 * @param iDoor
	 *		The door index on this level for this lift (starting from 0).
	 * @return
	 *		The door position.
	 */
	public Position getLevelDoorPosition(int iLevel, int iDoor)
	{
		// Make sure we have a valid level index.
		if (iLevel < 0 || iLevel >= m_aLevels.length)
		{
			return null;
		}

		// Make sure we have a port on this level.
		BeltPort port = m_aPorts[iLevel];
		if (port == null)
		{
			return null;
		}

		// Redirect to the port for this door position.
		return port.getDoorPosition(iDoor);
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
		// Make sure we have a valid level index.
		if (iLevel < 0 || iLevel >= m_aLevels.length)
		{
			return null;
		}
		return m_aPorts[iLevel];
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
		return m_nLevelStacks;
	}

	/***********************************************************************************************
	 * Return the LiftLoop object.
	 *
	 * @return
	 *		The LiftLoop object.
	 */
	public LiftLoop getLiftLoop()
	{
		return m_liftLoop;
	}

	/***********************************************************************************************
	 * Return the Box, if available, waiting at this location to be paired up with.
	 *
	 * @param ePosDest
	 *		The enumeration of the position destination.
	 * @param position
	 *		The position we are trying to pair up from.
	 * @return
	 *		The Box, if available, waiting at this location to be paired up with.
	 */
	public Box getLoopPair(LiftLoop.PosEnum ePosDest, Position position)
	{
		int iDown = 0;
		switch (ePosDest)
		{
			case DOWN_LEVEL_STACK:
				iDown = 1;
			case UP_LEVEL_STACK:
				int zPos = position.getZ() / (4000 * m_nLevelsPerStack);
				return (Box)m_aaLiftReturns[iDown][zPos].getTrackCar();

			default:
				break;
		}
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void pushDestination(Drone person)
	{
		int iFloorGroup = person.getPosition().getZ() / (4000 * m_nLevelsPerStack);
		m_aaLiftReturns[1][iFloorGroup].pushDestination(person);
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
		m_aPorts[idFloorSrc].requestNewCar(idFloorDest);
	}

	/***********************************************************************************************
	 * Build connections to the structure levels after they have been instantiated.
	 */
	public void setLevelConnections() throws Exception
	{
		// Get the orientation towards the side to calculate positions for each lane.
		Orientation orientation = m_position.getOrientation();
		double heading = orientation.getHeadingInRadians();
		double sin_heading = Math.sin(heading);
		double cos_heading = Math.cos(heading);
		int xBack = - s_size.m_x / 3;

		int nStacks = m_aLevels.length / m_nLevelsPerStack;
		for (int j = 1; j < nStacks; ++j)
		{
			StructureLevel[] aStackLevels = new StructureLevel[m_nLevelsPerStack];
			for (int i = 0; i < m_nLevelsPerStack; ++i)
			{
				aStackLevels[i] = m_aLevels[j * m_nLevelsPerStack + i];
			}

			int ySeparation = -1000;
			for (int i = 0; i < 2; ++i)
			{
				Position liftPosition = new Position(
					(int)(m_position.m_x + (xBack * cos_heading) + (ySeparation * sin_heading)),
					(int)(m_position.m_y + (xBack * sin_heading) + (ySeparation * cos_heading)),
					m_position.m_z + j * 4000 * m_nLevelsPerStack,
					orientation);

				m_aaLiftReturns[i][j] = new LiftReturn(liftPosition, aStackLevels, m_rollavator, i);
				ySeparation += 2000;
			}
		}
		
		for (int i = m_nLevelsPerStack; i < m_aLevels.length; ++i)
		{
			Position portPosition = new Position(
				m_position.m_x,
				m_position.m_y,
				m_position.m_z + i * 4000,
				orientation);
			m_aPorts[i] = new BeltPort(portPosition, m_aLevels[i], m_rollavator);
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("rollavator.Shaft.update()");

		// Update the main lift loop that travels to the top and back down.
		m_liftLoop.update(sim);

		// Update each of the stacked lifts that take cars to specific floors.
		for (int i = 0; i < 2; ++i)
		{
			for (int j = 1; j < m_aaLiftReturns[0].length; ++j)
			{
				m_aaLiftReturns[i][j].update(sim);
			}
		}

		// Update the ports.
		for (int i = m_nLevelsPerStack; i < m_aLevels.length; ++i)
		{
			m_aPorts[i].update(sim);
		}

		Trace.endOf("rollavator.Shaft.update()");
	}
}

/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A LevelLobby is a ground level TowerLevel that provides extra access to lifts.
 */

package com.jot.jottower;

import com.jot.item.*;
import com.jot.lift.*;
import com.jot.random.Random;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.*;

import java.util.*;
import java.awt.Graphics;
import java.awt.Image;

/***************************************************************************************************
 * An LevelLoddy is a StructureLevel where people enter and exit the structure.
 */
public class LevelLobby extends StructureLevelImpl
{
	private static final int s_mass = 0;
	private static final Size s_size = new Size(106000, 122400, 4000);
	private static final int s_carouselOffsets[][] =
		{ { 12990, 22500 }, { 25980, 0 }, { 12990, -22500 }, { -12990, -22500 }, { -25980, 0 }, { -12990, 22500 } };
	private static final int s_carouselEdgeOffsets[][] =
		{ { 17940, 31760 }, { 36340, 1710 }, { 19380, -30820 }, { -19380, -30820 }, { -36340, 1710 }, { -17940, 31760 } };
	private static final int s_egressOffsets[][] =
		{ { 33150, 42080 }, { 53000, 7650 }, { 19880, -49730 }, { -19880, -49730 }, { -53000, 7650 }, { -33150, 42080 } };
	
	private static final CollisionGrid.GridItem s_gridItems[] =
	{
		// Support pillars from center out.
		new CollisionGrid.GridCircle(0, -60000, 1000),
		new CollisionGrid.GridCircle(0, -45000, 1000),
		new CollisionGrid.GridCircle(0, -30000, 1000),
		new CollisionGrid.GridCircle(0, -15000, 1000),
		new CollisionGrid.GridCircle(0,      0, 1000),
		new CollisionGrid.GridCircle(0,  30000, 1000),
		new CollisionGrid.GridCircle(0,  45000, 1000),
		new CollisionGrid.GridCircle(0,  60000, 1000),
		
		new CollisionGrid.GridCircle(-13000, -52500, 1000),	new CollisionGrid.GridCircle(13000, -52500, 1000),
		new CollisionGrid.GridCircle(-13000, -37500, 1000),	new CollisionGrid.GridCircle(13000, -37500, 1000),
		new CollisionGrid.GridCircle(-13000, -22500, 1000),	new CollisionGrid.GridCircle(13000, -22500, 1000),
		new CollisionGrid.GridCircle(-13000,   7500, 1000),	new CollisionGrid.GridCircle(13000,   7500, 1000),
		new CollisionGrid.GridCircle(-13000,  22500, 1000),	new CollisionGrid.GridCircle(13000,  22500, 1000),
		new CollisionGrid.GridCircle(-13000,  37500, 1000),	new CollisionGrid.GridCircle(13000,  37500, 1000),
		new CollisionGrid.GridCircle(-13000,  52500, 1000),	new CollisionGrid.GridCircle(13000,  52500, 1000),
		
		new CollisionGrid.GridCircle(-26000, -45000, 1000),	new CollisionGrid.GridCircle(26000, -45000, 1000),
		new CollisionGrid.GridCircle(-26000, -30000, 1000),	new CollisionGrid.GridCircle(26000, -30000, 1000),
		new CollisionGrid.GridCircle(-26000, -15000, 1000),	new CollisionGrid.GridCircle(26000, -15000, 1000),
		new CollisionGrid.GridCircle(-26000,      0, 1000),	new CollisionGrid.GridCircle(26000,      0, 1000),
		new CollisionGrid.GridCircle(-26000,  15000, 1000),	new CollisionGrid.GridCircle(26000,  15000, 1000),
		new CollisionGrid.GridCircle(-26000,  30000, 1000),	new CollisionGrid.GridCircle(26000,  30000, 1000),
		new CollisionGrid.GridCircle(-26000,  45000, 1000),	new CollisionGrid.GridCircle(26000,  45000, 1000),
		
		new CollisionGrid.GridCircle(-39000, -37500, 1000),	new CollisionGrid.GridCircle(39000, -37500, 1000),
		new CollisionGrid.GridCircle(-39000, -22500, 1000),	new CollisionGrid.GridCircle(39000, -22500, 1000),
		new CollisionGrid.GridCircle(-39000,  -7500, 1000),	new CollisionGrid.GridCircle(39000,  -7500, 1000),
		new CollisionGrid.GridCircle(-39000,   7500, 1000),	new CollisionGrid.GridCircle(39000,   7500, 1000),
		new CollisionGrid.GridCircle(-39000,  22500, 1000),	new CollisionGrid.GridCircle(39000,  22500, 1000),
		new CollisionGrid.GridCircle(-39000,  37500, 1000),	new CollisionGrid.GridCircle(39000,  37500, 1000),
		
		new CollisionGrid.GridCircle(-52000, -30000, 1000),	new CollisionGrid.GridCircle(52000, -30000, 1000),
		new CollisionGrid.GridCircle(-52000, -15000, 1000),	new CollisionGrid.GridCircle(52000, -15000, 1000),
		new CollisionGrid.GridCircle(-52000,      0, 1000),	new CollisionGrid.GridCircle(52000,      0, 1000),
		new CollisionGrid.GridCircle(-52000,  15000, 1000),	new CollisionGrid.GridCircle(52000,  15000, 1000),
		new CollisionGrid.GridCircle(-52000,  30000, 1000),	new CollisionGrid.GridCircle(52000,  30000, 1000),
		
		// Elevator Carousels:
		new CollisionGrid.GridCircle( 13000,  22500, 8500),
		new CollisionGrid.GridCircle( 26000,      0, 8500),
		new CollisionGrid.GridCircle( 13000, -22500, 8500),
		new CollisionGrid.GridCircle(-13000, -22500, 8500),
		new CollisionGrid.GridCircle(-26000,      0, 8500),
		new CollisionGrid.GridCircle(-13000,  22500, 8500),
		
		// Suite dividing walls.
		new CollisionGrid.GridRectangle(     0,  21130,      0,  60000, 1000),
		new CollisionGrid.GridRectangle( 18300, -10570,  52000, -30000, 1000),
		new CollisionGrid.GridRectangle(-18300, -10570, -52000, -30000, 1000),
		
		// Core walls
		new CollisionGrid.GridRectangle(-16800, -11430,  -1000, -11430, 1000),
		new CollisionGrid.GridRectangle( 16800, -11430,   1000, -11430, 1000),
		new CollisionGrid.GridRectangle(-18300,  -8340, -10460,   4880, 1000),
		new CollisionGrid.GridRectangle( 18300,  -8340,  10460,   4880, 1000),
		new CollisionGrid.GridRectangle( -9460,   6620,  -1500,  20270, 1000),
		new CollisionGrid.GridRectangle(  9460,   6620,   1500,  20270, 1000),
	};

	protected CollisionGrid m_collisionGrid;
	private Lift[] m_aLifts;
	private Location[] m_aLocCarousel;
	private Location[] m_aLocCarouselEdge;
	private Location[] m_aLocEgress;
	
	private List<Drone> m_lPersonsDirected;

	/***********************************************************************************************
	 * LevelLobby constructor based on its position and lifts.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param towerLifts
	 *	    An array of lifts that can be used to access this level.
	 */
	public LevelLobby(Position position, Lift[] towerLifts) throws Exception
	{
		super(s_mass, s_size, position);

		Trace.startOf("LevelLobby()");

		m_aLifts = towerLifts;
		m_lPersonsDirected = new ArrayList<Drone>();

		// Generate an array of carousel Locations.
		m_aLocCarousel = new Location[s_carouselOffsets.length];
		for (int i = 0; i < s_carouselOffsets.length; ++i)
		{
			m_aLocCarousel[i] = new Location(
				position.m_x + s_carouselOffsets[i][0],
				position.m_y + s_carouselOffsets[i][1],
				position.m_z);
		}

		// Generate an array of carousel edge Locations.
		m_aLocCarouselEdge = new Location[s_carouselEdgeOffsets.length];
		for (int i = 0; i < s_carouselEdgeOffsets.length; ++i)
		{
			m_aLocCarouselEdge[i] = new Location(
				position.m_x + s_carouselEdgeOffsets[i][0],
				position.m_y + s_carouselEdgeOffsets[i][1],
				position.m_z);
		}

		// Generate an array of egress Locations.
		m_aLocEgress = new Location[s_egressOffsets.length];
		for (int i = 0; i < s_egressOffsets.length; ++i)
		{
			m_aLocEgress[i] = new Location(
				position.m_x + s_egressOffsets[i][0],
				position.m_y + s_egressOffsets[i][1],
				position.m_z);
		}
		
		m_collisionGrid = new CollisionGrid(110000, 124000 /* SIZE.x, SIZE.y */);
		m_collisionGrid.addBlock(s_gridItems);

		Trace.endOf("LevelLobby()");
	}

	/***********************************************************************************************
	 * Return a two dimensional image of the collision grid that represents the walls and people.
	 * If needed, update the image to include the latest changes.
	 *
	 * @return
	 *		A two dimensional item of this item.
	 */
	public Image getImage()
	{
		return m_collisionGrid.getImage();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(Drone drone) throws Exception
	{
		Robot person = (Robot)drone;
		
		// UNTIL THE CITY CONTROLS THE LOCATION OF PEOPLE, PICK A RANDOM SIDE FOR THE
		// PERSON TO ENTER FROM.
		int side = Random.GetInt(m_aLocEgress.length);
		
		// Change the person's location to that of the egress.
		person.setLocation(m_aLocEgress[side]);

		Trace.important("LevelLobby: person (" + person + ") arrived at position: " + m_aLocEgress[side]);

		// If there is a vacant location at the lobby's egress, place our person there.
		if (!m_collisionGrid.offer(person))
		{
			Trace.critical("LevelLobby.arrive(): Collision occured. Person rejected.");
			return false;
		}
		person.setCollisionGrid(m_collisionGrid);

		// Pop off the lobby from the destination stack and direct the person towards the lift.
		person.popDestination();
		person.pushDestination(m_aLifts[side]);

		// Tell the drone it is in the control of the lobby;
		person.setPlace(this);

		return m_lPersonsDirected.add(person);
	}

	/***********************************************************************************************
	 * Paint the graphical representation of the current time state of the simulation.
	 */
	public void paint(Graphics g)
	{
		m_collisionGrid.paint(g);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("LevelLobby.update()");

		for (Drone person : m_lPersonsDirected)
		{
			Trace.trivial("LevelLobby.update(): Update " + person);
			person.update(sim);
		}

		Trace.endOf("LevelLobby.update()");
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return m_collisionGrid.toString();
	}
}

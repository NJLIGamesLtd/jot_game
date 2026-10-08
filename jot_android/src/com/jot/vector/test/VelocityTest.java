// Author: Jot Russell (copyright 2014, all rights reserved)

package com.jot.vector.test;

import org.junit.Test;

import com.jot.simulation.Cycles;
import com.jot.vector.Location;
import com.jot.vector.Velocity;

import static org.junit.Assert.*;

public class VelocityTest
{
	@Test
	public void testPositiveX()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(1000, 2000, 3000), new Location(2000, 2000, 3000));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(velTest.m_x, 1000 / Cycles.GetCyclesPerSecond());
			assertEquals(velTest.m_y, 0);
			assertEquals(velTest.m_z, 0);
		}
	}

	@Test
	public void testNegativeX()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(-1000, 2000, 3000), new Location(-2000, 2000, 3000));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(velTest.m_x, -1000 / Cycles.GetCyclesPerSecond());
			assertEquals(velTest.m_y, 0);
			assertEquals(velTest.m_z, 0);
		}
	}

	@Test
	public void testPositiveY()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(1000, 2000, 3000), new Location(1000, 3000, 3000));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(velTest.m_x, 0);
			assertEquals(velTest.m_y, 1000 / Cycles.GetCyclesPerSecond());
			assertEquals(velTest.m_z, 0);
		}
	}

	@Test
	public void testNegativeY()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(1000, -2000, 3000), new Location(1000, -3000, 3000));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(velTest.m_x, 0);
			assertEquals(velTest.m_y, -1000 / Cycles.GetCyclesPerSecond());
			assertEquals(velTest.m_z, 0);
		}
	}

	@Test
	public void testPositiveZ()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(1000, 2000, 3000), new Location(1000, 2000, 4000));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(velTest.m_x, 0);
			assertEquals(velTest.m_y, 0);
			assertEquals(velTest.m_z, 1000 / Cycles.GetCyclesPerSecond());
		}
	}

	@Test
	public void testNegativeZ()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(1000, 2000, -3000), new Location(1000, 2000, -4000));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(velTest.m_x, 0);
			assertEquals(velTest.m_y, 0);
			assertEquals(velTest.m_z, -1000 / Cycles.GetCyclesPerSecond());
		}
	}

	@Test
	public void testPositiveXPositiveY()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(1000, 2000, 0), new Location(2732, 3000, 0));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(velTest.m_x, (int)(0.5 + (Math.cos(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond())));
			assertEquals(velTest.m_y, (int)(0.5 + (Math.sin(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond())));
			assertEquals(velTest.m_z, 0);
		}
	}

	@Test
	public void testPositiveXNegativeY()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(1000, -2000, 0), new Location(2732, -3000, 0));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(velTest.m_x, (int)(0.5 + (Math.cos(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond())));
			assertEquals(velTest.m_y, (int)(-(0.5 + (Math.sin(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond()))));
			assertEquals(velTest.m_z, 0);
		}
	}

	@Test
	public void testNegativeXPositiveY()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(-1000, 2000, 0), new Location(-2732, 3000, 0));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(velTest.m_x, (int)(-(0.5 + (Math.cos(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond()))));
			assertEquals(velTest.m_y, (int)(0.5 + (Math.sin(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond())));
			assertEquals(velTest.m_z, 0);
		}
	}

	@Test
	public void testNegativeXNegativeY()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(-1000, -2000, 0), new Location(-2732, -3000, 0));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(velTest.m_x, (int)(-(0.5 + (Math.cos(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond()))));
			assertEquals(velTest.m_y, (int)(-(0.5 + (Math.sin(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond()))));
			assertEquals(velTest.m_z, 0);
		}
	}

	@Test
	public void testPositiveXPositiveYPositiveZ()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(1000, 2000, 4000), new Location(2732, 3000, 6000));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(
				velTest.m_x,
				(int)(0.5 + (Math.cos(Math.PI / 4) * Math.cos(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond())));
			assertEquals(
				velTest.m_y,
				(int)(0.5 + (Math.cos(Math.PI / 4) * Math.sin(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond())));
			assertEquals(
				velTest.m_z,
				(int)(0.5 + (Math.sin(Math.PI / 4) * 1000 / Cycles.GetCyclesPerSecond())));
		}
	}

	@Test
	public void testPositiveXPositiveYNegativeZ()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(1000, 2000, -4000), new Location(2732, 3000, -6000));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(
				velTest.m_x,
				(int)(0.5 + (Math.cos(Math.PI / 4) * Math.cos(Math.PI / 6) * 1000/ Cycles.GetCyclesPerSecond())));
			assertEquals(
				velTest.m_y,
				(int)(0.5 + (Math.cos(Math.PI / 4) * Math.sin(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond())));
			assertEquals(
				velTest.m_z,
				(int)(-(0.5 + (Math.sin(Math.PI / 4) * 1000 / Cycles.GetCyclesPerSecond()))));
		}
	}

	@Test
	public void testPositiveXNegativeYNegativeZ()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(1000, -2000, -4000), new Location(2732, -3000, -6000));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(
				velTest.m_x,
				(int)(0.5 + (Math.cos(Math.PI / 4) * Math.cos(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond())));
			assertEquals(
				velTest.m_y,
				(int)(-(0.5 + (Math.cos(Math.PI / 4) * Math.sin(Math.PI / 6) * 1000/ Cycles.GetCyclesPerSecond()))));
			assertEquals(
				velTest.m_z,
				(int)(-(0.5 + (Math.sin(Math.PI / 4) * 1000 / Cycles.GetCyclesPerSecond()))));
		}
	}

	@Test
	public void testNegativeXNegativeYNegativeZ()
	{
		Velocity velTest;
		
		try
		{
			velTest = new Velocity(1000, new Location(-1000, -2000, -4000), new Location(-2732, -3000, -6000));
		}
		catch (Exception e)
		{
			fail("Unable to create Velocity object" + e.getMessage());
			velTest = null;
		}
		if (velTest != null)
		{
			assertEquals(
				velTest.m_x,
				(int)(-(0.5 + (Math.cos(Math.PI / 4) * Math.cos(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond()))));
			assertEquals(
				velTest.m_y,
				(int)(-(0.5 + (Math.cos(Math.PI / 4) * Math.sin(Math.PI / 6) * 1000 / Cycles.GetCyclesPerSecond()))));
			assertEquals(
				velTest.m_z,
				(int)(-(0.5 + (Math.sin(Math.PI / 4) * 1000 / Cycles.GetCyclesPerSecond()))));
		}
	}
}

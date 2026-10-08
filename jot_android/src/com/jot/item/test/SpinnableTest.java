/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Spinnable Unit Test.
 */

package com.jot.item.test;

import com.jot.item.SpinnableImpl;

import org.junit.Test;

import static org.junit.Assert.*;

public class SpinnableTest
{
	@Test
	public void testSpinnableOne()
	{
		// Create our two tracks based on these positions.
		SpinnableImpl spin;
		try
		{
			spin = new SpinnableImpl(0, null, null);
		}
		catch (Exception e)
		{
			fail("Failed to create SpinnableImpl: " + e.getMessage());
			return;
		}

		// Spin our item now by a delta heading.
		spin.addHeading(15);
		assertEquals(spin.getHeading(), 15, 0);

		// Spin our item now by a delta heading.
		spin.addHeading(30);
		assertEquals(spin.getHeading(), 45, 0);
	}
}

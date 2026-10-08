/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Spinnable Item is a stationary item that can spin based on an angular velocity.
 * A good example of this is merry-go-round.
 */

package com.jot.item;

import com.jot.simulation.Cycles;
import com.jot.vector.Orientation;

public interface Spinnable extends Place
{
	/***********************************************************************************************
	 * Immediately spin our two dimensional heading, in the X,Y plane, by a delta.
	 *
	 * @param delta
	 *		Spin by this delta of the heading.
	 */
	public void addHeading(int delta);
	
	/***********************************************************************************************
	 * Immediately change our orientation to a new three dimensional vector.
	 *
	 * @param orientation
	 *		Spin to this orientation.
	 */
	public void setOrientation(final Orientation orientation);
	
	/***********************************************************************************************
	 * Progressively spin based on an angular step velocity.
	 *
	 * @param cyclesAngular
	 *		Represents a velocity of spin based on a cycle count.
	 */
	public void directOrientation(Cycles cyclesAngular);
}

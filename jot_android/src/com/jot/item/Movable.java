/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Movable item is a Spinnable item (change in orientation) that can move.
 */

package com.jot.item;

import com.jot.vector.*;

public interface Movable extends Spinnable
{
	/***********************************************************************************************
	 * Spin this item based on an origin that is a measured distance away.
	 *
	 * @param deltaHeading
	 *		A flat delta.
	 * @param locOrigin
	 *		The vector location of the origin.
	 * @param distOrigin
	 *		The distance this item is from the origin.
	 */
	public void addHeading(int deltaHeading, Location locOrigin, int distOrigin);

	/***********************************************************************************************
	 * Immediately move our existing location object by a three dimensional delta.
	 *
	 * @param locDelta
	 *		A 3D vector delta.
	 */
	public void addLocation(final Location locDelta);

	/***********************************************************************************************
	 * Direct our item to move towards another location based on a flat velocity.
	 *
	 * @param velocity
	 *		A flat velocity.
	 * @param locFinal
	 *		New vector location that our item will eventually end up at.
	 */
	public void directLocation(int velocity, Location locFinal) throws Exception;
	
	/***********************************************************************************************
	 * Direct our item to move towards another location based on a vector velocity.
	 *
	 * @param velocity
	 *		A vector velocity.
	 * @param locFinal
	 *		New vector location that our item will eventually end up at.
	 */
	public void directLocation(Velocity velocity, Location locFinal) throws Exception;
	
	/***********************************************************************************************
	 * Direct our orientation towards another.
	 *
	 * @param orientFinal
	 *		New vector orientation that our item will eventually end up towards.
	 */
	public void directOrientation(Orientation orientFinal);
}

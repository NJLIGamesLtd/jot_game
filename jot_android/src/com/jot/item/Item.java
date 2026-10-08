/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * An Item is a tangible object, that has mass, position and a size.
 */

package com.jot.item;

import com.jot.vector.*;
import java.awt.*;

public interface Item
{
	/***********************************************************************************************
	 * Return the anglar heading of this Orientation.
	 *
	 * @return
	 *		The anglar heading of this Orientation.
	 */
	public int getHeading();

	/***********************************************************************************************
	 * Return a two dimensional item of this item.
	 *
	 * @return
	 *		A two dimensional item of this item.
	 */
	public Image getImage();

	/***********************************************************************************************
	 * Return the vector location of this item.
	 *
	 * @return
	 *		The vector location of this item.
	 */
	public Location getLocation();

	/***********************************************************************************************
	 * Return the kg mass of this item.
	 *
	 * @return
	 *		The kg mass of this item.
	 */
	public int getMass();

	/***********************************************************************************************
	 * Return the vector orientation of this item.
	 *
	 * @return
	 *		The vector orientation of this item.
	 */
	public Orientation getOrientation();
	
	/***********************************************************************************************
	 * Return the anglar pitch value of this Orientation.
	 *
	 * @return
	 *		The anglar pitch value of this Orientation.
	 */
	public int getPitch();
	
	/***********************************************************************************************
	 * Return the vector position (location and orientation) of this item.
	 *
	 * @return
	 *		The vector position (location and orientation) of this item.
	 */
	public Position getPosition();

	/***********************************************************************************************
	 * Return the safe radius to avoid collision with this item.
	 *
	 * @return
	 *		The safe radius to avoid collision with this item.
	 */
	public int getRadius();

	/***********************************************************************************************
	 * Return the anglar roll value of this Orientation.
	 *
	 * @return
	 *		The anglar roll value of this Orientation.
	 */
	public int getRoll();
	
	/***********************************************************************************************
	 * Return the vector size of this item.
	 *
	 * @return
	 *		The vector size of this item.
	 */
	public Size getSize();

	/***********************************************************************************************
	 * Immediately assign our existing location to the new location values provided.
	 *
	 * @param x
	 *		New x vector location.
	 * @param y
	 *		New y vector location.
	 */
	public void setLocation(int x, int y);

	/***********************************************************************************************
	 * Immediately assign our existing location to the new location values provided.
	 *
	 * @param x
	 *		New x vector location.
	 * @param y
	 *		New y vector location.
	 * @param z
	 *		New z vector location.
	 */
	public void setLocation(int x, int y, int z);

	/***********************************************************************************************
	 * Immediately assign our existing location to the new location values provided.
	 *
	 * @param location
	 *		New vector location.
	 */
	public void setLocation(final Location location);

	/***********************************************************************************************
	 * Assign the kg mass of this item.
	 *
	 * @param mass
	 *		The new kg mass of this item.
	 */
	public void setMass(int mass);

	/***********************************************************************************************
	 * Assign a new position object to define our location and orientation.
	 *
	 * @param position
	 *		A vector position object.
	 */
	public void setPosition(Position position);
}

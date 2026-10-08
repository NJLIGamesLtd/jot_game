/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Size is Vector extension that represents a 3D vector width, length and height of an item.
 */

package com.jot.vector;

public class Size extends Vector
{
	/***********************************************************************************************
	 * Constructor that defaults to zero values.
	 */
	public Size()
	{
		super();
	}

	/***********************************************************************************************
	 * Size constructor requires initial 3D vector values.
	 *
	 * @param width
	 *		Initial width of the item Size.
	 * @param length
	 *		Initial length of the item Size.
	 * @param heading
	 *		Initial height of the item Size.
	 */
	public Size(int width, int length, int height)
	{
		super(width, length, height);
	}

	/***********************************************************************************************
	 * Return the width vector of this Size.
	 *
	 * @return
	 *		The width vector of this Size.
	 */
	public int getWidth()
	{
		return m_x;
	}
	
	/***********************************************************************************************
	 * Return the length vector of this Size.
	 *
	 * @return
	 *		The length vector of this Size.
	 */
	public int getLength()
	{
		return m_y;
	}
	
	/***********************************************************************************************
	 * Return the height vector of this Size.
	 *
	 * @return
	 *		The height vector of this Size.
	 */
	public int getHeight()
	{
		return m_z;
	}

	/***********************************************************************************************
	 * Assign the width vector of this Size.
	 *
	 * @param width
	 *		The width vector of this Size.
	 */
	public void setWidth(int width)
	{
		m_x = width;
	}
	
	/***********************************************************************************************
	 * Assign the length vector of this Size.
	 *
	 * @param length
	 *		The length vector of this Size.
	 */
	public void setLength(int length)
	{
		m_y = length;
	}
	
	/***********************************************************************************************
	 * Assign the height vector of this Size.
	 *
	 * @param height
	 *		The height vector of this Size.
	 */
	public void setHeight(int height)
	{
		m_z = height;
	}

	/***********************************************************************************************
	 * Override to output the string representation of this Size based
	 * on its class label and 3D Vector values.
	 *
	 * @return
	 *		A string representation of this Size.
	 */
	public String toString()
	{
		return "Size" + super.toString();
	}
}

package com.jot.crossword;

//**************************************************************************************************

public class Bitmap
{
	public enum Config
	{
		ARGB_8888
	}
	
	public static Bitmap
	createBitMap(
		int[][] bitmap,
		int width,
		int height,
		Config eConfig)
	{
		return new Bitmap();
	}

	public static Bitmap
	createBitMap(
		int width,
		int height,
		Config eConfig)
	{
		return new Bitmap();
	}

	public void
	clear(
		int x,
		int y,
		int width,
		int height)
	{
	}

	public void
	drawBitmap(
		Bitmap bmSquare,
		int x,
		int y,
		Object obj)
	{
	}

	public int
	getHeight()
	{
		return 0;
	}

	public int
	getWidth()
	{
		return 0;
	}
}

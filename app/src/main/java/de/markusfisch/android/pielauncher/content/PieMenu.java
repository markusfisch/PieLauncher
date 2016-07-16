package de.markusfisch.android.pielauncher.content;

import java.util.ArrayList;

public class PieMenu
{
	private static final double TAU = Math.PI+Math.PI;
	private static final double HALF_PI = Math.PI*.5f;

	protected static class Icon
	{
		protected double weight;
		protected double size;
		protected double cellSize;
		protected int x;
		protected int y;
	}

	protected final ArrayList<Icon> icons = new ArrayList<>();

	protected int numberOfIcons = 0;
	protected int selectedIcon = -1;
	protected int centerX = -1;
	protected int centerY = -1;
	protected double radius = 0;
	protected double twist = 0;

	public void set( int x, int y, double radius )
	{
		set( x, y, radius, 0 );
	}

	public void set( int x, int y, double radius, double twist )
	{
		centerX = x;
		centerY = y;
		this.radius = radius;
		this.twist = twist;
	}

	public void calculate( float x, float y )
	{
		selectedIcon = -1;

		if( numberOfIcons < 1 ||
			icons == null )
			return;

		// calculate positions and sizes
		int closestIcon = 0;
		boolean cursorNearCenter = false;
		double circumference = Math.PI*(radius*2f);
		double pixelsPerRadian = TAU/circumference;
		double centeredY = y-centerY;
		double centeredX = x-centerX;
		double cursorAngle = Math.atan2( centeredY, centeredX );
		double cellSize = TAU/numberOfIcons;
		double closestAngle = 0;
		double weight = 0;
		double maxIconSize = .8f*radius;
		double maxWeight;

		// calculate weight of each icon
		{
			double cursorRadius = Math.sqrt(
				centeredY*centeredY+
				centeredX*centeredX );
			double infieldRadius = radius/2f;
			double factor = cursorRadius/infieldRadius;

			if( cursorRadius < infieldRadius )
			{
				double b = circumference/numberOfIcons*.75f;

				if( b < maxIconSize )
					maxIconSize = b+(maxIconSize-b)*factor;

				cursorNearCenter = true;
			}

			// determine how close every icon is to the cursor
			{
				double closestDistance = TAU;
				double a = twist;
				double m = maxIconSize*pixelsPerRadian/cellSize;

				maxWeight = HALF_PI+Math.pow( Math.PI, m );

				for( int n = 0; n < numberOfIcons; ++n )
				{
					double d = Math.abs(
						getAngleDifference( a, cursorAngle ) );

					if( d < closestDistance )
					{
						closestDistance = d;
						closestIcon = n;
						closestAngle = a;
					}

					if( cursorRadius < infieldRadius )
						d *= factor;

					Icon ic = icons.get( n );

					ic.weight = HALF_PI+Math.pow( Math.PI-d, m );
					weight += ic.weight;

					if( (a += cellSize) > Math.PI )
						a -= TAU;
				}

				if( !cursorNearCenter )
					selectedIcon = closestIcon;
			}
		}

		// calculate size of icons
		{
			double sizeUnit = circumference/weight;

			for( int n = numberOfIcons; n-- > 0; )
			{
				Icon ic = icons.get( n );

				ic.size = ic.cellSize = sizeUnit*ic.weight;
			}

			// scale icons within cell
			{
				double maxSize = sizeUnit*maxWeight;

				if( maxSize > maxIconSize )
				{
					double f = maxIconSize/maxSize;

					for( int n = numberOfIcons; n-- > 0; )
						icons.get( n ).size *= f;
				}
			}
		}

		// calculate icon positions
		{
			double difference = getAngleDifference(
				cursorAngle, closestAngle );
			double angle = getValidAngle(
				cursorAngle-(
					pixelsPerRadian*
					icons.get( closestIcon ).cellSize)/
				cellSize*difference );

			// active icon
			{
				Icon ic = icons.get( closestIcon );

				ic.x = centerX+(int)Math.round(
					radius*Math.cos( angle ) );
				ic.y = centerY+(int)Math.round(
					radius*Math.sin( angle ) );
			}

			// calculate positions of all other icons
			{
				double leftAngle = angle;
				double rightAngle = angle;
				int left = closestIcon;
				int right = closestIcon;
				int previousRight = closestIcon;
				int previousLeft = closestIcon;

				for( int n = 0; ; ++n )
				{
					if( (--left) < 0 )
						left = numberOfIcons-1;

					// break here when number of icons is odd
					if( right == left )
						break;

					if( (++right) >= numberOfIcons )
						right = 0;

					Icon lic = icons.get( left );

					leftAngle = getValidAngle(
						leftAngle-(
							(.5f*icons.get( previousLeft ).cellSize)+
							(.5f*lic.cellSize)
						)*pixelsPerRadian );

					lic.x = centerX+(int)Math.round(
						radius*Math.cos( leftAngle ) );
					lic.y = centerY+(int)Math.round(
						radius*Math.sin( leftAngle ) );

					// break here when number of icons is even
					if( left == right )
						break;

					Icon ric = icons.get( right );

					rightAngle = getValidAngle(
						rightAngle+
						(
							(.5f*icons.get( previousRight ).cellSize)+
							(.5f*ric.cellSize)
						)*pixelsPerRadian );

					ric.x = centerX+(int)Math.round(
						radius*Math.cos( rightAngle ) );
					ric.y = centerY+(int)Math.round(
						radius*Math.sin( rightAngle ) );

					previousRight = right;
					previousLeft = left;
				}
			}
		}
	}

	private static double getAngleDifference( double a, double b )
	{
		double c = a-b;
		double d = a > b ?
			a-(b+TAU) :
			a-(b-TAU);

		return Math.abs( c ) < Math.abs( d ) ? c : d;
	}

	private static double getValidAngle( double a )
	{
		return (a+TAU) % TAU;
	}
}

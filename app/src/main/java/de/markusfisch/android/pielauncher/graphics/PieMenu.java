package de.markusfisch.android.pielauncher.graphics;

import java.util.ArrayList;

public class PieMenu {
	public static final double TAU = Math.PI + Math.PI;
	public static final double HALF_PI = Math.PI * .5f;

	public static class Icon {
		public double weight;
		public double size;
		public double cellSize;
		public int x;
		public int y;
	}

	public final ArrayList<Icon> icons = new ArrayList<>();

	private int selectedIcon = -1;
	private int centerX = -1;
	private int centerY = -1;
	private double radius = 0;
	private double twist = 0;
	private float iconScale = 0;

	public static double getPositiveAngle(double a) {
		return (a + TAU + TAU) % TAU;
	}

	public static double getAngleDifference(double a, double b) {
		double d = getPositiveAngle(a - b);
		if (d > Math.PI) {
			d -= TAU;
		}
		return d;
	}

	public int getSelectedIcon() {
		return selectedIcon;
	}

	public int getCenterX() {
		return centerX;
	}

	public int getCenterY() {
		return centerY;
	}

	public void set(int centerX, int centerY, double radius, double twist,
			float iconScale) {
		this.centerX = centerX;
		this.centerY = centerY;
		this.radius = radius;
		this.twist = twist;
		this.iconScale = iconScale;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public double getTwist() {
		return twist;
	}

	public void setTwist(double twist) {
		this.twist = getPositiveAngle(twist);
	}

	public void calculate(float x, float y) {
		calculate(x, y, 1f);
	}

	public void calculate(float x, float y, float t) {
		selectedIcon = -1;

		int numberOfIcons = icons.size();
		if (numberOfIcons < 1) {
			return;
		}

		// Calculate positions and sizes.
		int closestIcon = 0;
		boolean cursorNearCenter = false;
		double rad = radius + (1f - t) * radius * .25f;
		double circumference = Math.PI * rad * 2f;
		double pixelsPerRadian = TAU / circumference;
		double centeredY = y - centerY;
		double centeredX = x - centerX;
		double cursorAngle = Math.atan2(centeredY, centeredX);
		double cellSize = TAU / numberOfIcons;
		double closestAngle = 0;
		double weight = 0;
		double maxIconSize = .8f * rad;
		double maxWeight;

		// Calculate weight of each icon.
		{
			double cursorRadius = Math.sqrt(
					centeredY * centeredY + centeredX * centeredX);
			double infieldRadius = rad / 2f;
			double factor = cursorRadius / infieldRadius;

			if (cursorRadius < infieldRadius) {
				double b = circumference / numberOfIcons * .75f;
				if (b < maxIconSize) {
					maxIconSize = b + (maxIconSize - b) * factor;
				}
				cursorNearCenter = true;
			}

			// Determine how close every icon is to the cursor.
			{
				double closestDistance = TAU;
				double a = twist;
				double m = maxIconSize * pixelsPerRadian / cellSize;

				maxWeight = HALF_PI + Math.pow(Math.PI, m);

				for (int i = 0; i < numberOfIcons; ++i) {
					double d = Math.abs(getAngleDifference(a, cursorAngle));
					if (d < closestDistance) {
						closestDistance = d;
						closestIcon = i;
						closestAngle = a;
					}

					if (cursorRadius < infieldRadius) {
						d *= factor;
					}

					Icon ic = icons.get(i);
					ic.weight = HALF_PI + Math.pow(Math.PI - d, m);
					weight += ic.weight;

					if ((a += cellSize) > Math.PI) {
						a -= TAU;
					}
				}

				if (!cursorNearCenter) {
					selectedIcon = closestIcon;
				}
			}
		}

		// Calculate size of icons.
		{
			double sizeUnit = circumference / weight;
			double maxSize = sizeUnit * maxWeight;
			double f = Math.min(1f, maxIconSize / maxSize) * iconScale;
			for (int i = numberOfIcons; i-- > 0; ) {
				Icon ic = icons.get(i);
				ic.cellSize = sizeUnit * ic.weight;
				// Scale icons within cell.
				ic.size = ic.cellSize * f;
			}
		}

		// Calculate icon positions.
		{
			double difference = getAngleDifference(cursorAngle, closestAngle);
			double angle = getPositiveAngle(cursorAngle -
					(pixelsPerRadian * icons.get(closestIcon).cellSize) /
							cellSize * difference);

			// Calculate active icon.
			{
				Icon ic = icons.get(closestIcon);
				ic.x = centerX + (int) Math.round(
						rad * Math.cos(angle));
				ic.y = centerY + (int) Math.round(
						rad * Math.sin(angle));
			}

			// Calculate positions of all other icons.
			{
				double leftAngle = angle;
				double rightAngle = angle;
				int left = closestIcon;
				int right = closestIcon;
				int previousRight = closestIcon;
				int previousLeft = closestIcon;

				for (; ; ) {
					if ((--left) < 0) {
						left = numberOfIcons - 1;
					}

					// Break here when number of icons is odd.
					if (right == left) {
						break;
					}

					if ((++right) >= numberOfIcons) {
						right = 0;
					}

					Icon lic = icons.get(left);

					leftAngle = getPositiveAngle(leftAngle -
							(.5f * icons.get(previousLeft).cellSize +
									.5f * lic.cellSize) * pixelsPerRadian);

					lic.x = centerX + (int) Math.round(
							rad * Math.cos(leftAngle));
					lic.y = centerY + (int) Math.round(
							rad * Math.sin(leftAngle));

					// Break here when number of icons is even.
					if (left == right) {
						break;
					}

					Icon ric = icons.get(right);

					rightAngle = getPositiveAngle(rightAngle +
							(.5f * icons.get(previousRight).cellSize +
									.5f * ric.cellSize) * pixelsPerRadian);

					ric.x = centerX + (int) Math.round(
							rad * Math.cos(rightAngle));
					ric.y = centerY + (int) Math.round(
							rad * Math.sin(rightAngle));

					previousRight = right;
					previousLeft = left;
				}
			}
		}
	}
}

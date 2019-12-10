package de.markusfisch.android.pielauncher.graphics;

import java.util.ArrayList;

public class PieMenu {
	private static final double TAU = Math.PI + Math.PI;
	private static final double HALF_PI = Math.PI * .5f;

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

	public int getSelectedIcon() {
		return selectedIcon;
	}

	public void set(int x, int y, double radius) {
		set(x, y, radius, 0);
	}

	public void set(int x, int y, double radius, double twist) {
		centerX = x;
		centerY = y;
		this.radius = radius;
		this.twist = twist;
	}

	public void calculate(float x, float y) {
		selectedIcon = -1;

		int numberOfIcons = icons.size();
		if (numberOfIcons < 1) {
			return;
		}

		// calculate positions and sizes
		int closestIcon = 0;
		boolean cursorNearCenter = false;
		double circumference = Math.PI * (radius * 2f);
		double pixelsPerRadian = TAU / circumference;
		double centeredY = y - centerY;
		double centeredX = x - centerX;
		double cursorAngle = Math.atan2(centeredY, centeredX);
		double cellSize = TAU / numberOfIcons;
		double closestAngle = 0;
		double weight = 0;
		double maxIconSize = .8f * radius;
		double maxWeight;

		// calculate weight of each icon
		{
			double cursorRadius = Math.sqrt(
					centeredY * centeredY + centeredX * centeredX);
			double infieldRadius = radius / 2f;
			double factor = cursorRadius / infieldRadius;

			if (cursorRadius < infieldRadius) {
				double b = circumference / numberOfIcons * .75f;
				if (b < maxIconSize) {
					maxIconSize = b + (maxIconSize - b) * factor;
				}
				cursorNearCenter = true;
			}

			// determine how close every icon is to the cursor
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

		// calculate size of icons
		{
			double sizeUnit = circumference / weight;

			for (int i = numberOfIcons; i-- > 0; ) {
				Icon ic = icons.get(i);
				ic.size = ic.cellSize = sizeUnit * ic.weight;
			}

			// scale icons within cell
			{
				double maxSize = sizeUnit * maxWeight;
				if (maxSize > maxIconSize) {
					double f = maxIconSize / maxSize;
					for (int i = numberOfIcons; i-- > 0; ) {
						icons.get(i).size *= f;
					}
				}
			}
		}

		// calculate icon positions
		{
			double difference = getAngleDifference(cursorAngle, closestAngle);
			double angle = getPositiveAngle(cursorAngle -
					(pixelsPerRadian * icons.get(closestIcon).cellSize) /
							cellSize * difference);

			// active icon
			{
				Icon ic = icons.get(closestIcon);
				ic.x = centerX + (int) Math.round(
						radius * Math.cos(angle));
				ic.y = centerY + (int) Math.round(
						radius * Math.sin(angle));
			}

			// calculate positions of all other icons
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

					// break here when number of icons is odd
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
							radius * Math.cos(leftAngle));
					lic.y = centerY + (int) Math.round(
							radius * Math.sin(leftAngle));

					// break here when number of icons is even
					if (left == right) {
						break;
					}

					Icon ric = icons.get(right);

					rightAngle = getPositiveAngle(rightAngle +
							(.5f * icons.get(previousRight).cellSize +
									.5f * ric.cellSize) * pixelsPerRadian);

					ric.x = centerX + (int) Math.round(
							radius * Math.cos(rightAngle));
					ric.y = centerY + (int) Math.round(
							radius * Math.sin(rightAngle));

					previousRight = right;
					previousLeft = left;
				}
			}
		}
	}

	private static double getAngleDifference(double a, double b) {
		double d = ((a - b) + TAU) % TAU;
		if (d > Math.PI) {
			d -= TAU;
		}
		return d;
	}

	private static double getPositiveAngle(double a) {
		return (a + TAU) % TAU;
	}
}

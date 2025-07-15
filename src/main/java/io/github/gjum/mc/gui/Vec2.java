package io.github.gjum.mc.gui;

public class Vec2 {
	public static final int LARGE = 999999;
	public final int x;
	public final int y;

	public Vec2(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Vec2(double x, double y) {
		this.x = (int) x;
		this.y = (int) y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Vec2 vec2 = (Vec2) o;

		if (x != vec2.x) return false;
		return y == vec2.y;
	}

	@Override
	public int hashCode() {
		int result = x;
		result = 31 * result + y;
		return result;
	}

	public int getDim(Direction direction) {
		if (direction == Direction.HORIZONTAL) {
			return x;
		} else {
			return y;
		}
	}

	public static Vec2 setDims(int main, int other, Direction direction) {
		if (direction == Direction.HORIZONTAL) {
			return new Vec2(main, other);
		} else {
			return new Vec2(other, main);
		}
	}

	@Override
	public String toString() {
		return "Vec2{" + x + ", " + y + '}';
	}

	public enum Direction {
		HORIZONTAL, VERTICAL;

		public Direction other() {
			if (this == HORIZONTAL) {
				return VERTICAL;
			} else {
				return HORIZONTAL;
			}
		}
	}
}

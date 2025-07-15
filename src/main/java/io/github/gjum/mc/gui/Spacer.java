package io.github.gjum.mc.gui;

public class Spacer extends GuiElement {
	public Spacer() {
		setMinSize(new Vec2(0, 0));
		setMaxSize(new Vec2(Vec2.LARGE, Vec2.LARGE));
		setWeight(new Vec2(1, 1));
	}

	public Spacer(Vec2 size) {
		setFixedSize(size);
	}
}

package io.github.gjum.mc.gui;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class Clickable extends GuiElement {
	private static int nextId = 1;

	protected static int getId() {
		return nextId++;
	}

	@Nullable
	public Consumer<Clickable> clickHandler = null;

	// type hack to return subclass/pass subclass to consumer
	@SuppressWarnings("unchecked")
	public <T extends Clickable> T onClick(@Nullable Consumer<T> clickHandler) {
		this.clickHandler = (Consumer<Clickable>) clickHandler;
		return (T) this;
	}

	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean handleMouseClicked(Vec2 mouse, int mouseButton) {
		if (mouseButton != 0) return false;
		if (!isEnabled()) return false;
		if (!isMouseInside(mouse)) return false;
		if (clickHandler == null) return false;

		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		try {
			clickHandler.accept(this);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return true;
	}

	boolean isMouseInside(Vec2 mouse) {
		if (getPos() == null) return false;
		return mouse.x >= getPos().x
				&& mouse.y >= getPos().y
				&& mouse.x < getPos().x + getSize().x
				&& mouse.y < getPos().y + getSize().y;
	}
}

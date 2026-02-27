package io.github.gjum.mc.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class GuiRoot extends Screen implements GuiParent {
	public final Screen parentScreen;
	@Nullable
	private GuiElement root = null;
	private boolean dirtyLayout = false;
	private Vec2 prevSize = null;
	private Vec2 prevMouse = new Vec2(0, 0);
	private Vec2 dragStart = null;

	public GuiRoot(Screen parentScreen, Component title) {
		super(title);
		//? if <1.21.11 {
		/*minecraft = Minecraft.getInstance();
		*///?}
		this.parentScreen = parentScreen;
	}

	public abstract GuiElement build();

	/**
	 * If elements need to be added/removed, call this to rebuild the whole layout tree and elements list.
	 */
	public void rebuild() {
		if (root != null) root.handleDestroyed();
		root = null;
		dirtyLayout = true;
	}

	protected void handleError(Throwable e) {
		e.printStackTrace();
		minecraft.setScreen(parentScreen);
	}

	@Override
	public void init() {
		try {
			rebuild();
		} catch (Throwable e) {
			handleError(e);
		}
	}

	/**
	 * Mark the layout to need to be recomputed on the next render.
	 * Useful when the size of an element changed.
	 */
	@Override
	public void invalidateLayout() {
		dirtyLayout = true;
	}

	@Override
	public <T extends GuiEventListener & NarratableEntry> T addWidget(T child) {
		return super.addWidget(child);
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
		try {
			//? if >=1.21.6 {
			// In 1.21.6+, renderBackground causes "Can only blur once per frame" exception
			// Use renderPanorama instead for a simple background
			renderPanorama(context, partialTicks);
			//?} else {
			/*renderBackground(context, mouseX, mouseY, partialTicks);
			*///?}

			if (root == null) {
				root = build();
				root.setParent(this);
				prevSize = null;
				dirtyLayout = true;
			}

			final Vec2 newSize = new Vec2(width, height);
			if (!newSize.equals(prevSize)) {
				prevSize = newSize;
				dirtyLayout = true;
			}

			if (dirtyLayout && root != null) {
				root.updateSize(newSize);
				root.setPos(new Vec2(0, 0));
				dirtyLayout = false;
			}

			if (root != null) {
				final Vec2 mousePos = new Vec2(mouseX, mouseY);
				root.draw(context, mousePos, newSize, partialTicks);
				root.drawOverlays(context, mousePos, newSize, partialTicks);
			}
		} catch (Throwable e) {
			handleError(e);
		}
	}

	//? if >=1.21.11 {
	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
		try {
			if (root != null) root.handleMouseClicked(new Vec2(click.x(), click.y()), click.button());
			dragStart = new Vec2(click.x(), click.y());
			prevMouse = new Vec2(click.x(), click.y());
		} catch (Throwable e) {
			handleError(e);
		}
		return false;
	}
	//?} else {
	/*@Override
	public boolean mouseClicked(double x, double y, int mouseButton) {
		try {
			if (root != null) root.handleMouseClicked(new Vec2(x, y), mouseButton);
			dragStart = new Vec2(x, y);
			prevMouse = new Vec2(x, y);
		} catch (Throwable e) {
			handleError(e);
		}
		return false;
	}
	*///?}

	//? if >=1.21.11 {
	@Override
	public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent click, double deltaX, double deltaY) {
		try {
			if (root != null) root.handleMouseDragged(
					new Vec2(click.x(), click.y()),
					prevMouse,
					dragStart,
					click.button());
			prevMouse = new Vec2(click.x(), click.y());
		} catch (Throwable e) {
			handleError(e);
		}
		return false;
	}
	//?} else {
	/*@Override
	public boolean mouseDragged(double x, double y, int clickedMouseButton, double xPrev, double yPrev) {
		try {
			if (root != null) root.handleMouseDragged(
					new Vec2(x, y),
					prevMouse,
					dragStart,
					clickedMouseButton);
			prevMouse = new Vec2(x, y);
		} catch (Throwable e) {
			handleError(e);
		}
		return false;
	}
	*///?}

	//? if >=1.21.11 {
	@Override
	public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent click) {
		try {
			this.setDragging(false);
			if (root != null) {
				root.handleMouseReleased(new Vec2(click.x(), click.y()),
						dragStart != null ? dragStart : prevMouse,
						click.button());
			}
		} catch (Throwable e) {
			handleError(e);
		}
		dragStart = null;
		return false;
	}
	//?} else {
	/*@Override
	public boolean mouseReleased(double x, double y, int state) {
		try {
			this.setDragging(false);
			if (root != null) {
				root.handleMouseReleased(new Vec2(x, y),
						dragStart != null ? dragStart : prevMouse,
						state);
			}
		} catch (Throwable e) {
			handleError(e);
		}
		dragStart = null;
		return false;
	}
	*///?}

	@Override
	public boolean mouseScrolled(double x, double y, double xAmount, double yAmount) {
		try {
			if (yAmount == 0) return false;
			if (root != null) {
				root.handleMouseScrolled(new Vec2(x, y), yAmount);
			}
		} catch (Throwable e) {
			handleError(e);
		}
		return false;
	}

	/**
	 * override if your screen should stay open or should open the parent instead
	 */
	public void handleEscape() {
		minecraft.setScreen(null);
	}

	//? if >=1.21.11 {
	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		try {
			if (root != null) {
				root.handleKeyPressed(event.key(), event.scancode(), event.modifiers());
			}
			if (event.key() == InputConstants.KEY_ESCAPE) {
				handleEscape();
			}
		} catch (Throwable e) {
			handleError(e);
		}
		return true;
	}
	//?} else {
	/*@Override
	public boolean keyPressed(int keyCode, int scanCode, int mods) {
		try {
			if (root != null) {
				root.handleKeyPressed(keyCode, scanCode, mods);
			}
			if (keyCode == InputConstants.KEY_ESCAPE) {
				handleEscape();
			}
		} catch (Throwable e) {
			handleError(e);
		}
		return true;
	}
	*///?}

	//? if >=1.21.11 {
	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
		try {
			if (root != null) {
				root.handleCharTyped((char) event.codepoint(), event.modifiers());
			}
		} catch (Throwable e) {
			handleError(e);
		}
		return true;
	}
	//?} else {
	/*@Override
	public boolean charTyped(char keyChar, int keyCode) {
		try {
			if (root != null) {
				root.handleCharTyped(keyChar, keyCode);
			}
		} catch (Throwable e) {
			handleError(e);
		}
		return true;
	}
	*///?}

	@Override
	public void onClose() {
		try {
			if (root != null) root.handleDestroyed();
		} catch (Throwable e) {
			handleError(e);
		}
	}
}

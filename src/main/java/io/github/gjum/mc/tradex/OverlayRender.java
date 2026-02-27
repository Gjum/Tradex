package io.github.gjum.mc.tradex;

//? if >=1.21.11 || <1.21.6 {
public class OverlayRender {}
//?} else {
/*import io.github.gjum.mc.tradex.model.Pos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static io.github.gjum.mc.tradex.TradexMod.mod;
import static io.github.gjum.mc.tradex.Utils.mc;

// Renders dot indicators on the HUD showing where search/nearby highlights are when occluded.
// Dots only appear when the block is behind something (not visible directly).
// When the block is visible, the normal in-world highlight shows instead.
public class OverlayRender {
	// ARGB colors
	private static final int COLOR_LIGHTBLUE = 0x4400FFFF;
	private static final int COLOR_MAGENTA = 0x44FF00FF;

	// Scale factor for the indicator relative to actual block size
	// 0.3 means indicator is 30% of the block's screen size
	private static final float BLOCK_SCALE_FACTOR = 0.8f;

	// Minimum indicator size in pixels (for very distant blocks)
	private static final int MIN_DOT_SIZE = 2;

	public static void renderHudOverlay(GuiGraphics graphics, DeltaTracker tickDelta) {
		if (mc.player == null || mc.level == null) return;
		if (mc.options.hideGui) return;

		var now = System.currentTimeMillis();
		var hourMs = 3600_000;
		int range = 200;

		var drew = new HashSet<Pos>();

		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		Matrix4f projectionMatrix = mc.gameRenderer.getProjectionMatrix(mc.options.fov().get());
		Matrix4f viewMatrix = getViewMatrix();
		Matrix4f viewProj = new Matrix4f(projectionMatrix).mul(viewMatrix);

		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();

		List<DotIndicator> dotsToDraw = new ArrayList<>();

		// Collect search results
		var searchResult = mod.highlightManager.searchSource.getSearchResult();
		if (searchResult != null && searchResult.ts > now - hourMs) {
			for (var exchange : searchResult.exchanges) {
				if (exchange.pos == null) continue;
				if (drew.contains(exchange.pos)) continue;
				if (exchange.pos.block().distSqr(mc.player.blockPosition()) > range * range) continue;

				var blockPos = exchange.pos.block();
				var center = Vec3.atCenterOf(blockPos);

				// Only show dot if the block is occluded (not visible)
				if (!isBlockOccluded(camPos, blockPos)) continue;

				var screenPos = projectPoint(center, camPos, viewProj, screenWidth, screenHeight);
				if (screenPos != null) {
					float distance = (float) camPos.distanceTo(center);
					int size = calculateDotSize(center, camPos, viewProj, screenHeight, mc.options.fov().get());
					dotsToDraw.add(new DotIndicator((int) screenPos[0], (int) screenPos[1],
							size, COLOR_LIGHTBLUE, distance));
					drew.add(exchange.pos);
				}
			}
		}

		// Collect nearby exchanges
		var nearbyResult = mod.highlightManager.nearbySource.getSearchResult();
		if (nearbyResult != null && nearbyResult.ts > now - hourMs) {
			for (var exchange : nearbyResult.exchanges) {
				if (exchange.pos == null) continue;
				if (drew.contains(exchange.pos)) continue;
				if (exchange.pos.block().distSqr(mc.player.blockPosition()) > range * range) continue;

				var blockPos = exchange.pos.block();
				var center = Vec3.atCenterOf(blockPos);

				// Only show dot if the block is occluded (not visible)
				if (!isBlockOccluded(camPos, blockPos)) continue;

				// Determine color based on stock and update time (same as 3D boxes)
				// Out of stock = RED, outdated (> 1 week) = ORANGE, recent & stocked = green-blue mixture
				var color = exchange.stock <= 0 ? Color.RED
						: exchange.time < now - Utils.weekMs ? Color.ORANGE
						: new Color(0, 255, 127);

				var screenPos = projectPoint(center, camPos, viewProj, screenWidth, screenHeight);
				if (screenPos != null) {
					float distance = (float) camPos.distanceTo(center);
					int size = calculateDotSize(center, camPos, viewProj, screenHeight, mc.options.fov().get());
					dotsToDraw.add(new DotIndicator((int) screenPos[0], (int) screenPos[1],
							size, colorToArgb(color, 0.27f), distance));
					drew.add(exchange.pos);
				}
			}
		}

		// Sort by distance (far to near) so nearer dots draw on top
		dotsToDraw.sort((a, b) -> Float.compare(b.distance, a.distance));

		// Draw all dots
		for (var dot : dotsToDraw) {
			drawDot(graphics, dot);
		}
	}

	// Check if a block is occluded (something is blocking the view between camera and block)
	private static boolean isBlockOccluded(Vec3 camPos, BlockPos targetBlock) {
		if (mc.level == null) return false;

		Vec3 targetCenter = Vec3.atCenterOf(targetBlock);

		// Raycast from camera to target block
		ClipContext context = new ClipContext(
			camPos,
			targetCenter,
			ClipContext.Block.VISUAL,
			ClipContext.Fluid.NONE,
			mc.player
		);

		BlockHitResult hitResult = mc.level.clip(context);

		// If we hit nothing or hit the target block itself, it's not occluded
		if (hitResult.getType() == HitResult.Type.MISS) {
			return false;
		}

		// If we hit the target block, it's visible (not occluded)
		if (hitResult.getBlockPos().equals(targetBlock)) {
			return false;
		}

		// We hit something else before reaching the target - it's occluded
		return true;
	}

	// Calculate dot size based on actual perspective projection of a block
	// Returns the screen size that a block would appear, scaled by BLOCK_SCALE_FACTOR
	private static int calculateDotSize(Vec3 worldPos, Vec3 camPos, Matrix4f viewProj,
	                                  int screenHeight, float fov) {
		// Project block center to get w coordinate (depth factor)
		float relX = (float) (worldPos.x - camPos.x);
		float relY = (float) (worldPos.y - camPos.y);
		float relZ = (float) (worldPos.z - camPos.z);

		Vector4f clipPos = new Vector4f(relX, relY, relZ, 1.0f);
		clipPos.mul(viewProj);

		// w contains the perspective divide factor
		if (clipPos.w <= 0.01f) return MIN_DOT_SIZE;

		// In perspective projection, screen size is proportional to viewport_height / w * tan(fov/2)
		// A 1-unit object in world space takes up: (viewport_height / (2 * w * tan(fov/2))) pixels on screen
		float fovRad = (float) Math.toRadians(fov);
		float screenScale = screenHeight / (2.0f * (float) Math.tan(fovRad / 2.0f));
		float blockScreenSize = screenScale / Math.abs(clipPos.w);

		// Apply scale factor and clamp to minimum
		int indicatorSize = (int) (blockScreenSize * BLOCK_SCALE_FACTOR);
		return Math.max(MIN_DOT_SIZE, indicatorSize);
	}

	// Build the view matrix from camera rotation.
	private static Matrix4f getViewMatrix() {
		var camera = mc.gameRenderer.getMainCamera();
		Matrix4f viewMatrix = new Matrix4f();
		viewMatrix.identity();
		viewMatrix.rotateX((float) Math.toRadians(camera.getXRot()));
		viewMatrix.rotateY((float) Math.toRadians(camera.getYRot() + 180f));
		return viewMatrix;
	}

	// Project a 3D point to screen coordinates. Returns null if behind camera or off-screen.
	private static float[] projectPoint(Vec3 worldPos, Vec3 camPos, Matrix4f viewProj,
										int screenWidth, int screenHeight) {
		float relX = (float) (worldPos.x - camPos.x);
		float relY = (float) (worldPos.y - camPos.y);
		float relZ = (float) (worldPos.z - camPos.z);

		Vector4f clipPos = new Vector4f(relX, relY, relZ, 1.0f);
		clipPos.mul(viewProj);

		// Behind camera
		if (clipPos.w <= 0.01f) return null;

		float ndcX = clipPos.x / clipPos.w;
		float ndcY = clipPos.y / clipPos.w;

		// Off-screen (with some margin)
		if (ndcX < -1.1f || ndcX > 1.1f || ndcY < -1.1f || ndcY > 1.1f) return null;

		float screenX = (0.5f + ndcX * 0.5f) * screenWidth;
		float screenY = (0.5f - ndcY * 0.5f) * screenHeight;

		return new float[]{screenX, screenY};
	}

	// Draw a dot indicator (filled square with border)
	private static void drawDot(GuiGraphics graphics, DotIndicator dot) {
		int halfSize = dot.size / 2;
		int x1 = dot.x - halfSize;
		int y1 = dot.y - halfSize;
		int x2 = dot.x + halfSize;
		int y2 = dot.y + halfSize;

		// Clamp to screen
		x1 = Math.max(0, x1);
		y1 = Math.max(0, y1);

		// Draw filled square
		graphics.fill(x1, y1, x2, y2, dot.color);

		// Draw border (darker)
		int borderColor = (dot.color & 0xFF000000) | ((dot.color & 0x00FEFEFE) >> 1);
		// Top
		graphics.fill(x1, y1, x2, y1 + 1, borderColor);
		// Bottom
		graphics.fill(x1, y2 - 1, x2, y2, borderColor);
		// Left
		graphics.fill(x1, y1, x1 + 1, y2, borderColor);
		// Right
		graphics.fill(x2 - 1, y1, x2, y2, borderColor);
	}

	// Convert java.awt.Color to ARGB format with alpha
	private static int colorToArgb(Color color, float alpha) {
		return ((int) (alpha * 255) << 24)
				| (color.getRed() << 16)
				| (color.getGreen() << 8)
				| color.getBlue();
	}

	record DotIndicator(int x, int y, int size, int color, float distance) {}
}
*///?}

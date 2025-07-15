package io.github.gjum.mc.tradex;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.gjum.mc.tradex.model.Pos;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;

import java.util.HashSet;

import static io.github.gjum.mc.tradex.TradexMod.mod;
import static io.github.gjum.mc.tradex.Utils.mc;

public class Render {
	public static void render(WorldRenderContext context) {
		if (mc.player == null || mc.level == null) return;
		if (mc.options.hideGui) return; // F1 mode
		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.mul(context.matrixStack().last().pose());
		modelViewStack.translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);

		//? if =1.21.1
		/*RenderSystem.applyModelViewMatrix();*/

		// common config for all modes
		RenderSystem.enableBlend();

		int range = 200;
		var now = System.currentTimeMillis();
		var hourMs = 3600_000;

		// render through blocks
		RenderSystem.disableDepthTest();

		var drew = new HashSet<Pos>();

		for (var kv : mod.exploredExchanges.entrySet()) {
			var pos = kv.getKey();
			if (drew.contains(pos)) continue; // already drawn from search results
			if (pos.block().distSqr(mc.player.blockPosition()) > range * range) continue;

			var anyNull = false;
			long minTime = now;
			for (var exchange : kv.getValue().list) {
				if (exchange == null) {
					anyNull = true;
					continue;
				}
				minTime = Math.min(minTime, exchange.time);
				if (kv.getValue().list.size() < exchange.multi) {
					anyNull = true;
				}
			}

			var color = anyNull ? Color.YELLOW
					: minTime < now - 8 * hourMs ? Color.GRAY
					: Color.GREEN;

			// inflate 0.01 to show above barrel without z fighting
			var aabb = new AABB(pos.block()).inflate(0.01);

			renderFilledBox(aabb, color, 0.3f);
			drew.add(pos);
		}

		if (mod.lastSearchResult != null && mod.lastSearchResult.ts > now - hourMs) {
			for (var exchange : mod.lastSearchResult.exchanges) {
				if (drew.contains(exchange.pos)) continue; // multiple results in same container
				if (exchange.pos.block().distSqr(mc.player.blockPosition()) > range * range) continue;
				// inflate 0.01 to show above barrel without z fighting
				var aabb = new AABB(exchange.pos.block()).inflate(0.01);
				renderFilledBox(aabb, Color.LIGHTBLUE, 0.3f);
				drew.add(exchange.pos);
			}
		}

		// cleanup
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);

		modelViewStack.popMatrix();
	}

	record Color(float r, float g, float b) {
		public static final Color WHITE = new Color(1, 1, 1);
		public static final Color GRAY = new Color(.5f, .5f, .5f);
		public static final Color RED = new Color(1, 0, 0);
		public static final Color GREEN = new Color(0, 1, 0);
		public static final Color BLUE = new Color(0, 0, 1);
		public static final Color LIGHTBLUE = new Color(0, 1, 1);
		public static final Color YELLOW = new Color(1, 1, 0);
		public static final Color ORANGE = new Color(1, .5f, 0);
	}

	private static void renderFilledBox(AABB box, Color color, float a) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

		//? if <=1.21.3 {
		/*RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
		*///?} else {
		RenderSystem.setShader(net.minecraft.client.renderer.CoreShaders.POSITION_COLOR);
		 //?}

		float r = color.r;
		float g = color.g;
		float b = color.b;
		float minX = (float) box.minX;
		float minY = (float) box.minY;
		float minZ = (float) box.minZ;
		float maxX = (float) box.maxX;
		float maxY = (float) box.maxY;
		float maxZ = (float) box.maxZ;

		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);

		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
	}
}

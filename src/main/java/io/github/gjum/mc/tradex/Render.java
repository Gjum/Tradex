package io.github.gjum.mc.tradex;

//? if >=1.21.6 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=1.21.11 {
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
*///?}
//?} else {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4fStack;
*///?}

import io.github.gjum.mc.tradex.model.Pos;
//? if >=1.21.11 {
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
*///?}
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.awt.Color;import java.util.HashSet;

import static io.github.gjum.mc.tradex.TradexMod.mod;
import static io.github.gjum.mc.tradex.Utils.mc;

public class Render {
	//? if >=1.21.11 {
	/** Custom render type identical to debugFilledBox but with NO_DEPTH_TEST. */
	private static RenderType noDepthFilledBox;
	private static RenderType getNoDepthFilledBox() {
		if (noDepthFilledBox == null) {
			try {
				// Build a pipeline identical to DEBUG_FILLED_BOX but with NO_DEPTH_TEST.
				// We build from scratch to avoid reflection on obfuscated field names.
				var pipeline = RenderPipeline.builder()
						.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
						.withUniform("Projection", UniformType.UNIFORM_BUFFER)
						.withVertexShader("core/position_color")
						.withFragmentShader("core/position_color")
						.withBlend(BlendFunction.TRANSLUCENT)
						.withDepthWrite(false)
						.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
						.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
						.withLocation(Identifier.fromNamespaceAndPath("tradex", "pipeline/no_depth_filled_box"))
						.build();

				// Precompile the pipeline on the GPU
				RenderSystem.getDevice().precompilePipeline(pipeline);

				// Create RenderSetup matching vanilla debugFilledBox
				var setup = RenderSetup.builder(pipeline)
						.sortOnUpload()
						.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
						.createRenderSetup();

				// Create RenderType via constructor reflection
				// (constructor names are never obfuscated, unlike method names)
				var ctor = RenderType.class.getDeclaredConstructor(String.class, RenderSetup.class);
				ctor.setAccessible(true);
				noDepthFilledBox = ctor.newInstance("tradex_no_depth_filled_box", setup);
				System.out.println("[Tradex] Successfully created no-depth render type for see-through boxes");
			} catch (Exception e) {
				System.err.println("[Tradex] Failed to create no-depth render type, falling back to debugFilledBox: " + e);
				e.printStackTrace();
				noDepthFilledBox = RenderTypes.debugFilledBox();
			}
		}
		return noDepthFilledBox;
	}
	//?}
	public static void render(WorldRenderContext context) {
		if (mc.player == null || mc.level == null) return;
		if (mc.options.hideGui) return; // F1 mode

		//? if >=1.21.6 {
		// Get the matrix stack and consumers from the context
		//? if >=1.21.11 {
		PoseStack matrices = context.matrices();
		//?} else {
		/*PoseStack matrices = context.matrixStack();
		*///?}
		MultiBufferSource consumers = context.consumers();

		// consumers may be null in some render events
		if (matrices == null || consumers == null) return;

		//? if >=1.21.11 {
		Vec3 camPos = context.worldState().cameraRenderState.pos;
		//?} else {
		/*Vec3 camPos = context.camera().getPosition();
		*///?}

		matrices.pushPose();
		matrices.translate(-camPos.x, -camPos.y, -camPos.z);
		//?} else {
		/*Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.mul(context.matrixStack().last().pose());
		modelViewStack.translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);

		//? if =1.21.1
		/^RenderSystem.applyModelViewMatrix();^/

		// common config for all modes
		RenderSystem.enableBlend();

		// render through blocks
		RenderSystem.disableDepthTest();
		*///?}

		int range = 200;
		var now = System.currentTimeMillis();
		var hourMs = 3600_000;

		var drew = new HashSet<Pos>();

		// Check if nearby highlighting is active - if so, render it first and skip explored for those positions
		boolean nearbyActive = mod.highlightManager.nearbySource.hasActiveHighlights();

		// Render nearby exchanges (highlighted with the "Highlight Nearby" button)
		// When active, render BEFORE explored to take priority (updated colors)
		// For 1.21.11+: render through walls using NO_DEPTH_TEST pipeline
		// For 1.21.6-1.21.10: render with depth testing (blocked by walls), dot overlay shows positions
		if (nearbyActive) {
			var nearbyResult = mod.highlightManager.nearbySource.getSearchResult();
			if (nearbyResult != null && nearbyResult.ts > now - hourMs) {
				//? if >=1.21.11 {
				// Use a separate BufferSource for nearby boxes
				var nearbyBuffer = new ByteBufferBuilder(1024);
				try {
					var nearbyConsumers = MultiBufferSource.immediate(nearbyBuffer);
					for (var exchange : nearbyResult.exchanges) {
						if (drew.contains(exchange.pos)) continue; // already drawn from search results
						if (exchange.pos.block().distSqr(mc.player.blockPosition()) > range * range) continue;

						// Determine color based on stock and update time
						// Out of stock = RED, outdated (> 1 week) = ORANGE, recent & stocked = green-blue mixture
						var color = exchange.stock <= 0 ? Color.RED
								: exchange.time < now - Utils.weekMs ? Color.ORANGE
								: new Color(0, 1f, 0.5f);

						// inflate 0.01 to show above barrel without z fighting
						var aabb = new AABB(exchange.pos.block()).inflate(0.01);
						renderFilledBox(matrices, nearbyConsumers, aabb, color, 0.3f, true);
						drew.add(exchange.pos);
					}
					// Flush and close our dedicated buffer
					((MultiBufferSource.BufferSource) nearbyConsumers).endBatch();
				} finally {
					nearbyBuffer.close();
				}
				//?} else if >=1.21.6 {
				/*// For 1.21.6-1.21.10: render with depth testing (blocked by walls)
				// Dot overlay (OverlayRender) shows positions through walls
				for (var exchange : nearbyResult.exchanges) {
					if (drew.contains(exchange.pos)) continue; // already drawn from search results
					if (exchange.pos.block().distSqr(mc.player.blockPosition()) > range * range) continue;

					// Determine color based on stock and update time
					// Out of stock = RED, outdated (> 1 week) = ORANGE, recent & stocked = green-blue mixture
					var color = exchange.stock <= 0 ? Color.RED
							: exchange.time < now - Utils.weekMs ? Color.ORANGE
							: new Color(0, 1f, 0.5f);

					// inflate 0.01 to show above barrel without z fighting
					var aabb = new AABB(exchange.pos.block()).inflate(0.01);
					renderFilledBox(matrices, consumers, aabb, color, 0.3f);
					drew.add(exchange.pos);
				}
				*///?}
			}
		}

		// Render explored exchanges (from chat)
		// These should respect depth test (blocked by walls)
		//? if <1.21.6 {
		/*RenderSystem.enableDepthTest();
		*///?}
		var exploredPositions = mod.highlightManager.exploredSource.getActivePositions(
			mod.getPlayerPos(), now, pos -> pos.block().distSqr(mc.player.blockPosition()) <= range * range
		);
		for (Pos pos : exploredPositions) {
			if (drew.contains(pos)) continue; // already drawn (from search or nearby)

			var chest = mod.highlightManager.exploredSource.getChest(pos);
			if (chest == null) continue;

			var anyNull = false;
			long minTime = now;
			for (var exchange : chest.list) {
				if (exchange == null) {
					anyNull = true;
					continue;
				}
				minTime = Math.min(minTime, exchange.time);
				if (chest.list.size() < exchange.multi) {
					anyNull = true;
				}
			}

			var color = anyNull ? Color.YELLOW
					: minTime < now - 8 * hourMs ? Color.GRAY
					: Color.GREEN;

			// inflate 0.01 to show above barrel without z fighting
			var aabb = new AABB(pos.block()).inflate(0.01);

			//? if >=1.21.11 {
			renderFilledBox(matrices, consumers, aabb, color, 0.3f, false);
			//?} else if >=1.21.6 {
			/*renderFilledBox(matrices, consumers, aabb, color, 0.3f);
			*///?} else {
			/*renderFilledBox(aabb, color, 0.3f);
			*///?}
			drew.add(pos);
		}
		//? if <1.21.6 {
		/*RenderSystem.disableDepthTest();
		*///?}

		// Render search results - prefer upstream's lastSearchResult if available
		// For 1.21.11+: render through walls using NO_DEPTH_TEST pipeline
		// For 1.21.6-1.21.10: render with depth testing (blocked by walls), dot overlay shows positions
		// For <1.21.6: render through walls using RenderSystem.disableDepthTest()
		var searchResult = mod.highlightManager.searchSource.getSearchResult();
		if (searchResult != null && searchResult.ts > now - hourMs) {
			//? if >=1.21.11 {
			// Use a separate BufferSource for blue boxes so we fully control the flush
			// and our custom NO_DEPTH_TEST pipeline is properly applied.
			var blueBuffer = new ByteBufferBuilder(1024);
			try {
				var blueConsumers = MultiBufferSource.immediate(blueBuffer);
				for (var exchange : searchResult.exchanges) {
					if (drew.contains(exchange.pos)) continue; // multiple results in same container
					if (exchange.pos.block().distSqr(mc.player.blockPosition()) > range * range) continue;
					// inflate 0.01 to show above barrel without z fighting
					var aabb = new AABB(exchange.pos.block()).inflate(0.01);
					renderFilledBox(matrices, blueConsumers, aabb, Color.LIGHTBLUE, 0.3f, true);
					drew.add(exchange.pos);
				}
				// Flush and close our dedicated buffer - this is what actually draws with our pipeline
				((MultiBufferSource.BufferSource) blueConsumers).endBatch();
			} finally {
				blueBuffer.close();
			}
			//?} else if >=1.21.6 {
			/*// For 1.21.6-1.21.10: render with depth testing (blocked by walls)
			// Dot overlay (OverlayRender) shows positions through walls
			for (var exchange : searchResult.exchanges) {
				if (drew.contains(exchange.pos)) continue;
				if (exchange.pos.block().distSqr(mc.player.blockPosition()) > range * range) continue;
				var aabb = new AABB(exchange.pos.block()).inflate(0.01);
				renderFilledBox(matrices, consumers, aabb, Color.LIGHTBLUE, 0.3f);
				drew.add(exchange.pos);
			}
			*///?} else {
			/*// For versions below 1.21.6, render search results
			// Global depth disable makes them work through walls
			for (var exchange : searchResult.exchanges) {
				if (drew.contains(exchange.pos)) continue; // multiple results in same container
				if (exchange.pos.block().distSqr(mc.player.blockPosition()) > range * range) continue;
				// inflate 0.01 to show above barrel without z fighting
				var aabb = new AABB(exchange.pos.block()).inflate(0.01);
				renderFilledBox(aabb, Color.LIGHTBLUE, 0.3f);
				drew.add(exchange.pos);
			}
			*///?}
		} else {
			// Fallback: use HighlightSource (from WIP)
			var searchPositions = mod.highlightManager.searchSource.getActivePositions(
				mod.getPlayerPos(), now, pos -> pos.block().distSqr(mc.player.blockPosition()) <= range * range
			);
			for (Pos pos : searchPositions) {
				if (drew.contains(pos)) continue; // multiple results in same container
				// inflate 0.01 to show above barrel without z fighting
				var aabb = new AABB(pos.block()).inflate(0.01);
				//? if >=1.21.11 {
				renderFilledBox(matrices, consumers, aabb, Color.LIGHTBLUE, 0.3f, false);
				//?} else if >=1.21.6 {
				/*renderFilledBox(matrices, consumers, aabb, Color.LIGHTBLUE, 0.3f);
				*///?} else {
				/*renderFilledBox(aabb, Color.LIGHTBLUE, 0.3f);
				*///?}
				drew.add(pos);
			}
		}

		// Render nearby exchanges (highlighted with the "Highlight Nearby" button)
		// Only render if not already rendered above (for backwards compatibility)
		// For 1.21.11+: render through walls using NO_DEPTH_TEST pipeline
		// For 1.21.6-1.21.10: render with depth testing (blocked by walls), dot overlay shows positions
		// For <1.21.6: render through walls using RenderSystem.disableDepthTest()
		if (!nearbyActive) {
			var nearbyResult = mod.highlightManager.nearbySource.getSearchResult();
			if (nearbyResult != null && nearbyResult.ts > now - hourMs) {
				//? if >=1.21.11 {
				// Use a separate BufferSource for nearby boxes so we fully control the flush
				var nearbyBuffer = new ByteBufferBuilder(1024);
				try {
					var nearbyConsumers = MultiBufferSource.immediate(nearbyBuffer);
					for (var exchange : nearbyResult.exchanges) {
						if (drew.contains(exchange.pos)) continue; // already drawn from other sources
						if (exchange.pos.block().distSqr(mc.player.blockPosition()) > range * range) continue;

						// Determine color based on stock and update time
						// Out of stock = RED, outdated (> 1 week) = ORANGE, recent & stocked = green-blue mixture
						var color = exchange.stock <= 0 ? Color.RED
								: exchange.time < now - Utils.weekMs ? Color.ORANGE
								: new Color(0, 1f, 0.5f);

						// inflate 0.01 to show above barrel without z fighting
						var aabb = new AABB(exchange.pos.block()).inflate(0.01);
						renderFilledBox(matrices, nearbyConsumers, aabb, color, 0.3f, true);
						drew.add(exchange.pos);
					}
					// Flush and close our dedicated buffer
					((MultiBufferSource.BufferSource) nearbyConsumers).endBatch();
				} finally {
					nearbyBuffer.close();
				}
				//?} else if >=1.21.6 {
				/*// For 1.21.6-1.21.10: render with depth testing (blocked by walls)
				// Dot overlay (OverlayRender) shows positions through walls
				for (var exchange : nearbyResult.exchanges) {
					if (drew.contains(exchange.pos)) continue; // already drawn from other sources
					if (exchange.pos.block().distSqr(mc.player.blockPosition()) > range * range) continue;

					// Determine color based on stock and update time
					// Out of stock = RED, outdated (> 1 week) = ORANGE, recent & stocked = green-blue mixture
					var color = exchange.stock <= 0 ? Color.RED
							: exchange.time < now - Utils.weekMs ? Color.ORANGE
							: new Color(0, 1f, 0.5f);

					// inflate 0.01 to show above barrel without z fighting
					var aabb = new AABB(exchange.pos.block()).inflate(0.01);
					renderFilledBox(matrices, consumers, aabb, color, 0.3f);
					drew.add(exchange.pos);
				}
				*///?}
			}
		}


		//? if >=1.21.6 {
		matrices.popPose();
		//?} else {
		/*// cleanup
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);

		modelViewStack.popMatrix();
		*///?}
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
		public static final Color MAGENTA = new Color(1, 0, 1);
	}

	//? if >=1.21.6 {
	/**
	 * Renders a filled box using the 1.21.6+ rendering API.
	 */
	//? if >=1.21.11 {
	private static void renderFilledBox(PoseStack matrices, MultiBufferSource consumers, AABB box, Color color, float alpha, boolean throughBlocks) {
	//?} else {
	/*private static void renderFilledBox(PoseStack matrices, MultiBufferSource consumers, AABB box, Color color, float alpha) {
	boolean throughBlocks = false;
	*///?}
		//? if >=1.21.11 {
		VertexConsumer vc = consumers.getBuffer(throughBlocks ? getNoDepthFilledBox() : RenderTypes.debugFilledBox());
		PoseStack.Pose pose = matrices.last();
		float r = color.r, g = color.g, b = color.b, a = alpha;
		float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
		float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;
		// Bottom face (y = minY)
		vc.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a);
		vc.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a);
		vc.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a);
		vc.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a);
		// Top face (y = maxY)
		vc.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a);
		vc.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a);
		vc.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a);
		vc.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a);
		// Front face (z = minZ)
		vc.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a);
		vc.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a);
		vc.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a);
		vc.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a);
		// Back face (z = maxZ)
		vc.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a);
		vc.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a);
		vc.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a);
		vc.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a);
		// Left face (x = minX)
		vc.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a);
		vc.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a);
		vc.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a);
		vc.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a);
		// Right face (x = maxX)
		vc.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a);
		vc.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a);
		vc.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a);
		vc.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a);
		//?} else {
		/*VertexConsumer vertexConsumer = consumers.getBuffer(RenderType.debugFilledBox());
		ShapeRenderer.addChainedFilledBoxVertices(
				matrices,
				vertexConsumer,
				box.minX, box.minY, box.minZ,
				box.maxX, box.maxY, box.maxZ,
				color.r, color.g, color.b, alpha
		);
		*///?}
	}
	//?} else {
	/*private static void renderFilledBox(AABB box, Color color, float a) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

		//? if <=1.21.3 {
		/^RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
		^///?} else {
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
	*///?}
}

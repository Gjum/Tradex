# Minecraft 1.21.8 Rendering API Migration Task

## Context
You are tasked with updating a Minecraft Fabric mod from 1.21.4 to 1.21.8. The configuration files have already been updated. The ONLY remaining issue is migrating the rendering code to be compatible with Minecraft 1.21.6+ rendering changes.

## Background: Minecraft 1.21.6+ Rendering Changes
Starting in Minecraft 1.21.6, Mojang began separating the rendering pipeline into two stages:
1. **Extraction stage**: All renderable data is separated from the game
2. **Render phase**: The previously extracted data is rendered

Many methods in `RenderSystem` have been removed without direct replacement. The same capabilities exist by combining the new **RenderPipelines** with **RenderLayers**.

## Files Requiring Changes

### 1. `src/main/java/io/github/gjum/mc/tradex/Render.java`
**Purpose**: Renders colored transparent boxes around shop chest locations in the 3D world

**Current Implementation** (1.21.4):
- Uses `WorldRenderContext` from Fabric API's `WorldRenderEvents.AFTER_TRANSLUCENT`
- Renders filled semi-transparent boxes at block positions
- Uses deprecated RenderSystem methods:
  - `RenderSystem.getModelViewStack()` - Gets the model-view matrix stack
  - `RenderSystem.enableBlend()` - Enables alpha blending for transparency
  - `RenderSystem.disableDepthTest()` - Disables depth testing to render through blocks
  - `RenderSystem.enableDepthTest()` - Re-enables depth testing
  - `RenderSystem.depthMask(true)` - Controls depth buffer writing
  - `RenderSystem.setShader(CoreShaders.POSITION_COLOR)` - Sets the shader for colored vertices

**Rendering Logic**:
```java
public static void render(WorldRenderContext context) {
    // Gets camera position and sets up matrix transformation
    Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
    Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
    modelViewStack.pushMatrix();
    modelViewStack.mul(context.matrixStack().last().pose());
    modelViewStack.translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);
    
    // Enable transparency and render through blocks
    RenderSystem.enableBlend();
    RenderSystem.disableDepthTest();
    
    // Render colored boxes at shop locations
    for (shop locations) {
        AABB aabb = new AABB(pos.block()).inflate(0.01);
        renderFilledBox(aabb, color, 0.3f); // 0.3f = 30% alpha
    }
    
    // Restore render state
    RenderSystem.enableDepthTest();
    RenderSystem.depthMask(true);
    modelViewStack.popMatrix();
}

private static void renderFilledBox(AABB box, Color color, float alpha) {
    Tesselator tesselator = Tesselator.getInstance();
    BufferBuilder bufferBuilder = tesselator.begin(
        VertexFormat.Mode.TRIANGLE_STRIP, 
        DefaultVertexFormat.POSITION_COLOR
    );
    
    RenderSystem.setShader(CoreShaders.POSITION_COLOR);
    
    // Builds geometry for all 6 faces of the box
    bufferBuilder.addVertex(x, y, z).setColor(r, g, b, alpha);
    // ... more vertices ...
    
    BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
}
```

**Colors Used**: GREEN (active), YELLOW (incomplete), GRAY (old), LIGHTBLUE (search results)

### 2. `src/main/java/io/github/gjum/mc/gui/ScrollBox.java`
**Purpose**: GUI component that clips child content within a scrollable area

**Current Implementation** (1.21.4):
```java
@Override
public void draw(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
    context.flush();
    
    double guiScale = mc.getWindow().getGuiScale();
    RenderSystem.enableScissor(
        (int) (guiScale * getPos().x),
        (int) (guiScale * (mc.screen.height - bottom)),
        (int) (guiScale * getInnerWidth()),
        (int) (guiScale * getInnerHeight())
    );
    
    child.draw(context, mouse, winSize, partialTicks);
    
    context.flush();
    RenderSystem.disableScissor();
}
```

**Note**: Check if `enableScissor`/`disableScissor` still exist in 1.21.6+. These may still be valid.

## Research Required

### 1. Investigate Available APIs in 1.21.8
- What RenderLayer types are available for transparent colored rendering?
- Does `GuiGraphics.enableScissor()` exist as a replacement for `RenderSystem.enableScissor()`?
- What is the proper way to handle matrix transformations without `RenderSystem.getModelViewStack()`?
- How to control blend modes and depth testing with the new pipeline?

### 2. Find Example Code
Look for:
- Fabric mods that render transparent boxes/overlays in world space (similar to Xaero's Minimap waypoints or JourneyMap markers)
- Fabric API rendering examples for 1.21.6+
- WorldRenderContext usage patterns in 1.21.6+

### 3. Identify Replacement Patterns
For each deprecated method, determine:
- Is there a direct replacement?
- Do we need to use RenderLayer/RenderType instead?
- Are there new methods on WorldRenderContext or GuiGraphics?

## Migration Requirements

### Must Preserve Functionality:
1. **Render transparent colored boxes** at 3D world positions
2. **Render through blocks** (disable depth test)
3. **Multiple colors** with alpha transparency (GREEN, YELLOW, GRAY, LIGHTBLUE at 30% opacity)
4. **Integration with Fabric WorldRenderEvents.AFTER_TRANSLUCENT**
5. **GUI scissor clipping** for the scroll box

### Code Quality:
- Use proper 1.21.8 APIs (no deprecated methods)
- Maintain performance (this renders every frame)
- Follow Minecraft/Fabric conventions
- Keep code similar structure for maintainability

## Your Task

1. **Research the 1.21.8 rendering APIs**
   - Find the correct RenderLayer/RenderType for colored transparent boxes
   - Determine matrix transformation approach without RenderSystem.getModelViewStack()
   - Identify blend mode and depth test configuration methods

2. **Update `Render.java`**
   - Replace all deprecated RenderSystem calls
   - Maintain the same visual output (colored transparent boxes through walls)
   - Preserve the render event hook point (WorldRenderEvents.AFTER_TRANSLUCENT)

3. **Update `ScrollBox.java` if needed**
   - Check if RenderSystem.enableScissor/disableScissor still exist
   - If removed, find and implement the replacement API

4. **Test compilation**
   - Ensure code compiles for Minecraft 1.21.8
   - No deprecated API usage warnings

## Deliverables

Provide the complete updated code for:
1. `src/main/java/io/github/gjum/mc/tradex/Render.java` (full file)
2. `src/main/java/io/github/gjum/mc/gui/ScrollBox.java` (full file, only if changes needed)

Include:
- Explanation of what APIs replaced the deprecated ones
- Any imports that changed
- Notes on behavioral differences (if any)

## Project Structure Reference

```
build.gradle.kts - Uses Fabric Loom, modImplementation for fabric-rendering-v1
fabric.mod.json - Client-side mod, mixins registered
TradexMod.java - Registers WorldRenderEvents.AFTER_TRANSLUCENT.register(mod::render)
```

**Dependencies available**:
- `fabric-rendering-v1` (already included in build.gradle.kts)
- Full Fabric API access if needed for other modules
- Minecraft 1.21.8 + Yarn mappings

## Starting Point Hints

Consider investigating:
- `WorldRenderContext.worldRenderer()` and its methods
- `RenderType` or `RenderLayer` for transparent colored quads
- `PoseStack` from the context instead of RenderSystem's model-view stack
- Whether blend/depth configuration moved to RenderType definitions
- `GuiGraphics.enableScissor()` as a potential replacement

Good luck! Focus on finding the modern 1.21.8 equivalents that achieve the same visual result.

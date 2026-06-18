plugins {
    id("dev.kikugie.stonecutter")
    // id("me.modmuss50.mod-publish-plugin") version "0.8.+" apply false // Publishes builds to hosting websites
}

stonecutter active "26.1"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"" + property("mod.version") + "\";"
    swaps["minecraft"] = "\"" + node.metadata.version + "\";"
    // constants["release"] = property("mod.id") != "template" // TODO git status
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        string(current.parsed >= "26.1") {
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace("registerKeyBinding", "registerKeyMapping")
            replace("WorldRenderEvents", "LevelRenderEvents")
            replace("WorldRenderContext", "LevelRenderContext")
            replace("classTweaker v1 named", "classTweaker v1 official")
        }
    }
}

/*
// Make newer versions be published last
stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}
 */

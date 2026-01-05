plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.10+" apply false
    // id("me.modmuss50.mod-publish-plugin") version "0.8.+" apply false // Publishes builds to hosting websites
}

stonecutter active "1.21.8"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"" + property("mod.version") + "\";"
    swaps["minecraft"] = "\"" + node.metadata.version + "\";"
    // constants["release"] = property("mod.id") != "template" // TODO git status
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String
}

/*
// Make newer versions be published last
stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}
 */

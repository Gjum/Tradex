import kotlin.math.abs

plugins {
    `maven-publish`
    id("fabric-loom")
    // id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${stonecutter.current.version}"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://maven.fabricmc.net/")

    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }

    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.version}")

    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
//    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    for (it in listOf(
        "fabric-command-api-v2",
        "fabric-key-binding-api-v1",
        "fabric-lifecycle-events-v1",
        "fabric-rendering-v1",
    )) modImplementation(fabricApi.module(it, property("deps.fabric_api") as String))

    testImplementation("net.fabricmc:fabric-loader-junit:${property("deps.fabric_loader")}")
    testImplementation(sourceSets.main.get().output)
}

loom {
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true")
        runDir = "../../run" // Shares the run directory between versions
    }
    
    // Enable Microsoft authentication for multiplayer
    runConfigs.configureEach {
        programArgs("--username", System.getProperty("username", "Dev"))
        if (System.getProperty("fabric-loom.dev.auth", "false").toBoolean()) {
            property("fabric.loom.msa.enabled", "true")
        }
    }
    // Additional debug run configuration: starts client with JDWP enabled on a per-project port
    runConfigs.create("debugClient") {
        ideConfigGenerated(true)
        environment = "client"
        mainClass = "net.minecraft.client.main.Main"
        // Allow overriding the debug port with -PdebugPort=<port> when invoking Gradle
        // Default: compute a stable per-project port to avoid collisions when multiple subprojects
        val defaultPortBase = 5005
        val computedPort = (defaultPortBase + (abs(project.path.hashCode()) % 1000))
        val debugPort = if (project.hasProperty("debugPort")) project.property("debugPort").toString().toInt() else computedPort
        // Allow making the JVM suspend until debugger attaches via -PdebugSuspend=true
        val suspendFlag = if (project.hasProperty("debugSuspend") && project.property("debugSuspend").toString().lowercase() in listOf("1","y","yes","true")) "y" else "n"
        vmArgs(
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=$suspendFlag,address=127.0.0.1:$debugPort"
        )
        runDir = "../../run"
    }
}

java {
    withSourcesJar()
    val requiresJava21: Boolean = stonecutter.eval(stonecutter.current.version, ">=1.20.6")
    val javaVersion: JavaVersion =
        if (requiresJava21) JavaVersion.VERSION_21
        else JavaVersion.VERSION_17
    targetCompatibility = javaVersion
    sourceCompatibility = javaVersion
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))

        val props = mapOf(
            "mod_id" to project.property("mod.id"),
            "mod_name" to project.property("mod.name"),
            "mod_version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep")
        )

        filesMatching("fabric.mod.json") { expand(props) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

tasks.test {
    useJUnitPlatform()
}

sourceSets {
    test {
        compileClasspath += sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
    }
}

/*
publishMods {
    file = tasks.remapJar.get().archiveFile
    additionalFiles.from(tasks.remapSourcesJar.get().archiveFile)
    displayName = "${mod.name} ${mod.version} for $mcVersion"
    version = mod.version
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = providers.environmentVariable("MODRINTH_TOKEN")
        .getOrNull() == null || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth").toString()
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add(mcVersion)
        requires {
            slug = "fabric-api"
        }
    }

    curseforge {
        projectId = property("publish.curseforge").toString()
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.add(mcVersion)
        requires {
            slug = "fabric-api"
        }
    }
}
*/
/*
publishing {
    repositories {
        maven("...") {
            name = "..."
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${mod.id}"
            artifactId = mod.version
            version = mcVersion

            from(components["java"])
        }
    }
}
*/

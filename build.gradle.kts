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
    maven("https://maven.shedaniel.me/") { name = "Shedaniel" }
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

    modApi("me.shedaniel.cloth:cloth-config-fabric:${property("deps.cloth_config")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    modImplementation("maven.modrinth:modmenu:${property("deps.modmenu")}")

    // Do not add Fabric's JUnit launcher to test classpath — it tries to initialise
    // the Fabric loader and loads all mods which breaks plain unit tests (namespace mismatch).
    // Use plain JUnit on the test classpath instead.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.3")
    testImplementation(sourceSets.main.get().output)
}

loom {
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        val chosenRunDir = "../../versions/${stonecutter.current.version}" // Use version-specific run dir (uses versions/<version>/mods)
        runDir = chosenRunDir
        // Log the chosen runDir so Gradle console shows which run directory is used
        logger.lifecycle("Loom runDir set to: $chosenRunDir")
        // Enable DevAuth and point it at a per-version config directory so it can create config
        val devauthEnabled = "true"
        val devauthConfigDir = file("$chosenRunDir/.devauth").absolutePath
        vmArgs("-Ddevauth.enabled=$devauthEnabled")
        vmArgs("-Ddevauth.configDir=$devauthConfigDir")
        logger.lifecycle("DevAuth: enabled=$devauthEnabled, configDir=$devauthConfigDir")
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

    // Sources JAR should not include fabric.mod.json since it contains placeholders
    // The actual mod metadata is in the main remapped JAR
    named<Jar>("sourcesJar") {
        from("src/main/resources") {
            exclude("fabric.mod.json")
        }
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
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
    // Ensure tests run on the plain test runtime classpath (exclude Loom/ remapped mod jars)
    classpath = sourceSets.test.get().runtimeClasspath
    testLogging {
        events("passed", "skipped", "failed")
    }
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

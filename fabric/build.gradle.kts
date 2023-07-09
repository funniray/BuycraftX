import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("fabric-loom")
    id("java")
}

object Versions {
    const val minecraft_version = "1.19.2"

    // check this on https://modmuss50.me/fabric.html
    const val yarn_mappings = "1.19.2+build.28"
    const val loader_version = "0.14.18"
    const val fabric_version = "0.76.0+1.19.2"

    const val permission_version = "0.2-SNAPSHOT"
}

val shadowJar: ShadowJar by tasks

tasks {
    build {
        dependsOn(remapJar)
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        archiveBaseName.set("buycraftx-${project.name}")
    }

    named<ShadowJar>("shadowJar") {
        configurations = listOf(project.configurations.shadow.get())

        relocate("com.google.gson", "net.buycraft.plugin.internal.gson")
        relocate("com.google.common", "net.buycraft.plugin.internal.common")
        relocate("okhttp3", "net.buycraft.plugin.internal.okhttp3")
        relocate("okio", "net.buycraft.plugin.internal.okio")
        relocate("retrofit2", "net.buycraft.plugin.internal.retrofit2")
        relocate("com.fasterxml.jackson", "net.buycraft.plugin.internal.jackson")
        relocate("org.slf4j", "net.buycraft.plugin.internal.slf4j")
        relocate("org.jetbrains.annotations", "net.buycraft.plugin.internal.jetbrains")
        relocate("net.buycraft.plugin", "net.buycraft.plugin")

        minimize()
    }

    processResources {
        files("fabric.mod.json") {
            expand(
                "pluginVersion" to project.version,
                "loaderVersion" to ">=${Versions.loader_version}",
                "permissionsVersion" to ">=${Versions.permission_version}",
                "minecraftVersion" to Versions.minecraft_version
            )
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft_version}")
    mappings("net.fabricmc:yarn:${Versions.yarn_mappings}:v2")

    modImplementation("net.fabricmc:fabric-loader:${Versions.loader_version}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${Versions.fabric_version}")
    modImplementation("me.lucko:fabric-permissions-api:${Versions.permission_version}")

    shadow(project(":plugin-shared"))
}

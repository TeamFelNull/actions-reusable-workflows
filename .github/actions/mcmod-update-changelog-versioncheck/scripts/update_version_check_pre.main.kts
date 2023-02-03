#!/usr/bin/env kotlin
/*
Forgeのバージョン確認Jsonを更新
*/

@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:DependsOn("com.vdurmont:semver4j:3.1.0")

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

val gson: Gson = Gson()

val rawVersion = args[0]
val rawChangeLog = args[1]
val repo = args[2]

val wrkDir: Path = System.getenv("GITHUB_WORKSPACE")?.let(Path::of) ?: Paths.get("./")

val gp: Map<String, String> = wrkDir.resolve("gradle.properties")
        .let { Files.lines(it) }
        .filter { it.isNotBlank() }
        .filter { !it.trim().startsWith("#") }
        .map { it.split("=") }
        .collect(Collectors.toMap({ it[0].trim() }, { it[1].trim() }))

val version = rawVersion.substring(1)
val defaultHomepage = "https://github.com/$repo/releases"
var mcVersions = setOf(gp["minecraft_version"] ?: gp["mc_version"])
gp["support_versions"]?.let { str -> mcVersions += str.split(",").map { it.trim() } }

val changeLog = rawChangeLog.let {
    val line = it.lines().filter { tis -> tis.isNotEmpty() }
    var ret = ""
    for (i in line.indices) {
        val lie = line[i]
        if (lie.startsWith("### ") && (i == line.size - 1 || line[i + 1].startsWith("### ")))
            continue

        ret += if (lie.startsWith("### ")) {
            lie.substring("### ".length) + "\n"
        } else {
            lie + "\n"
        }
    }
    return@let ret
}

val recommended = gp["release_type"]?.equals("release", true) ?: true

val jo = JsonObject()
jo.addProperty("version", version)
jo.addProperty("default_homepage", defaultHomepage)

val vja = JsonArray()
mcVersions.forEach { vja.add(it) }

jo.add("mc_versions", vja)
jo.addProperty("change_log", changeLog)
jo.addProperty("recommended", recommended)

print(gson.toJson(jo))
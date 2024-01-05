#!/usr/bin/env kotlin
/*
Forgeのバージョン確認Jsonを更新
*/

@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:DependsOn("com.vdurmont:semver4j:3.1.0")

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.SemverException
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths

val gson: Gson = GsonBuilder().setPrettyPrinting().create()

val revJo: JsonObject = gson.fromJson(args[0], JsonObject::class.java)

val version: String = revJo.get("version").asString
val defaultHomepage: String = revJo.get("default_homepage").asString
val mcVersions = revJo.getAsJsonArray("mc_versions").asList().map { it.asString }.toSet()
val changeLog: String = revJo.get("change_log").asString
val recommended: Boolean = revJo.get("recommended").asBoolean

val wrkDir: Path = System.getenv("GITHUB_WORKSPACE")?.let(Path::of) ?: Paths.get("./")
val vcName = args[1]
val vcFile: File = wrkDir.resolve(vcName).toFile()

var jo: JsonObject

if (vcFile.exists()) {
    FileReader(vcFile, StandardCharsets.UTF_8).use { r -> BufferedReader(r).use { jo = gson.fromJson(it, JsonObject::class.java) } }
} else {
    jo = JsonObject()
    jo.addProperty("homepage", defaultHomepage)
}

val promosJo = jo.getAsJsonObject("promos") ?: JsonObject()

for (mcVersion in mcVersions) {
    val vEntry = jo.getAsJsonObject(mcVersion) ?: JsonObject()
    vEntry.addProperty(version, changeLog)
    jo.add(mcVersion, vEntry)

    promosJo.addProperty("$mcVersion-latest", version)

    var flg = true
    val pjName = "$mcVersion-recommended"

    if (promosJo.has(pjName) && promosJo.isJsonPrimitive && promosJo.getAsJsonPrimitive(pjName).isString) {
        try {
            flg = !Semver(promosJo.getAsJsonPrimitive(pjName).asString).isStable
        } catch (_: SemverException) {
        }
    }

    if (recommended || flg)
        promosJo.addProperty(pjName, version)
}

jo.add("promos", promosJo)

FileWriter(vcFile, StandardCharsets.UTF_8).use { w -> BufferedWriter(w).use { gson.toJson(jo, it) } }
#!/usr/bin/env kotlin

/**
 * プロジェクトの検証
 * @author MORIMORI0317
 */

@file:Import("../../../../gradle-properties-loader.main.kts")

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

val tag = args[0]
val changeLog = args[1]
val version = tag.substring(1)

if (changeLog.lines().filter { it.trim().isNotEmpty() }
        .none { !it.trim().startsWith("### ") && it.trim().startsWith("- ") })
    throw Exception("Unwritten change log/更新ログが未記述です")

fun assertFile(pathStr: String) {
    if (!File(pathStr).exists())
        throw Exception("Required file does not exist/必要なファイルが存在しません: $pathStr")
}

assertFile("CHANGELOG.md")
assertFile("README.md")
assertFile("LICENSE")

val gp: Map<String, String> = getGradleProperties()

fun assertGradleProperties(name: String, orName1: String, orName2: String) {
    if (gp[name] == null && gp[orName1] == null && gp[orName2] == null)
        throw Exception("Required value does not exist in gradle.properties/gradle.propertiesに必要な値が存在しません: $name,$orName1,$orName2")
}

fun assertGradleProperties(name: String, orName: String) {
    if (gp[name] == null && gp[orName] == null)
        throw Exception("Required value does not exist in gradle.properties/gradle.propertiesに必要な値が存在しません: $name,$orName")
}

fun assertGradleProperties(name: String) {
    if (gp[name] == null)
        throw Exception("Required value does not exist in gradle.properties/gradle.propertiesに必要な値が存在しません: $name")
}

assertGradleProperties("minecraft_version", "mc_version")
assertGradleProperties("archives_base_name", "mod_id")
assertGradleProperties("mod_display_name", "mod_name", "archives_name")
assertGradleProperties("version", "mod_version")
assertGradleProperties("release_type")

var modVersion = getModVersion(gp)

if (version != modVersion)
    throw Exception("gradle.properties version and tag version do not match/gradle.propertiesのバージョンとタグのバージョンが一致しません: $version")

var releaseType = gp["release_type"]

if (!listOf("release", "beta", "alpha").contains(releaseType))
    throw Exception("Release type not valid/リリースタイプが有効ではありません: $releaseType")

println("Project verification completed!")

#!/usr/bin/env kotlin

/**
 * "gradle.properties"の読み書き関係
 * @author MORIMORI0317
 */

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors


/**
 * 作業ディレクトリ
 */
val wrkDir: Path = System.getenv("GITHUB_WORKSPACE")?.let(Path::of) ?: Paths.get("./")

/**
 * "gradle.properties"のファイルパス
 */
val gpPath: Path = wrkDir.resolve("gradle.properties")

/**
 * "gradle.properties"を読み込んで取得する
 */
fun getGradleProperties(): Map<String, String> {
    return gpPath
        .let { Files.lines(it) }
        .filter { it.isNotBlank() }
        .map {
            var ret = it
            val commentIndex = it.indexOf("#")

            if (commentIndex >= 0) {
                ret = it.substring(0, commentIndex)
            }
            ret
        }
        .filter { it.contains("=") }
        .map { it.split("=") }
        .collect(Collectors.toMap({ it[0].trim() }, { it[1].trim() }))
}

/**
 * "gradle.properties"を書き換える
 */
fun setGradleProperties(key: String, value: String) {
    val sb = StringBuilder()

    gpPath.let { Files.lines(it) }
        .forEach {
            var ret = it

            var noComment = it
            val commentIndex = it.indexOf("#")

            if (commentIndex >= 0) {
                noComment = it.substring(0, commentIndex)
            }

            if (noComment.trim().isNotEmpty() && noComment.contains("=")) {
                val splited = noComment.split("=")
                val entry: Pair<String, String> = Pair(splited[0].trim(), splited[1].trim())

                if (entry.first == key) {
                    val valueSt = it.indexOf("=") + 1
                    val valueEn = it.substring(valueSt).indexOf("#")

                    val valueText = if (valueEn >= 0) {
                        it.substring(valueSt, valueSt + valueEn)
                    } else {
                        it.substring(valueSt)
                    }

                    ret = it.substring(0, valueSt) + (valueText.replace(entry.second, value))

                    if (valueEn >= 0) {
                        ret += it.substring(valueSt + valueEn)
                    }
                }
            }

            sb.append(ret).append("\n")

            Files.writeString(gpPath, sb.toString())
        }
}

/**
 * MODのバージョンを取得
 */
fun getModVersion(gp: Map<String, String> = getGradleProperties()): String? {
    return gp["version"] ?: gp["mod_version"]
}

/**
 * MODのバージョンを書き換える
 */
fun setModVersion(version: String) {
    val gp = getGradleProperties()

    var versionKey: String? = null

    if (gp["version"] != null) {
        versionKey = "version"
    } else if (gp["mod_version"] != null) {
        versionKey = "mod_version"
    }

    if (versionKey != null) {
        setGradleProperties(versionKey, version)
    } else {
        throw Exception("MOD version information does not exist in gradle.properties/gradle.propertiesにMODのバージョンを情報が存在しません")
    }
}
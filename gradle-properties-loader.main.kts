#!/usr/bin/env kotlin

/**
 * "gradle.properties"から値を取得する
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
        .filter { !it.trim().startsWith("#") }
        .map { it.split("=") }
        .collect(Collectors.toMap({ it[0].trim() }, { it[1].trim() }))
}


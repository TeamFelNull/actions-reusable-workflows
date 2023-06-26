#!/usr/bin/env kotlin

/**
 * 次の動作確認用ビルドのバージョンを求めて更新する
 *
 * @author MORIMORI0317
 */

@file:Import("../../../../gradle-properties-loader.main.kts")

@file:DependsOn("com.vdurmont:semver4j:3.1.0")

import com.vdurmont.semver4j.Semver


/**
 * 現在のバージョン
 */
val currentVersion = Semver("1.2.3-beta.1+pre.39") //1.2.3-beta.1+pre.1

var nextVersion: Semver

// 次のビルドバージョンを求める
if (currentVersion.build == null) {
    nextVersion = currentVersion.withBuild("pre.1")
} else {
    val splitBuild = currentVersion.build.split(".")
    val buildNum = splitBuild[splitBuild.size - 1].toIntOrNull()

    if (buildNum == null) {
        throw Exception("Invalid existing build version/既存のビルドバージョンが不正です: ${currentVersion.build}")
    }

    val sb = StringBuilder()

    for (i in 0 until splitBuild.size - 1) {
        sb.append(splitBuild[i]).append(".")
    }

    sb.append(buildNum + 1)

    nextVersion = currentVersion.withBuild(sb.toString())
}

setModVersion(nextVersion.toString())
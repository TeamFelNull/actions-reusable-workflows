#!/usr/bin/env kotlin

/**
 * 現在のブランチ、デフォルトブランチ、タグ一覧から動作確認用ビルドが実行できるか検証
 * @author MORIMORI0317
 */

@file:Import("../../../../gradle-properties-loader.main.kts")

@file:DependsOn("com.vdurmont:semver4j:3.1.0")

import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.SemverException
import java.util.regex.Pattern

/**
 * 現在のブランチ
 */
val currentBranch = args[0]

/**
 * デフォルトブランチ
 */
val defaultBranch = args[1]

/**
 * タグ一覧
 */
val allTags = args[2]

/**
 * 最新のタグ
 */
val latestTag = args[3]

/**
 * セムバージョンのパターン
 */
val semVerPatten: Pattern =
        Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?\$");

/**
 * 現在のブランチがメインのバージョンかどうか
 */
val mainVersion = currentBranch == defaultBranch

/**
 * すべてのバージョン一覧
 */
val allVersions = allTags.lines()
        .filter { it.isNotEmpty() }
        .map { it.substring(1) }
        .filter { semVerPatten.matcher(it).matches() }

/**
 * プロジェクトのバージョン
 */
var projectVersion = getModVersion()

println("Current Branch: $currentBranch")
println("Default Branch: $defaultBranch")
println("Project Version: $projectVersion")
println("Latest Tag: $latestTag")
println("Main Version: $mainVersion")
println("All Versions: $allVersions")
println()

/**
 * すべてのセムバージョン
 */
val allSemVer = allVersions.map {
    try {

        val sem = Semver(it)
        if (sem.suffixTokens.isNotEmpty()) {
            if (sem.suffixTokens.size != 2 || sem.suffixTokens[0].all { r -> r.isDigit() } || sem.suffixTokens[1].any { r -> !r.isDigit() })
                return@map null;
        }
        return@map sem
    } catch (e: SemverException) {
        return@map null;
    }
}
        .filterNotNull()
        .distinct()

// プロジェクトのバージョンが存在しているか確認
if (projectVersion == null) {
    throw Exception("Project version information does not exist/プロジェクトのバージョンの情報が存在しません")
}

// プロジェクトのバージョンが不正ではないか確認
if (projectVersion?.let { semVerPatten.matcher(it).matches() } == false) {
    throw Exception("Invalid project version/プロジェクトのバージョンが不正です: $projectVersion")
}

val projectSemVer = Semver(projectVersion)

// 現在のブランチから推測される過去のバージョンを取得
var beforeVersions = allSemVer

if (!mainVersion) {
    var latestVersion: Semver? = null

    if (latestTag.isNotEmpty()) {
        try {
            latestVersion = Semver(latestTag.substring(1))
        } catch (_: SemverException) {
        }
    }

    if (latestVersion != null) {
        beforeVersions = beforeVersions
                .filter { it.isLowerThanOrEqualTo(latestVersion) }
    } else {
        beforeVersions = listOf()
    }
}

// プロジェクトのバージョンが一番新しいバージョンより新しいかどうか確認
if (beforeVersions.isNotEmpty()) {
    val mostGreaterVersion = beforeVersions.max()

    if (projectSemVer.isLowerThanOrEqualTo(mostGreaterVersion)) {
        throw Exception("Project version is same or older than latest version/プロジェクトのバージョンが最新のバージョンと同じか古いです: ProjectVersion[$projectSemVer] LatestVersion[$mostGreaterVersion]")
    }
}

println("Version verification completed!")

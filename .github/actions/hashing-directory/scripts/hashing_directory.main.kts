#!/usr/bin/env kotlin
/**
 * 指定したフォルダ内のファイルすべてのハッシュを記述したJsonファイルを生成する
 * @author MORIMORI0317
 */

@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:DependsOn("com.google.guava:guava:32.0.1-jre")
@file:DependsOn("org.apache.commons:commons-lang3:3.12.0")

import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 処理開始時間
 */
val startTime = System.currentTimeMillis()

/**
 * ハッシュ生成対象フォルダのパス
 */
val targetDir: Path = Paths.get(args[0])

/**
 * ハッシュを情報を出力するファイル名
 */
val hashJsonName = "hash"


/**
 * Json処理用GSOオォン！アォン！
 */
val gson: Gson = GsonBuilder().setPrettyPrinting().create()

/**
 * 処理に利用するExecutor
 */
val executor: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
        BasicThreadFactory.Builder().namingPattern("process-worker-%d").daemon(true).build())

/**
 * 非同期でファイルごとのハッシュを生成して、Jsonにして返す
 */
fun genHashJsonAsync(file: File): CompletableFuture<JsonObject> {
    val cfs: MutableList<CompletableFuture<Pair<String, String>>> = mutableListOf()

    return CompletableFuture.supplyAsync({
        file.readBytes()
    }, executor).thenComposeAsync({ data ->
        cfs.add(computeHashAsync("MD5", Hashing.md5(), data))
        cfs.add(computeHashAsync("SHA-1", Hashing.sha1(), data))
        cfs.add(computeHashAsync("SHA-256", Hashing.sha256(), data))
        cfs.add(computeHashAsync("SHA-512", Hashing.sha512(), data))

        CompletableFuture.allOf(*cfs.toTypedArray())
    }, executor).thenApplyAsync({ _ ->
        val hashJson = JsonObject()

        cfs.forEach {
            val ret = it.get()
            hashJson.addProperty(ret.first, ret.second)
        }

        hashJson
    }, executor)
}

/**
 * 非同期でハッシュを計算する
 */
fun computeHashAsync(name: String, hashFunction: HashFunction, data: ByteArray): CompletableFuture<Pair<String, String>> {
    return CompletableFuture.supplyAsync({
        Pair(name, hashFunction.hashBytes(data).toString())
    }, executor)
}

val retJson = JsonObject()
val cfs = LinkedList<CompletableFuture<Pair<String, JsonObject>>>()

val targetFile: File = targetDir.toFile()

if (!targetFile.exists())
    throw Exception("Target directory does not exist/対象のディレクトリが存在しません: ${targetDir.toAbsolutePath()}")

//非同期でハッシュを生成してJsonObjectとして出力する
targetFile.walk().forEach { file ->
    if (file.isDirectory)
        return@forEach

    cfs.add(genHashJsonAsync(file).thenApplyAsync({ jo ->
        var name = targetDir.relativize(file.toPath()).toString()
        name = name.replace("\\", "/")

        println("Generate hash: $name")

        Pair(name, jo)
    }, executor))
}

//非同期で出力した結果をJsonへ追加
cfs.forEach {
    val ret = it.get()
    retJson.add(ret.first, ret.second)
}


val hashFile = targetDir.resolve("$hashJsonName.json").toFile()

// Jsonをファイルに書き込む
FileOutputStream(hashFile).use { steam ->
    OutputStreamWriter(steam, StandardCharsets.UTF_8).use { writer ->
        gson.toJson(retJson, writer)
    }
}


val elapsedTime = System.currentTimeMillis() - startTime

println("Hash generation completed: ${elapsedTime}ms")
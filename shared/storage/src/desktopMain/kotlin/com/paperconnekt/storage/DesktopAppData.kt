package com.paperconnekt.storage

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.attribute.PosixFilePermissions

fun desktopAppDataDirectory(): Path =
    System.getProperty("paperconnekt.data.dir")?.let {
        require(it.isNotBlank()) { "paperconnekt.data.dir must not be blank." }
        Paths.get(it)
    } ?: Paths.get(System.getProperty("user.home"), ".paperconnekt-contest")

fun createPrivateDirectories(path: Path) {
    if (path.fileSystem.supportedFileAttributeViews().contains("posix")) {
        val permissions = PosixFilePermissions.fromString("rwx------")
        Files.createDirectories(path, PosixFilePermissions.asFileAttribute(permissions))
        Files.setPosixFilePermissions(path, permissions)
    } else {
        Files.createDirectories(path)
    }
}

fun makePrivateFile(path: Path) {
    if (path.fileSystem.supportedFileAttributeViews().contains("posix")) {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
    }
}

fun writePrivateFileAtomically(file: Path, content: ByteArray) {
    createPrivateDirectories(file.parent)
    val temporary = Files.createTempFile(file.parent, "save-", ".tmp")
    try {
        makePrivateFile(temporary)
        Files.write(temporary, content)
        try {
            Files.move(temporary, file, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, file, REPLACE_EXISTING)
        }
    } finally {
        Files.deleteIfExists(temporary)
    }
}

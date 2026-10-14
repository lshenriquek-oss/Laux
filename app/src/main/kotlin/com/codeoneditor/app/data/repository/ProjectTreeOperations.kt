package com.codeoneditor.app.data.repository

import com.codeoneditor.app.data.model.FileItem
import com.codeoneditor.core.editor.model.FileType
import com.codeoneditor.core.editor.syntax.FileTypeDetector
import com.codeoneditor.app.data.model.ProjectItem
import java.io.File

object ProjectTreeOperations {

fun findNodeByPath(list: List<FileItem>, path: String): FileItem? {
        for (item in list) {
            if (item.path == path) return item
            if (item.isDirectory) findNodeByPath(item.children, path)?.let { return it }
        }
        return null
    }

fun replaceNode(list: MutableList<FileItem>, id: String, transform: (FileItem) -> FileItem): Boolean {
        for (index in list.indices) {
            val item = list[index]
            if (item.id == id) {
                list[index] = transform(item)
                return true
            }
            if (item.isDirectory && replaceNode(item.children, id, transform)) return true
        }
        return false
    }

fun findNodeById(list: List<FileItem>, id: String): FileItem? {
        for (item in list) {
            if (item.id == id) return item
            if (item.isDirectory) {
                findNodeById(item.children, id)?.let { return it }
            }
        }
        return null
    }

fun updateDescendantPaths(list: List<FileItem>, parentId: String) {
        val parent = findNodeById(list, parentId) ?: return
        fun updateChildren(children: MutableList<FileItem>, parentPath: String) {
            for (index in children.indices) {
                val child = children[index]
                val newPath = "$parentPath/${child.name}"
                children[index] = child.copy(path = newPath)
                if (child.isDirectory) updateChildren(children[index].children, newPath)
            }
        }
        if (parent.isDirectory) updateChildren(parent.children, parent.path)
    }

fun deepCopyProject(project: ProjectItem): ProjectItem {
        fun copyFile(item: FileItem): FileItem = item.copy(
            children = item.children.map(::copyFile).toMutableList()
        )
        return project.copy(files = project.files.map(::copyFile).toMutableList())
    }

fun projectRootDir(project: ProjectItem, storageRootDir: File?): File? {
        project.rootPath?.takeIf { it.isNotBlank() }?.let { return File(it) }
        storageRootDir?.let { return File(it, project.name) }
        return null
    }

fun detectFileType(fileName: String, isDir: Boolean): FileType =
    if (isDir) FileType.OTHER else FileTypeDetector.detect(fileName)

fun addNodeToTree(list: List<FileItem>, parentId: String, newNode: FileItem): Boolean {
        for (item in list) {
            if (item.id == parentId && item.isDirectory) {
                item.children.add(newNode)
                return true
            }
            if (item.isDirectory && addNodeToTree(item.children, parentId, newNode)) {
                return true
            }
        }
        return false
    }

fun renameNodeInTree(list: List<FileItem>, fileId: String, newName: String, newType: FileType): Boolean {
        for (item in list) {
            if (item.id == fileId) {
                val idx = list.indexOf(item)
                val newPath = if (item.path.contains('/')) item.path.substringBeforeLast('/') + "/" + newName else newName
                (list as? MutableList)?.set(idx, item.copy(name = newName, path = newPath, fileType = newType))
                return true
            }
            if (item.isDirectory && renameNodeInTree(item.children, fileId, newName, newType)) {
                return true
            }
        }
        return false
    }

fun deleteNodeFromTree(list: MutableList<FileItem>, fileId: String): Boolean {
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.id == fileId) {
                iterator.remove()
                return true
            }
            if (item.isDirectory && deleteNodeFromTree(item.children, fileId)) {
                return true
            }
        }
        return false
    }

fun updateFileContentInTree(list: List<FileItem>, fileId: String, newContent: String): Boolean {
        for (item in list) {
            if (item.id == fileId) {
                val idx = list.indexOf(item)
                (list as? MutableList)?.set(idx, item.copy(content = newContent))
                return true
            }
            if (item.isDirectory && updateFileContentInTree(item.children, fileId, newContent)) {
                return true
            }
        }
        return false
    }

fun findFileByName(list: List<FileItem>, name: String): FileItem? {
        for (item in list) {
            if (item.name == name && !item.isDirectory) return item
            if (item.isDirectory) {
                val found = findFileByName(item.children, name)
                if (found != null) return found
            }
        }
        return null
    }

fun findFirstCodeFile(list: List<FileItem>): FileItem? {
        for (item in list) {
            if (!item.isDirectory) return item
            val found = findFirstCodeFile(item.children)
            if (found != null) return found
        }
        return null
    }
}

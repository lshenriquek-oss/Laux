package com.codeoneditor.core.editor.syntax

import com.codeoneditor.core.editor.model.FileType
import org.junit.Assert.assertEquals
import org.junit.Test

class SyntaxDetectorTest {
    @Test fun detectsAndroidFiles() {
        assertEquals(FileType.KOTLIN, SyntaxDetector.detect("MainActivity.kt"))
        assertEquals(FileType.GRADLE_KTS, SyntaxDetector.detect("build.gradle.kts"))
        assertEquals(FileType.XML, SyntaxDetector.detect("AndroidManifest.xml"))
        assertEquals(FileType.TOML, SyntaxDetector.detect("libs.versions.toml"))
    }
    @Test fun detectsCommonLanguages() {
        assertEquals(FileType.TYPESCRIPT, SyntaxDetector.detect("app.ts"))
        assertEquals(FileType.PYTHON, SyntaxDetector.detect("main.py"))
        assertEquals(FileType.RUST, SyntaxDetector.detect("main.rs"))
        assertEquals(FileType.CPP, SyntaxDetector.detect("native.cpp"))
    }
}

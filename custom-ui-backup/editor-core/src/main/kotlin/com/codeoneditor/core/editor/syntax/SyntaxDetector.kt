package com.codeoneditor.core.editor.syntax

import com.codeoneditor.core.editor.model.FileType

object SyntaxDetector {
    fun detect(fileName: String): FileType = FileTypeDetector.detect(fileName)
}

object FileTypeDetector {
    fun detect(fileName: String): FileType {
        val lower = fileName.lowercase()
        val ext = lower.substringAfterLast('.', "")
        return when {
            lower == "dockerfile" -> FileType.DOCKERFILE
            lower == "makefile" || lower == "gnumakefile" -> FileType.MAKEFILE
            lower == "cmakelists.txt" -> FileType.CMAKE
            lower == ".env" || lower.startsWith(".env.") -> FileType.ENV
            lower == ".gitignore" -> FileType.GITIGNORE
            lower == "androidmanifest.xml" -> FileType.XML
            lower.endsWith(".gradle.kts") -> FileType.GRADLE_KTS
            lower.endsWith(".gradle") -> FileType.GRADLE
            lower == "libs.versions.toml" || ext == "toml" -> FileType.TOML
            lower == "proguard-rules.pro" -> FileType.PROGUARD
            ext == "proguard" -> FileType.PROGUARD
            ext == "py" -> FileType.PYTHON
            ext == "js" || ext == "mjs" || ext == "cjs" -> FileType.JAVASCRIPT
            ext == "ts" -> FileType.TYPESCRIPT
            ext == "jsx" -> FileType.JSX
            ext == "tsx" -> FileType.TSX
            ext == "html" || ext == "htm" -> FileType.HTML
            ext == "css" -> FileType.CSS
            ext == "scss" -> FileType.SCSS
            ext == "sass" -> FileType.SASS
            ext == "less" -> FileType.LESS
            ext in setOf("cpp", "cc", "cxx", "hpp", "hh", "hxx") -> FileType.CPP
            ext == "c" || ext == "h" -> FileType.C
            ext == "cs" -> FileType.CSHARP
            ext == "rs" -> FileType.RUST
            ext == "go" -> FileType.GO
            ext == "kt" || ext == "kts" -> FileType.KOTLIN
            ext == "java" -> FileType.JAVA
            ext == "php" -> FileType.PHP
            ext == "swift" -> FileType.SWIFT
            ext == "m" || ext == "mm" -> FileType.OBJECTIVE_C
            ext == "dart" -> FileType.DART
            ext == "rb" -> FileType.RUBY
            ext == "scala" || ext == "sc" -> FileType.SCALA
            ext == "ex" || ext == "exs" -> FileType.ELIXIR
            ext == "erl" || ext == "hrl" -> FileType.ERLANG
            ext == "lua" -> FileType.LUA
            ext == "r" -> FileType.R
            ext == "jl" -> FileType.JULIA
            ext == "sql" -> FileType.SQL
            ext == "sh" || ext == "bash" -> FileType.BASH
            ext == "zsh" || ext == "fish" -> FileType.SHELL
            ext == "ps1" || ext == "psm1" -> FileType.POWERSHELL
            ext == "pl" || ext == "pm" -> FileType.PERL
            ext == "raku" || ext == "rakumod" -> FileType.RAKU
            ext == "awk" -> FileType.AWK
            ext == "tcl" -> FileType.TCL
            ext == "s" || ext == "asm" -> FileType.ASSEMBLY
            ext == "zig" -> FileType.ZIG
            ext == "d" -> FileType.D
            ext in setOf("f", "f90", "f95", "for") -> FileType.FORTRAN
            ext == "sol" -> FileType.SOLIDITY
            ext == "move" -> FileType.MOVE
            ext == "hs" || ext == "lhs" -> FileType.HASKELL
            ext == "clj" || ext == "cljs" -> FileType.CLOJURE
            ext == "lisp" || ext == "lsp" -> FileType.LISP
            ext == "fs" || ext == "fsx" -> FileType.FSHARP
            ext == "ml" || ext == "mli" -> FileType.OCAML
            ext == "pro" -> FileType.PROLOG
            ext == "xml" -> FileType.XML
            ext == "groovy" || ext == "gvy" -> FileType.GROOVY
            ext == "json" || ext == "jsonc" -> FileType.JSON
            ext == "yaml" || ext == "yml" -> FileType.YAML
            ext in setOf("properties", "ini", "cfg", "conf") -> FileType.PROPERTIES
            ext == "md" || ext == "markdown" -> FileType.MARKDOWN
            ext == "graphql" || ext == "gql" -> FileType.GRAPHQL
            ext == "cmake" -> FileType.CMAKE
            ext == "gd" -> FileType.GDSCRIPT
            ext == "hlsl" -> FileType.HLSL
            ext in setOf("glsl", "vert", "frag") -> FileType.GLSL
            ext == "shader" -> FileType.SHADERLAB
            else -> FileType.OTHER
        }
    }
}

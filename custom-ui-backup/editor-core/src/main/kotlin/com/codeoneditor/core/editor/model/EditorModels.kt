package com.codeoneditor.core.editor.model

enum class FileType {
    PYTHON, JAVASCRIPT, TYPESCRIPT, HTML, CSS, SCSS, SASS, LESS,
    JSX, TSX, CPP, C, CSHARP, RUST, GO, KOTLIN,
    JAVA, PHP, SWIFT, OBJECTIVE_C, DART, RUBY, SCALA, ELIXIR,
    ERLANG, LUA, R, JULIA, MATLAB, SQL, BASH, SHELL,
    POWERSHELL, PERL, RAKU, AWK, TCL, ASSEMBLY, ZIG, D,
    FORTRAN, SOLIDITY, MOVE, HASKELL, CLOJURE, LISP, FSHARP, OCAML,
    PROLOG, XML, GRADLE, GRADLE_KTS, GROOVY, JSON, YAML, TOML,
    PROPERTIES, MARKDOWN, GRAPHQL, CMAKE, DOCKERFILE, MAKEFILE, PROGUARD, ENV,
    GDSCRIPT, HLSL, GLSL, SHADERLAB, OTHER, KEYSTORE, GITIGNORE
}

data class EditorTab(
    val fileId: String,
    val fileName: String,
    val filePath: String,
    val fileType: FileType,
    val content: String,
    val isDirty: Boolean = false
)

data class EditorDocument(
    val fileId: String,
    val fileName: String,
    val filePath: String,
    val fileType: FileType,
    val content: String,
    val cursor: Int = 0,
    val selectionStart: Int = cursor,
    val selectionEnd: Int = cursor
)

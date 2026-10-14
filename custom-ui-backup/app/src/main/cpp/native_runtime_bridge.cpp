#include <jni.h>
#include <sys/stat.h>
#include <string>

namespace {
bool is_regular_file(const std::string& path) {
    struct stat info {};
    return stat(path.c_str(), &info) == 0 && S_ISREG(info.st_mode) && info.st_size > 0;
}
bool is_directory(const std::string& path) {
    struct stat info {};
    return stat(path.c_str(), &info) == 0 && S_ISDIR(info.st_mode);
}
std::string join(const std::string& base, const std::string& child) {
    return base + "/" + child;
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_codeoneditor_app_runtime_NativeRuntimeBridge_nativePreflight(
        JNIEnv* env, jclass, jstring staged_directory, jstring abi_name) {
    if (staged_directory == nullptr || abi_name == nullptr) {
        return env->NewStringUTF("BLOCKED:Diretório do runtime ou ABI ausente.");
    }
    const char* raw = env->GetStringUTFChars(staged_directory, nullptr);
    const char* raw_abi = env->GetStringUTFChars(abi_name, nullptr);
    if (raw == nullptr || raw_abi == nullptr) {
        if (raw != nullptr) env->ReleaseStringUTFChars(staged_directory, raw);
        if (raw_abi != nullptr) env->ReleaseStringUTFChars(abi_name, raw_abi);
        return env->NewStringUTF("BLOCKED:Não foi possível ler os parâmetros.");
    }
    const std::string root(raw);
    const std::string abi(raw_abi);
    env->ReleaseStringUTFChars(staged_directory, raw);
    env->ReleaseStringUTFChars(abi_name, raw_abi);

    if (!is_directory(root) || !is_regular_file(join(root, "base.apk")) ||
        !is_regular_file(join(root, "staged-runtime.json"))) {
        return env->NewStringUTF("BLOCKED:Diretório, APK base ou manifesto de preparação ausente.");
    }
    const std::string libdir = join(join(root, "lib"), abi);
    if (!is_directory(libdir)) {
        return env->NewStringUTF("BLOCKED:O APK preparado não contém bibliotecas para a ABI escolhida.");
    }
    const char* required[] = {"libc++_shared.so", "libfmod.so", "libMediaDecoders_Android.so", "libminecraftpe.so"};
    for (const char* name : required) {
        if (!is_regular_file(join(libdir, name))) {
            const std::string message = std::string("BLOCKED:Biblioteca obrigatória ausente: ") + name;
            return env->NewStringUTF(message.c_str());
        }
    }

    // A file/ABI preflight is not a game loader. Fail closed until the reference project's
    // GamePackageManager + loading Activity + native dependency graph are ported and tested.
    return env->NewStringUTF(
        "NOT_READY:Arquivos e bibliotecas obrigatórias encontrados. Ainda falta integrar a Activity do jogo, o carregador nativo, splits/recursos e o redirecionamento real dos diretórios por perfil; inicialização bloqueada.");
}

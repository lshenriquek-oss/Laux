# Launcher — Etapa 1: base real do Minecraft isolado

Esta etapa muda a base de compilação para o projeto de referência LeviLaunchroid (LiteLDev), em vez de simular o runtime dentro do aplicativo anterior. O código original do projeto do usuário foi preservado em `custom-ui-backup/` e não é incluído como módulo compilado.

## O que esta etapa contém

- Código Android completo da referência LeviLaunchroid, incluindo o fluxo de preparação de pacote, versões/perfis e inicialização do Minecraft.
- Módulos `minecraft` e `pojav_controls` exigidos pela referência.
- Identificador de instalação alterado para `com.codeoneditor.app`, rótulo `Launcher` e versão `0.112`; o namespace Kotlin interno permanece `org.levimc.launcher` para não quebrar referências `R` e `BuildConfig`.
- Licença Apache 2.0 original mantida em `LICENSE`.
- Projeto anterior guardado em `custom-ui-backup/` para futura migração de componentes de interface.

## Limites importantes

Esta é uma migração da base do projeto, não uma validação de runtime no dispositivo. A referência requer JDK 21 ou posterior, Android SDK e NDK/CMake. Os submódulos nativos `app/src/main/cpp/preloader` e `app/src/main/cpp/libHttpClient` não vêm preenchidos em arquivos ZIP normais. Inicialize-os com Git antes de compilar:

```bash
git submodule update --init --recursive
```

Como este pacote foi gerado a partir de um ZIP de fonte e não de um clone Git com metadados, para inicializar os submódulos é necessário clonar o repositório original e copiar os submódulos para estes diretórios, ou usar diretamente um clone recursivo:

```bash
git clone --recurse-submodules https://github.com/LiteLDev/LeviLaunchroid.git
```

Depois, copie os diretórios `app/src/main/cpp/preloader` e `app/src/main/cpp/libHttpClient` desse clone para os diretórios correspondentes deste projeto. Não remova os avisos de licença/atribuição.

## Próxima validação

1. Instalar JDK 21, Android SDK, NDK 28.2.13676358 e CMake.
2. Restaurar os submódulos nativos.
3. Executar `./gradlew assembleDebug`.
4. Corrigir erros de compilação reais antes de testar importação do Minecraft oficial.
5. No dispositivo, importar o Minecraft oficial e testar um perfil isolado com mundo descartável. Só considerar isolamento confirmado depois de verificar que o jogo usa a pasta do perfil e que os dados da instalação original não mudam.

O jogo oficial/licenciado é necessário. Esta etapa não inclui APK do Minecraft nem redistribui arquivos proprietários do jogo.

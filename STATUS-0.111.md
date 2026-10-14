# Launcher 0.111 — estado técnico

## Alterações desta versão

- Versão do aplicativo incrementada para `0.111` (`versionCode 111`).
- Pipeline do GitHub Actions ajustado para instalar JDK 21, Android SDK 36, NDK `28.2.13676358` e CMake `3.22.1`.
- O pipeline busca as dependências nativas `preloader` e `libHttpClient` no repositório de referência no servidor de CI, sem exigir compilação no telefone.
- A tarefa de CI agora tenta gerar `:app:assembleDebug` e publica o APK como artefato.
- A configuração Firebase de CI usa o application ID `com.codeoneditor.app`; uma chave real continua sendo opcional para a compilação de diagnóstico.

## Limitações que não devem ser ocultadas

A interface personalizada original continua preservada em `custom-ui-backup/`, mas **não foi conectada à atividade inicial ativa nesta versão**. A tela inicial ativa continua sendo a do runtime LeviLaunchroid.

O isolamento real do Minecraft também **não está concluído nem validado**. Criar diretórios/perfis privados não prova que o processo do jogo usa exclusivamente esses diretórios. Não se deve prometer proteção dos mundos originais nem chamar o recurso de funcional até ligar o fluxo de inicialização completo ao perfil isolado e testar em um aparelho.

O workflow foi preparado, mas só uma execução no GitHub Actions poderá confirmar se as dependências externas e o código compilam juntos. Não há APK pré-compilado incluído neste ZIP.

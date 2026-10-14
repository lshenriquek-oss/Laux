# Launcher 0.113 — interface integrada e runtime

A atividade inicial agora usa a interface personalizada preservada em `app/src/main/kotlin/com/codeoneditor/app/`, com tema escuro e sem emojis. O runtime de LeviLaunchroid permanece como base do carregador.

O botão Jogar procura uma versão personalizada já registrada no `VersionManager`, força `versionIsolation = true` e solicita o lançamento pelo `MinecraftLauncher` existente. Se nenhuma versão privada estiver registrada, o botão informa o que falta e não abre o Minecraft oficial por engano.

Importante: a integração foi conectada no código, mas a compilação e o isolamento efetivo ainda precisam ser validados no GitHub Actions e em aparelho Android. O APK importado pela opção de preparação da interface não é automaticamente registrado como versão jogável. A validação real deve confirmar que `VERSION_ISOLATION` redireciona arquivos, dados, cache e mundos para as pastas do perfil selecionado.

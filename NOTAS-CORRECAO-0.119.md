# Laux 0.119 — integração de referência

- Mantida a interface própria do Laux em `com.codeoneditor.app`.
- Mantidos o identificador do aplicativo, o tema, os recursos visuais e o fluxo de inicialização oculto do Laux.
- Restaurado `MinecraftRuntimePreparer.kt` a partir do projeto de referência `LeviLaunchroid-main`, pois era a diferença de lógica de carregamento nativo mais diretamente ligada ao início do Minecraft.
- Não foram copiados ícones `ic_leaf_logo` nem `ic_notification_leaf` do projeto de referência.

Esta alteração é uma tentativa direcionada com base na comparação dos arquivos. A abertura do Minecraft precisa ser validada no dispositivo; a criação do ZIP não equivale a um teste de execução.

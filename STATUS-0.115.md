# Laux 0.115 — integração do fluxo de inicialização

## Alterações
- O botão Jogar envia `LAUNCH_WITH_URI` e `LAUX_LAUNCH_ONLY` para o fluxo de inicialização existente.
- A Activity Java original mantém sua hierarquia interna para que o mecanismo de lançamento e os componentes do launcher continuem disponíveis, mas sua raiz fica invisível no modo Laux.
- O modo Laux não dispara a verificação de atualização do launcher de referência, não abre o fluxo de EULA e não mostra o fluxo pós-EULA.
- Falhas de inicialização são apresentadas por Toast, já que os diálogos da interface original ficam ocultos.
- A biblioteca de pacotes agora relê os arquivos persistidos ao abrir a tela e inclui ação para exportar um pacote selecionado.
- Versão do aplicativo elevada para 0.115.

## Limitações e validação
- O arquivo ZIP foi verificado estruturalmente, mas a compilação não pôde ser executada neste ambiente: o Gradle Wrapper precisa baixar Gradle 8.13 de `services.gradle.org`, e a rede do ambiente retornou `UnknownHostException`.
- O modo de inicialização invisível ainda precisa ser validado num dispositivo real. O crash nativo do driver gráfico visto no bugreport não pode ser confirmado como corrigido apenas por alteração do fluxo de Activities.
- Importar/exportar pacotes mantém os arquivos no armazenamento privado do Laux, mas não instala nem ativa automaticamente os pacotes dentro dos mundos do Minecraft.
- Backups de mundos, associação dos perfis ao processo do Minecraft e gerenciamento direto dos mundos continuam exigindo integração com o diretório de dados real usado pela instalação Bedrock no dispositivo. Esta versão não declara essas funções como concluídas.

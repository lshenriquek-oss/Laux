# Bedrock Launcher — primeira versão de interface

Este projeto adapta a base Android Editor110 para uma interface horizontal de launcher.

## Já incluído nesta etapa
- Paleta monocromática: preto, branco e cinza neutro.
- Preparação de diretórios privados para versões, perfis, bibliotecas do runtime, mods, backups, logs e importações.
- Registro local de perfis com nomes únicos, IDs gerados, metadados versionados e diretórios próprios para worlds/config/logs/mods.
- Diagnóstico de estado das pastas na tela Configurações.
- Interface em modo paisagem.
- Navegação: Início, Mods, Mundos, Perfis, Backups e Configurações.
- Botão para tentar abrir o Minecraft Bedrock oficial instalado (`com.mojang.minecraftpe`).
- Importação de arquivos para o armazenamento privado do launcher.
- Avisos explícitos sobre funções que ainda não foram implementadas.
- GitHub Actions para compilar um APK de depuração sem compilar no celular.

## Limitações importantes
- Importar um `.mcpack` ou `.mcaddon` aqui só guarda o arquivo no espaço privado do launcher; não ativa o conteúdo no Minecraft.
- O gerenciador de perfis desta etapa separa apenas pastas e metadados do launcher; ainda não redireciona o Minecraft para esses diretórios. O runtime isolado, a inicialização de APKs importados, leitura/restauração de mundos, backups automáticos e carregamento de módulos nativos ainda não estão implementados.
- O APK gerado pelo workflow é de depuração e não está assinado para distribuição pública.

## Gerar o APK
No GitHub, abra a aba **Actions**, escolha **Build Android APK** e execute **Run workflow**. Quando terminar com sucesso, abra a execução e baixe o artefato `bedrock-launcher-debug-apk`.

## Etapa 2 — diagnóstico de compatibilidade (preparação)
- Adicionada leitura somente de consulta do nível de API Android, ABIs suportadas e presença/versão do pacote oficial `com.mojang.minecraftpe`.
- O diagnóstico é exibido em Configurações e não tenta alterar, copiar ou injetar bibliotecas no jogo.
- Este diagnóstico não comprova que um APK importado possa ser executado isoladamente. A integração real requer adaptar o fluxo de inicialização do projeto de referência, confirmar dependências/licenças e validar com versões/ABIs reais do Minecraft.

## Etapa 3 — preparação segura da versão do jogo

- A ação **Selecionar APK oficial para preparar** confere que o pacote declara `com.mojang.minecraftpe`, calcula SHA-256 e extrai somente bibliotecas `.so` das ABIs suportadas pelo dispositivo para uma pasta privada por versão.
- O manifesto `staged-runtime.json` registra versão, hash, ABIs e declara explicitamente `nativeRuntimeReady: false` e `worldsCopied: false`.
- O botão **Jogar** não abre mais diretamente o Minecraft instalado: isso poderia usar os dados/mundos originais, contrariando o objetivo de isolamento.
- **Limite atual:** a preparação do APK e das bibliotecas não é, sozinha, um runtime executável. Ainda falta implementar e validar a ponte de carregamento nativo/Activity e a redireção dos dados do jogo para o perfil privado. Até isso existir, o launcher bloqueia o início em vez de fingir que há isolamento.
- A validação atual não verifica a assinatura criptográfica do APK, portanto o identificador do pacote não prova sua autenticidade. A etapa não acessa nem modifica os mundos originais e não promete preservar conquistas; isso depende da implementação e dos testes posteriores.

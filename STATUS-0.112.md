# Launcher 0.112 — integração da interface

## Feito nesta versão
- A atividade inicial do aplicativo aponta para a interface personalizada, não para a tela padrão do LeviLaunchroid.
- Fontes da interface e dos gestores de armazenamento/perfil foram integrados ao módulo ativo.
- Dependências e plugin do Jetpack Compose foram adicionados ao Gradle.
- O botão Jogar tenta iniciar uma versão personalizada já registrada no gerenciador, marcando `versionIsolation = true`.
- Se não existir versão privada registrada, o launcher bloqueia a abertura acidental da instalação oficial e mostra uma mensagem de orientação.
- A numeração do app e do artefato foi incrementada para 0.112.

## Ainda precisa ser confirmado
- Build real no GitHub Actions.
- A opção de importar/preparar APK da interface ainda não o registra automaticamente no VersionManager; uma versão personalizada deve estar previamente importada no gerenciador do runtime.
- Teste em aparelho para comprovar que o runtime efetivamente usa os diretórios isolados para mundos, dados, cache e arquivos.
- Teste de autenticação e dependências nativas no dispositivo.

Não declarar isolamento validado até passar os testes de runtime. A implementação usa o mecanismo `versionIsolation` existente do runtime; não copia nem modifica automaticamente os mundos da instalação oficial.

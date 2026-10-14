
## Launcher 0.0.03 — Etapa 4: ponte JNI e revisão de densidade da interface

- Adicionada uma biblioteca JNI/C++ (`bedrock_runtime_bridge`) com uma pré-verificação de arquivos no runtime preparado.
- A ponte opera em modo *fail-closed*: mesmo com APK, manifesto e diretório de bibliotecas presentes, retorna `NOT_READY` porque isso não prova que o carregamento nativo do jogo, dependências, Activity, splits ou isolamento de mundos funcionem.
- O botão **Jogar** continua bloqueado. A ponte não executa o Minecraft nem acessa os dados do pacote oficial.
- A tela de configurações agora pode rolar verticalmente e usa espaçamentos, títulos, texto e ícones mais compactos para reduzir cortes na orientação paisagem.
- A configuração de compilação agora requer Android NDK e CMake 3.22.1. A compilação precisa ser validada num ambiente que tenha esses componentes instalados.

**Limitação explícita:** esta etapa integra a fronteira JNI e o teste nativo de pré-condições, não o carregador completo de Minecraft. O isolamento real só pode ser confirmado depois de integrar a Activity/runtime compatível e testar cópia, escrita e restauração de mundos com dados de teste, sem apontar para os dados originais.

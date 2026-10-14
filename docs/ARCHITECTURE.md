# Arquitetura do Laux

## Princípios
1. Manter uma implementação ativa do launcher.
2. Integrar a interface personalizada ao fluxo principal.
3. Não declarar isolamento de perfis sem testes reais.
4. Usar Termux para Git e pequenas edições; compilar no GitHub Actions.
5. Não incluir credenciais ou chaves privadas no repositório.

## Componentes a integrar
- Aplicativo Android e atividade principal.
- Interface personalizada.
- Gerenciamento de versões e perfis.
- Runtime Bedrock e dependências nativas compatíveis.
- Workflow de compilação e publicação do APK.

## Critérios de validação
- Compilação concluída no GitHub Actions.
- APK gerado e instalável em dispositivo compatível.
- Interface abre corretamente.
- Inicialização do jogo funciona.
- Dados e mundos por perfil são testados.
- O launcher não inicia acidentalmente outra instalação do Minecraft.

# Laux

Launcher Android para Minecraft Bedrock.

## Objetivos
- Interface personalizada, escura e minimalista.
- Organização de perfis e versões.
- Isolamento de dados e mundos, a implementar e validar.
- Compilação Android pelo GitHub Actions.
- Preparação para recursos inteligentes com IA.

## Estado atual
Esta é a estrutura inicial de organização. Ainda é necessário integrar
o código real do launcher, suas dependências nativas e o runtime do jogo.
Nenhum APK funcional foi validado nesta etapa.

## Organização
- `.github/workflows/android-build.yml`: automação de compilação.
- `docs/ARCHITECTURE.md`: arquitetura e critérios de validação.
- `scripts/termux-push.sh`: auxiliar de envio pelo Termux.
- `app/`: deverá receber o projeto Android real.

## Compilar
Depois da integração do projeto Android, abra a aba Actions no GitHub
e execute o workflow Android Build. O APK será disponibilizado como
artefato somente se a compilação terminar com sucesso.

## Segurança
Não envie tokens, senhas, chaves de assinatura ou arquivos pessoais.

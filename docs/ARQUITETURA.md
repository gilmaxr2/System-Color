# Arquitetura do YouTube Color

- `MainActivity.kt`: autorização de sobreposição, iniciar/parar serviço.
- `OverlayService.kt`: notificação persistente, camada de cor e painel flutuante.
- `AndroidManifest.xml`: permissões e declaração do serviço.
- `app/build.gradle.kts`: versão Android, SDK e compilação.
- `.github/workflows/build-apk.yml`: gera APK debug automaticamente.
- `tools/validate_project.py`: valida estrutura básica sem Android SDK.

## Restrições técnicas

A sobreposição é uma View translúcida de tela inteira. Ela não processa o vídeo nem altera pixels do YouTube. A camada afeta também a interface e outros aplicativos enquanto ativa. Algumas telas protegidas podem impedir sobreposições. O app não usa acessibilidade, root, captura de tela ou acesso aos dados do YouTube.

## Próximas evoluções

Separar presets e preferências em classes próprias; tornar bolha arrastável; adicionar opção para exibir apenas quando o YouTube estiver em primeiro plano com solução de detecção consentida; melhorar layout e acessibilidade.

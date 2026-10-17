# Poliglota

Aplicativo Android em Kotlin/Jetpack Compose para selecionar jogos instalados, exportar o APK base e inspecionar a estrutura de um APK escolhido pelo usuário.

## Funcionalidade atual

- Lista aplicativos com inicializador e identifica jogos quando o Android fornece essa categoria.
- Pesquisa por nome e identificador de pacote.
- Exporta o APK base de um aplicativo instalado para um destino escolhido pelo usuário.
- Permite selecionar um APK e faz inspeção estrutural básica: tamanho, arquivos XML em `res/values`, presença de `assets/` e bibliotecas nativas.
- Não envia APKs para servidores nem os publica no repositório.

## Limite importante

**A tradução e reconstrução integral de APK ainda não estão implementadas.** A inspeção ZIP não decodifica `resources.arsc` nem XML binário do Android. Um fluxo completo precisa integrar uma versão compatível do Apktool (ou ferramenta equivalente), traduzir apenas recursos textuais selecionados, reconstruir o pacote, assinar o APK e validar o resultado. Jogos com split APKs, recursos baixados, textos em imagens, proteções ou verificações de assinatura exigem tratamento adicional. Não desinstale o jogo original até validar uma versão modificada funcional; a assinatura diferente normalmente impede instalar como atualização e a desinstalação pode apagar dados.

## Build

O workflow do GitHub Actions executa `gradle assembleDebug` e publica o APK de depuração como artefato da execução. Para instalar, baixe o artefato de uma execução concluída em Actions.

## Tecnologias

- Kotlin, Android SDK e Jetpack Compose
- Gradle Kotlin DSL

## Privacidade

Não inclua APKs de jogos, chaves privadas, senhas ou certificados de assinatura neste repositório público.

# Firmware e USB no A15

O [tutorial de instalação](../README.md#tutorial-rápido) funciona sem trocar o Android. Este guia registra a configuração do aparelho testado e o que é necessário para decidir se vale a pena adaptar outro.

## Identificação e recuperação

- Nome lido no sistema original **SLSM A15**. Placa `tb8735ba1_bsp`, plataforma declarada `mt6737m`, kernel `3.18.79` e ABIs `armeabi-v7a,armeabi`. O build de fábrica era `W8658M1_SLSM_A15-S8J_116-JFT2K_20260313`.
- O vendor conservado se identificava como `T03S/T03S/T03S:8.1.0/O11019/1609328566:user/release-keys`. O sistema trocado é a PHH AOSP 9 v123, ARM32 A-only sem GApps e sem pacote SU pré-instalados. A GSI do projeto tinha build `userdebug`, assinatura `test-keys` e patch declarado de dezembro de 2019. O projeto da imagem foi arquivado. Considere esse estado de segurança antes de usar o aparelho para contas ou dados sensíveis.
- Confirme modelo, placa, ABI e layout de partições no próprio aparelho. A capacidade anunciada e o nome A15 não identificam a variante de firmware. Guarde localmente as fotos, o firmware exato de restauração obtido da fabricante e as instruções de recuperação da variante. Confirme que o computador detecta o modo de recuperação apropriado antes de iniciar mudanças.
- Desbloquear o bootloader pode apagar os dados. Confira o backup antes e confirme a ação separadamente no aparelho. Faça uma etapa de cada vez com carga suficiente e cabo de dados estável. [Bootloader e desbloqueio no AOSP](https://source.android.com/docs/core/architecture/bootloader/locking_unlocking)

Em Linux ou macOS, execute o diagnóstico de leitura a partir da raiz do repositório. Para Windows, rode os comandos ADB equivalentes no PowerShell ou use um terminal Bash com o ADB do Windows selecionado. O script usa o cliente apontado por `ADB`, ou `adb` por padrão. Se houver mais de um aparelho, selecione o desejado com `ANDROID_SERIAL` no ambiente local.

```bash
adb devices -l
./scripts/diagnostico.sh
```

A confirmação de uma GSI adequada inclui Treble, ABI, layout da partição e caminho de recuperação. Na unidade testada, o arquivo original foi [system-arm-aonly-vanilla-nosu.img.xz](https://github.com/phhusson/treble_experimentations/releases/download/v123/system-arm-aonly-vanilla-nosu.img.xz). As somas no comando abaixo são as registradas a partir da cópia do projeto. [Release PHH v123](https://github.com/phhusson/treble_experimentations/releases/tag/v123) · [Documentação de GSI do AOSP](https://source.android.com/docs/core/tests/vts/gsi)

```bash
# Na raiz do repositório, após baixar o arquivo para um local temporário.
mv system-arm-aonly-vanilla-nosu.img.xz aosp-9-v123-arm-aonly-vanilla-nosu.img.xz
sha256sum --ignore-missing -c docs/aosp-9-v123.sha256
xz -dk aosp-9-v123-arm-aonly-vanilla-nosu.img.xz
sha256sum -c docs/aosp-9-v123.sha256
```

## Modo de gravação

ADB é usado com Android iniciado. A gravação depende do modo de bootloader reconhecido pelo `fastboot`. Uma tela preta com luz de fundo pode aparecer ao mudar de modo. Verifique em qual modo o computador realmente reconheceu o aparelho.

```bash
adb devices -l
fastboot devices
```

Somente prossiga se a placa, o layout ARM32 A-only, a partição de sistema, o desbloqueio e o meio de restauração estiverem confirmados para a unidade específica. Para esse layout, com o aparelho no fastboot e a imagem já descomprimida, a operação de substituição do sistema é `fastboot flash system aosp-9-v123-arm-aonly-vanilla-nosu.img`. Confirme o efeito com o dono antes de executar. Siga as exigências de reset da variante e deixe o primeiro boot concluir. Preserve vendor, boot e preloader. Caso a unidade não seja detectada em fastboot, interrompa a gravação e retome o diagnóstico de driver, porta e modo. A combinação física de botões variou durante os testes.

## Navegação após iniciar

Abra as configurações de gestos e procure o controle da navegação do sistema. Em builds do Pie que exibem *Deslizar para cima no botão Início*, desligar esse gesto habilita o modo clássico de três botões. Confirme na tela se Voltar, Início e Recentes aparecem separados e respondem. O comando exato usado no histórico para o ajuste não foi recuperado. A densidade lógica confirmada foi `160`. Se quiser reproduzi-la, guarde o valor anterior de `adb shell wm density` antes de aplicar `adb shell wm density 160`.

## USB que some após iniciar

1. Comece com cabo de dados, porta direta e tablet desbloqueado. Confira a depuração e a autorização na tela. No Windows, observe o Gerenciador de Dispositivos para ver o que aparece em Android e em fastboot. Confirme se cada interface detectada tem o driver compatível. [Drivers USB para Android](https://developer.android.com/studio/run/oem-usb)
2. Com o Android iniciado, veja se o ADB está ativo e quais funções o sistema configurou.
   ```bash
   adb devices -l
   adb shell getprop sys.usb.config
   adb shell getprop sys.usb.state
   ```
3. Se o objetivo for transferir arquivos, selecione *Transferência de arquivos* e teste o MTP separadamente. Repita após bloquear, desbloquear, reiniciar e reconectar. Uma conexão ADB funcional já permite usar `adb pull` e `adb push` para os arquivos acessíveis.
4. No diagnóstico final deste aparelho, `sys.usb.state` reportava `adb` e um ajuste de inicialização registrava `sys.a15.usb_boot=applied`. O ajuste específico ficou sem fonte recuperável e o MTP sem confirmação de estabilidade. O reinício do daemon em um teste de `adb root` derrubou a conexão até um novo boot. A conexão USB variou com a GSI e o modo de boot. A participação do driver, do firmware de fábrica ou do cabo em cada queda ficou sem confirmação.

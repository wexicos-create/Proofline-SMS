# Proofline SMS

Una aplicación de mensajería segura y privada con una funcionalidad innovadora de comunicación offline a través de enlaces encriptados.

## Descripción General

Proofline SMS es una aplicación de mensajería diseñada para ofrecer una comunicación segura, priorizando la privacidad y la capacidad de operar incluso sin conexión a internet activa. La característica principal, "Enlace Seguro", permite a los usuarios enviar mensajes encriptados de extremo a extremo utilizando enlaces únicos como el único medio de transporte.

## Característica Clave: Enlace Seguro

Esta funcionalidad permite:
* **Mensajería Offline:** Envía y recibe mensajes sin necesidad de una conexión a internet constante.
* **Encriptación de Extremo a Extremo (E2EE):** Tus mensajes están seguros y solo pueden ser leídos por el destinatario previsto.
* **Comunicación Basada en Enlaces:** Los mensajes se transportan de forma segura a través de enlaces únicos generados por la aplicación.
* **Privacidad por Diseño:** Reduce la dependencia de servidores centralizados para la transferencia de mensajes.

## Cómo Funciona (Enlace Seguro)

1. **Envío:** El remitente redacta un mensaje y selecciona la opción "Enviar Vía Enlace Seguro". La aplicación genera un enlace único y encriptado que contiene el mensaje cifrado y la clave de desencriptación. Este enlace se puede copiar y compartir externamente.
2. **Recepción:** El destinatario abre el enlace. La aplicación de Proofline SMS detecta el enlace, lo procesa, desencripta el mensaje y lo presenta en la interfaz de chat.

## Tecnologías Utilizadas

* Kotlin/Java
* Android Framework
* AndroidX Security Cryptography
* BouncyCastle
* Gradle Build System

## Instalación y Uso

Para compilar y ejecutar la aplicación:
1. Clona este repositorio: `git clone https://github.com/Wexicos-create/Proofline-SMS.git`
2. Abre el proyecto en Android Studio.
3. Asegúrate de tener un dispositivo Android o un emulador configurado.
4. Ejecuta la aplicación.

## Estructura del Proyecto

```
/
├── .github/
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md
│       └── feature_request.md
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/proofline/sms/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── chat/
│   │   │   │   │   │   ├── ChatActivity.java
│   │   │   │   │   │   └── MessageAdapter.java
│   │   │   │   │   ├── compose/
│   │   │   │   │   │   ├── ComposeActivity.java
│   │   │   │   │   │   └── SecureLinkGenerator.java
│   │   │   │   │   └── main/
│   │   │   │   │       └── MainActivity.java
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   └── Message.kt
│   │   │   │   │   └── crypto/
│   │   │   │   │       └── EncryptionHelper.java
│   │   │   │   └── util/
│   │   │   │       └── LinkHandler.java
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_compose.xml
│   │   │   │   │   ├── item_message.xml
│   │   │   │   │   └── activity_main.xml
│   │   │   │   └── values/
│   │   │   │       └── strings.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── java/com/proofline/sms/
│   └── build.gradle
├── .gitignore
├── build.gradle
├── settings.gradle
├── LICENSE
└── README.md
```

## Contribuciones

¡Las contribuciones son bienvenidas! Por favor, consulta la sección de licencias y las guías de contribución para más detalles.

## Licencia

Este proyecto está licenciado bajo la Licencia Pública General de Proofline - ver el archivo `LICENSE` para más detalles.

**Creador:** Wexicos-create

**Créditos:** Josue Israel Cervantes Alvarado
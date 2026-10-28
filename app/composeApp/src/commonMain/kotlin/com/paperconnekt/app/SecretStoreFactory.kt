package com.paperconnekt.app

import com.paperconnekt.secrets.SecretStore

expect fun createSecretStore(): SecretStore

expect fun configureSecretStore(platformContext: Any?)


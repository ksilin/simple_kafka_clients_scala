package com.example;

import com.example.util.{ ClientConnectionProps, ClientConnectionSetup, SpecBase };

class ConfigSpec extends SpecBase {

  "must resolve spec" in {

    val configPath  = Some("local")
    val clientProps = ClientConnectionProps.create(None, configPath)

    println(s"clientProps: ${clientProps}")

    val clientSetup: ClientConnectionSetup = ClientConnectionSetup(configPath = configPath)
    println(s"clientProps: ${clientSetup.clientProps}")
    println(s"commonProps: ${clientSetup.commonProps}")
    println(s"adminClient: ${clientSetup.adminClient}")
  }

}

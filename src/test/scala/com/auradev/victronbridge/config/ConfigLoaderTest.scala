package com.auradev.victronbridge.config

object ConfigLoaderTest extends weaver.SimpleIOSuite:
  test("it should load default config"):
    for config <- ConfigLoader.loadConfig()
    yield expect(config.httpServerConfig.hostname == "0.0.0.0") and expect(config.httpServerConfig.port == 8080)

# th2 Sailfish Utils (3.5.0)

This library contains classes to convert messages from th2 to Sailfish format and vice versa. They are used in several th2 projects to reuse Sailfish features: message comparison, codec/connect implementations, etc.

## Release Notes

### 3.5.0

#### Changed:
+ Update `th2-common` version to `3.17.0`
+ Update `sailfish-core` version to `3.2.1622`

#### Added:
+ `MessageFactoryProxy` wrapper for `IMessageFactory`
+ `DefaultMessageFactoryProxy` implementation. Can be used without dictionary

### 3.4.0

+ Update `th2-common` version to `3.16.5`

### 3.3.5

+ Use newer version of `com.exactpro.sf:sailfish-core` which fixes representation of `BigDecimal` fields in string representation of `MapMessage`

### 3.3.4

+ Added converter JavaType to the class in `ProtoToIMessageConverter` to improve performance

### 3.3.3

+ Added message properties to the Sailfish IMessage from th2 proto Message

### 3.3.1

+ removed gRPC event loop handling
+ fixed dictionary reading

### 3.3.0

+ reads dictionaries from the /var/th2/config/dictionary folder.
+ uses mq_router, grpc_router, cradle_manager optional JSON configs from the /var/th2/config folder
+ tries to load log4j.properties files from sources in order: '/var/th2/config', '/home/etc', configured path via cmd, default configuration
+ updated Cradle version. Introduced async API for storing events

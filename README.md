# th2 Sailfish Utils (3.3.4)

This library contains classes to convert messages from th2 to Sailfish format and vice versa. 
They are used in several th2 projects to reuse Sailfish features: message comparison, codec/connect implementations, etc.

## Release Notes

### 3.3.4

+ Added converter JavaType to class in `ProtoToIMessageConverter` to improve performance

### 3.3.3

+ Add message properties to the Sailfish IMessage from th2 proto Message

### 3.3.1

+ removed gRPC event loop handling
+ fixed dictionary reading

### 3.3.0

+ reads dictionaries from the /var/th2/config/dictionary folder.
+ uses mq_router, grpc_router, cradle_manager optional JSON configs from the /var/th2/config folder
+ tries to load log4j.properties files from sources in order: '/var/th2/config', '/home/etc', configured path via cmd, default configuration
+ update Cradle version. Introduce async API for storing events

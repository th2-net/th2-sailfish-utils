# th2 Sailfish Utils (3.15.0)

This library contains classes to convert messages from th2 to Sailfish format and vice versa. They are used in several th2 projects to reuse Sailfish features: message comparison, codec/connect implementations, etc.

## Release Notes

### 3.15.0
+ Sailfish version is updated from `3.3.54` to `3.3.106`
  + IMessageToProtoConverter: Support for explicit null values in IMessage. 
+ Owasp plugin updated to `8.2.1`
  + Dependency check task created. 

### 3.14.0

Sailfish version is updated from `3.3.11` to `3.3.54`
+ Versions for dependencies with vulnerabilities was updated:
  + BOM `4.0.1` -> `4.1.0`
  + common `3.41.0` -> `3.44.0`

### 3.13.0

+ Sailfish version is updated from `3.2.1741` to `3.3.11`
+ Versions for dependencies with vulnerabilities was updated:
  + BOM `3.0.0` -> `4.0.1`
  + common `3.31.3` -> `3.41.0`
  + log4j `1.2` removed from dependencies

### 3.12.4

+ Changed conversion from IMessage to protobuf message:
  + The format for time and date time will always have milliseconds part

### 3.12.3

+ Improved condition output format for `EQ_PRECISION`, `WILDCARD`, `LIKE`, `IN`, `MORE`, `LESS` operations and their negative versions

### 3.12.2

+ Added new parameter `checkNullValueAsEmpty` in the `FilterSettings` witch is used for `EMPTY` and `NOT_EMPTY` operations to check if `NULL_VALUE` value is empty. For example, if the `checkNullValueAsEmpty` parameter is:
+ `true`, then `NULL_VALUE` is equal to `EMPTY`, otherwise `NULL_VALUE` is equal to `NOT_EMPTY` 

### 3.12.1

+ Migrated `common-j` version from `3.31.2` to `3.31.3`
  + Added null value support for filter to table conversion

### 3.12.0

+ Add ability to check exact `null` value in the message.

### 3.11.1

+ Fixed propagation of filter settings to sub-messages

### 3.11.0

+ Add parameter for marking `null` values from message for filters

### 3.10.2
+ Migrated sailfish-core version from `3.2.1676` to `3.2.1741`
+ Replaced sailfish equality filters with their counterparts

### 3.10.1
+ Fixed conversion of `null` values

### 3.10.0
+ Migrated th2-common version from `3.26.2` to `3.26.5`
  + Added a new filter operations `EQ_DECIMAL_PRECISION` and `EQ_TIME_PRECISION`

### 3.9.1
+ Update th2-common version from `3.26.0` to `3.26.2` with fix filters treeTable convertation   

### 3.9.0
+ Update th2-common version from `3.23.0` to `3.26.0` for new Filter SimpleList usage 

### 3.8.1

#### Changed:
+ Migrated sailfish-core version from `3.2.1622` to `3.2.1676`

#### Added:
+ Parameter for `ComparatorSettings`:
  + **keepResultGroupOrder** - contain verification in order matches the actual message

### 3.8.0

#### Changed:
+ The converter adds `BigDecimal` in `plain` format to proto `Message`

#### Added:
+ Parameters for `IMessageToProtoConverter`:
  + **stripTrailingZeros** - removes trailing zeroes for `BigDecimal` (_0.100000_ -> _0.1_)

### 3.7.0

#### Added:

+ `IN`, `LIKE`, `MORE`, `LESS`, `WILDCARD` FilterOperation and their negative versions

### 3.6.0

#### Added:

+ `Parameters` to configure the `ProtoToIMessageConverter`


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

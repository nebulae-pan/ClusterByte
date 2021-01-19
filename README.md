## ClusterByte
字节码简化处理插件

### 可以做什么
- 简化新增一个gradle插件所需要的编码
- 简化transform开发，封装了ASM、ASM TreeAPI、Javassist等调用方式，不同transform间可以随意切换
- transform修改为在内存中传递输出，减少原生transform通过IO传递结果的次数
- 多线程处理transform中不同的文件，速度平均提升10倍以上
- 框架生成的transform可单独作为原生transform执行，也可以加入框架的transform流

## 如何使用
### 接入
开发插件时引入如下依赖，当前最新版本为1.2.0
```
implementation "io.nebula.platform.clusterbyte:byte-core:x.x.x"
implementation "io.nebula.platform.clusterbyte:byte-plugin:x.x.x"
```

### 使用BaseSlicePlugin
新建`Plugin`类，继承`BaseSlicePlugin`，实现`apply0()`写入插件逻辑。
`BaseSlicePlugin`的范型可以填入自定义的Extension，框架将会构建一个新的对象并赋值给extension对象。
如果需要transform进行所有代码的处理逻辑，可以调用`registerTransform()`，并传入`BaseSliceTransform`的子类

### 使用BaseSliceTransform
在使用transform时，可以使用继承`ByteArraySliceTransform`替代原本的继承`Transform`。
新增的API说明如下，其他API除`transform(TransformInvocation)`外与`Transform`原本的含义相同，如果是已有类的迁移，需要将`transform(TransformInvocation)`内的逻辑拆分到以下接口中。

#### void preTransform(TransformInvocation invocation)
在transform执行之前调用，可以做一些时间统计之类的操作

| 参数|说明 |
|-|-|
|invocation | Transform中传递信息的参数|

#### void traversalDir(DirectoryInput dirInput)
遍历所有的DirectoryInput，可以为某些AOP工具在操作前预先指明检索范围，便于后续遍历流程中的额外处理。在incremental编译的情况下，仍会遍历整个工程

| 参数|说明 |
|-|-|
|dirInput | 可以通过TransformInvocation获得，代表当前module输入class的文件夹合集，可以通过该类获得每一个文件的增量情况|

#### void traversalJar(File file)
遍历所有的JarInput，并以文件的形式返回，可以为某些AOP工具在操作前预先指明检索范围，便于后续遍历流程中的额外处理。在incremental编译的情况下，仍会遍历整个工程

#### boolean onClassVisited(Status status, byte[] fileEntity)
复写该方法，可以通过参数中的fileEntity引用所有项目中的class（包括sub-project）。该方法的回调相当于第二次遍历。

| 参数|说明 |
|-|-|
|status | 在incremental编译时，当前文件的状态，分为NOTCHANGED，ADDED，CHANGED，REMOVED，可以根据该状态进行增量编译的处理。不是incremental时，一直为ADDED。|
|fileEntity | class文件在内存中的字节数组|

返回值：true为当前transform消耗掉当前fileEntity，将不会传递给下一个transform进行处理；false为默认值，不会进行消耗

#### boolean onJarVisited(Status status, File jarFile)
复写该方法，可以通过参数中的fileEntity引用所有项目中的jar包（不包括sub-project）。该方法的回调相当于第二次遍历。

| 参数|说明 |
|-|-|
|status | 在incremental编译时，当前文件的状态。不是incremental编译时，一直为ADDED。|
|jarFile | jar依赖的本地文件，如果是aar，将会仅解压并使用classes.jar|

返回值：true为当前transform消耗掉当前fileEntity，将不会传递给下一个transform进行处理；false为默认值，不会进行消耗

#### void postTransform(TransformInvocation invocation)
在两次遍历之后调用，可以做一些收尾或统一操作。

| 参数|说明 |
|-|-|
|invocation | Transform中传递信息的参数|

### Transform使用AOP工具
ClusterByte支持了ASM和Javassist的使用，需要使用ASM时，可以引入依赖
```
implementation "io.nebula.platform.clusterbyte:converter-asm:x.x.x"
```
在plugin中调用
```
// kotlin
registerConverterFactory(AsmConverterFactory())
registerConverterFactory(AsmClassNodeConverterFactory()) //Tree API
```
在编写Transform时，不使用`ByteArraySliceTransform`或`BaseSliceTransform`，而是使用`VisitorSliceTransform`或`ClassNodeSliceTransform`（TreeApi），override `onClassVisited()`方法时，第二个参数就会由byte[]替换为`VisitorChain`或是`ClassNode`。

同理，使用Javassist时，引入依赖
```
implementation "io.nebula.platform.clusterbyte:converter-javassist:x.x.x"
```
在plugin中调用
```
// kotlin
registerConverterFactory(JavassistConverterFactory())
```
使用`JavassistSliceTransform`，就可以对每一个.class文件的`CtClass`对象进行操作

#### 自定义工具转换
实现`ConverterFactory`
```
interface ConverterFactory<T> {
	// 从文件的byteArray转换为transform中处理的classEntity
    fun classConverter(): ClassConverter<ByteArray, T>

    // 从classEntity转换为byteArray，用于传递给下一个transform或进行文件写入
    fun tempConverter(): ClassConverter<T, ByteArray?>
}
```


### 注意事项
- SliceTransform的visitor方法都会被并发调用，注意处理好多线程问题
- 框架最新版本基于agp 3.5.3开发

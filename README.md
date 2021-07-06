## 对象自动封装组件
> 用于解决 java 多态下的对象封装问题， 例如 IOrder 有多个实现类， 每一个实现类都包含一部分共有的对象，也包含一部分自己独有的对象，这个组件可以对含有 Composite 方法实现的对象做自动封装。

> 默认懒加载（@see FetchType），仅有在实际用到对象时，才会调用 Composite 去封装属性 
 
### 自动封装有如下约束：
> 待封装的属性类型仅限于普通类型、List、 ArrayList、Set、HashSet 。
> 待封装的属性只能是 public、protected，或者包含 set 方法

> 返回的对象类型仅限于普通类型、List、Map 

>  @AutoField 的 property 只能是 public、protected，或者包含 get 方法
### 使用方式
#### 引入pom
```xml
<dependency>
    <groupId>com.huafon</groupId>
    <artifactId>auto-composite</artifactId>
    <version>${version}</version>
</dependency>
```

#### 添加 Composite 的实现类， 其中第一个泛型表示参数类型，第二个泛型表示返回值类型

```java
// Composite 默认的参数类型是待封装对象本身，返回值类型是带封装对象的属性，可以通过 AutoField 替换参数类型

import com.huafon.common.auto.composite.annotation.AutoField;

@AutoField(property = "属性名称", paramName = "Composite 方法里， 作为参数名传入， 可以用于区分是来自于哪里的参数")

public class OrderImpl implements IOrder {
    
    private long id;

    /**
     * 在没有 AutoField 时， 需要 Composite<IOrder,OrderLog> 的实现类
     * 在有如下的 AutoField 的实现时， 需要 Composite<Long, OrderLog> 的实现类
     * 都没有的情况下， 有一个 MybatisPlusComposite， 需要指定 paramName 
     */
    @AutoField(property = "id", paramName = "ORDER_ID")
    private List<OrderLog> orderLogs;
}

/**
 *
 * 通过 IOrder 获取对应的 OrderLog， IOrder 所有的实现类都默认会自动封装 orderLog 属性
 */
public class OrderLogComposite implements Composite<IOrder, OrderLog> {

    @Override
    public List<OrderLog> queryList(IOrder order) {

    }
}
```
### ParamComposite, 简化参数类型设定，只约定了返回值类型
```java

public class OrderLogComposite implements ParamComposite<OrderLog> {
    
    @Override
    public Set<Class<?>> supportedParamTypes() {
        return Sets.newHashSet(IOrder.class);
    }
    
    @Override
    public List<OrderLog> queryList(Param param) {
        
    }
}
```

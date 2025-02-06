package org.hy.common.redis;

import java.util.List;
import java.util.Map;

import org.hy.common.Date;
import org.hy.common.TablePartitionRID;





/**
 * Redis数据库访问接口
 * 
 * 特点：创建逻辑表的概念。通过三级信息构建
 *          1. 一级Hash(库名称): 保存表的列表信息。  Hash.key = 表名称，Hash.value = 表的创建时间
 *          2. 二级Hash(表名称): 保存表的行主键列表。Hash.key = 行主键，Hash.value = 行主键的创建
 *          3. 三级Hash(行主键): 保存一行数据信息。  Hash.key = 字段名，Hash.value = 字段值
 * 
 * 概念1：行主键  RowKey。表中一行数据的唯一标示
 *                      注：行主键默认为 "表名称.ID" 的形式
 * 
 * 概念2：关键字  Key   。Redis数据库中一个Key-Value的Key值。就是Map集合的Key值。
 * 
 * 注：行获取：一行数据是在Redis中用HSet命令存储的，即在获取时(get)，可用行主键直接获取，不用通过查库、查表、查主键后再获取。
 * 注：行写入，因为表、主键关系，所以在Insert、Update一行数据时，要输入库、表名称。
 * 注：全部主键获取："库名.表名" 为关键字即可获取所有行主键列表
 * 注：支持行级数据的过期时间的设置
 * 
 * @author      ZhengWei(HY)
 * @createDate  2024-03-15
 * @version     v1.0
 *              v2.0  2024-09-14  添加：插入、更新和保存一行数据时，可设置过期时间
 *              v3.0  2024-09-23  添加：开放字符串的get、set方法
 *              v4.0  2025-02-06  添加：两个getRow(库名 ,表名 ,类型)接口。
 *                                     用于在支持外界定义行数据类型是一个通用Map结构的同时，允许外界定义Map结构中的每个元素的Java类型
 */
public interface IRedis
{
    
    /** 内存库名称的前缘 */
    public static final String $Object_Database = "RDB";
    
    
    
    /**
     * 获取Redis真实操作的原始对象。方便对外界提供更多的基础功能
     * 
     * 如 Lettuce 返回 RedisAdvancedClusterCommands<String ,String> 对象
     * 如 Jedis   返回 ShardedJedis 对象
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @return
     */
    public Object getSource();
    
    
    
    /**
     * 获取Redis服务的当前时间（Unix时间）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-21
     * @version     v1.0
     *
     * @return
     */
    public Date getNowTime();
    
    
    
    /**
     * 获取Redis服务的当前时间（指定时区）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-21
     * @version     v1.0
     *
     * @param i_Timezone  时间区，如中国时区为 +8
     * @return
     */
    public Date getNowTime(int i_Timezone);
    
    
    
    /**
     * 创建内存表
     * 
     *   注：库名称不存在时，自动创建
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     */
    public boolean createTable(String i_Database ,String i_TableName);
    
    
    
    /**
     * 删除内存表
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     */
    public boolean dropTable(String i_Database ,String i_TableName);
    
    
    
    /**
     * 删除整个数据库
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @param i_Database  库名称
     * @return
     */
    public boolean dropDatabase(String i_Database);
    
    
    
    /**
     * 清空内存表数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @return             返回影响的行数。负数表示异常
     */
    public Long truncate(String i_Database ,String i_TableName);
    
    
    
    /**
     * 删除一行记录
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     *
     * @param i_Database    库名称
     * @param i_TableName   表名称
     * @param i_PrimaryKey  行主键
     * @return              返回影响的行数。负数表示异常
     */
    public Long delete(String i_Database ,String i_TableName ,String i_PrimaryKey);
    
    
    
    /**
     * 插入一行中的一个字段的数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Field      行字段
     * @param i_Value      行字段值
     * @return             返回影响的行数。负数表示异常
     */
    public Long insert(String i_Database ,String i_TableName ,String i_PrimaryKey ,String i_Field ,String i_Value);
    
    
    
    /**
     * 插入一行中的一个字段的数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_RData      数据信息
     * @return             返回影响的行数。负数表示异常
     */
    public Long insert(String i_Database ,String i_TableName ,RData i_RData);
    
    
    
    /**
     * 插入一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息
     * @return             返回影响的行数。负数表示异常
     */
    public Long insert(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas);
    
    
    
    /**
     * 插入一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-14
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息
     * @param i_ExpireTime 过期时间（单位：秒）
     * @return             返回影响的行数。负数表示异常
     */
    public Long insert(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,Long i_ExpireTime);
    
    
    
    /**
     * 插入一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息
     * @return             返回影响的行数。负数表示异常
     */
    public Long insert(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas);
    
    
    
    /**
     * 插入一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-14
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息
     * @param i_ExpireTime 过期时间（单位：秒）
     * @return             返回影响的行数。负数表示异常
     */
    public Long insert(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas ,Long i_ExpireTime);
    
    
    
    /**
     * 更新一行中的一个字段的数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 注：当行数据不存时：在Redis创建行数据，即有 insert() 方法的能力
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Field      对象属性
     * @param i_Value      对象值。当为 null 时，将执行Redis删除命令
     * @return             返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,String i_Field ,String i_Value);
    
    
    
    /**
     * 更新一行中的一个字段的数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 注：当行数据不存时：在Redis创建行数据，即有 insert() 方法的能力
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_RData      数据信息。当属性值为 null 时，将执行Redis删除命令
     * @return             返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,RData i_RData);
    
    
    
    /**
     * 更新一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 注：当行数据不存时：在Redis创建行数据，即有 insert() 方法的能力
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息。对象成员属性为 null 时，当 i_HaveNullValue 为假时，对象成员属性不参与更新
     * @return             返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas);
    
    
    
    /**
     * 更新一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 注：当行数据不存时：在Redis创建行数据，即有 insert() 方法的能力
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-18
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息。对象成员属性为 null 时，当 i_HaveNullValue 为假时，对象成员属性不参与更新
     * @param i_ExpireTime 过期时间（单位：秒）
     * @return             返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,Long i_ExpireTime);
    
    
    
    /**
     * 更新一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 注：当行数据不存时：在Redis创建行数据，即有 insert() 方法的能力
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-20
     * @version     v1.0
     * 
     * @param i_Database       库名称
     * @param i_TableName      表名称
     * @param i_PrimaryKey     行主键
     * @param i_Datas          数据信息。对象成员属性为 null 时，当 i_HaveNullValue 为假时，对象成员属性不参与更新
     *                                  对象成员属性为 null 时，当 i_HaveNullValue 为真时，对象成员属性将从Redis中删除
     * @param i_HaveNullValue  是否包含对象属性值为null的元素
     * @return                 返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,boolean i_HaveNullValue);
    
    
    
    /**
     * 更新一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 注：当行数据不存时：在Redis创建行数据，即有 insert() 方法的能力
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-18
     * @version     v1.0
     * 
     * @param i_Database       库名称
     * @param i_TableName      表名称
     * @param i_PrimaryKey     行主键
     * @param i_Datas          数据信息。对象成员属性为 null 时，当 i_HaveNullValue 为假时，对象成员属性不参与更新
     *                                  对象成员属性为 null 时，当 i_HaveNullValue 为真时，对象成员属性将从Redis中删除
     * @param i_HaveNullValue  是否包含对象属性值为null的元素
     * @param i_ExpireTime     过期时间（单位：秒）
     * @return                 返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,boolean i_HaveNullValue ,Long i_ExpireTime);
    
    
    
    /**
     * 更新一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 注：当行数据不存时：在Redis创建行数据，即有 insert() 方法的能力
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息。当为 Map.value 为 null 时，将执行Redis删除命令
     * @return             返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas);
    
    
    
    /**
     * 更新一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 注：当行数据不存时：在Redis创建行数据，即有 insert() 方法的能力
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-18
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息。当为 Map.value 为 null 时，将执行Redis删除命令
     * @param i_ExpireTime 过期时间（单位：秒）
     * @return             返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas ,Long i_ExpireTime);
    
    
    
    /**
     * 保存一行中的一个字段的数据（数据不存时：创建。数据存时：更新或删除）
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-20
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Field      对象属性
     * @param i_Value      对象值。当为 null 时，将执行Redis删除命令
     * @return             返回影响的行数。负数表示异常
     */
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,String i_Field ,String i_Value);
    
    
    
    /**
     * 保存一行中的一个字段的数据（数据不存时：创建。数据存时：更新或删除）
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-20
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_RData      数据信息。当属性值为 null 时，将执行Redis删除命令
     * @return             返回影响的行数。负数表示异常
     */
    public Long save(String i_Database ,String i_TableName ,RData i_RData);
    
    
    
    /**
     * 保存一行数据（数据不存时：创建。数据存时：更新或删除）
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-20
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息。对象成员属性为 null 时，当 i_HaveNullValue 为假时，对象成员属性不参与更新
     * @return             返回影响的行数。负数表示异常
     */
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas);
    
    
    
    /**
     * 保存一行数据（数据不存时：创建。数据存时：更新或删除）
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-18
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息。对象成员属性为 null 时，当 i_HaveNullValue 为假时，对象成员属性不参与更新
     * @param i_ExpireTime 过期时间（单位：秒）
     * @return             返回影响的行数。负数表示异常
     */
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,Long i_ExpireTime);
    
    
    
    /**
     * 保存一行数据（数据不存时：创建。数据存时：更新或删除）
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-20
     * @version     v1.0
     * 
     * @param i_Database       库名称
     * @param i_TableName      表名称
     * @param i_PrimaryKey     行主键
     * @param i_Datas          数据信息。对象成员属性为 null 时，当 i_HaveNullValue 为假时，对象成员属性不参与更新
     *                                  对象成员属性为 null 时，当 i_HaveNullValue 为真时，对象成员属性将从Redis中删除
     * @param i_HaveNullValue  是否包含对象属性值为null的元素
     * @return                 返回影响的行数。负数表示异常
     */
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,boolean i_HaveNullValue);
    
    
    
    /**
     * 保存一行数据（数据不存时：创建。数据存时：更新或删除）
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-18
     * @version     v1.0
     * 
     * @param i_Database       库名称
     * @param i_TableName      表名称
     * @param i_PrimaryKey     行主键
     * @param i_Datas          数据信息。对象成员属性为 null 时，当 i_HaveNullValue 为假时，对象成员属性不参与更新
     *                                  对象成员属性为 null 时，当 i_HaveNullValue 为真时，对象成员属性将从Redis中删除
     * @param i_HaveNullValue  是否包含对象属性值为null的元素
     * @param i_ExpireTime     过期时间（单位：秒）
     * @return                 返回影响的行数。负数表示异常
     */
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,boolean i_HaveNullValue ,Long i_ExpireTime);
    
    
    
    /**
     * 保存一行数据（数据不存时：创建。数据存时：更新或删除）
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-20
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息。当为 Map.value 为 null 时，将执行Redis删除命令
     * @return             返回影响的行数。负数表示异常
     */
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas);
    
    
    
    /**
     * 保存一行数据（数据不存时：创建。数据存时：更新或删除）
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-18
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息。当为 Map.value 为 null 时，将执行Redis删除命令
     * @param i_ExpireTime 过期时间（单位：秒）
     * @return             返回影响的行数。负数表示异常
     */
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas ,Long i_ExpireTime);
    
    
    
    /**
     * 获取一行数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-17
     * @version     v1.0
     *
     * @param i_PrimaryKey 行主键
     * @param i_RowClass   行类型的元类
     * @return             Map.key字段名，Map.value字段值
     */
    public Map<String ,String> getRow(String i_PrimaryKey);
    
    
    
    /**
     * 获取一行数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-17
     * @version     v1.0
     *
     * @param <E>          行类型
     * @param i_PrimaryKey 行主键
     * @param i_RowClass   行类型的元类
     * @return
     */
    public <E> E getRow(String i_PrimaryKey ,Class<E> i_RowClass);
    
    
    
    /**
     * 获取一行数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-17
     * @version     v1.0
     *
     * @param <E>          行类型
     * @param i_PrimaryKey 行主键
     * @param io_RowObject 行对象
     * @return
     */
    public <E> E getRow(String i_PrimaryKey ,E io_RowObject);
    
    
    
    /**
     * 获取一行数据（Map结构中元素类型的翻译）
     * 
     *   当i_RowClass为Map结构，并且配置有 RedisMapType 时，将按其转换为Java类型
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-02-06
     * @version     v1.0
     *
     * @param <E>          行类型
     * @param i_Database   库名称（允许为空或NULL）
     * @param i_TableName  表名称（允许为空或NULL）
     * @param i_PrimaryKey 行主键
     * @param i_RowClass   行类型的元类
     * @return
     */
    public <E> E getRow(String i_Database ,String i_TableName ,String i_PrimaryKey ,Class<E> i_RowClass);
    
    
    
    /**
     * 获取一行数据（Map结构中元素类型的翻译）
     * 
     *   当i_RowClass为Map结构，并且配置有 RedisMapType 时，将按其转换为Java类型
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-02-06
     * @version     v1.0
     *
     * @param <E>          行类型
     * @param i_Database   库名称（允许为空或NULL）
     * @param i_TableName  表名称（允许为空或NULL）
     * @param i_PrimaryKey 行主键
     * @param io_RowObject 行对象
     * @return
     */
    public <E> E getRow(String i_Database ,String i_TableName ,String i_PrimaryKey ,E io_RowObject);
    
    
    
    /**
     * 获取全库所有的表数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @return             Map.key行主键，Map.key表名称，Map.value表的创建时间
     */
    public Map<String ,Date> getTables(String i_Database);
    
    
    
    /**
     * 获取全库所有的表数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @return             Map.key行主键，Map.key表名称，Map.value表的创建时间
     */
    public Map<String ,String> getRows(String i_Database);
    
    
    
    /**
     * 获取全表数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-17
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @return             Map.key行主键，Map.Map.key字段名，Map.Map.value字段值
     */
    public TablePartitionRID<String ,String> getRows(String i_Database ,String i_TableName);
    
    
    
    /**
     * 获取全表数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-17
     * @version     v1.0
     *
     * @param <E>          行类型
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_RowClass   行类型的元类
     * @return             Map.key行主键，Map.value行数据
     */
    public <E> Map<String ,E> getRows(String i_Database ,String i_TableName ,Class<E> i_RowClass);
    
    
    
    /**
     * 获取全表数据（返回List集合）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @param <E>          行类型
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_RowClass   行类型的元类
     * @return             Map.key行主键，Map.value行数据
     */
    public <E> List<E> getRowsList(String i_Database ,String i_TableName ,Class<E> i_RowClass);
    
    
    
    /**
     * 获取数据库的创建时间
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @param i_Database  库名称
     * @return
     */
    public Date getCreateTime(String i_Database);
    
    
    
    /**
     * 获取表的创建时间
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @return
     */
    public Date getCreateTime(String i_Database ,String i_TableName);
    
    
    
    /**
     * 获取行记录的创建时间
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @return
     */
    public Date getCreateTime(String i_Database ,String i_TableName ,String i_PrimaryKey);
    
    
    
    /**
     * 库是否存在
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @return
     */
    public boolean isExists(String i_Database);
    
    
    
    /**
     * 表是否存在
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @return
     */
    public boolean isExists(String i_Database ,String i_TableName);
    
    
    
    /**
     * 行主键是否存在
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @param i_Database    库名称
     * @param i_TableName   表名称
     * @param i_PrimaryKey  行主键
     * @return
     */
    public boolean isExists(String i_Database ,String i_TableName ,String i_PrimaryKey);
    
    
    
    /**
     * 行字段是否存在
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @param i_Database    库名称
     * @param i_TableName   表名称
     * @param i_PrimaryKey  行主键
     * @param i_Field       行字段
     * @return
     */
    public boolean isExists(String i_Database ,String i_TableName ,String i_PrimaryKey ,String i_Field);
    
    
    
    /**
     * 设置关键字的过期时长
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-14
     * @version     v1.0
     *
     * @param i_Key         关键字
     * @param i_ExpireTime  过期时间（单位：秒）
     * @return
     */
    public boolean expire(String i_Key ,Long i_ExpireTime);
    
    
    
    /**
     * 设置数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-23
     * @version     v1.0
     *
     * @param i_Key    关键字
     * @param i_Value  数据
     * @return         成功返回true
     */
    public Boolean set(String i_Key ,String i_Value);
    
    
    
    /**
     * 设置数据，并且设定过期时长
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-23
     * @version     v1.0
     *
     * @param i_Key         关键字
     * @param i_Value       数据
     * @param i_ExpireTime  过期时间（单位：秒）
     * @return              成功返回true
     */
    public Boolean setex(String i_Key ,String i_Value ,Long i_ExpireTime);
    
    
    
    /**
     * 设置数据，仅在关键字不存在时设置数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-23
     * @version     v1.0
     *
     * @param i_Key    关键字
     * @param i_Value  数据
     * @return         是否设置数据
     */
    public Boolean setnx(String i_Key ,String i_Value);
    
    
    
    /**
     * 获取数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-23
     * @version     v1.0
     *
     * @param i_Key  关键字
     * @return
     */
    public String get(String i_Key);
    
    
    
    /**
     * 获取数据并删除
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-23
     * @version     v1.0
     *
     * @param i_Key  关键字
     * @return
     */
    public String getdel(String i_Key);
    
    
    
    /**
     * 删除数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-23
     * @version     v1.0
     *
     * @param i_Keys  一个或多个关键字
     * @return        返回删除数据的数量
     */
    public Long del(String ... i_Keys);
    
}

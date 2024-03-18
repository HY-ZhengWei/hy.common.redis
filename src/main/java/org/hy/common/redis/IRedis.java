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
 * 
 * @author      ZhengWei(HY)
 * @createDate  2024-03-15
 * @version     v1.0
 */
public interface IRedis
{
    
    /** 内存库名称的前缘 */
    public static final String $Object_Database = "RDB";
    
    
    
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
     * @param i_Field      对象属性
     * @param i_Value      对象值
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
     * 更新一行中的一个字段的数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Field      对象属性
     * @param i_Value      对象值
     * @return             返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,String i_Field ,String i_Value);
    
    
    
    /**
     * 更新一行中的一个字段的数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_RData      数据信息
     * @return             返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,RData i_RData);
    
    
    
    /**
     * 更新一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息
     * @return             返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas);
    
    
    
    /**
     * 更新一行数据
     * 
     * 注：表不存时，自动创建表、库关系等信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     * 
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_PrimaryKey 行主键
     * @param i_Datas      数据信息
     * @return             返回影响的行数。负数表示异常
     */
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas);
    
    
    
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
    
}

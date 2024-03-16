package org.hy.common.redis;

import java.util.Map;





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
     * 插入一行或多行数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_TableName  表名称
     * @param i_RDatas     数据信息(相同行的多个字段信息的RData的getKey()为同一值)
     * @return             按顺序返回行主键集合，其行主键的个数为实际行个数，而不i_RDatas.length的个数
     */
    //public List<String> inserts(String i_TableName ,RData ... i_RDatas);
    
    
    
    /**
     * 插入一行或多行数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_TableName  表名称
     * @param i_RDatas     数据信息(相同行的多个字段信息的RData的getKey()为同一值)
     * @return             按顺序返回行主键集合，其行主键的个数为实际行个数，而不i_RDatas.length的个数
     */
    //public List<String> inserts(String i_TableName ,List<RData> i_RDatas);
    
    
    
    /**
     * 修改一行中的一个字段的数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_TableName  表名称
     * @param i_Key        对象ID
     * @param i_Field      对象属性
     * @param i_Value      对象值
     * @return             返回行主键
     */
    //public String update(String i_TableName ,String i_Key ,String i_Field ,String i_Value);
    
    
    
    /**
     * 修改一行中的一个字段的数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_TableName  表名称
     * @param i_RData      数据信息
     * @return             返回行主键
     */
    //public String update(String i_TableName ,RData i_RData);
    
    
    
    /**
     * 修改一行或多行数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_TableName  表名称
     * @param i_RDatas     数据信息(相同行的多个字段信息的RData的getKey()为同一值)
     * @return             按顺序返回行主键集合，其行主键的个数为实际行个数，而不i_RDatas.length的个数
     */
    //public List<String> update(String i_TableName ,RData ... i_RDatas);
    
    
    
    /**
     * 修改一行或多行数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_TableName  表名称
     * @param i_RDatas     数据信息(相同行的多个字段信息的RData的getKey()为同一值)
     * @return             按顺序返回行主键集合，其行主键的个数为实际行个数，而不i_RDatas.length的个数
     */
    //public List<String> update(String i_TableName ,List<RData> i_RDatas);
    
    
    
    /**
     * 删除行
     * 
     * 如果整行数据都不存在，同时删除行信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_TableName  表名称
     * @param i_Keys       对象ID--关键字。注意不是行主键
     */
    //public void delete(String i_TableName ,String ... i_Keys);
    
    
    
    /**
     * 删除行
     * 
     * 如果整行数据都不存在，同时删除行信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_TableName  表名称
     * @param i_Keys       对象ID--关键字。注意不是行主键
     */
    //public void delete(String i_TableName ,List<String> i_Keys);
    
    
    
    /**
     * 删除字段。
     * 
     * 如果整行数据都不存在，同时删除行信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_TableName
     * @param i_RDatas
     */
    //public void delete(String i_TableName ,RData ... i_RDatas);
    
    
    
    /**
     * 删除字段。
     * 
     * 如果整行数据都不存在，同时删除行信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     * 
     * @param i_TableName
     * @param i_RDatas
     */
    //public void delete(String i_TableName ,List<RData> i_RDatas);
    
}

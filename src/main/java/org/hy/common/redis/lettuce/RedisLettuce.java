package org.hy.common.redis.lettuce;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hy.common.Date;
import org.hy.common.Help;
import org.hy.common.MethodReflect;
import org.hy.common.TablePartitionRID;
import org.hy.common.redis.IRedis;
import org.hy.common.redis.RData;
import org.hy.common.redis.cluster.RedisClusterConfig;
import org.hy.common.xml.SerializableDef;
import org.hy.common.xml.log.Logger;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;





/**
 * Redis数据库访问的Lettuce实现（集群模式）
 *
 * @author      ZhengWei(HY)
 * @createDate  2024-03-15
 * @version     v1.0
 *              v2.0  2024-09-14  添加：主动删除因过期时间、脏数据等原因的数据
 *                                添加：插入、更新和保存一行数据时，可设置过期时间
 *              v3.0  2025-02-06  添加：支持外界定义行数据类型(Class<E> i_RowClass)不是一个Java类，而是一个通用Map结构。
 *                                    注：仅支持Map<String ,Object>结构的Map集合
 */
public class RedisLettuce implements IRedis
{

    private static final Logger                          $Logger       = new Logger(RedisLettuce.class);
    
    private static final long                            $UnixBaseTime = new Date("1970-01-01 00:00:00.000").getTime();

    private RedisClusterClient                           clusterClient;

    private RedisAdvancedClusterCommands<String ,String> clusterCmd;



    public RedisLettuce(RedisClusterConfig i_RedisClusterConfig)
    {
        this(i_RedisClusterConfig.toLettuce());
    }



    public RedisLettuce(RedisClusterClient i_ClusterClient)
    {
        this.clusterClient = i_ClusterClient;
        
        try
        {
            // 创建连接到 Redis 集群的连接
            this.clusterCmd = this.clusterClient.connect().sync();
        }
        catch (Exception exce)
        {
            $Logger.error("Failed to connect to the Redis cluster" ,exce);
            throw exce;
        }
    }



    @Override
    protected void finalize() throws Throwable
    {
        this.clusterClient.shutdown();
        this.clusterClient = null;
    }
    
    
    
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
    @Override
    public Object getSource()
    {
        return this.clusterCmd;
    }
    
    
    
    /**
     * 获取Redis服务的当前时间（Unix时间）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-21
     * @version     v1.0
     *
     * @return     返回时间精度：毫秒
     */
    @Override
    public Date getNowTime()
    {
        return this.getNowTime(0);
    }
    
    
    
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
    @Override
    public Date getNowTime(int i_Timezone)
    {
        List<String> v_UnixTime = this.clusterCmd.time();
        if ( Help.isNull(v_UnixTime) || v_UnixTime.size() < 2 )
        {
            return null;
        }
        
        long v_Second      = Long.parseLong(v_UnixTime.get(0));        // 1970-01-01以来的秒数
        long v_MilliSecond = Long.parseLong(v_UnixTime.get(1)) / 1000; // 微秒转为毫秒
        
        v_UnixTime.clear();
        v_UnixTime = null;
        
        return new Date($UnixBaseTime + v_Second * 1000 + v_MilliSecond + i_Timezone * 60 * 60 * 1000);
    }



    /**
     * 获取库的物理名称。即将逻辑名称转为真实保存在Redis的Key值
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     *
     * @param i_Database  库名称（逻辑名称）
     * @return            表的物理名称
     */
    private String getDatabaseID(String i_Database)
    {
        return $Object_Database + i_Database;
    }



    /**
     * 获取表的物理名称。即将逻辑名称转为真实保存在Redis的Key值
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     *
     * @param i_Database   库名称（逻辑名称）
     * @param i_TableName  表名称（逻辑名称）
     * @return             表的物理名称
     */
    private String getTableID(String i_Database ,String i_TableName)
    {
        return i_Database + "." + i_TableName;
    }



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
    @Override
    public synchronized boolean createTable(String i_Database ,String i_TableName)
    {
        String v_DBID    = this.getDatabaseID(i_Database);
        String v_TableID = this.getTableID(   i_Database ,i_TableName);
        
        // 判定表对象是否存在
        if ( this.isExistsTable_Core(v_TableID) )
        {
            String v_CreateTime = this.clusterCmd.hget(v_DBID ,v_TableID);
            $Logger.error("Table[" + v_TableID + "] exists ,it was created at " + v_CreateTime);
            return false;
        }
        
        if ( !this.isExistsDatabase_Core(v_DBID) )
        {
            // 添加一个空主键，使用空字段实现预占用的创建库Hash对象
            // 不通过返回值判定，也不报错，提高容错性
            this.clusterCmd.hsetnx(v_DBID ,"" ,new Date().getFull());
        }
        
        // 判定表是否关系到库（不存在是创建关系，而不是报错，提高容错性）
        String v_Now = Date.getNowTime().getFull();
        if ( !this.clusterCmd.hexists(v_DBID ,v_TableID) )
        {
            if ( !this.clusterCmd.hset(v_DBID ,v_TableID ,v_Now) )
            {
                $Logger.error("An exception occurred while creating the Table[" + v_TableID + "] for MetaData.");
                return false;
            }
        }
        
        // 添加一个空主键，使用空字段实现预占用的创建表Hash对象
        // 不通过返回值判定，也不报错，提高容错性
        this.clusterCmd.hsetnx(v_TableID ,"" ,v_Now);
        return true;
    }



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
    @Override
    public boolean dropTable(String i_Database ,String i_TableName)
    {
        if ( Help.isNull(i_Database) )
        {
            return false;
        }
        if ( Help.isNull(i_TableName) )
        {
            return false;
        }
        
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        if ( !this.isExistsTable_Core(v_TableID) )
        {
            return false;
        }
        
        String v_DBID = this.getDatabaseID(i_Database);
        if ( !this.isExistsDatabase_Core(v_DBID) )
        {
            return false;
        }
        
        return this.dropTable_Core(v_DBID ,v_TableID);
    }
    
    
    
    /**
     * 删除内存表
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     *
     * @param i_DBID     库的物理名称。即在Redis中保存的真实Key值
     * @param i_TableID  表的物理名称。即在Redis中保存的真实Key值
     */
    private boolean dropTable_Core(String i_DBID ,String i_TableID)
    {
        this.truncate_Core(i_TableID);                        // 清空数据
        this.clusterCmd.hdel(i_TableID ,"");                  // 删除 空主键
        this.clusterCmd.del(i_TableID);                       // 删除表
        return this.clusterCmd.hdel(i_DBID ,i_TableID) >= 1L; // 删除表库关系
    }
    
    
    
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
    @Override
    public boolean dropDatabase(String i_Database)
    {
        if ( Help.isNull(i_Database) )
        {
            return false;
        }
        
        String v_DBID = this.getDatabaseID(i_Database);
        if ( !this.isExistsDatabase_Core(v_DBID) )
        {
            return false;
        }
        
        Map<String ,String> v_Tables = this.getRows(i_Database);
        if ( !Help.isNull(v_Tables) )
        {
            for (String v_TableID : v_Tables.keySet())
            {
                if ( !dropTable_Core(v_DBID ,v_TableID) )
                {
                    return false;
                }
            }
        }
        
        return this.clusterCmd.del(v_DBID) >= 1L;
    }



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
    @Override
    public Long truncate(String i_Database ,String i_TableName)
    {
        if ( Help.isNull(i_Database) )
        {
            return -1L;
        }
        if ( Help.isNull(i_TableName) )
        {
            return -1L;
        }
        
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        if ( !this.isExistsTable_Core(v_TableID) )
        {
            return -1L;
        }
        
        return truncate_Core(v_TableID);
    }
    
    
    
    /**
     * 清空内存表数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     *
     * @param i_TableID  表的物理名称。即在Redis中保存的真实Key值
     * @return             返回影响的行数。负数表示异常
     */
    private Long truncate_Core(String i_TableID)
    {
        Map<String ,String> v_RowIDs = this.clusterCmd.hgetall(i_TableID);
        if ( Help.isNull(v_RowIDs) )
        {
            return 0L;
        }
        
        long v_Count = 0L;
        for (String v_RowID : v_RowIDs.keySet())
        {
            if ( Help.isNull(v_RowID) )
            {
                continue;
            }
            
            Long v_DelRet = this.delete_Core(i_TableID ,v_RowID);
            if ( v_DelRet >= 0L )
            {
                v_Count += v_DelRet;
            }
        }
        
        return v_Count;
    }



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
    @Override
    public Long delete(String i_Database ,String i_TableName ,String i_PrimaryKey)
    {
        if ( Help.isNull(i_Database) )
        {
            return -1L;
        }
        if ( Help.isNull(i_TableName) )
        {
            return -1L;
        }
        if ( Help.isNull(i_PrimaryKey) )
        {
            return -1L;
        }
        
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        if ( !this.isExistsTable_Core(v_TableID) )
        {
            return -1L;
        }
        
        return this.delete_Core(v_TableID ,i_PrimaryKey);
    }



    /**
     * 删除一行记录
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     *
     * @param i_TableID     表的物理名称。即在Redis中保存的真实Key值
     * @param i_PrimaryKey  主键
     * @return
     */
    private Long delete_Core(String i_TableID ,String i_PrimaryKey)
    {
        this.clusterCmd.hdel(i_TableID ,i_PrimaryKey);
        return this.clusterCmd.del(i_PrimaryKey);
    }



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
    @Override
    public Long insert(String i_Database ,String i_TableName ,String i_PrimaryKey ,String i_Field ,String i_Value)
    {
        if ( Help.isNull(i_Database) )
        {
            return -1L;
        }
        if ( Help.isNull(i_TableName) )
        {
            return -1L;
        }
        if ( Help.isNull(i_PrimaryKey) )
        {
            return -1L;
        }
        if ( Help.isNull(i_Field) )
        {
            return -1L;
        }
        
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        if ( !this.isExistsTable_Core(v_TableID) )
        {
            if ( !this.createTable(i_Database ,i_TableName) )
            {
                return -2L;
            }
        }
        return this.insert_Core(v_TableID ,i_PrimaryKey ,i_Field ,i_Value);
    }



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
    @Override
    public Long insert(String i_Database ,String i_TableName ,RData i_RData)
    {
        if ( Help.isNull(i_Database) )
        {
            return -1L;
        }
        if ( Help.isNull(i_TableName) )
        {
            return -1L;
        }
        if ( i_RData == null )
        {
            return -1L;
        }
        if ( Help.isNull(i_RData.getKey()) )
        {
            return -1L;
        }
        if ( Help.isNull(i_RData.getField()) )
        {
            return -1L;
        }
        
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        if ( !this.isExistsTable_Core(v_TableID) )
        {
            if ( !this.createTable(i_Database ,i_TableName) )
            {
                return -2L;
            }
        }
        return insert_Core(v_TableID ,i_RData.getKey() ,i_RData.getField() ,i_RData.getValue());
    }
    
    
    
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
    @Override
    public Long insert(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas)
    {
        return this.insert(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas ,null);
    }



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
    @Override
    public Long insert(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,Long i_ExpireTime)
    {
        if ( i_Datas == null )
        {
            return -1L;
        }
        
        // 基础类型无法按一行数据写入
        if ( Help.isBasicDataType(i_Datas.getClass()) )
        {
            return -8L;
        }
        
        try
        {
            return this.insert(i_Database ,i_TableName ,i_PrimaryKey ,Help.toMap(i_Datas) ,i_ExpireTime);
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return -9L;
        }
    }
    
    
    
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
    @Override
    public Long insert(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas)
    {
        return this.insert(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas ,null);
    }



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
    @Override
    public Long insert(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas ,Long i_ExpireTime)
    {
        if ( Help.isNull(i_Database) )
        {
            return -1L;
        }
        if ( Help.isNull(i_TableName) )
        {
            return -1L;
        }
        if ( Help.isNull(i_PrimaryKey) )
        {
            return -1L;
        }
        if ( Help.isNull(i_Datas) )
        {
            return -1L;
        }
        
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        if ( !this.isExistsTable_Core(v_TableID) )
        {
            if ( !this.createTable(i_Database ,i_TableName) )
            {
                return -2L;
            }
        }
        
        long v_Count = 0L;
        for (Map.Entry<String ,Object> v_Data : i_Datas.entrySet())
        {
            if ( v_Data.getValue() == null )
            {
                v_Count += this.insert_Core(v_TableID ,i_PrimaryKey ,v_Data.getKey() ,null);
            }
            else
            {
                v_Count += this.insert_Core(v_TableID ,i_PrimaryKey ,v_Data.getKey() ,v_Data.getValue().toString());
            }
        }
        
        // 设置行级过期时间
        if ( v_Count >= 1 )
        {
            this.expire(i_PrimaryKey ,i_ExpireTime);
        }
        
        return v_Count;
    }



    /**
     * 插入一行中的一个字段的数据
     * 
     * @author     ZhengWei(HY)
     * @createDate 2024-03-16
     * @version    v1.0
     *
     * @param i_TableID     表的物理名称。即在Redis中保存的真实Key值
     * @param i_PrimaryKey  行主键
     * @param i_Field       对象属性
     * @param i_Value       对象值
     * @return              返回影响的行数。负数表示异常
     */
    private Long insert_Core(String i_TableID ,String i_PrimaryKey ,String i_Field ,String i_Value)
    {
        // 表、主键关系
        this.clusterCmd.hsetnx(i_TableID ,i_PrimaryKey ,Date.getNowTime().getFull());
        
        // 一行中的一个字段的数据
        if ( this.clusterCmd.hsetnx(i_PrimaryKey ,i_Field ,i_Value) )
        {
            return 1L;
        }
        else
        {
            return 0L;
        }
    }



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
    @Override
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,String i_Field ,String i_Value)
    {
        if ( Help.isNull(i_Database) )
        {
            return -1L;
        }
        if ( Help.isNull(i_TableName) )
        {
            return -1L;
        }
        if ( Help.isNull(i_PrimaryKey) )
        {
            return -1L;
        }
        if ( Help.isNull(i_Field) )
        {
            return -1L;
        }
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        if ( !this.isExistsTable_Core(v_TableID) )
        {
            if ( !this.createTable(i_Database ,i_TableName) )
            {
                return -2L;
            }
        }
        return this.update_Core(v_TableID ,i_PrimaryKey ,i_Field ,i_Value);
    }



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
    @Override
    public Long update(String i_Database ,String i_TableName ,RData i_RData)
    {
        if ( Help.isNull(i_Database) )
        {
            return -1L;
        }
        if ( Help.isNull(i_TableName) )
        {
            return -1L;
        }
        if ( i_RData == null )
        {
            return -1L;
        }
        if ( Help.isNull(i_RData.getKey()) )
        {
            return -1L;
        }
        if ( Help.isNull(i_RData.getField()) )
        {
            return -1L;
        }
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        if ( !this.isExistsTable_Core(v_TableID) )
        {
            if ( !this.createTable(i_Database ,i_TableName) )
            {
                return -2L;
            }
        }
        return this.update_Core(v_TableID ,i_RData.getKey() ,i_RData.getField() ,i_RData.getValue());
    }



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
    @Override
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas)
    {
        return this.update(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas ,false);
    }
    
    
    
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
    @Override
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,Long i_ExpireTime)
    {
        return this.update(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas ,false ,i_ExpireTime);
    }
    
    
    
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
     * @param i_Database       库名称
     * @param i_TableName      表名称
     * @param i_PrimaryKey     行主键
     * @param i_Datas          数据信息。对象成员属性为 null 时，当 i_HaveNullValue 为假时，对象成员属性不参与更新
     *                                  对象成员属性为 null 时，当 i_HaveNullValue 为真时，对象成员属性将从Redis中删除
     * @param i_HaveNullValue  是否包含对象属性值为null的元素
     * @return                 返回影响的行数。负数表示异常
     */
    @Override
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,boolean i_HaveNullValue)
    {
        return this.update(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas ,i_HaveNullValue ,null);
    }
    
    
    
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
    @SuppressWarnings("unchecked")
    @Override
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,boolean i_HaveNullValue ,Long i_ExpireTime)
    {
        if ( i_Datas == null )
        {
            return -1L;
        }
        
        // 基础类型无法按一行数据写入
        if ( Help.isBasicDataType(i_Datas.getClass()) )
        {
            return -8L;
        }
        
        try
        {
            if ( MethodReflect.isExtendImplement(i_Datas ,Map.class) )
            {
                return this.update(i_Database ,i_TableName ,i_PrimaryKey ,(Map<String ,Object>)i_Datas ,i_ExpireTime);
            }
            else
            {
                return this.update(i_Database ,i_TableName ,i_PrimaryKey ,Help.toMap(i_Datas ,null ,i_HaveNullValue ,false) ,i_ExpireTime);
            }
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return -9L;
        }
    }



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
    @Override
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas)
    {
        return this.update(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas ,null);
    }
    
    
    
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
    @Override
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas ,Long i_ExpireTime)
    {
        if ( Help.isNull(i_Database) )
        {
            return -1L;
        }
        if ( Help.isNull(i_TableName) )
        {
            return -1L;
        }
        if ( Help.isNull(i_PrimaryKey) )
        {
            return -1L;
        }
        if ( Help.isNull(i_Datas) )
        {
            return -1L;
        }
        
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        if ( !this.isExistsTable_Core(v_TableID) )
        {
            if ( !this.createTable(i_Database ,i_TableName) )
            {
                return -2L;
            }
        }
        
        long v_Count = 0L;
        for (Map.Entry<String ,Object> v_Data : i_Datas.entrySet())
        {
            if ( v_Data.getValue() == null )
            {
                v_Count += this.update_Core(v_TableID ,i_PrimaryKey ,v_Data.getKey() ,null);
            }
            else
            {
                v_Count += this.update_Core(v_TableID ,i_PrimaryKey ,v_Data.getKey() ,v_Data.getValue().toString());
            }
        }
        
        // 设置行级过期时间
        if ( v_Count >= 1 )
        {
            this.expire(i_PrimaryKey ,i_ExpireTime);
        }
        
        return v_Count;
    }



    /**
     * 更新一行中的一个字段的数据
     * 
     * 注：当行数据不存时：在Redis创建行数据，即有 insert() 方法的能力
     * 
     * @author ZhengWei(HY)
     * @createDate 2024-03-16
     * @version v1.0
     *
     * @param i_TableID     表的物理名称。即在Redis中保存的真实Key值
     * @param i_PrimaryKey  行主键
     * @param i_Field       对象属性
     * @param i_Value       对象值。当为 null 时，将执行Redis删除命令
     * @return              返回影响的行数。负数表示异常
     */
    private Long update_Core(String i_TableID ,String i_PrimaryKey ,String i_Field ,String i_Value)
    {
        // 表、主键关系
        this.clusterCmd.hsetnx(i_TableID ,i_PrimaryKey ,Date.getNowTime().getFull());
        
        if ( i_Value == null )
        {
            // 一行中的一个字段被删除
            this.clusterCmd.hdel(i_PrimaryKey ,i_Field);
        }
        else
        {
            // 一行中的一个字段的数据
            this.clusterCmd.hset(i_PrimaryKey ,i_Field ,i_Value);
        }
        return 1L;
    }
    
    
    
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
    @Override
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,String i_Field ,String i_Value)
    {
        return this.update(i_Database ,i_TableName ,i_PrimaryKey ,i_Field ,i_Value);
    }
    
    
    
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
    @Override
    public Long save(String i_Database ,String i_TableName ,RData i_RData)
    {
        return this.update(i_Database ,i_TableName ,i_RData);
    }
    
    
    
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
    @Override
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas)
    {
        return this.update(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas);
    }
    
    
    
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
    @Override
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,Long i_ExpireTime)
    {
        return this.update(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas ,i_ExpireTime);
    }
    
    
    
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
    @Override
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,boolean i_HaveNullValue)
    {
        return this.update(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas ,i_HaveNullValue);
    }
    
    
    
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
    @Override
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas ,boolean i_HaveNullValue ,Long i_ExpireTime)
    {
        return this.update(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas ,i_HaveNullValue ,i_ExpireTime);
    }
    
    
    
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
    @Override
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas)
    {
        return this.update(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas);
    }
    
    
    
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
    @Override
    public Long save(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas ,Long i_ExpireTime)
    {
        return this.update(i_Database ,i_TableName ,i_PrimaryKey ,i_Datas ,i_ExpireTime);
    }
    
    
    
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
    @Override
    public Map<String ,String> getRow(String i_PrimaryKey)
    {
        if ( Help.isNull(i_PrimaryKey) )
        {
            return null;
        }
        
        return this.clusterCmd.hgetall(i_PrimaryKey);
    }



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
    @Override
    public <E> E getRow(String i_PrimaryKey ,Class<E> i_RowClass)
    {
        if ( Help.isNull(i_PrimaryKey) )
        {
            return null;
        }
        
        if ( i_RowClass == null )
        {
            return null;
        }
        
        try
        {
            E v_RowObject = i_RowClass.getDeclaredConstructor().newInstance();
            return this.getRow(i_PrimaryKey ,v_RowObject);
        }
        catch (Exception Exce)
        {
            $Logger.error(Exce);
        }
        return null;
    }



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
     * @return             查不时返回NULL
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public <E> E getRow(String i_PrimaryKey ,E io_RowObject)
    {
        if ( Help.isNull(i_PrimaryKey) )
        {
            return null;
        }
        
        if ( io_RowObject == null )
        {
            return null;
        }
        
        Map<String ,String> v_RowDatas = this.clusterCmd.hgetall(i_PrimaryKey);
        
        if ( Help.isNull(v_RowDatas) )
        {
            return null;
        }
        else if ( MethodReflect.isExtendImplement(io_RowObject ,Map.class) )
        {
            for (Map.Entry<String ,String> v_Item : v_RowDatas.entrySet())
            {
                ((Map<String ,Object>)io_RowObject).put(v_Item.getKey() ,v_Item.getValue());
            }
        }
        else if ( io_RowObject instanceof SerializableDef )
        {
            ((SerializableDef) io_RowObject).initNotNull(v_RowDatas);
        }
        else
        {
            Map<String ,Method> v_SetMethods = MethodReflect.getSetMethodsMG(io_RowObject.getClass());
            
            for (Map.Entry<String ,String> v_Item : v_RowDatas.entrySet())
            {
                String v_MethodName = v_Item.getKey().substring(0 ,1).toUpperCase() + v_Item.getKey().substring(1);
                Method v_SetMethod  = v_SetMethods.get(v_MethodName);
                if ( v_SetMethod == null )
                {
                    continue;
                }
                
                Type     v_ParameterType  = v_SetMethod.getParameters()[0].getParameterizedType();
                Class<?> v_ParameterClass = (Class<?>) v_ParameterType;
                Object   v_ParameterValue = null;
                
                try
                {
                    if ( v_Item.getValue() != null )
                    {
                        if ( "".equals(v_Item.getValue().trim()) )
                        {
                            if ( String.class.equals(v_ParameterClass) )
                            {
                                v_ParameterValue = v_Item.getValue();
                            }
                        }
                        else
                        {
                            v_ParameterValue = Help.toObject(v_ParameterClass ,v_Item.getValue());
                        }
                    }
                    
                    v_SetMethod.invoke(io_RowObject ,v_ParameterValue);
                }
                catch (Exception exce)
                {
                    $Logger.error(exce);
                }
            }
        }
        
        v_RowDatas.clear();
        v_RowDatas = null;
        return io_RowObject;
    }
    
    
    
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
    @Override
    public Map<String ,Date> getTables(String i_Database)
    {
        if ( Help.isNull(i_Database) )
        {
            return null;
        }
        
        Map<String ,Date>   v_Ret   = new HashMap<String ,Date>();
        Map<String ,String> v_Datas = this.getRows(i_Database);
        
        if ( !Help.isNull(v_Datas) )
        {
            for (Map.Entry<String ,String> v_Item : v_Datas.entrySet())
            {
                v_Ret.put(v_Item.getKey() ,new Date(v_Item.getValue()));
            }
        }
        
        v_Datas.clear();
        v_Datas = null;
        return v_Ret;
    }
    
    
    
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
    @Override
    public Map<String ,String> getRows(String i_Database)
    {
        if ( Help.isNull(i_Database) )
        {
            return null;
        }
        
        String v_DBID = this.getDatabaseID(i_Database);
        Map<String ,String> v_Datas = this.clusterCmd.hgetall(v_DBID);
        
        if ( !Help.isNull(v_Datas) )
        {
            // 空主键是创建【库】时预留的
            v_Datas.remove("");
        }
        
        return v_Datas;
    }
    
    
    
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
    @Override
    public TablePartitionRID<String ,String> getRows(String i_Database ,String i_TableName)
    {
        if ( Help.isNull(i_Database) )
        {
            return null;
        }
        
        if ( Help.isNull(i_TableName) )
        {
            return null;
        }
        
        TablePartitionRID<String ,String> v_Rows     = new TablePartitionRID<String ,String>();
        String                            v_TableID  = this.getTableID(i_Database ,i_TableName);
        Map<String ,String>               v_RowDatas = this.clusterCmd.hgetall(v_TableID);
        
        if ( !Help.isNull(v_RowDatas) )
        {
            for (Map.Entry<String ,String> v_RowItem : v_RowDatas.entrySet())
            {
                // 空主键是创建表时预留的
                if ( Help.isNull(v_RowItem.getKey()) )
                {
                    continue;
                }
                
                Map<String ,String> v_RowObject = this.getRow(v_RowItem.getKey());
                
                if ( !Help.isNull(v_RowObject) )
                {
                    v_Rows.putRows(v_RowItem.getKey() ,v_RowObject);
                }
                else
                {
                    // 2024-09-14 Add
                    // 只有主键信息，没有行数据信息时，删除主键信息
                    // 这情况产生的原因有：
                    //     情况1：行数据信息因到过期时间后被Redis释放
                    //     情况2：本类库Bug而生成的脏数据
                    //     情况3：本类库生成的结构被第三方窜改
                    this.delete(i_Database ,i_TableName ,v_RowItem.getKey());
                }
            }
        }
        
        v_RowDatas.clear();
        v_RowDatas = null;
        return v_Rows;
    }
    
    
    
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
    @Override
    public <E> Map<String ,E> getRows(String i_Database ,String i_TableName ,Class<E> i_RowClass)
    {
        if ( Help.isNull(i_Database) )
        {
            return null;
        }
        
        if ( Help.isNull(i_TableName) )
        {
            return null;
        }
        
        Map<String ,E>      v_Rows     = new HashMap<String ,E>();
        String              v_TableID  = this.getTableID(i_Database ,i_TableName);
        Map<String ,String> v_RowDatas = this.clusterCmd.hgetall(v_TableID);
        
        if ( !Help.isNull(v_RowDatas) )
        {
            for (Map.Entry<String ,String> v_RowItem : v_RowDatas.entrySet())
            {
                // 空主键是创建表时预留的
                if ( Help.isNull(v_RowItem.getKey()) )
                {
                    continue;
                }
                
                E v_RowObject = this.getRow(v_RowItem.getKey() ,i_RowClass);
                
                if ( v_RowObject != null )
                {
                    v_Rows.put(v_RowItem.getKey() ,v_RowObject);
                }
                else
                {
                    // 2024-09-14 Add
                    // 只有主键信息，没有行数据信息时，删除主键信息
                    // 这情况产生的原因有：
                    //     情况1：行数据信息因到过期时间后被Redis释放
                    //     情况2：本类库Bug而生成的脏数据
                    //     情况3：本类库生成的结构被第三方窜改
                    this.delete(i_Database ,i_TableName ,v_RowItem.getKey());
                }
            }
        }
        
        v_RowDatas.clear();
        v_RowDatas = null;
        return v_Rows;
    }
    
    
    
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
    @Override
    public <E> List<E> getRowsList(String i_Database ,String i_TableName ,Class<E> i_RowClass)
    {
        if ( Help.isNull(i_Database) )
        {
            return null;
        }
        
        if ( Help.isNull(i_TableName) )
        {
            return null;
        }
        
        List<E>             v_Rows     = new ArrayList<E>();
        String              v_TableID  = this.getTableID(i_Database ,i_TableName);
        Map<String ,String> v_RowDatas = this.clusterCmd.hgetall(v_TableID);
        
        if ( !Help.isNull(v_RowDatas) )
        {
            for (Map.Entry<String ,String> v_RowItem : v_RowDatas.entrySet())
            {
                // 空主键是创建表时预留的
                if ( Help.isNull(v_RowItem.getKey()) )
                {
                    continue;
                }
                
                E v_RowObject = this.getRow(v_RowItem.getKey() ,i_RowClass);
                
                if ( v_RowObject != null )
                {
                    v_Rows.add(v_RowObject);
                }
                else
                {
                    // 2024-09-14 Add
                    // 只有主键信息，没有行数据信息时，删除主键信息
                    // 这情况产生的原因有：
                    //     情况1：行数据信息因到过期时间后被Redis释放
                    //     情况2：本类库Bug而生成的脏数据
                    //     情况3：本类库生成的结构被第三方窜改
                    this.delete(i_Database ,i_TableName ,v_RowItem.getKey());
                }
            }
        }
        
        v_RowDatas.clear();
        v_RowDatas = null;
        return v_Rows;
    }
    
    
    
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
    @Override
    public Date getCreateTime(String i_Database)
    {
        if ( Help.isNull(i_Database) )
        {
            return null;
        }
        
        String v_DBID = this.getDatabaseID(i_Database);
        String v_Time = this.clusterCmd.hget(v_DBID ,"");
        
        if ( v_Time == null )
        {
            return null;
        }
        else
        {
            return new Date(v_Time);
        }
    }
    
    
    
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
    @Override
    public Date getCreateTime(String i_Database ,String i_TableName)
    {
        if ( Help.isNull(i_Database) )
        {
            return null;
        }
        
        if ( Help.isNull(i_TableName) )
        {
            return null;
        }
        
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        String v_Time = this.clusterCmd.hget(v_TableID ,"");
        
        if ( v_Time == null )
        {
            return null;
        }
        else
        {
            return new Date(v_Time);
        }
    }
    
    
    
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
    @Override
    public Date getCreateTime(String i_Database ,String i_TableName ,String i_PrimaryKey)
    {
        if ( Help.isNull(i_Database) )
        {
            return null;
        }
        
        if ( Help.isNull(i_TableName) )
        {
            return null;
        }
        
        if ( Help.isNull(i_PrimaryKey) )
        {
            return null;
        }
        
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        String v_Time = this.clusterCmd.hget(v_TableID ,i_PrimaryKey);
        
        if ( v_Time == null )
        {
            return null;
        }
        else
        {
            return new Date(v_Time);
        }
    }
    
    
    
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
    @Override
    public boolean isExists(String i_Database)
    {
        if ( Help.isNull(i_Database) )
        {
            return false;
        }
        
        String v_DBID = this.getDatabaseID(i_Database);
        return this.isExistsDatabase_Core(v_DBID);
    }
    
    
    
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
    @Override
    public boolean isExists(String i_Database ,String i_TableName)
    {
        if ( Help.isNull(i_Database) )
        {
            return false;
        }
        
        if ( Help.isNull(i_TableName) )
        {
            return false;
        }
        
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        return this.isExistsTable_Core(v_TableID);
    }
    
    
    
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
    @Override
    public boolean isExists(String i_Database ,String i_TableName ,String i_PrimaryKey)
    {
        // 注：不用判定库名称是否为空，因为没有用到它
        
        if ( Help.isNull(i_TableName) )
        {
            return false;
        }
        
        if ( Help.isNull(i_PrimaryKey) )
        {
            return false;
        }
        
        return this.isExistsPrimaryKey_Core(i_PrimaryKey);
    }
    
    
    
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
    @Override
    public boolean isExists(String i_Database ,String i_TableName ,String i_PrimaryKey ,String i_Field)
    {
        // 注：不用判定库、表名称是否为空，因为没有用到它
        
        if ( Help.isNull(i_PrimaryKey) )
        {
            return false;
        }
        
        if ( Help.isNull(i_Field) )
        {
            return false;
        }
        
        return isExistsField_Core(i_PrimaryKey ,i_Field);
    }
    
    
    
    /**
     * 库是否存在
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-18
     * @version     v1.0
     *
     * @param i_DBID  库的物理名称。即在Redis中保存的真实Key值
     * @return
     */
    private boolean isExistsDatabase_Core(String i_DBID)
    {
        if ( this.clusterCmd.exists(i_DBID) >= 1L )
        {
            return true;
        }
        else
        {
            return false;
        }
    }



    /**
     * 表是否存在
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     *
     * @param i_TableID  表的物理名称。即在Redis中保存的真实Key值
     * @return
     */
    private boolean isExistsTable_Core(String i_TableID)
    {
        if ( this.clusterCmd.exists(i_TableID) >= 1L )
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    
    /**
     * 行主键是否存在
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     *
     * @param i_PrimaryKey  行主键
     * @return
     */
    private boolean isExistsPrimaryKey_Core(String i_PrimaryKey)
    {
        if ( this.clusterCmd.exists(i_PrimaryKey) >= 1L )
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    
    /**
     * 行主键是否存在
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-15
     * @version     v1.0
     *
     * @param i_PrimaryKey  行主键
     * @param i_Field       行字段
     * @return
     */
    private boolean isExistsField_Core(String i_PrimaryKey ,String i_Field)
    {
        return this.clusterCmd.hexists(i_PrimaryKey ,i_Field);
    }
    
    
    
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
    @Override
    public boolean expire(String i_Key ,Long i_ExpireTime)
    {
        if ( i_ExpireTime == null || i_ExpireTime <= 0L )
        {
            return false;
        }
        else
        {
            return this.clusterCmd.expireat(i_Key ,i_ExpireTime);
        }
        
    }
    
    
    
    /**
     * 获取过期时长（单位：秒）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-23
     * @version     v1.0
     *
     * @param i_Key  关键字
     * @return
     */
    public Long expiretime(String i_Key)
    {
        return this.clusterCmd.expiretime(i_Key);
    }
    
    
    
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
    @Override
    public Boolean set(String i_Key ,String i_Value)
    {
        return "OK".equals(this.clusterCmd.set(i_Key ,i_Value));
    }
    
    
    
    /**
     * 设置数据，并且设定过期时长
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-09-23
     * @version     v1.0
     *
     * @param i_Key         关键字
     * @param i_Value        数据
     * @param i_ExpireTime  过期时间（单位：秒）
     * @return              成功返回true
     */
    @Override
    public Boolean setex(String i_Key ,String i_Value ,Long i_ExpireTime)
    {
        return "OK".equals(this.clusterCmd.setex(i_Key ,i_ExpireTime ,i_Value));
    }
    
    
    
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
    @Override
    public Boolean setnx(String i_Key ,String i_Value)
    {
        return this.clusterCmd.setnx(i_Key ,i_Value);
    }
    
    
    
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
    @Override
    public String get(String i_Key)
    {
        return this.clusterCmd.get(i_Key);
    }
    
    
    
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
    @Override
    public String getdel(String i_Key)
    {
        return this.clusterCmd.getdel(i_Key);
    }
    
    
    
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
    @Override
    public Long del(String ... i_Keys)
    {
        return this.clusterCmd.del(i_Keys);
    }
    
}

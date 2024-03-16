package org.hy.common.redis.lettuce;

import java.util.Map;

import org.hy.common.Date;
import org.hy.common.Help;
import org.hy.common.redis.IRedis;
import org.hy.common.redis.RData;
import org.hy.common.redis.cluster.RedisClusterConfig;
import org.hy.common.xml.log.Logger;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;





/**
 * Redis数据库访问的Lettuce实现
 *
 * @author      ZhengWei(HY)
 * @createDate  2024-03-15
 * @version     v1.0
 */
public class RedisLettuce implements IRedis
{
    
    private static final Logger $Logger = new Logger(RedisLettuce.class);
    
    
    
    private RedisClusterClient                           clusterClient;
    
    private RedisAdvancedClusterCommands<String ,String> clusterCmd;
    
    
    
    public RedisLettuce(RedisClusterConfig i_RedisClusterConfig)
    {
        this(i_RedisClusterConfig.toLettuce());
    }
    
    
    
    public RedisLettuce(RedisClusterClient i_ClusterClient)
    {
        this.clusterClient = i_ClusterClient;
        
        // 创建连接到 Redis 集群的连接
        try
        {
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
     * 获取库的物理名称。即将逻辑名称转为真实保存在Redis的Key值
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     *
     * @param i_Database   库名称（逻辑名称）
     * @return             表的物理名称
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
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        if ( this.clusterCmd.exists(v_TableID) >= 1L )
        {
            $Logger.error("Table[" + v_TableID + "] exists.");
            return false;
        }
        
        String v_DBID = this.getDatabaseID(i_Database);
        if ( this.clusterCmd.exists(v_DBID) <= 0L )
        {
            if ( !this.clusterCmd.hsetnx(v_DBID ,v_TableID ,Date.getNowTime().getFull()) )
            {
                $Logger.error("An exception occurred while creating the Table[" + v_TableID + "] for MetaData.");
                return false;
            }
        }
        
        if ( this.clusterCmd.exists(v_TableID) >= 1L )
        {
            String v_CreateTime = this.clusterCmd.hget(v_DBID ,v_TableID);
            $Logger.error("Table[" + v_TableID + "] exists ,it was created at " + v_CreateTime);
            return false;
        }
        
        // 添加一个空主键，使用空字段实现预占用的创建表Hash对象
        if ( this.clusterCmd.hset(v_TableID ,"" ,"") )
        {
            return true;
        }
        else
        {
            $Logger.error("An exception occurred while creating the Table[" + v_TableID + "].");
            this.clusterCmd.hdel(v_DBID ,v_TableID);
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
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @return
     */
    @Override
    public boolean isExists(String i_Database ,String i_TableName)
    {
        String v_TableID = this.getTableID(i_Database ,i_TableName);
        return this.isExists(v_TableID);
    }
    
    
    
    /**
     * 表是否在
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     *
     * @param i_TableID  表的物理名称。即在Redis中保存的真实Key值
     * @return
     */
    public boolean isExists(String i_TableID)
    {
        if ( this.clusterCmd.exists(i_TableID) >= 1L )
        {
            return true;
        }
        
        return false;
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
        if ( !this.isExists(v_TableID) )
        {
            return false;
        }
        
        String v_DBID = this.getDatabaseID(i_Database);
        if ( this.clusterCmd.exists(v_DBID) <= 0L )
        {
            return false;
        }
        
        this.truncate(i_Database ,i_TableName);                 // 清空数据
        this.clusterCmd.hdel(v_TableID ,"");                    // 删除 空主键
        this.clusterCmd.del(v_TableID);                         // 删除表
        
        return this.clusterCmd.hdel(v_DBID ,v_TableID) >= 1L;   // 删除表库关系
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
        if ( !this.isExists(v_TableID) )
        {
            return -1L;
        }
        
        Map<String ,String> v_RowIDs  = this.clusterCmd.hgetall(v_TableID);
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
            
            Long v_DelRet = this.delete_Core(v_TableID ,v_RowID);
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
     * @param i_PrimaryKey  主键
     * @return
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
        if ( !this.isExists(v_TableID) )
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
     * @param i_Field      对象属性
     * @param i_Value      对象值
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
        if ( !this.isExists(v_TableID) )
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
        if ( !this.isExists(v_TableID) )
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
        try
        {
            return insert(i_Database ,i_TableName ,i_PrimaryKey ,Help.toMap(i_Datas));
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
        if ( !this.isExists(v_TableID) )
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
        
        return v_Count;
    }
    
    
    
    /**
     * 插入一行中的一个字段的数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     *
     * @param i_TableID
     * @param i_PrimaryKey 行主键
     * @param i_Field      对象属性
     * @param i_Value      对象值
     * @return             返回影响的行数。负数表示异常
     */
    private Long insert_Core(String i_TableID ,String i_PrimaryKey ,String i_Field ,String i_Value)
    {
        // 表、主键关系
        if ( this.clusterCmd.hsetnx(i_TableID ,i_PrimaryKey ,Date.getNowTime().getFull()) )
        {
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
        else
        {
            return 0L;
        }
    }
    
    
    
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
        if ( !this.isExists(v_TableID) )
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
        if ( !this.isExists(v_TableID) )
        {
            if ( !this.createTable(i_Database ,i_TableName) )
            {
                return -2L;
            }
        }
        
        return update_Core(v_TableID ,i_RData.getKey() ,i_RData.getField() ,i_RData.getValue());
    }
    
    
    
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
    @Override
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Object i_Datas)
    {
        try
        {
            return update(i_Database ,i_TableName ,i_PrimaryKey ,Help.toMap(i_Datas));
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
    @Override
    public Long update(String i_Database ,String i_TableName ,String i_PrimaryKey ,Map<String ,Object> i_Datas)
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
        if ( !this.isExists(v_TableID) )
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
        
        return v_Count;
    }
    
    
    
    /**
     * 插入一行中的一个字段的数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-16
     * @version     v1.0
     *
     * @param i_TableID
     * @param i_PrimaryKey 行主键
     * @param i_Field      对象属性
     * @param i_Value      对象值
     * @return             返回影响的行数。负数表示异常
     */
    private Long update_Core(String i_TableID ,String i_PrimaryKey ,String i_Field ,String i_Value)
    {
        // 表、主键关系
        this.clusterCmd.hsetnx(i_TableID ,i_PrimaryKey ,Date.getNowTime().getFull());
            
        // 一行中的一个字段的数据
        this.clusterCmd.hset(i_PrimaryKey ,i_Field ,i_Value);
        return 1L;
    }
    
}

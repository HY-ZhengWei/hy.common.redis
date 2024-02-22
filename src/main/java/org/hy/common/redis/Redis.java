package org.hy.common.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hy.common.Date;
import org.hy.common.JavaHelp;
import org.hy.common.ListMap;
import org.hy.common.StringHelp;
import org.hy.common.thread.Job;
import org.hy.common.thread.Jobs;
import org.hy.common.xml.XJava;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;





/**
 * Redis数据库访问
 * 
 * 特色1：所有操作不用显性的释放资源，由本类及相关类内部释放，保证访问资源的安全
 * 
 * 特色2：创建逻辑表的概念。通过三级信息构建
 *          1. 一级Hash($MetaData_Tables):保存表的列表信息。   Hash.key = 表名称，Hash.value = 表的创建时间
 *          2. 二级Hash(表名称)          :保存表的行主键列表。Hash.key = 行主键，Hash.value = 行主键的创建或修改时间
 *          3. 三级Hash(行主键)          :保存一行数据信息。   Hash.key = 字段名，Hash.value = 字段值
 * 
 * 概念1：行主键  RowKey。表中一行数据的唯一标示
 *                      注意：行主键默认为 "表名称.ID" 的形式
 * 
 * 概念2：关键字  Key   。Redis数据库中一个Key-Value的Key值。就是Map集合的Key值。
 * 
 * 注：在对象构建时，已保存在XJava中，其XID为this.getXID()
 * 
 * @author ZhengWei(HY)
 * @create 2014-09-16
 */
public class Redis
{
    
    /** 行主键的类型 */
    public enum RowKeyType
    {
        /** 有结构形式的："表名称.ID"。默认 */
        $TableName_ID
        
        /** 无结构的行主键。此时行主键与ID相同 */
       ,$ID
    }
    
    /**
     * Redis的运行模式
     * 当Jedis配置多套集群并采用分布式时，由我们人为的控制其运行模式
     */
    public enum RunMode
    {
        /**
         * 分布式集群模式(碎片化)
         * 
         * 这是Jedis或Redis服务本身的功能。
         * 即按Key的命名规则，将不同Key保存在不同Redis单个服务器上
         * 
         * 当配置了多套集群时，默认启用此模式
         */
        $Shard
        
        /**
         * 备份容灾模式
         * 
         * 我们人为的将多套集群间的数据完全一样。
         * 同时，在这样的模式下，对多数据操作实现事务处理
         * 
         * 当只配置了唯一一套集群时，默认启用此模式，因为它有事务处理机制
         * 
         * 多套集群的情况，其中一套集群异常无法连接时，自动将其剔除在允许访问之列。
         * 同时，启动探测模式，间隔探测异常的集群是否恢复正常。
         * 在其恢复正常后，再添加到允许访问之列。
         * 但异常期间的数据不向其同步。
         */
       ,$Backup
    }
    
    
    
    private static final String             $MetaData_Tables = "$MetaData_Tables";
    
    /**
     * 分布式集群信息
     * 
     * Memcached完全基于分布式集群，而Redis是Master-Slave。
     * 如果想把Reids，做成集群模式，无外乎多做几套Master-Slave，
     * 每套Master-Slave完成各自的容灾处理，通过Client工具，完成一致性哈希。
     * 
     * Map.key 为分布式集群中集群的名称(逻辑上唯一标识)
     */
    private ListMap<String ,JedisShardInfo> shardInfoMap;
    
    /** 出现异常的分布式集群信息 (只用于备份容灾模式) */
    private ListMap<String ,JedisShardInfo> exceptionShards;
    
    /** 分布式集群的资源池 */
    private ShardedJedisPool                shardedPool;
    
    private RedisKey                        redisKey;
    
    private RedisString                     redisString;
    
    private RedisSet                        redisSet;
    
    private RedisHash                       redisHash;
    
    private RedisServer                     redisServer;
    
    private String                          xjavaID;
    
    private RunMode                         runMode;
    
    private boolean                         isKeyOrder;
    
    private RowKeyType                      rowKeyType;
    
    private Job                             job;
    
    private Jobs                            jobs;
    
    
    
    public Redis(String i_IP)
    {
        this(null ,i_IP ,6379);
    }
    
    
    
    public Redis(String i_IP ,int i_Port)
    {
        this(null ,i_IP ,i_Port);
    }
    
    
    
    public Redis(JedisPoolConfig i_PoolConfig ,String i_IP ,int i_Port)
    {
        this(i_PoolConfig ,newOneList(i_IP ,i_Port ,null));
    }
    
    
    
    public Redis(JedisPoolConfig i_PoolConfig ,String i_IP ,int i_Port ,String i_Password)
    {
        this(i_PoolConfig ,newOneList(i_IP ,i_Port ,i_Password));
    }
    
    
    
    public Redis(List<JedisShardInfo> i_JedisShardInfos)
    {
        this(null ,i_JedisShardInfos);
    }
    
    
    
    public Redis(JedisPoolConfig i_PoolConfig ,List<JedisShardInfo> i_JedisShardInfos)
    {
        if ( JavaHelp.isNull(i_JedisShardInfos) )
        {
            throw new NullPointerException("HostAndPorts is null.");
        }
        
        shardInfoMap    = new ListMap<String ,JedisShardInfo>(i_JedisShardInfos.size() ,false);
        exceptionShards = new ListMap<String ,JedisShardInfo>(i_JedisShardInfos.size() ,false);
        
        for (JedisShardInfo v_Host : i_JedisShardInfos)
        {
            if ( JavaHelp.isNull(v_Host.getHost()) )
            {
                throw new NullPointerException("IP is null.");
            }
            
            if ( 0 >= v_Host.getPort() || v_Host.getPort() >= 65535 )
            {
                throw new RuntimeException("Port is not 0~65535.");
            }
            
            if ( JavaHelp.isNull(v_Host.getName()) )
            {
                String         v_Name  = v_Host.getHost() + ":" + v_Host.getPort();
                JedisShardInfo v_Clone = new JedisShardInfo(v_Host.getHost() ,v_Host.getPort() ,v_Host.getConnectionTimeout() ,v_Name);
                v_Clone.setPassword(v_Host.getPassword());
                
                if ( shardInfoMap.containsKey(v_Name) )
                {
                    throw new RuntimeException("JedisShardInfo name[" + v_Name + "] is same.");
                }
                shardInfoMap.put(v_Name ,v_Clone);
            }
            else
            {
                if ( shardInfoMap.containsKey(v_Host.getName()) )
                {
                    throw new RuntimeException("JedisShardInfo name[" + v_Host.getName() + "] is same.");
                }
                shardInfoMap.put(v_Host.getName() ,v_Host);
            }
        }
        
        JedisPoolConfig v_PoolConfig = i_PoolConfig;
        if ( v_PoolConfig == null )
        {
            v_PoolConfig = new JedisPoolConfig();
            v_PoolConfig.setMaxTotal(1024);
            v_PoolConfig.setMaxIdle(10);
            v_PoolConfig.setMinIdle(1);
        }
        
        shardedPool = new ShardedJedisPool(v_PoolConfig ,JavaHelp.toList(shardInfoMap));
        
        if ( this.shardInfoMap.size() <= 1 )
        {
            this.runMode = RunMode.$Backup;
        }
        else
        {
            this.runMode = RunMode.$Shard;
        }
        
        this.redisKey    = new RedisKey(this);
        this.redisString = new RedisString(this);
        this.redisSet    = new RedisSet(this);
        this.redisHash   = new RedisHash(this);
        this.redisServer = new RedisServer(this);
        this.isKeyOrder  = true;
        this.rowKeyType  = RowKeyType.$TableName_ID;
        this.xjavaID     = "XID_REDIS_" + StringHelp.getUUID();
        XJava.putObject(this.xjavaID ,this);
    }
    
    
    
    protected static List<JedisShardInfo> newOneList(String i_IP ,int i_Port ,String i_Password)
    {
        List<JedisShardInfo> v_Ret = new ArrayList<JedisShardInfo>(1);
        
        v_Ret.add(new JedisShardInfo(i_IP ,i_Port));
        
        if ( i_Password != null )
        {
            v_Ret.get(0).setPassword(i_Password);
        }
        
        return v_Ret;
    }
    
    
    
    protected Map<String ,String> newMap()
    {
        if ( this.isKeyOrder )
        {
            return new LinkedHashMap<String ,String>();
        }
        else
        {
            return new HashMap<String ,String>();
        }
    }
    
    
    
    protected Map<String ,Map<String ,String>> newMapMap()
    {
        if ( this.isKeyOrder )
        {
            return new LinkedHashMap<String ,Map<String ,String>>();
        }
        else
        {
            return new HashMap<String ,Map<String ,String>>();
        }
    }
    
    
    
    protected Map<String ,List<String>> newMapList()
    {
        if ( this.isKeyOrder )
        {
            return new LinkedHashMap<String ,List<String>>();
        }
        else
        {
            return new HashMap<String ,List<String>>();
        }
    }
    
    
    
    protected Map<String ,Set<String>> newMapSet()
    {
        if ( this.isKeyOrder )
        {
            return new LinkedHashMap<String ,Set<String>>();
        }
        else
        {
            return new HashMap<String ,Set<String>>();
        }
    }
    
    
    
    /**
     * 获取写对象
     * 
     * @return
     */
    protected ShardedJedis getWriter()
    {
        return this.shardedPool.getResource();
    }
    
    
    
    /**
     * 获取读对象
     * 
     * @return
     */
    protected ShardedJedis getReader()
    {
        return this.shardedPool.getResource();
    }
    
    
    
    /**
     * 读、写完成后必须执行此方法释放资源
     * 
     * 将 returnResource 重写成两个方法的原因是：
     *    减少一个 i_Exce == null 的判断，提高性能
     * 
     * @param i_ShardedJedis
     */
    protected void returnResource(ShardedJedis i_ShardedJedis)
    {
        try
        {
            shardedPool.close();
            // 2.6.0的版本如下方法释放
            // shardedPool.returnResource(i_ShardedJedis);
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
            
            // 释放Redis对象。
            // 在程序出错时，必须调用returnBrokenResource返还给pool，
            // 否则下次通过getResource得到的instance的缓冲区可能还存在数据，出现问题
            // 2.6.0的版本如下方法释放
            // shardedPool.returnBrokenResource(i_ShardedJedis);
        }
    }
    
    
    
    /**
     * 读、写完成后必须执行此方法释放资源
     * 
     * 将 returnResource 重写成两个方法的原因是：
     *    尽可能的减少一个 i_Exce == null 的判断，提高性能
     * 
     * @param i_ShardedJedis
     * @param i_Exce
     */
    protected void returnResource(ShardedJedis i_ShardedJedis ,Exception i_Exce)
    {
        if ( i_ShardedJedis == null )
        {
            if ( i_Exce != null )
            {
                i_Exce.printStackTrace();
            }
        }
        
        try
        {
            if ( i_Exce == null )
            {
                // 返还到连接池。正常情况下使用此方法
                shardedPool.close();
                // 2.6.0的版本如下方法释放
                // shardedPool.returnResource(i_ShardedJedis);
            }
            else
            {
                i_Exce.printStackTrace();
                
                // 释放Redis对象。
                // 在程序出错时，必须调用returnBrokenResource返还给pool，
                // 否则下次通过getResource得到的instance的缓冲区可能还存在数据，出现问题
                // 2.6.0的版本如下方法释放
                // shardedPool.returnBrokenResource(i_ShardedJedis);
                // shardedPool.returnResource(      i_ShardedJedis);
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
    
    
    
    @Override
    protected void finalize()
    {
        shardedPool.destroy();
    }
    
    
    
    public synchronized void createTable(String i_TableName)
    {
        if ( this.getRKey().isExists(i_TableName) )
        {
            throw new RuntimeException("TableName[" + i_TableName + "] is exists.");
        }
        
        if ( !this.getRHash().isExists($MetaData_Tables ,i_TableName) )
        {
            this.getRHash().put(true ,$MetaData_Tables ,i_TableName ,"" + Date.getNowTime().getTime());
        }
        
        // 使用空字段实现预占用的创建表Hash对象
        this.getRHash().put(true ,i_TableName ,"" ,"");
        
        // 删除因创建表而预占用的空字段信息
        // 注意：当Hash中没有子元素(域)时，它本身也就给删除了。
        // this.getRHash().del(i_TableName ,"");
    }
    
    
    
    /**
     * 清空表数据
     * 
     * 但表的行主键信息还没有删除
     * 
     * @param i_TableName
     */
    private void core_deleteAll(String i_TableName)
    {
        Set<String> v_RowKeys = this.getRHash().getFields(i_TableName);
        
        // 因创建表而预占用的空字段信息。
        // 将其排除在外不删除，因为这个空的行主键没有真实对应的信息
        v_RowKeys.remove("");
        
        this.getRKey().dels(v_RowKeys);
    }
    
    
    
    /**
     * 删除表
     * 
     * @param i_TableName
     */
    public synchronized void dropTable(String i_TableName)
    {
        this.core_deleteAll(i_TableName);
        
        // 删除表本身及所有行主键信息
        this.getRKey().dels(i_TableName);
        
        // 删除表的元数据信息
        this.getRHash().del(true ,$MetaData_Tables ,i_TableName);
    }
    
    
    
    /**
     * 清空表数据
     * 
     * 实际是Drop表后重建
     * 
     * @param i_TableName
     */
    public synchronized void deleteAll(String i_TableName)
    {
        this.dropTable(i_TableName);
        
        this.createTable(i_TableName);
    }
    
    
    
    public void truncate(String i_TableName)
    {
        this.deleteAll(i_TableName);
    }
    
    
    
    /**
     * 判断表是否存在
     * 
     * @param i_TableName
     * @return
     */
    public boolean isExistsTable(String i_TableName)
    {
        if ( this.getRKey().isExists(i_TableName) )
        {
            if ( this.getRHash().isExists($MetaData_Tables ,i_TableName) )
            {
                return true;
            }
        }
        
        return false;
    }
    
    
    
    /**
     * 创建一个行主键
     * 
     * @param i_TableName
     * @param i_Key
     * @return
     */
    public String makeRowKey(String i_TableName ,String i_Key)
    {
        if ( this.rowKeyType == RowKeyType.$TableName_ID )
        {
            return i_TableName + "." + i_Key;
        }
        else
        {
            return i_Key;
        }
    }
    
    
    
    /**
     * 创建一组行主键
     * 
     * @param i_TableName
     * @param i_Keys
     * @return
     */
    public String [] makeRowKeys(String i_TableName ,List<String> i_Keys)
    {
        return this.makeRowKeys(i_TableName ,i_Keys.toArray(new String []{}));
    }
    
    
    
    /**
     * 创建一组行主键
     * 
     * @param i_TableName
     * @param i_Keys
     * @return
     */
    public String [] makeRowKeys(String i_TableName ,String [] i_Keys)
    {
        if ( this.rowKeyType == RowKeyType.$TableName_ID )
        {
            String [] v_RowKeys = new String[i_Keys.length];
            
            for (int i=0; i<i_Keys.length; i++)
            {
                v_RowKeys[i] = i_TableName + "." + i_Keys[i];
            }
            
            return v_RowKeys;
        }
        else
        {
            return i_Keys;
        }
    }
    
    
    
    /**
     * 插入一行中的一个字段的数据
     * 
     * @param i_TableName  表名称
     * @param i_Key        对象ID--关键字
     * @param i_Field      对象属性
     * @param i_Value      对象值
     * @return             返回行主键
     */
    public String insert(String i_TableName ,String i_Key ,String i_Field ,String i_Value)
    {
        return this.insert(i_TableName ,new RData(i_Key ,i_Field ,i_Value));
    }
    
    
    
    /**
     * 插入一行中的一个字段的数据
     * 
     * @param i_TableName  表名称
     * @param i_RData      数据信息
     * @return             返回行主键
     */
    public String insert(String i_TableName ,RData i_RData)
    {
        List<String> v_Ret = this.core_puts(i_TableName ,new RData[]{i_RData});
        
        return v_Ret.get(0);
    }
    
    
    
    /**
     * 插入一行或多行数据
     * 
     * @param i_TableName  表名称
     * @param i_RDatas     数据信息(相同行的多个字段信息的RData的getKey()为同一值)
     * @return             按顺序返回行主键集合，其行主键的个数为实际行个数，而不i_RDatas.length的个数
     */
    public List<String> inserts(String i_TableName ,RData ... i_RDatas)
    {
        return this.core_puts(i_TableName ,i_RDatas);
    }
    
    
    
    /**
     * 插入一行或多行数据
     * 
     * @param i_TableName  表名称
     * @param i_RDatas     数据信息(相同行的多个字段信息的RData的getKey()为同一值)
     * @return             按顺序返回行主键集合，其行主键的个数为实际行个数，而不i_RDatas.length的个数
     */
    public List<String> inserts(String i_TableName ,List<RData> i_RDatas)
    {
        return this.core_puts(i_TableName ,i_RDatas.toArray(new RData []{}));
    }
    
    
    
    /**
     * 修改一行中的一个字段的数据
     * 
     * @param i_TableName  表名称
     * @param i_Key        对象ID
     * @param i_Field      对象属性
     * @param i_Value      对象值
     * @return             返回行主键
     */
    public String update(String i_TableName ,String i_Key ,String i_Field ,String i_Value)
    {
        return this.update(i_TableName ,new RData(i_Key ,i_Field ,i_Value));
    }
    
    
    
    /**
     * 修改一行中的一个字段的数据
     * 
     * @param i_TableName  表名称
     * @param i_RData      数据信息
     * @return             返回行主键
     */
    public String update(String i_TableName ,RData i_RData)
    {
        List<String> v_Ret = this.core_puts(i_TableName ,new RData[]{i_RData});
        
        return v_Ret.get(0);
    }
    
    
    
    /**
     * 修改一行或多行数据
     * 
     * @param i_TableName  表名称
     * @param i_RDatas     数据信息(相同行的多个字段信息的RData的getKey()为同一值)
     * @return             按顺序返回行主键集合，其行主键的个数为实际行个数，而不i_RDatas.length的个数
     */
    public List<String> update(String i_TableName ,RData ... i_RDatas)
    {
        return this.core_puts(i_TableName ,i_RDatas);
    }
    
    
    
    /**
     * 修改一行或多行数据
     * 
     * @param i_TableName  表名称
     * @param i_RDatas     数据信息(相同行的多个字段信息的RData的getKey()为同一值)
     * @return             按顺序返回行主键集合，其行主键的个数为实际行个数，而不i_RDatas.length的个数
     */
    public List<String> update(String i_TableName ,List<RData> i_RDatas)
    {
        return this.core_puts(i_TableName ,i_RDatas.toArray(new RData []{}));
    }
    
    
    
    /**
     * 插入或修改一行或多行数据
     * 
     * @param i_TableName  表名称
     * @param i_RDatas     数据信息(相同行的多个字段信息的RData的getKey()为同一值)
     * @return             按顺序返回行主键集合，其行主键的个数为实际行个数，而不i_RDatas.length的个数
     */
    private List<String> core_puts(String i_TableName ,RData [] i_RDatas)
    {
        if ( !this.getRKey().isExists(i_TableName) )
        {
            throw new RuntimeException("TableName[" + i_TableName + "] is exists.");
        }
        
        if ( JavaHelp.isNull(i_RDatas) )
        {
            throw new NullPointerException("RDatas is null.");
        }
        
        for (int i=0; i<i_RDatas.length; i++)
        {
            RData v_RData = i_RDatas[i];
            if ( v_RData == null )
            {
                throw new NullPointerException("RDatas[" + i + "] is null.");
            }
            
            if ( v_RData.getKey() == null )
            {
                throw new NullPointerException("RDatas[" + i + "].key is null.");
            }
            
            if ( v_RData.getField() == null )
            {
                throw new NullPointerException("RDatas[" + i + "].field is null.");
            }
        }
        
        
        List<RData>  v_RDatas  = new ArrayList<RData>(i_RDatas.length * 2);
        List<String> v_RowKeys = new ArrayList<String>();
        
        for (int i=0; i<i_RDatas.length; i++)
        {
            RData  v_RData  = i_RDatas[i];
            String v_RowKey = this.makeRowKey(i_TableName ,v_RData.getKey());
            
            if ( !v_RowKeys.contains(v_RowKey) )
            {
                v_RowKeys.add(v_RowKey);
            }
            v_RDatas.add(new RData(i_TableName ,v_RowKey ,"" + Date.getNowTime().getTime()));
            v_RDatas.add(new RData(v_RowKey    ,v_RData.getField() ,JavaHelp.NVL(v_RData.getValue())));
        }
        
        this.getRHash().put(v_RDatas);
        
        return v_RowKeys;
    }
    
    
    
    /**
     * 删除行
     * 
     * @param i_TableName  表名称
     * @param i_Keys       对象ID--关键字。注意不是行主键
     */
    public void deleteRow(String i_TableName ,String ... i_Keys)
    {
        this.core_deleteRow(i_TableName ,i_Keys);
    }
    
    
    
    /**
     * 删除行
     * 
     * @param i_TableName  表名称
     * @param i_Keys       对象ID--关键字。注意不是行主键
     */
    public void deleteRow(String i_TableName ,List<String> i_Keys)
    {
        this.core_deleteRow(i_TableName ,i_Keys.toArray(new String []{}));
    }
    
    
    
    /**
     * 删除行
     * 
     * @param i_TableName  表名称
     * @param i_Keys       对象ID--关键字。注意不是行主键
     */
    private void core_deleteRow(String i_TableName ,String [] i_Keys)
    {
        if ( JavaHelp.isNull(i_Keys) )
        {
            throw new NullPointerException("i_Keys is null.");
        }
        
        if ( !this.getRKey().isExists(i_TableName) )
        {
            throw new RuntimeException("TableName[" + i_TableName + "] is exists.");
        }
        
        String [] v_RowKeys = this.makeRowKeys(i_TableName ,i_Keys);
        
        this.getRHash().dels(i_TableName ,v_RowKeys);
        
        this.getRKey().dels(v_RowKeys);
    }
    
    
    
    /**
     * 删除字段。
     * 
     * 如果整行数据都不存在，同时删除行信息
     * 
     * @param i_TableName
     * @param i_RDatas
     */
    public void delete(String i_TableName ,RData ... i_RDatas)
    {
        this.core_delete(i_TableName ,i_RDatas);
    }
    
    
    
    /**
     * 删除字段。
     * 
     * 如果整行数据都不存在，同时删除行信息
     * 
     * @param i_TableName
     * @param i_RDatas
     */
    public void delete(String i_TableName ,List<RData> i_RDatas)
    {
        this.core_delete(i_TableName ,i_RDatas.toArray(new RData []{}));
    }
    
    
    
    /**
     * 删除字段。
     * 
     * 如果整行数据都不存在，同时删除行信息
     * 
     * @param i_TableName
     * @param i_RDatas
     */
    private void core_delete(String i_TableName ,RData [] i_RDatas)
    {
        if ( !this.getRKey().isExists(i_TableName) )
        {
            throw new RuntimeException("TableName[" + i_TableName + "] is exists.");
        }
        
        if ( JavaHelp.isNull(i_RDatas) )
        {
            throw new NullPointerException("RDatas is null.");
        }
        
        
        List<String> v_Keys = this.getRHash().dels(i_RDatas);
        
        if ( !JavaHelp.isNull(v_Keys) )
        {
            // 如果整行数据都不存在，同时删除行信息
            for (int i=v_Keys.size()-1; i>=0; i--)
            {
                if ( this.getRKey().isExists(v_Keys.get(i)) )
                {
                    v_Keys.remove(i);
                }
            }
            if ( !JavaHelp.isNull(v_Keys) )
            {
                this.getRHash().dels(i_TableName ,v_Keys);
            }
        }
    }
    
    
    
    /**
     * 获取一行数据的所有字段信息
     * 
     * @param i_TableName  表名称
     * @param i_Key        对象ID--关键字。注意不是行主键
     * @return             Map.Key   为字段名称
     *                     Map.Value 为字段值
     */
    public Map<String ,String> getRow(String i_TableName ,String i_Key)
    {
        return this.getRHash().getValues(this.makeRowKey(i_TableName ,i_Key));
    }
    
    
    
    /**
     * 获取多行数据的所有字段信息
     * 
     * @param i_TableName  表名称
     * @param i_Keys       对象ID--关键字。注意不是行主键
     * @return        Map<String ,Map<String ,String>>.key       表示一个行主键
     *                Map<String ,Map<String ,String>>.Map       表示一个行记录的所有数据信息
     *                Map<String ,Map<String ,String>>.Map.key   表示一个字段名称
     *                Map<String ,Map<String ,String>>.Map.value 表示一个字段值
     */
    public Map<String ,Map<String ,String>> getRows(String i_TableName ,List<String> i_Keys)
    {
        return this.core_getRows(i_TableName ,i_Keys.toArray(new String []{}));
    }
    
    
    
    /**
     * 获取多行数据的所有字段信息
     * 
     * @param i_TableName  表名称
     * @param i_Keys       对象ID--关键字。注意不是行主键
     * @return        Map<String ,Map<String ,String>>.key       表示一个行主键
     *                Map<String ,Map<String ,String>>.Map       表示一个行记录的所有数据信息
     *                Map<String ,Map<String ,String>>.Map.key   表示一个字段名称
     *                Map<String ,Map<String ,String>>.Map.value 表示一个字段值
     */
    public Map<String ,Map<String ,String>> getRows(String i_TableName ,String ... i_Keys)
    {
        return this.core_getRows(i_TableName ,i_Keys);
    }
    
    
    
    /**
     * 获取多行数据的所有字段信息
     * 
     * @param i_TableName  表名称
     * @param i_Keys       对象ID--关键字。注意不是行主键
     * @return        Map<String ,Map<String ,String>>.key       表示一个行主键
     *                Map<String ,Map<String ,String>>.Map       表示一个行记录的所有数据信息
     *                Map<String ,Map<String ,String>>.Map.key   表示一个字段名称
     *                Map<String ,Map<String ,String>>.Map.value 表示一个字段值
     */
    private Map<String ,Map<String ,String>> core_getRows(String i_TableName ,String [] i_Keys)
    {
        return this.getRHash().getValues(this.makeRowKeys(i_TableName ,i_Keys));
    }
    
    
    
    /**
     * 显示表中的所有数据
     * 
     * @param i_TableName
     */
    public void showTableDatas(String i_TableName)
    {
        Map<String ,String>              v_RowKeys = this.getRHash().getValues(i_TableName);
        Map<String ,Map<String ,String>> v_Datas   = this.getRHash().getValues(JavaHelp.toListKeys(v_RowKeys));
        
        for (String v_RowKey : v_RowKeys.keySet())
        {
            String v_RowTimestamp = v_RowKeys.get(v_RowKey);
            
            System.out.println("-- " + v_RowTimestamp + " | " + v_RowKey + " | " + v_Datas.get(v_RowKey));
        }
    }
    
    
    
    public List<String> getShardNames()
    {
        return this.shardInfoMap.getKeys();
    }
    
    
    
    /**
     * 为了安全，不建议使用
     * 
     * @param i_Index
     * @return
     */
    @SuppressWarnings("unused")
    private String getShardName(int i_Index)
    {
        return this.shardInfoMap.getKey(i_Index);
    }
    
    
    
    /**
     * 为了安全，不建议使用
     * 
     * @param i_ShardName
     * @return
     */
    @SuppressWarnings("unused")
    private int getShardNameIndex(String i_ShardName)
    {
        return this.shardInfoMap.getIndex(i_ShardName);
    }
    
    
    
    public int getShardSize()
    {
        return this.shardInfoMap.size();
    }
    
    
    /**
     * 启用Jobs -- 检查异常的集群是否已恢复连接
     */
    private synchronized void startJobs()
    {
        if ( this.job == null )
        {
            this.job = new Job();
            
            this.job.setCode(        "JOB_Redis_ShardExceptionIsConnection");
            this.job.setName(        this.job.getName());
            this.job.setIntervalType(Job.$IntervalType_Minute);
            this.job.setIntervalLen( 1);
            this.job.setStartTime(   Date.getNowTime().getFull());
            this.job.setXid(         this.xjavaID);
            this.job.setMethodName(  "shardExceptionIsConnection");
        }
        
        if ( this.jobs == null )
        {
            this.jobs = new Jobs();
            this.jobs.addJob(this.job);
        }
        
        this.jobs.startup();
    }
    
    
    
    /**
     * 报告异常的分布时集群信息
     * 
     * 只用于备份容灾模式
     * 
     * @param i_ShardName
     */
    protected synchronized void shardException(String i_ShardName)
    {
        if ( this.runMode != RunMode.$Backup )
        {
            return;
        }
        
        if ( i_ShardName == null )
        {
            return;
        }
        
        if ( this.shardInfoMap.containsKey(i_ShardName) )
        {
            this.exceptionShards.put(i_ShardName ,this.shardInfoMap.remove(i_ShardName));
            this.startJobs();
        }
    }
    
    
    
    /**
     * 报告异常的分布时集群已恢复连接
     * 
     * 只用于备份容灾模式
     * 
     * @param i_ShardName
     */
    protected synchronized void shardConnection(String i_ShardName)
    {
        if ( this.runMode != RunMode.$Backup )
        {
            return;
        }
        
        if ( i_ShardName == null )
        {
            return;
        }
        
        if ( this.exceptionShards.containsKey(i_ShardName) )
        {
            this.shardInfoMap.put(i_ShardName ,this.exceptionShards.remove(i_ShardName));
        }
    }
    
    
    
    /**
     * 检查异常的集群是否已恢复连接
     * 
     * 只用于备份容灾模式
     */
    public synchronized void shardExceptionIsConnection()
    {
        try
        {
            if ( JavaHelp.isNull(this.exceptionShards) )
            {
                return;
            }
            
            for (int i=this.exceptionShards.size() - 1; i >= 0; i--)
            {
                try
                {
                    Jedis v_Jedis = this.exceptionShards.get(i).createResource();
                    
                    if ( v_Jedis == null )
                    {
                        continue;
                    }
                    
                    String v_IsPONG = v_Jedis.ping();
                    if ( !JavaHelp.isNull(v_IsPONG) && "PONG".equals(v_IsPONG) )
                    {
                        this.shardConnection(this.exceptionShards.getKey(i));
                    }
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
            }
        }
        finally
        {
            if ( JavaHelp.isNull(this.exceptionShards) )
            {
                this.jobs.shutdown();
            }
        }
    }
    
    
    
    public RunMode getRunMode()
    {
        return runMode;
    }


    
    public void setRunMode(RunMode runMode)
    {
        if ( this.shardInfoMap.size() > 1 )
        {
            this.runMode = runMode;
        }
        else
        {
            if ( runMode != RunMode.$Backup )
            {
                throw new RuntimeException("ShardInfo size is 1. Only $Backup runmode.");
            }
        }
    }
    
    
    
    public boolean isKeyOrder()
    {
        return isKeyOrder;
    }

    
    
    public void setKeyOrder(boolean isKeyOrder)
    {
        this.isKeyOrder = isKeyOrder;
    }


    
    public RowKeyType getRowKeyType()
    {
        return rowKeyType;
    }


    
    public void setRowKeyType(RowKeyType rowKeyType)
    {
        this.rowKeyType = rowKeyType;
    }
    
    
    
    public String getXID()
    {
        return this.xjavaID;
    }
    
    
    
    public RedisKey getRKey()
    {
        return this.redisKey;
    }
    
    
    
    public RedisString getRString()
    {
        return this.redisString;
    }
    
    
    
    public RedisSet getRSet()
    {
        return this.redisSet;
    }
    
    
    
    public RedisHash getRHash()
    {
        return redisHash;
    }
    
    
    
    public RedisServer getRServer()
    {
        return redisServer;
    }
    
}

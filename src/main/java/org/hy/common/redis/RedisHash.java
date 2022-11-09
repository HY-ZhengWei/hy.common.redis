package org.hy.common.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hy.common.JavaHelp;
import org.hy.common.TablePartition;
import org.hy.common.TablePartitionRID;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.Transaction;





/**
 * Redis数据库中的Hash对象的相关操作类
 * 
 * @author ZhengWei(HY)
 * @create 2014-09-16
 */
public class RedisHash
{
    
    private Redis redis;
    
    
    
    public RedisHash(Redis i_Redis)
    {
        if ( i_Redis == null )
        {
            throw new NullPointerException("Redis is null.");
        }
        
        this.redis = i_Redis;
    }
    
    
    
    public void put(String i_Key ,String i_Field ,String i_Value)
    {
        this.put(new RData(i_Key ,i_Field ,i_Value));
    }
    
    
    
    public void put(RData ... i_RDatas)
    {
        this.core_put(false ,i_RDatas);
    }
    
    
    
    public void put(List<RData> i_RDatas)
    {
        this.core_put(false ,i_RDatas.toArray(new RData []{}));
    }
    
    
    
    public void put(boolean i_IsEveryone ,String i_Key ,String i_Field ,String i_Value)
    {
        this.put(i_IsEveryone ,new RData(i_Key ,i_Field ,i_Value));
    }
    
    
    
    public void put(boolean i_IsEveryone ,RData ... i_RDatas)
    {
        this.core_put(i_IsEveryone ,i_RDatas);
    }
    
    
    
    public void put(boolean i_IsEveryone ,List<RData> i_RDatas)
    {
        this.core_put(i_IsEveryone ,i_RDatas.toArray(new RData []{}));
    }
    
    
    
    /**
     * 添加或修改多个哈希表
     * 
     * 原始命令为：HSET key field value
     * 
     * 将哈希表 key 中的域 field 的值设为 value 。
     * 如果 key 不存在，一个新的哈希表被创建并进行 HSET 操作。
     * 如果域 field 已经存在于哈希表中，旧值将被覆盖。
     * 
     * @param i_IsEveryone  是否操作每一个集群，与RunMode.$Backup同意。
     *                      RunMode.$Backup 为广义定义
     *                      i_IsEveryone    为私人定制
     * @param i_RDatas
     */
    private void core_put(boolean i_IsEveryone ,RData [] i_RDatas)
    {
        if ( i_RDatas == null )
        {
            throw new NullPointerException("RDatas is null.");
        }
        
        TablePartitionRID<String ,String> v_Datas = new TablePartitionRID<String ,String>();
        
        for (int i=0; i<i_RDatas.length; i++)
        {
            RData v_RData = i_RDatas[i];
            
            if ( v_RData.getKey() == null )
            {
                throw new NullPointerException("RDatas[" + i + "] key is null.");
            }
            
            if ( v_RData.getField() == null )
            {
                throw new NullPointerException("RDatas[" + i + "] field is null.");
            }
            
            v_Datas.putRow(v_RData.getKey() ,v_RData.getField() ,JavaHelp.NVL(v_RData.getValue()));
        }
        
        ShardedJedis v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            if ( this.redis.getRunMode() == Redis.RunMode.$Backup || i_IsEveryone )
            {
                List<String> v_ShardNames = this.redis.getShardNames();
                
                if ( JavaHelp.isNull(v_ShardNames) )
                {
                    throw new NullPointerException("ShardInfos is null.");
                }
                
                for (int v_ShardIndex=v_ShardNames.size() - 1; v_ShardIndex>=0; v_ShardIndex--)
                {
                    String v_ShardName = v_ShardNames.get(v_ShardIndex);
                    
                    try
                    {
                        if ( v_Datas.size() >= 2 )
                        {
                            Transaction v_Transaction = v_ShardedJedis.getShard(v_ShardName).multi();
                            
                            for (String v_Key : v_Datas.keySet())
                            {
                                v_Transaction.hmset(v_Key ,v_Datas.get(v_Key));
                            }
                            
                            v_Transaction.exec();
                        }
                        else
                        {
                            // 单Key操作，无须事务。即 for 实际上只循环一次
                            for (String v_Key : v_Datas.keySet())
                            {
                                v_ShardedJedis.getShard(v_ShardName).hmset(v_Key ,v_Datas.get(v_Key));
                            }
                        }
                    }
                    catch (Exception exce)
                    {
                        this.redis.shardException(v_ShardName);
                    }
                }
            }
            else
            {
                for (String v_Key : v_Datas.keySet())
                {
                    v_ShardedJedis.hmset(v_Key ,v_Datas.get(v_Key));
                }
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
    }
    
    
    
    /**
     * 原始命令为：HEXISTS key field
     * 
     * 查看哈希表 key 中，给定域 field 是否存在。
     * 
     * @param i_Key
     * @param i_Field
     * @return
     */
    public boolean isExists(String i_Key ,String i_Field)
    {
        boolean      v_Ret          = false;
        ShardedJedis v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            
            v_Ret = v_ShardedJedis.hexists(i_Key ,i_Field);
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 原始命令为：HGET key field
     * 
     * 返回哈希表 key 中给定域 field 的值。
     * 
     * 当给定域不存在或是给定 key 不存在时，返回 nil 。
     * 
     * @param i_Key
     * @param i_Field
     * @return
     */
    public String getValue(String i_Key ,String i_Field)
    {
        if ( i_Key == null )
        {
            throw new NullPointerException("Keys is null.");
        }
        
        if ( i_Field == null )
        {
            throw new NullPointerException("Field is null.");
        }
        
        String       v_Ret          = null;
        ShardedJedis v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            
            v_Ret = v_ShardedJedis.hget(i_Key ,i_Field);
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    public List<String> getValues(String i_Key ,List<String> i_Fields)
    {
        Map<String ,List<String>> v_Ret = this.core_getValuesByKeys(new String[]{i_Key} ,i_Fields.toArray(new String []{}));
        
        return v_Ret.get(i_Key);
    }
    
    
    
    public List<String> getValues(String i_Key ,String [] i_Fields)
    {
        Map<String ,List<String>> v_Ret = this.core_getValuesByKeys(new String[]{i_Key} ,i_Fields);
        
        return v_Ret.get(i_Key);
    }
    
    
    
    public Map<String ,List<String>> getValues(List<String> i_Keys ,List<String> i_Fields)
    {
        return this.core_getValuesByKeys(i_Keys.toArray(new String []{}) ,i_Fields.toArray(new String []{}));
    }
    
    
    
    public Map<String ,String> getValues(String i_Key)
    {
        Map<String ,Map<String ,String>> v_Ret = this.core_getValuesByKeys(new String[]{i_Key});
        
        return v_Ret.get(i_Key);
    }
    
    
    public Map<String ,Map<String ,String>> getValues(String ... i_Keys)
    {
        return this.core_getValuesByKeys(i_Keys);
    }
    
    
    
    public Map<String ,Map<String ,String>> getValues(List<String> i_Keys)
    {
        return this.core_getValuesByKeys(i_Keys.toArray(new String []{}));
    }
    
    
    
    /**
     * 获取多个哈希表的所有的域和值
     * 
     * 原始命令为：HGETALL key
     * 
     * 返回哈希表 key 中，所有的域和值。
     * 
     * 若 key 不存在，返回空列表。
     * 
     * @param i_Keys    只获取哪些Key的相关信息
     * @return        Map<String ,Map<String ,String>>.key       表示一个哈希表Key值
     *                Map<String ,Map<String ,String>>.Map       表示一个哈希表所有的域和值
     *                Map<String ,Map<String ,String>>.Map.key   表示一个哈希表中一个域
     *                Map<String ,Map<String ,String>>.Map.value 表示一个哈希表中一个值
     */
    private Map<String ,Map<String ,String>> core_getValuesByKeys(String [] i_Keys)
    {
        if ( i_Keys == null )
        {
            throw new NullPointerException("Keys is null.");
        }
        
        for (int i=0; i<i_Keys.length; i++)
        {
            if ( i_Keys[i] == null )
            {
                throw new NullPointerException("Keys[" + i + "] is null.");
            }
        }
        
        Map<String ,Map<String ,String>> v_Ret          = this.redis.newMapMap();
        ShardedJedis                     v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            
            for (int i=0; i<i_Keys.length; i++)
            {
                v_Ret.put(i_Keys[i] ,v_ShardedJedis.hgetAll(i_Keys[i]));
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 获取多个哈希表的所有的域和值
     * 
     * 原始命令为：HMGET key field [field ...]
     * 
     * 返回哈希表 key 中，一个或多个给定域的值。
     * 
     * 若 key 不存在，返回空列表。
     * 
     * @param i_Keys    只获取哪些Key的相关信息
     * @param i_Fields  只获取哪些字段
     * @return        Map<String ,List<String>>.key        表示一个哈希表Key值
     *                Map<String ,List<String>>.List       表示一个哈希表对应的值。表值的排列顺序和给定域参数的请求顺序一样。
     */
    private Map<String ,List<String>> core_getValuesByKeys(String [] i_Keys ,String [] i_Fields)
    {
        if ( i_Keys == null )
        {
            throw new NullPointerException("Keys is null.");
        }
        
        if ( JavaHelp.isNull(i_Fields) )
        {
            throw new NullPointerException("Fields is null.");
        }
        
        for (int i=0; i<i_Keys.length; i++)
        {
            if ( i_Keys[i] == null )
            {
                throw new NullPointerException("Keys[" + i + "] is null.");
            }
        }
        
        for (int i=0; i<i_Fields.length; i++)
        {
            if ( i_Fields[i] == null )
            {
                throw new NullPointerException("Fields[" + i + "] is null.");
            }
        }
        
        Map<String ,List<String>> v_Ret          = this.redis.newMapList();
        ShardedJedis              v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            
            for (int i=0; i<i_Keys.length; i++)
            {
                v_Ret.put(i_Keys[i] ,v_ShardedJedis.hmget(i_Keys[i] ,i_Fields));
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    public Set<String> getFields(String i_Key)
    {
        Map<String ,Set<String>> v_Ret = this.core_getFieldsByKeys(new String[]{i_Key});
        
        return v_Ret.get(i_Key);
    }
    
    
    
    public Map<String ,Set<String>> getFields(String ... i_Keys)
    {
        return this.core_getFieldsByKeys(i_Keys);
    }
    
    
    
    public Map<String ,Set<String>> getFields(List<String> i_Keys)
    {
        return this.core_getFieldsByKeys(i_Keys.toArray(new String []{}));
    }
    
    
    
    /**
     * 原始命令为：HKEYS key
     * 
     * 返回哈希表 key 中的所有域。
     *
     * 一个包含哈希表中所有域的表。当 key 不存在时，返回一个空表
     * 
     * @param i_Keys    只获取哪些Key的相关信息
     */
    private Map<String ,Set<String>> core_getFieldsByKeys(String [] i_Keys)
    {
        if ( i_Keys == null )
        {
            throw new NullPointerException("Keys is null.");
        }
        
        for (int i=0; i<i_Keys.length; i++)
        {
            if ( i_Keys[i] == null )
            {
                throw new NullPointerException("Keys[" + i + "] is null.");
            }
        }
        
        Map<String ,Set<String>> v_Ret          = this.redis.newMapSet();
        ShardedJedis              v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            
            for (int i=0; i<i_Keys.length; i++)
            {
                v_Ret.put(i_Keys[i] ,v_ShardedJedis.hkeys(i_Keys[i]));
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    public List<String> del(String i_Key ,String i_Field)
    {
        return this.core_dels(false ,new RData[]{new RData(i_Key ,i_Field ,null)});
    }
    
    
    
    public List<String> dels(String i_Key ,String ... i_Fields)
    {
        RData [] v_RDatas = new RData[i_Fields.length];
        
        for (int i=0; i<i_Fields.length; i++)
        {
            v_RDatas[i] = new RData(i_Key ,i_Fields[i] ,null);
        }
        
        return this.core_dels(false ,v_RDatas);
    }
    
    
    
    public List<String> dels(String i_Key ,List<String> i_Fields)
    {
        RData [] v_RDatas = new RData[i_Fields.size()];
        
        for (int i=0; i<i_Fields.size(); i++)
        {
            v_RDatas[i] = new RData(i_Key ,i_Fields.get(i) ,null);
        }
        
        return this.core_dels(false ,v_RDatas);
    }
    
    
    
    public List<String> dels(List<RData> i_RDatas)
    {
        return this.core_dels(false ,i_RDatas.toArray(new RData []{}));
    }
    
    
    
    public List<String> dels(RData ... i_RDatas)
    {
        return this.core_dels(false ,i_RDatas);
    }
    
    
    
    public List<String> del(boolean i_IsEveryone ,String i_Key ,String i_Field)
    {
        return this.core_dels(i_IsEveryone ,new RData[]{new RData(i_Key ,i_Field ,null)});
    }
    
    
    
    public List<String> dels(boolean i_IsEveryone ,String i_Key ,String ... i_Fields)
    {
        RData [] v_RDatas = new RData[i_Fields.length];
        
        for (int i=0; i<i_Fields.length; i++)
        {
            v_RDatas[i] = new RData(i_Key ,i_Fields[i] ,null);
        }
        
        return this.core_dels(i_IsEveryone ,v_RDatas);
    }
    
    
    
    public List<String> dels(boolean i_IsEveryone ,String i_Key ,List<String> i_Fields)
    {
        RData [] v_RDatas = new RData[i_Fields.size()];
        
        for (int i=0; i<i_Fields.size(); i++)
        {
            v_RDatas[i] = new RData(i_Key ,i_Fields.get(i) ,null);
        }
        
        return this.core_dels(i_IsEveryone ,v_RDatas);
    }
    
    
    
    public List<String> dels(boolean i_IsEveryone ,List<RData> i_RDatas)
    {
        return this.core_dels(i_IsEveryone ,i_RDatas.toArray(new RData []{}));
    }
    
    
    
    public List<String> dels(boolean i_IsEveryone ,RData ... i_RDatas)
    {
        return this.core_dels(i_IsEveryone ,i_RDatas);
    }
    
    
    
    /**
     * 原始命令为：HDEL key field [field ...]
     * 
     * 删除哈希表 key 中的一个或多个指定域，不存在的域将被忽略。
     * 
     * @param i_IsEveryone  是否操作每一个集群，与RunMode.$Backup同意。
     *                      RunMode.$Backup 为广义定义
     *                      i_IsEveryone    为私人定制
     * @param i_RDatas
     * @return              返回实际操作的Key集合
     */
    private List<String> core_dels(boolean i_IsEveryone ,RData [] i_RDatas)
    {
        if ( JavaHelp.isNull(i_RDatas) )
        {
            throw new NullPointerException("RDatas is null.");
        }
        
        TablePartition<String ,String> v_Datas = new TablePartition<String ,String>();
        
        for (int i=0; i<i_RDatas.length; i++)
        {
            RData v_RData = i_RDatas[i];
            
            if ( v_RData == null )
            {
                throw new NullPointerException("RDatas[" + i + "] is null.");
            }
            
            if ( v_RData.getKey() == null )
            {
                throw new NullPointerException("RDatas[" + i + "] key is null.");
            }
            
            v_Datas.putRow(v_RData.getKey() ,JavaHelp.NVL(v_RData.getValue()));
        }
        
        
        ShardedJedis v_ShardedJedis = null;
        List<String> v_Ret          = null;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            if ( this.redis.getRunMode() == Redis.RunMode.$Backup || i_IsEveryone )
            {
                List<String> v_ShardNames = this.redis.getShardNames();
                
                if ( JavaHelp.isNull(v_ShardNames) )
                {
                    throw new NullPointerException("ShardInfos is null.");
                }
                
                for (int v_ShardIndex=v_ShardNames.size() - 1; v_ShardIndex>=0; v_ShardIndex--)
                {
                    String v_ShardName = v_ShardNames.get(v_ShardIndex);
                    
                    try
                    {
                        if ( v_Datas.size() >= 2 )
                        {
                            Transaction v_Transaction = v_ShardedJedis.getShard(v_ShardName).multi();
                            
                            for (String v_Key : v_Datas.keySet())
                            {
                                v_Transaction.hdel(v_Key ,v_Datas.get(v_Key).toArray(new String[]{}));
                            }
                            
                            v_Transaction.exec();
                        }
                        else
                        {
                            // 单Key操作，无须事务。即 for 实际上只循环一次
                            for (String v_Key : v_Datas.keySet())
                            {
                                v_ShardedJedis.getShard(v_ShardName).hdel(v_Key ,v_Datas.get(v_Key).toArray(new String[]{}));
                            }
                        }
                    }
                    catch (Exception exce)
                    {
                        this.redis.shardException(v_ShardName);
                    }
                }
            }
            else
            {
                for (String v_Key : v_Datas.keySet())
                {
                    v_ShardedJedis.hdel(v_Key ,v_Datas.get(v_Key).toArray(new String[]{}));
                }
            }
            
            this.redis.returnResource(v_ShardedJedis);
            v_Ret = JavaHelp.toListKeys(v_Datas);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
}

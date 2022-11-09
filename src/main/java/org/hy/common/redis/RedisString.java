package org.hy.common.redis;

import java.util.List;

import org.hy.common.JavaHelp;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.Transaction;





/**
 * Redis数据库中的String对象的相关操作类
 * 
 * @author ZhengWei(HY)
 * @create 2014-10-15
 */
public class RedisString
{
    private final static String [] $PutType_NXXX   = {"" ,"NX" ,"XX"};
    
    /** EX|PX, expire time units: EX = seconds; PX = milliseconds */
    private final static String    $ExpireTimeType = "EX";
    
    
    private Redis redis;
    
    
    
    public RedisString(Redis i_Redis)
    {
        if ( i_Redis == null )
        {
            throw new NullPointerException("Redis is null.");
        }
        
        this.redis = i_Redis;
    }
    
    
    
    /**
     * 原始命令为：GET key
     * 
     * 返回 key 所关联的字符串值。
     * 如果 key 不存在那么返回特殊值 nil 。
     * 假如 key 储存的值不是字符串类型，返回null，因为 GET 只能用于处理字符串值。
     * 
     * @param i_Key
     * @return
     */
    public String get(String i_Key)
    {
        return this.core_get(i_Key);
    }
    
    
    
    /**
     * 原始命令为：GET key
     * 
     * 返回 key 所关联的字符串值。
     * 如果 key 不存在那么返回特殊值 nil 。
     * 假如 key 储存的值不是字符串类型，返回null，因为 GET 只能用于处理字符串值。
     * 
     * @param i_Key
     * @return
     */
    private String core_get(String i_Key)
    {
        if ( i_Key == null )
        {
            throw new NullPointerException("Key is null.");
        }
        
        ShardedJedis v_ShardedJedis = null;
        String       v_Ret          = null;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            
            v_Ret = v_ShardedJedis.get(i_Key);
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret = null;
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 原始命令为：STRLEN key
     * 
     * 返回 key 所储存的字符串值的长度。
     * 当 key 储存的不是字符串值时，返回null。
     * 
     * @param i_Key
     * @return
     */
    public Long length(String i_Key)
    {
        return this.core_StringLen(i_Key);
    }
    
    
    
    /**
     * 原始命令为：STRLEN key
     * 
     * 返回 key 所储存的字符串值的长度。
     * 当 key 储存的不是字符串值时，返回null。
     * 
     * @param i_Key
     * @return
     */
    private Long core_StringLen(String i_Key)
    {
        if ( i_Key == null )
        {
            throw new NullPointerException("Key is null.");
        }
        
        ShardedJedis v_ShardedJedis = null;
        Long         v_Ret          = null;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            
            v_Ret = v_ShardedJedis.strlen(i_Key);
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret = null;
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 原始命令为：GETRANGE key start end
     *
     * 返回 key 中字符串值的子字符串，字符串的截取范围由 start 和 end 两个偏移量决定(包括 start 和 end 在内)。
     * 负数偏移量表示从字符串最后开始计数， -1 表示最后一个字符， -2 表示倒数第二个，以此类推。
     * GETRANGE 通过保证子字符串的值域(range)不超过实际字符串的值域来处理超出范围的值域请求。
     * 
     * @param i_Key
     * @param i_Start
     * @param i_End
     * @return
     */
    public String getRange(String i_Key ,long i_Start ,long i_End)
    {
        return this.core_getRange(i_Key ,i_Start ,i_End);
    }
    
    
    
    /**
     * 原始命令为：GETRANGE key start end
     *
     * 返回 key 中字符串值的子字符串，字符串的截取范围由 start 和 end 两个偏移量决定(包括 start 和 end 在内)。
     * 负数偏移量表示从字符串最后开始计数， -1 表示最后一个字符， -2 表示倒数第二个，以此类推。
     * GETRANGE 通过保证子字符串的值域(range)不超过实际字符串的值域来处理超出范围的值域请求。
     * 
     * @param i_Key
     * @param i_Start
     * @param i_End
     * @return
     */
    private String core_getRange(String i_Key ,long i_Start ,long i_End)
    {
        if ( i_Key == null )
        {
            throw new NullPointerException("Key is null.");
        }
        
        ShardedJedis v_ShardedJedis = null;
        String       v_Ret          = null;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            
            v_Ret = v_ShardedJedis.getrange(i_Key ,i_Start ,i_End);
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret = null;
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 原始命令为：APPEND key value
     * 
     * 如果 key 已经存在并且是一个字符串， APPEND 命令将 value 追加到 key 原来的值的末尾。
     * 如果 key 不存在， APPEND 就简单地将给定 key 设为 value ，就像执行 SET key value 一样。
     * 
     * @param i_Key
     * @param i_Value
     * @return
     */
    public boolean append(String i_Key ,String i_Value)
    {
        return this.core_append(i_Key ,i_Value);
    }
    
    
    
    /**
     * 原始命令为：APPEND key value
     * 
     * 如果 key 已经存在并且是一个字符串， APPEND 命令将 value 追加到 key 原来的值的末尾。
     * 如果 key 不存在， APPEND 就简单地将给定 key 设为 value ，就像执行 SET key value 一样。
     * 
     * @param i_Key
     * @param i_Value
     * @return
     */
    private boolean core_append(String i_Key ,String i_Value)
    {
        if ( i_Key == null )
        {
            throw new NullPointerException("Key is null.");
        }
        
        if ( i_Value == null || i_Value.length() <= 0 )  // 允许空格，没有用JavaHelp.isNull()
        {
            throw new NullPointerException("Value is null.");
        }
        
        ShardedJedis v_ShardedJedis = null;
        boolean      v_Ret          = false;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            v_Ret = v_ShardedJedis.append(i_Key ,i_Value) > 0;
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret = false;
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 递增操作
     * 
     * 将 key 中储存的数字值增一。
     * 
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCR 操作。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * 
     * @param i_Key
     * @return         异常时返回null
     */
    public Long increment(String i_Key)
    {
        return this.core_IncrementDecrement(i_Key ,1);
    }
    
    
    
    /**
     * 递减操作
     * 
     * 将 key 中储存的数字值减一。
     * 
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 DECR 操作。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * 
     * @param i_Key
     * @return         异常时返回null
     */
    public Long decrement(String i_Key)
    {
        return this.core_IncrementDecrement(i_Key ,-1);
    }
    
    
    
    /**
     * 递增或递减操作
     * 
     * 原始命令为：INCRBY key increment
     * 原始命令为：DECRBY key decrement
     * 
     * 将 key 所储存的值加上增量 increment 。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCRBY 命令。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * 
     * @param i_Key
     * @param i_Value  正值为添加，负值为减少
     * @return         异常时返回null
     */
    public Long increment(String i_Key ,long i_Value)
    {
        return this.core_IncrementDecrement(i_Key ,i_Value);
    }
    
    
    
    /**
     * 递增或递减操作
     * 
     * 原始命令为：INCRBY key increment
     * 原始命令为：DECRBY key decrement
     * 
     * 将 key 所储存的值加上增量 increment 。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCRBY 命令。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * 
     * @param i_Key
     * @param i_Value  正值为添加，负值为减少
     * @return         异常时返回null
     */
    public Long decrement(String i_Key ,long i_Value)
    {
        return this.core_IncrementDecrement(i_Key ,i_Value);
    }
    
    
    
    /**
     * 递增或递减操作
     * 
     * 原始命令为：INCRBY key increment
     * 原始命令为：DECRBY key decrement
     * 
     * 将 key 所储存的值加上增量 increment 。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCRBY 命令。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回null
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * 
     * @param i_Key
     * @param i_Value  正值为添加，负值为减少
     * @return         异常时返回null
     */
    private Long core_IncrementDecrement(String i_Key ,long i_Value)
    {
        if ( i_Key == null )
        {
            throw new NullPointerException("Key is null.");
        }
        
        ShardedJedis v_ShardedJedis = null;
        Long         v_Ret          = null;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            v_Ret = v_ShardedJedis.incrBy(i_Key ,i_Value);
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret = null;
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 原始命令为：SET key value
     * 
     * 将字符串值 value 关联到 key 。
     * 
     * 如果 key 已经持有其他值， SET 就覆写旧值，无视类型
     * 
     * @param i_Key    关键字
     * @param i_Value  值
     * @return         返回值表示是否Set值执行成功
     */
    public boolean put(String i_Key ,String i_Value)
    {
        return this.core_put(new RData(i_Key ,i_Value));
    }
    
    
    
    /**
     * 原始命令为：SETNX key value
     * 
     * 将 key 的值设为 value ，当且仅当 key 不存在。
     * 
     * 若给定的 key 已经存在，则 SETNX 不做任何动作。
     * 
     * @param i_Key    关键字
     * @param i_Value  值
     * @return         返回值表示是否Set值执行成功
     */
    public boolean putNotExists(String i_Key ,String i_Value)
    {
        RData v_RData = new RData(i_Key ,i_Value);
        
        v_RData.setPutType(RData.PutType.NX);
        
        return this.core_put(v_RData);
    }
    
    
    
    /**
     * 原始命令为：SET key value EX seconds NX
     * 
     * 将 key 的值设为 value ，当且仅当 key 不存在时。并将 key 的生存时间设为 seconds (以秒为单位)。
     * 
     * 若给定的 key 已经存在，则 SETNX 不做任何动作。
     * 
     * @param i_Key      关键字
     * @param i_Value    值
     * @param i_Seconds  生存时间(以秒为单位)
     * @return           返回值表示是否Set值执行成功
     */
    public boolean putNotExists(String i_Key ,String i_Value ,int i_Seconds)
    {
        if ( i_Seconds < 1 )
        {
            throw new IndexOutOfBoundsException("ExpireTime Seconds < 1.");
        }
        
        RData v_RData = new RData(i_Key ,i_Value);
        
        v_RData.setPutType(   RData.PutType.NX);
        v_RData.setExpireTime(i_Seconds);
        
        return this.core_put(v_RData);
    }
    
    
    
    /**
     * 原始命令为：SETNX key value
     * 
     * 将值 value 关联到 key ，并将 key 的生存时间设为 seconds (以秒为单位)。
     * 
     * 如果 key 已经存在， SETEX 命令将覆写旧值。
     * 
     * @param i_Key
     * @param i_Value
     * @param i_Seconds  生存时间(以秒为单位)
     * @return           返回值表示是否Set值执行成功
     */
    public boolean put(String i_Key ,String i_Value ,int i_Seconds)
    {
        if ( i_Seconds < 1 )
        {
            throw new IndexOutOfBoundsException("ExpireTime Seconds < 1.");
        }
        
        RData v_RData = new RData(i_Key ,i_Value);
        
        v_RData.setExpireTime(i_Seconds);
        
        return this.core_put(v_RData);
    }
    
    
    
    /**
     * 参见于 core_put(...)方法
     * 
     * @param i_RData
     * @return         返回值表示是否Set值执行成功
     */
    public boolean put(RData i_RData)
    {
        return this.core_put(i_RData);
    }
    
    
    
    /**
     * 原始命令为：SET key value [EX seconds] [PX milliseconds] [NX|XX]
     * 
     * 将字符串值 value 关联到 key 。
     * 
     * 如果 key 已经持有其他值， SET 就覆写旧值，无视类型。
     * 
     * 对于某个原本带有生存时间（TTL）的键来说， 当 SET 命令成功在这个键上执行时， 这个键原有的 TTL 将被清除。
     * 
     * EX second ：设置键的过期时间为 second 秒。 SET key value EX second 效果等同于 SETEX key second value 。
     * PX millisecond ：设置键的过期时间为 millisecond 毫秒。 SET key value PX millisecond 效果等同于 PSETEX key millisecond value 。
     * 
     * NX ：只在键不存在时，才对键进行设置操作。 SET key value NX 效果等同于 SETNX key value 。
     * XX ：只在键已经存在时，才对键进行设置操作。
     * 
     * @param i_RData
     * @return         返回值表示是否Set值执行成功
     */
    private boolean core_put(RData i_RData)
    {
        if ( i_RData == null )
        {
            throw new NullPointerException("RData is null.");
        }
        
        if ( i_RData.getKey() == null )
        {
            throw new NullPointerException("RData.Key is null.");
        }
        
        if ( i_RData.getValue() == null )
        {
            throw new NullPointerException("RData.Value is null.");
        }
        
        ShardedJedis v_ShardedJedis = null;
        boolean      v_Ret          = false;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            if ( i_RData.getPutType() == RData.PutType.Normal && i_RData.getExpireTime() <= 0 )
            {
                v_ShardedJedis.set(i_RData.getKey() ,i_RData.getValue());
            }
            else if ( i_RData.getPutType() != RData.PutType.Normal && i_RData.getExpireTime() > 0 )
            {
                v_ShardedJedis.set(i_RData.getKey() ,i_RData.getValue() ,$PutType_NXXX[i_RData.getPutType().ordinal()] ,$ExpireTimeType ,i_RData.getExpireTime());
            }
            else if ( i_RData.getExpireTime() > 0 )
            {
                v_ShardedJedis.setex(i_RData.getKey() ,i_RData.getExpireTime() ,i_RData.getValue());
            }
            else if ( i_RData.getPutType() == RData.PutType.NX )
            {
                v_Ret = v_ShardedJedis.setnx(i_RData.getKey() ,i_RData.getValue()) == 1L;
            }
            else if ( i_RData.getPutType() == RData.PutType.XX )
            {
                // API没有提供这样的功能
                v_Ret = false;
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret = false;
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    public boolean puts(List<RData> i_RDatas)
    {
        return this.core_puts(i_RDatas.toArray(new RData[]{}));
    }
    
    
    
    /**
     * 单集群测试没有问题。但多集群分布式可能有问题
     * 
     * @param i_RDatas
     * @return
     */
    private boolean core_puts(RData [] i_RDatas)
    {
        if ( JavaHelp.isNull(i_RDatas) )
        {
            throw new NullPointerException("RDatas is null.");
        }
        
        for (int i=0; i<i_RDatas.length; i++)
        {
            RData v_RData = i_RDatas[i];
            
            if ( v_RData == null )
            {
                throw new NullPointerException("RData[" + i + "] is null.");
            }
            
            if ( v_RData.getKey() == null )
            {
                throw new NullPointerException("RData[" + i + "].Key is null.");
            }
            
            if ( v_RData.getValue() == null )
            {
                throw new NullPointerException("RData[" + i + "].Value is null.");
            }
        }
        
        ShardedJedis v_ShardedJedis = null;
        Transaction  v_Transaction  = null;
        boolean      v_Ret          = false;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            v_Transaction = v_ShardedJedis.getAllShards().iterator().next().multi();
            
            for (int i=0; i<i_RDatas.length; i++)
            {
                RData v_RData = i_RDatas[i];
                
                v_Transaction.set(v_RData.getKey() ,v_RData.getValue());
            }
            
            v_Transaction.exec();
            v_Ret = true;
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret = false;
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
}

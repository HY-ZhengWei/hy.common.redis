package org.hy.common.redis;

import java.util.List;
import java.util.Set;

import org.hy.common.JavaHelp;

import redis.clients.jedis.ShardedJedis;





/**
 * Redis数据库中的Key对象的相关操作类
 * 
 * @author ZhengWei(HY)
 * @create 2014-09-17
 */
public class RedisKey
{
    private Redis redis;
    
    
    
    public RedisKey(Redis i_Redis)
    {
        if ( i_Redis == null )
        {
            throw new NullPointerException("Redis is null.");
        }
        
        this.redis = i_Redis;
    }
    
    
    
    /**
     * 原始命令为：EXISTS key
     * 
     * 检查给定 key 是否存在
     * 
     * @param i_Key
     * @return
     */
    public boolean isExists(String i_Key)
    {
        if ( i_Key == null )
        {
            throw new NullPointerException("Key is null.");
        }
        
        ShardedJedis v_ShardedJedis = null;
        boolean      v_Ret          = false;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            
            v_Ret = v_ShardedJedis.exists(i_Key);
            
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
     * 原始命令为：TYPE key
     * 
     * 返回 key 所储存的值的类型。如下
     * 
     *   1. none    (key不存在) -- 已修改为返回 null
     *   2. string  (字符串)
     *   3. list    (列表)
     *   4. set     (集合)
     *   5. zset    (有序集)
     *   6. hash    (哈希表)
     * 
     * @param i_Key
     * @return
     */
    public String getKeyType(String i_Key)
    {
        if ( JavaHelp.isNull(i_Key) )
        {
            throw new NullPointerException("Key is null.");
        }
        
        ShardedJedis v_ShardedJedis = null;
        String       v_Ret          = null;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            
            v_Ret = v_ShardedJedis.type(i_Key);
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret = null;
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        if ( v_Ret == null )
        {
            return v_Ret;
        }
        else
        {
            return "none".equals(v_Ret) ? null : v_Ret;
        }
    }
    
    
    
    /**
     * 原始命令为：EXPIRE key seconds
     * 
     * 为给定 key 设置生存时间，当 key 过期时(生存时间为 0 )，它会被自动删除。
     * 
     * 生存时间可以通过使用 DEL 命令来删除整个 key 来移除，
     * 或者被 SET 和 GETSET 命令覆写(overwrite)，这意味着，
     * 
     * 如果一个命令只是修改(alter)一个带生存时间的 key 的值，
     * 而不是用一个新的 key 值来代替(replace)它的话，那么生存时间不会被改变。
     * 
     * 另一方面，如果使用 RENAME 对一个 key 进行改名，那么改名后的 key 的生存时间和改名前一样。
     * 
     * @param i_Key
     * @return
     */
    public boolean expire(String i_Key ,int i_Seconds)
    {
        if ( i_Key == null )
        {
            throw new NullPointerException("Key is null.");
        }
        
        if ( i_Seconds <= 0 )
        {
            throw new IndexOutOfBoundsException("i_Seconds <= 0.");
        }
        
        ShardedJedis v_ShardedJedis = null;
        boolean      v_Ret          = false;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            v_Ret = v_ShardedJedis.expire(i_Key ,i_Seconds) == 1;
            
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
     * 原始命令为：EXPIRE key seconds
     * 
     * 移除给定 key 的生存时间，将这个 key 从『易失的』(带生存时间 key )，
     * 转换成『持久的』(一个不带生存时间、永不过期的 key )
     * 
     * @param i_Key
     * @return
     */
    public boolean delExpire(String i_Key)
    {
        if ( i_Key == null )
        {
            throw new NullPointerException("Key is null.");
        }
        
        ShardedJedis v_ShardedJedis = null;
        boolean      v_Ret          = false;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            v_Ret = v_ShardedJedis.persist(i_Key) == 1;
            
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
     * 原始命令为：TTL key
     * 
     * 以秒为单位，返回给定 key 的剩余生存时间(TTL, time to live)。
     * 
     * @param i_Key
     * @return       当 key 不存在时，返回 -2 。
     *               当 key 存在但没有设置剩余生存时间时，返回 -1 。
     *               否则，以秒为单位，返回 key 的剩余生存时间。
     */
    public Long timeToLive(String i_Key)
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
            
            v_Ret = v_ShardedJedis.ttl(i_Key);
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret = null;
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    public void dels(String ... i_Keys)
    {
        this.core_dels(i_Keys);
    }
    
    
    
    public void dels(Set<String> i_Keys)
    {
        this.core_dels(i_Keys.toArray(new String []{}));
    }
    
    
    
    public void dels(List<String> i_Keys)
    {
        this.core_dels(i_Keys.toArray(new String []{}));
    }
    
    
    
    /**
     * 原始命令为：DEL key [key ...]
     * 
     * 删除给定的一个或多个 key 。
     * 
     * 不存在的 key 会被忽略。
     * 
     * @param i_Keys
     */
    private void core_dels(String [] i_Keys)
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
        
        ShardedJedis v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            for (int i=0; i<i_Keys.length; i++)
            {
                v_ShardedJedis.del(i_Keys[i]);
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
    }
    
}

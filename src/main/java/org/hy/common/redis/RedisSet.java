package org.hy.common.redis;

import redis.clients.jedis.ShardedJedis;





/**
 * Redis数据库中的Set对象的相关操作类
 * 
 * @author ZhengWei(HY)
 * @create 2014-09-16
 */
public class RedisSet
{
    
    private Redis redis;
    
    
    
    public RedisSet(Redis i_Redis)
    {
        if ( i_Redis == null )
        {
            throw new NullPointerException("Redis is null.");
        }
        
        this.redis = i_Redis;
    }
    
    
    
    /**
     * 原始命令为：SADD key member [member ...]
     * 
     * 将一个或多个 member 元素加入到集合 key 当中，
     * 已经存在于集合的 member 元素将被忽略。
     * 
     * 假如 key 不存在，则创建一个只包含 member 元素作成员的集合。
     * 当 key 不是集合类型时，返回一个错误。
     * 
     * @param i_Key
     * @param i_Value
     */
    public void add(String i_Key ,String ... i_Value)
    {
        ShardedJedis v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            v_ShardedJedis.sadd(i_Key ,i_Value);
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
    }
    
    
    
    /**
     * 原始命令为：SISMEMBER key member
     * 
     * 判断 member 元素是否集合 key 的成员。
     * 
     * @param i_Key
     * @param i_Value
     * @return
     */
    public boolean isExists(String i_Key ,String i_Value)
    {
        boolean      v_Ret          = false;
        ShardedJedis v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            
            v_Ret = v_ShardedJedis.sismember(i_Key ,i_Value);
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
}

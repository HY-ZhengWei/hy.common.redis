package org.hy.common.redis.cluster;

import java.util.ArrayList;
import java.util.List;

import org.hy.common.xml.SerializableDef;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;





/**
 * Redis集合配置
 *
 * @author      ZhengWei(HY)
 * @createDate  2024-03-14
 * @version     v1.0
 */
public class RedisClusterConfig extends SerializableDef
{

    private static final long serialVersionUID = -7839804471664587167L;
    
    
    /** Redis节点集合 */
    private List<RedisConfig> redisConfigs;
    
    
    
    public RedisClusterConfig()
    {
        this.redisConfigs = new ArrayList<RedisConfig>();
    }
    
    
    
    /**
     * 转化生成的Lettuce对象
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-14
     * @version     v1.0
     *
     * @return
     */
    public RedisClusterClient toLettuce()
    {
        List<RedisURI> v_RedisURIs = new ArrayList<RedisURI>();
        
        try
        {
            for (RedisConfig v_RedisConfig : this.redisConfigs)
            {
                v_RedisURIs.add(v_RedisConfig.toLettuce());
            }
            
            return RedisClusterClient.create(v_RedisURIs);
        }
        finally
        {
            v_RedisURIs.clear();
            v_RedisURIs = null;
        }
    }
    
    
    
    /**
     * 添加Redis节点
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-03-14
     * @version     v1.0
     *
     * @param i_RedisConfig
     */
    public void setAdd(RedisConfig i_RedisConfig)
    {
        this.redisConfigs.add(i_RedisConfig);
    }


    
    /**
     * 获取：Redis节点集合
     */
    public List<RedisConfig> getRedisConfigs()
    {
        return redisConfigs;
    }


    
    /**
     * 设置：Redis节点集合
     * 
     * @param i_RedisConfigs Redis节点集合
     */
    public void setRedisConfigs(List<RedisConfig> i_RedisConfigs)
    {
        this.redisConfigs = i_RedisConfigs;
    }
    
}

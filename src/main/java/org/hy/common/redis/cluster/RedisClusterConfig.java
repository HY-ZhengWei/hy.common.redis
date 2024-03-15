package org.hy.common.redis.cluster;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.hy.common.Help;
import org.hy.common.xml.SerializableDef;

import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.protocol.ProtocolVersion;





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
    
    /** 自动重新连接 */
    private Boolean           autoReconnect;
    
    /** 超时时长（单位：秒） */
    private Integer           timeout;
    
    /** 最大重定向次数 */
    private Integer           maxRedirects;
    
    /** Redis协议版本。（取值范围：2、3、4、5、6） */
    private Integer           redisVersion;
    
    /** 字符集 */
    private String            charset;
    
    
    
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
    public synchronized RedisClusterClient toLettuce()
    {
        if ( Help.isNull(this.redisConfigs) )
        {
            return null;
        }
        
        List<RedisURI> v_RedisURIs = new ArrayList<RedisURI>();
        
        try
        {
            for (RedisConfig v_RedisConfig : this.redisConfigs)
            {
                // 当未配置每个节点的超时时长，取集群统一的超时时长
                if ( v_RedisConfig.getTimeout() == null )
                {
                    if ( this.timeout != null )
                    {
                        v_RedisConfig.setTimeout(this.timeout);
                    }
                }
                
                v_RedisURIs.add(v_RedisConfig.toLettuce());
            }
            
            // 创建 GenericObjectPoolConfig 对象，并设置连接池参数
            GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
            poolConfig.setMaxTotal(20); // 设置连接池最大连接数
            poolConfig.setMaxIdle(10); // 设置连接池中最大空闲连接数
            
            RedisClusterClient           v_RedisCluster         = RedisClusterClient.create(v_RedisURIs);
            ClusterClientOptions.Builder v_ClusterClientBuilder = ClusterClientOptions.builder();
            
            if ( this.autoReconnect != null )
            {
                v_ClusterClientBuilder.autoReconnect(this.autoReconnect);
            }
            
            if ( this.timeout != null )
            {
                v_ClusterClientBuilder.timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(this.timeout)));
            }
            
            if ( this.maxRedirects != null )
            {
                v_ClusterClientBuilder.maxRedirects(this.maxRedirects);
            }
            
            if ( this.redisVersion != null )
            {
                if ( this.redisVersion >= 2 && this.redisVersion <= 5 )
                {
                    v_ClusterClientBuilder.protocolVersion(ProtocolVersion.RESP2);
                }
                else if ( this.redisVersion == 6 )
                {
                    v_ClusterClientBuilder.protocolVersion(ProtocolVersion.RESP3);
                }
            }
            
            if ( !Help.isNull(this.charset) )
            {
                v_ClusterClientBuilder.scriptCharset(Charset.forName(this.charset));
            }
            
            // 设置集群客户端选项
            ClusterClientOptions v_ClusterClientOptions = v_ClusterClientBuilder.build();
            v_RedisCluster.setOptions(v_ClusterClientOptions);
            
            return v_RedisCluster;
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
    public synchronized void setAdd(RedisConfig i_RedisConfig)
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


    
    /**
     * 获取：自动重新连接
     */
    public Boolean getAutoReconnect()
    {
        return autoReconnect;
    }


    
    /**
     * 设置：自动重新连接
     * 
     * @param i_AutoReconnect 自动重新连接
     */
    public void setAutoReconnect(Boolean i_AutoReconnect)
    {
        this.autoReconnect = i_AutoReconnect;
    }



    /**
     * 获取：超时时长（单位：秒）
     */
    public Integer getTimeout()
    {
        return timeout;
    }


    
    /**
     * 设置：超时时长（单位：秒）
     * 
     * @param i_Timeout 超时时长（单位：秒）
     */
    public void setTimeout(Integer i_Timeout)
    {
        this.timeout = i_Timeout;
    }

    
    
    /**
     * 获取：最大重定向次数
     */
    public Integer getMaxRedirects()
    {
        return maxRedirects;
    }

    
    
    /**
     * 设置：最大重定向次数
     * 
     * @param i_MaxRedirects 最大重定向次数
     */
    public void setMaxRedirects(Integer i_MaxRedirects)
    {
        this.maxRedirects = i_MaxRedirects;
    }

    
    
    /**
     * 获取：Redis协议版本。（取值范围：2、3、4、5、6）
     */
    public Integer getRedisVersion()
    {
        return redisVersion;
    }

    

    /**
     * 设置：Redis协议版本。（取值范围：2、3、4、5、6）
     * 
     * @param i_RedisVersion Redis协议版本。（取值范围：2、3、4、5、6）
     */
    public void setRedisVersion(Integer i_RedisVersion)
    {
        this.redisVersion = i_RedisVersion;
    }

    
    
    /**
     * 获取：字符集
     */
    public String getCharset()
    {
        return charset;
    }

    
    
    /**
     * 设置：字符集
     * 
     * @param i_Charset 字符集
     */
    public void setCharset(String i_Charset)
    {
        this.charset = i_Charset;
    }
    
}

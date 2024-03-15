package org.hy.common.redis.cluster;

import java.time.Duration;

import org.hy.common.Help;
import org.hy.common.xml.SerializableDef;

import io.lettuce.core.RedisURI;





/**
 * Redis配置信息
 *
 * @author      ZhengWei(HY)
 * @createDate  2024-03-14
 * @version     v1.0
 */
public class RedisConfig extends SerializableDef
{
    
    private static final long serialVersionUID = -6564548432812836694L;
    
    
    
    /** IP地址 */
    private String  host;
    
    /** 访问端口 */
    private Integer port;
    
    /** 密码 */
    private String  pwd;
    
    /** 连接数据库编号（下标从0开始） */
    private Integer database;
    
    /** 超时时长（单位：秒） */
    private Integer timeout;
    
    /** 开启 SSL 连接协议 */
    private Boolean ssl;
    
    /** 开启 tls 连接协议 */
    private Boolean tls;
    
    
    
    public RedisConfig()
    {
        
    }
    
    
    
    public RedisConfig(RedisConfig i_Other)
    {
        this.initNotNull(i_Other);
    }
    
    
    
    public void setInit(RedisConfig i_Other)
    {
        this.initNotNull(i_Other);
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
    public RedisURI toLettuce()
    {
        RedisURI.Builder v_Builder = RedisURI.builder();
        
        v_Builder.withHost(this.host);
        v_Builder.withPort(this.port);
        
        if ( !Help.isNull(this.pwd) )
        {
            v_Builder.withPassword(this.pwd.toCharArray());
        }
        
        if ( this.database != null )
        {
            v_Builder.withDatabase(this.database);
        }
        
        if ( this.timeout != null )
        {
            v_Builder.withTimeout(Duration.ofSeconds(this.timeout));
        }
        
        if ( this.ssl != null )
        {
            v_Builder.withSsl(this.ssl);
        }
        
        if ( this.ssl != null )
        {
            v_Builder.withStartTls(this.tls);
        }
        
        return v_Builder.build();
    }

    
    
    /**
     * 获取：IP地址
     */
    public String getHost()
    {
        return host;
    }

    
    /**
     * 设置：IP地址
     * 
     * @param i_Host IP地址
     */
    public RedisConfig setHost(String i_Host)
    {
        this.host = i_Host;
        return this;
    }

    
    /**
     * 获取：访问端口
     */
    public Integer getPort()
    {
        return port;
    }

    
    /**
     * 设置：访问端口
     * 
     * @param i_Port 访问端口
     */
    public RedisConfig setPort(Integer i_Port)
    {
        this.port = i_Port;
        return this;
    }

    
    /**
     * 获取：密码
     */
    public String getPwd()
    {
        return pwd;
    }

    
    /**
     * 设置：密码
     * 
     * @param i_Password 密码
     */
    public RedisConfig setPwd(String i_Password)
    {
        this.pwd = i_Password;
        return this;
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
     * 获取：开启 SSL 连接协议
     */
    public Boolean getSsl()
    {
        return ssl;
    }

    
    /**
     * 设置：开启 SSL 连接协议
     * 
     * @param i_Ssl 开启 SSL 连接协议
     */
    public RedisConfig setSsl(Boolean i_Ssl)
    {
        this.ssl = i_Ssl;
        return this;
    }

    
    /**
     * 获取：开启 tls 连接协议
     */
    public Boolean getTls()
    {
        return tls;
    }

    
    /**
     * 设置：开启 tls 连接协议
     * 
     * @param i_Tls 开启 tls 连接协议
     */
    public RedisConfig setTls(Boolean i_Tls)
    {
        this.tls = i_Tls;
        return this;
    }

    
    /**
     * 获取：连接数据库编号（下标从0开始）
     */
    public Integer getDatabase()
    {
        return database;
    }

    
    /**
     * 设置：连接数据库编号（下标从0开始）
     * 
     * @param i_Database 连接数据库编号（下标从0开始）
     */
    public RedisConfig setDatabase(Integer i_Database)
    {
        this.database = i_Database;
        return this;
    }
    
}

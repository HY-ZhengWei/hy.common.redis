package org.hy.common.redis.cluster;

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
        return RedisURI.builder()
                       .withHost(this.host)
                       .withPort(this.port)
                       .withPassword(this.pwd.toCharArray())
                       .build();
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
    
}

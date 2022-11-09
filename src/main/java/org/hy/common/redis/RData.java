package org.hy.common.redis;





/**
 * Redis数据库的数据信息
 * 
 * @author ZhengWei(HY)
 * @create 2014-09-17
 */
public class RData implements Cloneable
{
    
    /** 主要用于Redis.Set命令的扩展特性 */
    public enum PutType
    {
        /** 正常类型的。如果 key 已经持有其他值， SET 就覆写旧值，无视类型 */
        Normal
        
        /** 只在键不存在时，才对键进行设置操作 */
       ,NX
       
        /** 只在键已经存在时，才对键进行设置操作 */
       ,XX
    }
    
    
    private String   key;
    
    private String   field;
    
    private String   value;
    
    /**
     * 设置键的过期时间(单位：秒)
     * 
     * 0：表示不过期
     */
    private int      expireTime;
    
    /**  */
    private PutType  putType;
    
    /** 时间戳 */
    private String   timestamp;
    
    
    
    public RData()
    {
        this(null ,null ,null);
    }
    
    
    
    public RData(String i_Key ,String i_Value)
    {
        this(i_Key ,null ,i_Value);
    }
    
    
    
    public RData(String i_Key ,String i_Field ,String i_Value)
    {
        this.key        = i_Key;
        this.field      = i_Field;
        this.value      = i_Value;
        this.expireTime = 0;
        this.putType    = PutType.Normal;
    }

    
    
    public String getKey()
    {
        return key;
    }

    
    
    public void setKey(String key)
    {
        this.key = key;
    }

    
    
    public String getField()
    {
        return field;
    }
    

    
    public void setField(String field)
    {
        this.field = field;
    }

    
    
    public String getValue()
    {
        return value;
    }

    
    
    public void setValue(String value)
    {
        this.value = value;
    }

    
    
    public int getExpireTime()
    {
        return expireTime;
    }


    
    public void setExpireTime(int expireTime)
    {
        this.expireTime = expireTime;
    }


    
    public PutType getPutType()
    {
        return putType;
    }



    
    public void setPutType(PutType putType)
    {
        this.putType = putType;
    }



    public String getTimestamp()
    {
        return timestamp;
    }


    
    public void setTimestamp(String timestamp)
    {
        this.timestamp = timestamp;
    }



    @Override
    protected RData clone()
    {
        RData v_Clone = new RData(this.key ,this.field ,this.value);
        
        v_Clone.setTimestamp( this.getTimestamp());
        v_Clone.setExpireTime(this.getExpireTime());
        
        return v_Clone;
    }
    
}

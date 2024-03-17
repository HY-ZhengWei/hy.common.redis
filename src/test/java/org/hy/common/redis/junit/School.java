package org.hy.common.redis.junit;

import org.hy.common.Date;
import org.hy.common.xml.SerializableDef;





/**
 * 学校
 *
 * @author      ZhengWei(HY)
 * @createDate  2024-03-17
 * @version     v1.0
 */
public class School extends SerializableDef
{
    
    private static final long serialVersionUID = 8549793134914866215L;
    
    

    /** 学校名称 */
    private String name;
    
    /** 学费 */
    private Double price;
    
    /** 开课时间 */
    private Date   createTime;
    
    
    
    public School()
    {
        
    }
    
    
    
    public School(String i_Name ,Double i_Price ,Date i_CreateTime)
    {
        this.name       = i_Name;
        this.price      = i_Price;
        this.createTime = i_CreateTime;
    }

    
    
    /**
     * 获取：书名称
     */
    public String getName()
    {
        return name;
    }

    
    /**
     * 设置：书名称
     * 
     * @param i_Name 书名称
     */
    public void setName(String i_Name)
    {
        this.name = i_Name;
    }

    
    /**
     * 获取：单价
     */
    public Double getPrice()
    {
        return price;
    }

    
    /**
     * 设置：单价
     * 
     * @param i_Price 单价
     */
    public void setPrice(Double i_Price)
    {
        this.price = i_Price;
    }

    
    /**
     * 获取：上市时间
     */
    public Date getCreateTime()
    {
        return createTime;
    }

    
    /**
     * 设置：上市时间
     * 
     * @param i_CreateTime 上市时间
     */
    public void setCreateTime(Date i_CreateTime)
    {
        this.createTime = i_CreateTime;
    }
    
}

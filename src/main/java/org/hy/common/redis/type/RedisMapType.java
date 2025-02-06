package org.hy.common.redis.type;

import java.util.Map;

import org.hy.common.Help;
import org.hy.common.TablePartitionRID;





/**
 * 在支持外界定义行数据类型是一个通用Map结构的同时，允许外界定义Map结构中的每个元素的Java类型
 *
 * @author      ZhengWei(HY)
 * @createDate  2025-02-06
 * @version     v1.0
 */
public class RedisMapType
{
    
    private static final String $Level = ">";
    
    /** 
     * Map数据结构中每个元素的Java类型。
     * 
     * Map.分区为：库名>表名
     * Map.key为：字段名称
     * Map.value为：字段类型
     */
    private static TablePartitionRID<String ,Class<?>> $Types;
    
    
    
    /**
     * 新增定义或更新数据类型（批量）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-02-06
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_FieldTypes 字段类型的集合。Map.key为字段名称，Map.value为字段类型
     * @return
     */
    public static boolean save(String i_Database ,String i_TableName ,Map<String ,Class<?>> i_FieldTypes)
    {
        if ( Help.isNull(i_FieldTypes) )
        {
            return false;
        }
        
        for (Map.Entry<String ,Class<?>> v_Item : i_FieldTypes.entrySet())
        {
            if ( !save(i_Database ,i_TableName ,v_Item.getKey() ,v_Item.getValue()) )
            {
                return false;
            }
        }
        
        return true;
    }
    
    
    
    /**
     * 新增定义或更新数据类型
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-02-06
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_FieldName  字段名称
     * @param i_FieldType  字段类型
     * @return             成功时返回真，否则返回假
     */
    public static boolean save(String i_Database ,String i_TableName ,String i_FieldName ,Class<?> i_FieldType)
    {
        if ( Help.isNull(i_Database) )
        {
            return false;
        }
        if ( Help.isNull(i_TableName) )
        {
            return false;
        }
        if ( Help.isNull(i_FieldName) )
        {
            return false;
        }
        if ( i_FieldType == null )
        {
            return false;
        }
        
        getTypes().putRow(i_Database + $Level + i_TableName ,i_FieldName ,i_FieldType);
        return true;
    }
    
    
    
    /**
     * 获取存储的字段类型
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-02-06
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @param i_FieldName  字段名称
     * @return             返回字段类型
     */
    public static Class<?> getType(String i_Database ,String i_TableName ,String i_FieldName)
    {
        if ( Help.isNull(i_Database) )
        {
            return null;
        }
        if ( Help.isNull(i_TableName) )
        {
            return null;
        }
        if ( Help.isNull(i_FieldName) )
        {
            return null;
        }
        
        return getTypes().getRow(i_Database + $Level + i_TableName ,i_FieldName);
    }
    
    
    
    /**
     * 获取存储的字段类型（整张表的字段类型）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-02-06
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @return             返回整张表的字段类型
     */
    public static Map<String ,Class<?>> getTypes(String i_Database ,String i_TableName)
    {
        if ( Help.isNull(i_Database) )
        {
            return null;
        }
        if ( Help.isNull(i_TableName) )
        {
            return null;
        }
        
        return getTypes().get(i_Database + $Level + i_TableName);
    }
    
    
    
    /**
     * 获取所有类型
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-02-06
     * @version     v1.0
     *
     * @return
     */
    public synchronized static TablePartitionRID<String ,Class<?>> getTypes() 
    {
        if ( $Types == null )
        {
            $Types = new TablePartitionRID<String ,Class<?>>();
        }
        
        return $Types;
    }
    
    
    
    /**
     * 移除整张表的字段类型
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-02-06
     * @version     v1.0
     *
     * @param i_Database   库名称
     * @param i_TableName  表名称
     * @return             返回整张表的字段类型
     */
    public static Map<String ,Class<?>> remove(String i_Database ,String i_TableName)
    {
        if ( Help.isNull(i_Database) )
        {
            return null;
        }
        if ( Help.isNull(i_TableName) )
        {
            return null;
        }
        
        return getTypes().remove(i_Database + $Level + i_TableName);
    }
    
    
    
    /**
     * 移除所有类型
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-02-06
     * @version     v1.0
     *
     */
    public static void remove()
    {
        getTypes().clear();
    }
    
    
    
    private RedisMapType()
    {
        // Nothing.
    }
    
}

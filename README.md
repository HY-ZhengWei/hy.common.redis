Redis数据库访问


特色1：所有操作不用显性的释放资源，由本类及相关类内部释放，保证访问资源的安全

特色2：创建逻辑表的概念。通过三级信息构建

          1. 一级Hash($MetaData_Tables):保存表的列表信息。   Hash.key = 表名称，Hash.value = 表的创建时间
          
          2. 二级Hash(表名称)           :保存表的行主键列表。 Hash.key = 行主键，Hash.value = 行主键的创建或修改时间
          
          3. 三级Hash(行主键)           :保存一行数据信息。   Hash.key = 字段名，Hash.value = 字段值
          
        
 概念1：行主键  RowKey。表中一行数据的唯一标示
                      注意：行主键默认为 "表名称.ID" 的形式
                      
 概念2：关键字  Key   。Redis数据库中一个Key-Value的Key值。就是Map集合的Key值。
 
 
 注：在对象构建时，已保存在XJava中，其XID为this.getXID()
 
 
 引用 https://github.com/HY-ZhengWei/hy.common.base 类库
 
 引用 https://github.com/HY-ZhengWei/hy.common.tpool 类库
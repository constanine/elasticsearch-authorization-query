#elasticsearch-authorization-query/elasticsearch权限检查过滤插件

## 原理
- 在索引文档中,使用固定字段[authorization] type=keyword/不分词,其内容为 [${authorizationUnit}=[${roleID}...];...],存储其权限值
  如:CURRENCY=33504;DEPARTMENT=33107,33101;EXCHANGERATE=-1,223;
- 分析${authorizationUnit}=[${roleID}...];...,转化成Map<${authorizationUnit},Set[${roleID}...]>,文档中限定了可以访问的roleid,如果条件中设计了其${authorizationUnit},其对应${roleID}集合中不匹配,则为false,被过滤掉
- 例子:
  索引文档中的数据为 CURRENCY=33504;DEPARTMENT=33107,1;MATERIAL=32311,32312;
  查询是 条件 CURRENCY=33504;为true;
  查询是 条件 CURRENCY=33503;为false;
  查询是 条件 CURRENCY=33503;MATERIAL=32311为false;
  查询是 条件 CURRENCY=33503;MATERIAL=32311,32312,32313为true;
  查询是 条件 CURRENCY=33503;MATERIAL=32311,32312,32313;DEPARTMENT=33107,1为true;

  
## 检索语句,条件等式
  <pre>
  {
     "query":{
        "authorization" : {
            "authorization" : "${authorizationUnit}=[${roleID}...];..."
        }
     }
  }
  </pre>
  例如
  <pre>
  {
     "query":{
        "authorization" : {
            "authorization" : "CURRENCY=33504;DEPARTMENT=33107;"
        }
     }
  }
  </pre>
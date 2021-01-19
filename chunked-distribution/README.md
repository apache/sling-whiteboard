# chunked-distribution

https://cwiki.apache.org/confluence/display/SLING/Service+Authentication

    "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~chunked-distribution":{
      "user.default":"",
      "user.mapping":[
        "org.apache.sling.distribution.chunked=[repository-reader-service]"
      ]
    },

    
    curl -u admin:admin -F action=install -F bundlefile=@"target/org.apache.sling.distribution.chunked-0.1.0-SNAPSHOT.jar" http://localhost:8080/system/console/bundles
    curl -u admin:admin -X POST -d "apply=true" -d "propertylist=user.mapping" -d "user.mapping=org.apache.sling.distribution.chunked=[repository-reader-service]" -d "factoryPid=org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended" http://localhost:8080/system/console/configMgr
    
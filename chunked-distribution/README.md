# chunked-distribution


    "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~chunked-distribution":{
      "user.default":"",
      "user.mapping":[
        "org.apache.sling.distribution.chunked=[repository-reader-service]"
      ]
    },

    
    curl -i -u admin:admin -F action=install -F bundlestart=true -F bundlefile=@"target/org.apache.sling.distribution.chunked-0.1.0-SNAPSHOT.jar" http://localhost:8080/system/console/bundles

    curl -u admin:admin -X POST -d "apply=true" -d "propertylist=user.mapping" -d "user.mapping=org.apache.sling.distribution.chunked\=[repository-reader-service]" -d "factoryPid=org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended" http://localhost:8080/system/console/configMgr
    
    
## Stop job

    curl -i -u admin:admin -F command=stop -F id=<job id> http://localhost:8080/libs/sling/distribution/tree  
    
## Distribute path with just hierarchy noes

    curl -i -u admin:admin -F path=<path> http://localhost:8080/libs/sling/distribution/tree
    
## Distribute path with all nodes

    curl -i -u admin:admin -F path=<path> -Fmode=AllNodes http://localhost:8080/libs/sling/distribution/tree
    

## Get jobs
    
    curl -i -u admin:admin http://localhost:8080/libs/sling/distribution/tree 
    
## Observe progress
    
    watch -t -d curl -u admin:admin http://localhost:8080/libs/sling/distribution/tree

## See history of failed or stopped jobs
    
    watch -t -d curl -u admin:admin http://localhost:8080/libs/sling/distribution/tree?type=HISTORY

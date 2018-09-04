function() {    
  var env = karate.env; // get system property 'karate.env'
  // karate.log('karate.env system property is', env);
  if (!env) {
    env = 'dev';
  }
  var config = {
    env: env,
    baseURL: 'http://localhost:8080'
  }
  karate.log('Expecting a Sling instance at base URL', config.baseURL);
  return config;
}
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

  karate.callSingle('classpath:logBaseURL.js', config.baseURL);

  return config;
}
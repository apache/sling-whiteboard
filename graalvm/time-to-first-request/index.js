// This can be used to measure the time to first request,
// including starting up our Docker container:
//    npm install
//    node index.js
//
// And in a different terminal:
//    time curl -s http://localhost:9000/sling/chouc/route
//
// This proxy starts the container on the first request
// (for now, you need to stop it before running this utility) 
// so the reported time is a realistic "time to first request" 
// value.
//
// The "docker events" command also provides useful information
// in terms of container startup time.
//
const http = require('http');
const httpProxy = require('http-proxy');
const Docker = require('dockerode');
const waitOn = require('wait-on');
const elapsedTime = require('elapsed-time')

const listenPort = 9000;
const targetUrl = 'http://127.0.0.1:8080';
const waitUrl = `${targetUrl}/sling/chouc/route`;
const dockerImage = 'quarkus/org.apache.sling.graalvm.experiments';
const dockerStartOptions = {
  PortBindings: {
    "8080/tcp": [{
        "HostIP":"0.0.0.0",
        "HostPort": "8080"
    }],
  },
};

const proxy = httpProxy.createProxyServer({});

const waitOpts = {
    resources: [
      waitUrl
    ],
    delay: 0,
    interval: 10,
    timeout: 30000,
    window: 1,
};

const docker = new Docker();

var server = http.createServer(function(req, res) {
  const et = elapsedTime.new().start();
  // TODO detect if container is already running, instead of blind catch
  // (maybe use node-docker-api instead)

  console.log(`Starting container ${dockerImage}(${et.getValue()})`);
  docker.run(dockerImage, null, process.stderr, dockerStartOptions);
  console.log(`Waiting on ${waitOpts.resources[0]} (${et.getValue()})`);
  waitOn(waitOpts).then(() =>{
    console.log(`Time to first request to ${waitUrl}: (${et.getValue()})`);
    console.log(`Proxying ${req.url} (${et.getValue()})`);
    proxy.web(req, res, { target: targetUrl });
    console.log(`Done proxying (${et.getValue()})`);
  });  
});

console.log(`listening on port ${listenPort}, proxying to ${targetUrl}`);
server.listen(listenPort);
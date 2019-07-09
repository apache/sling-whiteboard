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
const actions = {};

const getContainer = (async imageName => {
  const containers = await docker.listContainers();
  return containers.find(container => {
    return container.Image == imageName;
  });
});

var server = http.createServer(async (req, res) => {
  const et = elapsedTime.new().start();
  existingContainer = await getContainer(dockerImage);

  if(existingContainer) {
    console.log(`Container is already running: ${dockerImage}(${et.getValue()})`);
  } else {
    actions.startedContainer = true;
    console.log(`Starting container: ${dockerImage}(${et.getValue()})`);
    docker.run(dockerImage, null, null, dockerStartOptions);
  }

  // No need to wait for async call, just wait for our URL
  console.log(`Waiting on ${waitOpts.resources[0]} (${et.getValue()})`);
  waitOn(waitOpts).then(() =>{
    console.log(`Time to wait for ${waitUrl}: (${et.getValue()})`);
    console.log(`Proxying ${req.url} (${et.getValue()})`);
    proxy.web(req, res, { target: targetUrl });
    console.log(`Done proxying (${et.getValue()})`);
  });  
});

const cleanup = async () => {
  if(!actions.startedContainer) {
    console.log('Did not start container, nothing to cleanup');
  } else {
    const runningContainer = await getContainer(dockerImage);
    if(runningContainer) {
      console.log(`Killing container ${dockerImage}/${runningContainer.Id.substring(0,12)} ...`);
      const container = await docker.getContainer(runningContainer.Id);
      await container.kill();
      console.log('killed');
    }
  }
  process.exit();
} 

[
  'SIGINT',
  'SIGTERM',
].forEach(signal => {
  process.on(signal, cleanup);
})

console.log(`listening on port ${listenPort}, proxying to ${targetUrl}`);
server.listen(listenPort)
function(paths) {
  // Cleanup a set of paths
  for(i in paths) {
    var args = { "pathToDelete" : paths[i] }
    karate.call("classpath:util/cleanup-path.feature", args);
  }
}
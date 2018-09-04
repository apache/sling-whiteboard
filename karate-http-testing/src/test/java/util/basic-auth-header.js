function(creds) {
  // Return Authorization header with basic auth, defaults to admin/admin
  if(!creds) {
    creds = { username: 'admin', password: 'admin' }
  }
  var temp = creds.username + ':' + creds.password
  var encoded = Java.type('java.util.Base64').getEncoder().encodeToString(temp.bytes);
  return { 'Authorization' : 'Basic ' + encoded }
}

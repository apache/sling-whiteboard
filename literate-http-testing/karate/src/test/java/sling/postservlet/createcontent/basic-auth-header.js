function(creds) {
  var temp = creds.username + ':' + creds.password;
  var encoded = Java.type('java.util.Base64').getEncoder().encodeToString(temp.bytes);
  return { 'Authorization' : 'Basic ' + encoded }
}

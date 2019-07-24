function main (params) {
  return new Promise(function (resolve, reject) {
    return resolve({ body:"Ce texte-ci\n"});
  })
}

module.exports.main = main
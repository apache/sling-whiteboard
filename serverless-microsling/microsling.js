function main (params) {
  const result = {
    body: `${params.__ow_path}`,
  };

  return new Promise(function (resolve, reject) {
    return resolve(result);
  })
}

if (require.main === module) {
  const result = main({
    __ow_path: process.argv[2],
    __ow_method: 'get',
  });
  console.log(result);
}

module.exports.main = main
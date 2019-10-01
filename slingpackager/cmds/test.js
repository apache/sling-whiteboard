const packager = require('../utils/packager')
const logger = require('../utils/consoleLogger')

exports.command = 'test'
exports.desc = 'test package manager service connection'
exports.handler = (argv) => {
    logger.init(argv);
    packager.test(argv, (success, packageManager) => {
        if(success) {
            logger.log(packageManager.getName(),"detected on",argv.server);
        }
    });
}
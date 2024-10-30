pushd target
rm -rf launcher

if [ -z "${SOURCE_DIR}" ]; then 
    export SOURCE_DIR=/tmp/docs
fi
if [ -z "${TARGET_DIR}" ]; then 
    export TARGET_DIR=/tmp/offliner
fi
echo Using SOURCE_DIR=$SOURCE_DIR
echo       TARGET_DIR=$TARGET_DIR
time ../launch-offline-bare.sh
popd

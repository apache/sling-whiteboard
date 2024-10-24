SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
echo script dir: $SCRIPT_DIR

pushd target
rm -rf launcher
rm /tmp/offliner/test.md.html
time ./sling_native -f file://$SCRIPT_DIR/target/slingfeature-tmp/feature.json
popd

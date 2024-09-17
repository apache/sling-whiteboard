pushd target
rm -rf launcher
time ./aem_native -f file:///Users/david/clones/sling-whiteboard_2/sling-mini/target/slingfeature-tmp/feature-offlineapp.json
popd

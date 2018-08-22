#!/bin/bash
URLS=$(cat <<EOF
http://localhost:8080/content/srr/examples/hello.html
http://localhost:8080/content/srr/examples/hello-v1.html
http://localhost:8080/content/srr/examples/precompiled-hello.html
http://localhost:8080/content/srr/examples/precompiled-hello-v1.html
http://localhost:8080/content/srr/examples/classic-hello.html
EOF
)
HOW_MANY=$(echo "$URLS" | wc -l)
index=0
CONCURRENCY=${CONCURRENCY:-1}
REQUESTS=${REQUESTS:-1}
for url in $(echo "$URLS" | sort -R); do
    echo -e "$url"
    ab -q -c $CONCURRENCY -n $REQUESTS $url | grep 'across all concurrent requests'
    if [[ ( $REQUESTS != 1 ) && (( $index < $((HOW_MANY - 1)) )) ]]; then
        sleep 10
    fi
    index=$((index + 1))
done

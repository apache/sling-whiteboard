#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
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

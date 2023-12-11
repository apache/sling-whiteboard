FROM ghcr.io/graalvm/native-image:ol8-java17-22.3.3 as native

COPY target/artifacts artifacts
COPY --chmod=777 build.sh build.sh
#COPY target/atomos-config/atomos_init.sh atomos_init.sh
#COPY target/atomos-config/resource-config.json resource-config.json
#COPY target/atomos-config/reflect-config.json reflect-config.json
#COPY target/atomos-config/proxy-config.json proxy-config.json
#COPY target/atomos-config/jni-config.json jni-config.json
#COPY target/atomos-config/serialization-config.json serialization-config.json
#COPY target/atomos-config/atomos.substrate.jar atomos.substrate.jar
COPY target/atomos-config/app.substrate.jar app.substrate.jar

RUN ./build.sh "-H:+StaticExecutableWithDynamicLibC" "-o launcher" "-Ob"

FROM gcr.io/distroless/base-nossl

COPY --from=native --chmod=777 /app/launcher /app/launcher

CMD ["/app/launcher"]



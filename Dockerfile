FROM ghcr.io/cirruslabs/android-sdk:35 AS build

WORKDIR /workspace
COPY . .

RUN ./gradlew --no-daemon \
    :app:web:wasmJsBrowserDistribution \
    :app:webServer:installDist

ARG PDFIUM_READER_TAG=v158.0.8111.0
ARG PDFIUM_BINARY_TAG=chromium/8111
RUN set -eux; \
    curl --fail --silent --show-error --location \
        "https://raw.githubusercontent.com/NucleusFramework/ComposePdfReader/${PDFIUM_READER_TAG}/pdfium/src/webMain/resources/pdfium_worker.mjs" \
        --output /workspace/app/web/build/dist/wasmJs/productionExecutable/pdfium_worker.mjs; \
    curl --fail --silent --show-error --location \
        "https://github.com/bblanchon/pdfium-binaries/releases/download/${PDFIUM_BINARY_TAG}/pdfium-wasm.tgz" \
        --output /tmp/pdfium-wasm.tgz; \
    mkdir -p /tmp/pdfium-wasm; \
    tar -xzf /tmp/pdfium-wasm.tgz -C /tmp/pdfium-wasm; \
    cp /tmp/pdfium-wasm/lib/pdfium.wasm \
        /workspace/app/web/build/dist/wasmJs/productionExecutable/pdfium.wasm; \
    python3 /workspace/tools/generate-pdfium-runtime.py

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/app/web/build/dist/wasmJs/productionExecutable ./web
COPY --from=build /workspace/app/webServer/build/install/webServer ./server

ENV PAPERCONNEKT_WEB_DIR=/app/web
ENV PAPERCONNEKT_HOST=0.0.0.0
ENV PAPERCONNEKT_PORT=8080

EXPOSE 8080
ENTRYPOINT ["/app/server/bin/webServer"]

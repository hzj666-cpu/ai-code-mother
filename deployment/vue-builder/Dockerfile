FROM node:22-alpine

WORKDIR /opt/template
COPY src/main/resources/vue-templates/vue-base-v1/package.json ./
COPY src/main/resources/vue-templates/vue-base-v1/package-lock.json ./
RUN npm ci --ignore-scripts --no-audit --no-fund && chown -R 10001:10001 /opt/template
COPY deployment/vue-builder/build.sh /usr/local/bin/build-vue
RUN chmod 755 /usr/local/bin/build-vue
USER 10001:10001
ENTRYPOINT ["/usr/local/bin/build-vue"]

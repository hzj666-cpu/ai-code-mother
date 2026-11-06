#!/bin/sh
set -eu
cd /work
cp /input/index.html /input/vite.config.js ./
cp -R /input/src ./src
if [ -d /input/public ]; then cp -R /input/public ./public; fi
ln -s /opt/template/node_modules ./node_modules
node /opt/template/node_modules/vite/bin/vite.js build --config vite.config.js
cp -R /work/dist/. /output/

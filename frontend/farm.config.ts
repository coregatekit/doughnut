import { defineConfig } from "@farmfe/core";
import { fileURLToPath, URL } from "url";
import vue from "@vitejs/plugin-vue";
import vueJsx from "@vitejs/plugin-vue-jsx";
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
import { VueRouterAutoImports } from 'unplugin-vue-router';
import VueRouter from 'unplugin-vue-router/vite';
import viteCompression from 'vite-plugin-compression';
import tsconfigPaths from 'vite-tsconfig-paths'

import farmJsPluginSass from '@farmfe/js-plugin-sass';

export default defineConfig({
  plugins: [
    farmJsPluginSass(),
  ],
  vitePlugins: [
    tsconfigPaths(),
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => /^x-/.test(tag),
        },
      },
    }),
    VueRouter(),
    vueJsx(),
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
      imports: [
	'vue',
        VueRouterAutoImports
      ],
      dts: true, // generate TypeScript declaration
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })]
    }),
    viteCompression()
  ],
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:9081",
	changeOrigin: true,
      },
      "/attachments": {
        target: "http://localhost:9081",
	changeOrigin: true,
      },
      "/logout": {
        target: "http://localhost:9081",
	changeOrigin: true,
      },
      "/testability": {
        target: "http://localhost:9081",
	changeOrigin: true,
      },
    },
  },
  compilation: {
    output: {
      path: "../backend/src/main/resources/static",
    },
    sourcemap: true,
    input: {
      main: fileURLToPath(new URL("index.html", import.meta.url))
    },
  },
});

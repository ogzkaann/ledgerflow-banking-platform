import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/gateway-readiness": {
        target: "http://localhost:8080",
        changeOrigin: true,
        rewrite: () => "/actuator/health/readiness",
      },
    },
  },
  preview: {
    port: 4173,
    strictPort: true,
  },
  test: {
    globals: true,
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    css: true,
    coverage: {
      provider: "v8",
      reporter: ["text", "json-summary", "html"],
      reportsDirectory: "./coverage",
      exclude: ["src/api/generated/**", "src/main.tsx", "src/test/**"],
    },
  },
});

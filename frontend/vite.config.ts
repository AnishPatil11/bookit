/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the browser talks to Vite (5173) and Vite forwards /api to Spring Boot (8080),
// so there are no CORS problems locally. In production set VITE_API_URL (see .env.example).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
  },
  test: { environment: 'node' },
});

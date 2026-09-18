import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// Proxy /api → backend 8080, tránh cấu hình URL cứng trong code.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');
  return {
    define: { global: 'globalThis' },
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: env.VITE_BACKEND_URL || 'http://localhost:8080',
          changeOrigin: true,
          ws: true, // proxy WebSocket cho SockJS /api/ws → /ws
          rewrite: (p) => p.replace(/^\/api/, ''),
        },
      },
    },
  };
});

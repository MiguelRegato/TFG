import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 6000,
  retries: 0,
  use: {
    baseURL: 'http://localhost:4200/',
    headless: true,
  },
});

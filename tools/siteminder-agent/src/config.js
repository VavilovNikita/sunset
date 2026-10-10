import { readFileSync, existsSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');

/** Minimal .env reader (KEY=VALUE, # comments); real environment variables win. */
function loadDotEnv() {
  const file = resolve(root, '.env');
  if (!existsSync(file)) return;
  for (const line of readFileSync(file, 'utf8').split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/);
    if (!m || line.trim().startsWith('#')) continue;
    const value = m[2].replace(/^(['"])(.*)\1$/, '$2');
    if (process.env[m[1]] === undefined) process.env[m[1]] = value;
  }
}

export class ConfigError extends Error {}

export function loadConfig({ needSiteMinder = true } = {}) {
  loadDotEnv();
  const env = process.env;
  const required = ['SUNSET_IMPORT_URL', 'SUNSET_INTEGRATION_KEY'];
  if (needSiteMinder) required.push('SM_LOGIN_URL', 'SM_USERNAME', 'SM_PASSWORD', 'SM_RESERVATIONS_URL');
  const missing = required.filter((k) => !env[k]);
  if (missing.length) throw new ConfigError(`Missing required settings: ${missing.join(', ')}`);
  return {
    loginUrl: env.SM_LOGIN_URL,
    username: env.SM_USERNAME,
    password: env.SM_PASSWORD,
    reservationsUrl: env.SM_RESERVATIONS_URL,
    tableSelector: env.SM_TABLE_SELECTOR || 'table',
    nextSelector: env.SM_NEXT_SELECTOR || '',
    maxPages: Number(env.SM_MAX_PAGES || 50),
    tzOffset: env.SM_TZ_OFFSET || '+07:00',
    importUrl: env.SUNSET_IMPORT_URL,
    integrationKey: env.SUNSET_INTEGRATION_KEY,
    stateDir: resolve(root, env.STATE_DIR || './state'),
    logFile: resolve(root, env.LOG_FILE || './logs/agent.log'),
    dumpDir: resolve(root, './dump'),
  };
}

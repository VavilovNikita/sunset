import { readFileSync, existsSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');

/** Minimal .env reader (KEY=VALUE, # comments); real environment variables win. */
export function loadDotEnv() {
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

export function loadConfig({ needSiteMinder = true, lookbackDays } = {}) {
  loadDotEnv();
  const env = process.env;
  const required = ['SUNSET_IMPORT_URL', 'SUNSET_INTEGRATION_KEY'];
  if (needSiteMinder) required.push('SM_LOGIN_URL', 'SM_USERNAME', 'SM_PASSWORD');
  if (env.SM_RESERVATIONS_URL) console.warn('NOTE: SM_RESERVATIONS_URL is ignored - the URL is built per run with fresh dates; remove it from .env');
  const missing = required.filter((k) => !env[k]);
  if (missing.length) throw new ConfigError(`Missing required settings: ${missing.join(', ')}`);
  return {
    loginUrl: env.SM_LOGIN_URL,
    username: env.SM_USERNAME,
    password: env.SM_PASSWORD,
    // The search URL is built per run (src/url.js); SM_RESERVATIONS_URL (old static, dated URL) is no longer read.
    reservationsBase: env.SM_RESERVATIONS_BASE || 'https://platform.siteminder.com/reservations',
    propertyId: env.SM_PROPERTY_ID || '455c0edf-8114-11e5-8827-02b1347ffa5b',
    hotelTimeZone: env.SM_HOTEL_TZ || 'Asia/Bangkok',
    lookbackDays: lookbackDays ?? Number(env.SM_LOOKBACK_DAYS || 3),
    pageSize: Number(env.SM_PAGE_SIZE || 10),
    extraQuery: env.SM_EXTRA_QUERY || '',
    tableSelector: env.SM_TABLE_SELECTOR || 'table',
    maxPages: Number(env.SM_MAX_PAGES || 50),
    // Only used when every numeric date in the table is ambiguous (all days <= 12); otherwise detected.
    dateOrder: env.SM_DATE_ORDER === 'dmy' ? 'dmy' : 'mdy',
    tzOffset: env.SM_TZ_OFFSET || '+07:00',
    importUrl: env.SUNSET_IMPORT_URL,
    integrationKey: env.SUNSET_INTEGRATION_KEY,
    stateDir: resolve(root, env.STATE_DIR || './state'),
    logFile: resolve(root, env.LOG_FILE || './logs/agent.log'),
    dumpDir: resolve(root, './dump'),
  };
}

import { mkdirSync, existsSync, writeFileSync, chmodSync } from 'node:fs';
import { join } from 'node:path';
import { chromium } from 'playwright';

export class LoginError extends Error {}
export class ScrapeError extends Error {}

const sessionFile = (cfg) => join(cfg.stateDir, 'storage-state.json');

export async function openBrowser(cfg, { headed }) {
  mkdirSync(cfg.stateDir, { recursive: true });
  const browser = await chromium.launch({ headless: !headed });
  const file = sessionFile(cfg);
  const context = await browser.newContext({
    storageState: existsSync(file) ? file : undefined,
    viewport: { width: 1440, height: 900 },
    locale: 'en-US',
  });
  context.setDefaultTimeout(30_000);
  return { browser, context, page: await context.newPage() };
}

export async function saveSession(cfg, context) {
  const file = sessionFile(cfg);
  await context.storageState({ path: file });
  try { chmodSync(file, 0o600); } catch { /* not supported on Windows */ }
}

const isLoginPage = async (page) =>
  /login|signin|sign-in|auth/i.test(new URL(page.url()).pathname) ||
  (await page.locator('input[type="password"]:visible').count()) > 0;

async function challengeText(page) {
  const body = (await page.locator('body').innerText().catch(() => '')).toLowerCase();
  const hit = ['captcha', 'verification code', 'two-factor', '2-step', 'one-time', 'one time', 'security code', 'authenticator', "i'm not a robot"].find((w) => body.includes(w));
  if (hit) return hit;
  if ((await page.locator('iframe[src*="recaptcha"], iframe[src*="hcaptcha"]').count()) > 0) return 'captcha';
  return null;
}

/**
 * Goes to the Reservations page, reusing the saved session when it is still valid and
 * logging in otherwise. In headed mode (--login) a captcha/2FA is left for the person at the
 * keyboard to finish; headless, it fails with a clear message instead of hanging.
 */
export async function ensureOnReservations(cfg, page, context, { headed, log }) {
  await page.goto(cfg.reservationsUrl, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle').catch(() => {});
  if (!(await isLoginPage(page))) {
    log.info('Saved session is still valid');
    return;
  }
  log.info('Session missing or expired - logging in');
  await page.goto(cfg.loginUrl, { waitUntil: 'domcontentloaded' });

  const user = page.locator('input[type="email"], input[name*="user" i], input[name*="email" i], input[id*="user" i], input[id*="email" i], input[type="text"]').first();
  const pass = page.locator('input[type="password"]').first();
  try {
    await user.waitFor({ state: 'visible', timeout: 15_000 });
    await user.fill(cfg.username);
    if (!(await pass.isVisible())) {
      // Two-step form: username first, password on the next screen.
      await page.keyboard.press('Enter');
      await pass.waitFor({ state: 'visible', timeout: 15_000 });
    }
    await pass.fill(cfg.password);
    await pass.press('Enter');
  } catch (e) {
    throw new LoginError(`Could not find/fill the login form at ${cfg.loginUrl} - SiteMinder's login page layout may have changed (${e.message.split('\n')[0]})`);
  }

  const deadline = Date.now() + (headed ? 5 * 60_000 : 30_000);
  while (Date.now() < deadline) {
    await page.waitForTimeout(1500);
    if (!(await isLoginPage(page)) && !(await challengeText(page))) break;
    const challenge = await challengeText(page);
    if (challenge && !headed) {
      throw new LoginError(`SiteMinder asked for "${challenge}". Run "npm run login" once on a machine with a display (headed) to pass it and save the session.`);
    }
    if (challenge && headed) console.log(`Waiting for you to complete "${challenge}" in the browser window...`);
  }
  if (await isLoginPage(page)) {
    throw new LoginError('Login did not complete - wrong username/password, an account lock, or an unrecognised challenge');
  }
  await page.goto(cfg.reservationsUrl, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle').catch(() => {});
  if (await isLoginPage(page)) throw new LoginError('Logged in, but the Reservations URL still redirects to a login page');
  await saveSession(cfg, context);
  log.info('Login OK, session saved');
}

/** Reads the largest table matching the selector, following the "next page" control if configured. */
export async function scrapeTable(cfg, page, log) {
  let headers = null;
  const rows = [];
  for (let p = 1; p <= cfg.maxPages; p++) {
    try {
      await page.locator(cfg.tableSelector).first().waitFor({ state: 'visible', timeout: 20_000 });
    } catch {
      throw new ScrapeError(`No table matching "${cfg.tableSelector}" appeared on ${page.url()} - the page layout changed or the session landed on another screen`);
    }
    const t = await page.$$eval(cfg.tableSelector, (tables) => {
      const text = (el) => (el.innerText ?? el.textContent ?? '').trim();
      const best = tables
        .map((tb) => ({ tb, n: tb.querySelectorAll('tr').length }))
        .sort((a, b) => b.n - a.n)[0];
      const trs = [...best.tb.querySelectorAll('tr')];
      const headRow = best.tb.querySelector('thead tr') ?? trs.find((tr) => tr.querySelector('th')) ?? trs[0];
      const body = trs.filter((tr) => tr !== headRow && !tr.querySelector('th'));
      return { headers: [...headRow.children].map(text), rows: body.map((tr) => [...tr.children].map(text)) };
    });
    headers ??= t.headers;
    rows.push(...t.rows);
    log.info(`Page ${p}: ${t.rows.length} rows`);
    if (!cfg.nextSelector) break;
    const next = page.locator(cfg.nextSelector).first();
    if ((await next.count()) === 0 || !(await next.isEnabled()) || !(await next.isVisible())) break;
    const before = JSON.stringify(t.rows[0] ?? []);
    await next.click();
    await page.waitForLoadState('networkidle').catch(() => {});
    await page.waitForTimeout(1000);
    const after = await page.$$eval(cfg.tableSelector, (tbs) => {
      const tb = tbs.sort((a, b) => b.querySelectorAll('tr').length - a.querySelectorAll('tr').length)[0];
      const tr = [...tb.querySelectorAll('tr')].find((r) => !r.querySelector('th'));
      return tr ? [...tr.children].map((c) => (c.innerText ?? '').trim()) : [];
    });
    // Same first row after the click: the control did nothing, stop rather than loop to maxPages.
    if (JSON.stringify(after) === before) break;
  }
  return { headers, rows };
}

/** --dump: save what the page looks like so selectors/headers can be tuned without guessing. */
export async function dumpPage(cfg, page, log) {
  mkdirSync(cfg.dumpDir, { recursive: true });
  const html = join(cfg.dumpDir, 'reservations.html');
  writeFileSync(html, await page.content(), { mode: 0o600 });
  await page.screenshot({ path: join(cfg.dumpDir, 'reservations.png'), fullPage: true });
  log.info(`Dumped page to ${cfg.dumpDir} (contains guest data - delete when done)`);
}

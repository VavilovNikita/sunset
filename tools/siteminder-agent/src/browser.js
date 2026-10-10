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
  log.info('Session missing or expired - logging in (two-step: username, then password)');
  await doLogin(cfg, page, { headed, log });
  await page.goto(cfg.reservationsUrl, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle').catch(() => {});
  if (await isLoginPage(page)) throw new LoginError('Logged in, but the Reservations URL still redirects to a login page');
  await saveSession(cfg, context);
  log.info('Step 6/6: session saved');
}

const USER_FIELD = 'input[type="email"], input[name*="user" i], input[name*="email" i], input[id*="user" i], input[id*="email" i], input[type="text"]';
const PASS_FIELD = 'input[type="password"]';
const SUBMIT_BUTTON = 'button[type="submit"], input[type="submit"]';

/** What is on screen right now, for the log when a step stalls. No field values, no credentials. */
async function describeScreen(page) {
  const d = await page.evaluate(() => {
    const vis = (el) => !!(el.offsetWidth || el.offsetHeight);
    const q = (s) => [...document.querySelectorAll(s)].filter(vis);
    return {
      inputs: q('input').map((i) => `${i.type}${i.name ? `[${i.name}]` : ''}`),
      checkboxes: q('input[type="checkbox"]').map((i) => (i.labels?.[0]?.innerText || i.name || 'unlabelled').trim()),
      buttons: q('button, input[type="submit"]').map((b) => (b.innerText || b.value || '').trim()).filter(Boolean),
      errors: q('[role="alert"], .error, [class*="error" i]').map((e) => e.innerText.trim()).filter(Boolean).slice(0, 3),
    };
  }).catch(() => ({ inputs: [], checkboxes: [], buttons: [], errors: [] }));
  const frames = await page.locator('iframe').evaluateAll((fs) => fs.map((f) => f.src.split('?')[0])).catch(() => []);
  return `url=${page.url().split('?')[0]} inputs=[${d.inputs}] checkboxes=[${d.checkboxes}] buttons=[${d.buttons}] iframes=[${frames}] errors=[${d.errors}]`;
}

async function submit(page, field) {
  const button = page.locator(SUBMIT_BUTTON).first();
  if (await button.isVisible().catch(() => false)) await button.click();
  else await field.press('Enter');
}

/**
 * Two screens: authx.siteminder.com/login asks for the username only; after submitting it moves
 * to /login/password for the password. The second step is awaited by the password field
 * appearing, not by matching the URL. Every step is logged so a stall shows where it stopped.
 * Headed: a captcha/2FA/extra prompt at any step is left to the person for up to 5 minutes.
 * Headless: the same situation fails immediately with a description of what was on screen.
 */
async function doLogin(cfg, page, { headed, log }) {
  const patience = headed ? 5 * 60_000 : 30_000;
  const fail = async (what) => {
    log.error(`Login stalled at: ${what}. Screen: ${await describeScreen(page)}`);
    throw new LoginError(`${what}${headed ? '' : ' (run "npm run login" headed to finish it by hand)'}`);
  };

  log.info(`Step 1/6: opening ${cfg.loginUrl}`);
  await page.goto(cfg.loginUrl, { waitUntil: 'domcontentloaded' });
  const user = page.locator(USER_FIELD).first();
  const pass = page.locator(PASS_FIELD).first();

  try { await user.waitFor({ state: 'visible', timeout: 20_000 }); } catch { await fail('username field not found on the login page'); }
  log.info('Step 2/6: found username field, filling and submitting');
  await user.fill(cfg.username);
  // "Remember me" (id login-remember on the username page) lengthens the session the agent will reuse.
  const remember = page.locator('input[type="checkbox"][id*="remember" i], input[type="checkbox"][name*="remember" i]').first();
  if (await remember.isVisible().catch(() => false) && !(await remember.isChecked())) {
    await remember.check();
    log.info('Ticked "remember me"');
  }
  await submit(page, user);

  log.info('Step 3/6: waiting for the password field');
  if (!(await waitFor(page, pass, patience, log, 'password field'))) await fail('password field never appeared after the username step');
  log.info(`Step 4/6: found password field (${await describeScreen(page)}), filling and submitting`);
  await pass.fill(cfg.password);
  await submit(page, pass);

  log.info('Step 5/6: waiting for login to finish');
  const deadline = Date.now() + patience;
  let lastNote = '';
  while (Date.now() < deadline) {
    await page.waitForTimeout(1500);
    const challenge = await challengeText(page);
    if (!challenge && !(await isLoginPage(page))) return;
    const note = challenge ? `challenge "${challenge}"` : 'still on a login screen';
    if (note !== lastNote) { log.warn(`${note}. Screen: ${await describeScreen(page)}`); lastNote = note; }
    if (challenge && !headed) await fail(`SiteMinder asked for "${challenge}"`);
  }
  await fail(headed ? 'timed out (5 min) waiting for the manual step to finish' : 'login did not complete - wrong credentials, account lock or an unrecognised prompt');
}

/** Waits for a locator, logging (once) when something other than the field is blocking the way. */
async function waitFor(page, locator, timeoutMs, log, label) {
  const deadline = Date.now() + timeoutMs;
  let noted = false;
  while (Date.now() < deadline) {
    if (await locator.isVisible().catch(() => false)) return true;
    if (!noted && Date.now() > deadline - timeoutMs + 5000) {
      noted = true;
      log.warn(`${label} not there after 5s. Screen: ${await describeScreen(page)}`);
      if (timeoutMs > 60_000) log.warn('Headed mode: complete whatever the page asks for in the browser window.');
    }
    await page.waitForTimeout(500);
  }
  return false;
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

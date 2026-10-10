import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

/**
 * --dump (also run automatically when the table can't be found): save what the page looks like so the
 * selectors can be written from evidence, not guessed. Writes to dump/:
 *   structure.txt  - layout summary with ALL text masked (letters->a, digits->9): safe to share
 *   reservations.html (shadow DOM inlined), reservations.png, api-*.json - contain guest data: do NOT share unredacted
 */
export async function dumpPage(cfg, page, log) {
  mkdirSync(cfg.dumpDir, { recursive: true });
  const api = [];
  page.on('response', async (res) => {
    try {
      const type = res.headers()['content-type'] ?? '';
      if (!type.includes('json') || !['fetch', 'xhr'].includes(res.request().resourceType())) return;
      api.push({ url: res.url(), status: res.status(), body: await res.text() });
    } catch { /* body unavailable */ }
  });
  // Reload with the listener attached so the list's own data requests are captured too.
  await page.reload({ waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(3000);

  const { html, structure } = await page.evaluate(() => {
    const mask = (t) => t.replace(/[A-Za-z฀-๿]/g, 'a').replace(/\d/g, '9').replace(/\s+/g, ' ').trim().slice(0, 40);
    const lines = [];
    const roots = [document];
    const hosts = [];
    const walk = (root) => {
      for (const el of root.querySelectorAll('*')) {
        if (el.shadowRoot) { hosts.push(el.tagName.toLowerCase()); roots.push(el.shadowRoot); walk(el.shadowRoot); }
      }
    };
    walk(document);
    const all = roots.flatMap((r) => [...r.querySelectorAll('*')]);
    const cls = (el) => (typeof el.className === 'string' ? el.className : '');
    const sig = (el) => `${el.tagName.toLowerCase()}${el.getAttribute('role') ? `[role=${el.getAttribute('role')}]` : ''}${[...el.classList].filter((c) => !/^\d/.test(c)).slice(0, 3).map((c) => '.' + c).join('')}`;
    const count = (sel) => roots.reduce((n, r) => n + r.querySelectorAll(sel).length, 0);

    lines.push(`url path: ${location.pathname}`);
    lines.push(`<table>: ${count('table')}  [role=table]: ${count('[role=table]')}  [role=grid]: ${count('[role=grid]')}  [role=row]: ${count('[role=row]')}  [role=columnheader]: ${count('[role=columnheader]')}  <th>: ${count('th')}  <tr>: ${count('tr')}`);
    lines.push(`shadow hosts: ${hosts.length ? [...new Set(hosts)].join(', ') : 'none'}`);
    lines.push(`custom elements: ${[...new Set(all.map((e) => e.tagName.toLowerCase()).filter((t) => t.includes('-')))].slice(0, 40).join(', ') || 'none'}`);

    // Candidate row lists: a parent with >=3 children of one signature whose first child holds a date.
    lines.push('\n== repeated-row candidates ==');
    const dateRe = /\d{1,2}[\s/-]\w{3,}[\s/-]\d{2,4}|\d{4}-\d{2}-\d{2}|\d{1,2}\/\d{1,2}\/\d{2,4}/;
    const seen = new Set();
    for (const parent of all) {
      const kids = [...parent.children];
      if (kids.length < 3) continue;
      const groups = {};
      kids.forEach((k) => { (groups[sig(k)] ??= []).push(k); });
      for (const [s, g] of Object.entries(groups)) {
        if (g.length < 3 || g[0].children.length < 2 || seen.has(parent) || !dateRe.test(g[0].innerText ?? '')) continue;
        seen.add(parent);
        const chain = [];
        for (let e = parent; e && chain.length < 6; e = e.parentElement ?? e.getRootNode().host) chain.push(sig(e));
        lines.push(`parent chain (inner->outer): ${chain.join(' < ')}`);
        lines.push(`  ${g.length} x ${s}`);
        const cells = [...g[0].children];
        lines.push(`  first row has ${cells.length} children:`);
        cells.forEach((c, i) => lines.push(`    ${i}: ${sig(c)}  text="${mask(c.innerText ?? '')}"`));
      }
    }

    lines.push('\n== header-like text ==');
    const heads = all
      .filter((e) => e.tagName === 'TH' || e.getAttribute('role') === 'columnheader' || /header|heading|col-title/i.test(cls(e)))
      .map((e) => (e.innerText ?? '').trim())
      .filter((t) => t && t.length < 40);
    lines.push([...new Set(heads)].slice(0, 40).join(' | ') || '(none found)');

    lines.push('\n== pagination candidates ==');
    const pag = all.filter((e) => /pagin|pager/i.test(cls(e)) || /pagin/i.test(e.getAttribute('aria-label') ?? ''));
    pag.slice(0, 6).forEach((e) => lines.push(`${sig(e)} -> controls: ${[...e.querySelectorAll('button,a,select,input')].slice(0, 14)
      .map((c) => `${sig(c)}${c.getAttribute('aria-label') ? `(aria="${c.getAttribute('aria-label')}")` : ''}${c.disabled ? '(disabled)' : ''} "${(c.innerText ?? '').trim().slice(0, 12)}"`).join('; ')}`));
    if (!pag.length) lines.push('(none found by class/aria; look at the screenshot)');
    const next = all.filter((e) => /^(BUTTON|A)$/.test(e.tagName) && /next|›|»/i.test((e.getAttribute('aria-label') ?? '') + (e.innerText ?? '').trim().slice(0, 6)));
    lines.push(`next-like buttons: ${next.slice(0, 6).map((e) => `${sig(e)} aria="${e.getAttribute('aria-label') ?? ''}"`).join('; ') || 'none'}`);

    // Full HTML with shadow roots inlined as <template shadowrootmode>.
    const ser = (node) => {
      if (node.nodeType === 3) return node.textContent;
      if (node.nodeType !== 1) return '';
      const tag = node.tagName.toLowerCase();
      if (tag === 'script' || tag === 'style' || tag === 'svg') return '';
      const attrs = [...node.attributes].map((a) => ` ${a.name}="${a.value.replace(/"/g, '&quot;')}"`).join('');
      const shadow = node.shadowRoot ? `<template shadowrootmode="open">${[...node.shadowRoot.childNodes].map(ser).join('')}</template>` : '';
      return `<${tag}${attrs}>${shadow}${[...node.childNodes].map(ser).join('')}</${tag}>`;
    };
    return { html: ser(document.documentElement), structure: lines.join('\n') };
  });

  writeFileSync(join(cfg.dumpDir, 'reservations.html'), html, { mode: 0o600 });
  await page.screenshot({ path: join(cfg.dumpDir, 'reservations.png'), fullPage: true });
  api.forEach((a, i) => writeFileSync(join(cfg.dumpDir, `api-${i}.json`), JSON.stringify({ url: a.url, status: a.status, body: a.body }), { mode: 0o600 }));
  // Query strings are dropped from the shareable summary; they can carry ids/dates.
  const apiList = api.map((a, i) => `api-${i}.json ${a.status} ${a.url.replace(/\?.*/, '')} (${a.body.length} bytes)`);
  writeFileSync(join(cfg.dumpDir, 'structure.txt'), `${structure}\n\n== JSON requests the page made ==\n${apiList.join('\n') || '(none)'}\n`, { mode: 0o600 });
  log.info(`Dumped to ${cfg.dumpDir}: structure.txt (masked, safe to share); reservations.html/png and api-*.json contain guest data`);
  log.info(`Summary:\n${structure.split('\n').slice(0, 14).join('\n')}`);
}

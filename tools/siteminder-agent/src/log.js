import { appendFileSync, mkdirSync } from 'node:fs';
import { dirname } from 'node:path';

/** Logs to stdout (systemd journal) and to a file for manual checking. Never logs credentials or guest contact data. */
export function createLogger(file) {
  if (file) mkdirSync(dirname(file), { recursive: true });
  const write = (level, msg) => {
    const line = `${new Date().toISOString()} ${level} ${msg}`;
    (level === 'ERROR' ? console.error : console.log)(line);
    if (file) {
      try { appendFileSync(file, line + '\n'); } catch { /* the journal still has it */ }
    }
  };
  return {
    info: (m) => write('INFO', m),
    warn: (m) => write('WARN', m),
    error: (m) => write('ERROR', m),
  };
}

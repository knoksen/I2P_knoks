const stateElement = document.getElementById('state');
const detailsElement = document.getElementById('details');
const logsElement = document.getElementById('logs');
const token = window.prompt('Guardian API token (stored in the Guardian data directory):') ?? '';
const headers = {
  Authorization: `Bearer ${token}`,
  'Content-Type': 'application/json'
};

async function api(path, options = {}) {
  const response = await fetch(`/v1/${path}`, { ...options, headers });
  if (!response.ok) throw new Error(await response.text());
  return response.json();
}

function renderStatus(status) {
  stateElement.textContent = String(status.state).toUpperCase();
  stateElement.className = status.state === 'connected' ? 'ok' : '';
  const pid = status.pid ?? '—';
  const endpoint = value => value ? '✓' : '—';
  detailsElement.textContent = [
    `PID: ${pid}`,
    `Uptime: ${status.uptimeSeconds}s`,
    `Restarts: ${status.restarts}`,
    `Console ${endpoint(status.endpoints.console)}`,
    `HTTP ${endpoint(status.endpoints.httpProxy)}`,
    `SOCKS ${endpoint(status.endpoints.socksProxy)}`,
    `SAM ${endpoint(status.endpoints.sam)}`
  ].join(' · ');
}

function renderLogs(payload) {
  const entries = Array.isArray(payload.logs) ? payload.logs : [];
  logsElement.textContent = entries.map(entry => {
    const at = String(entry.at ?? '');
    const level = String(entry.level ?? '').toUpperCase();
    const message = String(entry.message ?? '');
    return `${at} ${level} ${message}`;
  }).join('\n');
}

async function refresh() {
  try {
    const status = await api('status');
    renderStatus(status);
    renderLogs(await api('logs'));
  } catch (error) {
    stateElement.textContent = 'AUTHENTICATION OR SERVICE ERROR';
    stateElement.className = '';
    detailsElement.textContent = error instanceof Error ? error.message : String(error);
  }
}

async function runControlAction(action) {
  try {
    await api(`router/${action}`, { method: 'POST' });
    await refresh();
  } catch (error) {
    detailsElement.textContent = error instanceof Error ? error.message : String(error);
  }
}

async function enableLockdown() {
  try {
    await api('lockdown', {
      method: 'POST',
      body: JSON.stringify({ enabled: true })
    });
    await refresh();
  } catch (error) {
    detailsElement.textContent = error instanceof Error ? error.message : String(error);
  }
}

document.getElementById('start').addEventListener('click', () => { void runControlAction('start'); });
document.getElementById('restart').addEventListener('click', () => { void runControlAction('restart'); });
document.getElementById('stop').addEventListener('click', () => { void runControlAction('stop'); });
document.getElementById('lockdown').addEventListener('click', () => { void enableLockdown(); });

void refresh();
setInterval(() => { void refresh(); }, 5000);

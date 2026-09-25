const REPO = 'Tivek87/mc-class-mod';
const PRIORITIES = ['low', 'medium', 'high'];
const PLAYER = /^[^\u0000-\u001f\u007f`|\\<>]{1,16}$/;
const VERSION = /^[0-9A-Za-z.+-]{1,32}$/;
const MAX_BODY = 8192;

export default {
  async fetch(request, env) {
    if (new URL(request.url).pathname !== '/report') return answer(404, 'not found');
    if (request.method !== 'POST') return answer(405, 'POST only');
    const ip = request.headers.get('CF-Connecting-IP') ?? 'local';
    if (!(await env.LIMITER.limit({ key: ip })).success) return answer(429, 'too many reports');

    const text = await request.text();
    if (text.length > MAX_BODY) return answer(400, 'too long');
    let report;
    try {
      report = JSON.parse(text);
    } catch {
      return answer(400, 'not json');
    }
    const problem = check(report);
    if (problem) return answer(400, problem);

    const labels = ['bug-report', `priority: ${report.priority}`];
    const response = await github(env, `/repos/${REPO}/issues`, { title: line(report.title), body: issueBody(report), labels });
    if (response.status !== 201) {
      console.log(`GitHub answered ${response.status}: ${(await response.text()).slice(0, 300)}`);
      return answer(502, 'github refused');
    }
    const issue = await response.json();
    // GitHub silently drops labels on create when the token may not set them there.
    if (!issue.labels?.some((label) => label.name === 'bug-report')) {
      const labelled = await github(env, `/repos/${REPO}/issues/${issue.number}/labels`, { labels });
      if (!labelled.ok) console.log(`Labels on #${issue.number} failed: ${labelled.status}`);
    }
    return Response.json({ issue: issue.number }, { status: 201 });
  },
};

function github(env, path, body) {
  return fetch(`${env.GITHUB_API ?? 'https://api.github.com'}${path}`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${env.GITHUB_TOKEN}`,
      Accept: 'application/vnd.github+json',
      'X-GitHub-Api-Version': '2022-11-28',
      'User-Agent': 'mc-class-mod-bug-relay',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });
}

function check(report) {
  if (typeof report !== 'object' || report === null) return 'not an object';
  if (typeof report.title !== 'string' || !line(report.title) || report.title.length > 80) return 'title';
  if (typeof report.description !== 'string' || !report.description.trim() || report.description.length > 2000) {
    return 'description';
  }
  if (!PRIORITIES.includes(report.priority)) return 'priority';
  if (typeof report.username !== 'string' || !PLAYER.test(report.username)) return 'username';
  if (!VERSION.test(report.modVersion ?? '') || !VERSION.test(report.minecraftVersion ?? '')) return 'version';
  return null;
}

function line(text) {
  return quiet(text.replace(/[\u0000-\u001f\u007f]+/g, ' ').trim());
}

// A report must never ping GitHub users.
function quiet(text) {
  return text.replace(/@(?=[A-Za-z0-9-])/g, '@​');
}

function issueBody(report) {
  const priority = report.priority[0].toUpperCase() + report.priority.slice(1);
  return [
    '| Reporter (Minecraft) | Priority | Mod | Minecraft |',
    '|---|---|---|---|',
    `| \`${report.username}\` | ${priority} | ${report.modVersion} | ${report.minecraftVersion} |`,
    '',
    '### Description',
    '',
    quiet(report.description.trim()),
    '',
    '<sub>Sent from the in-game bug report screen.</sub>',
  ].join('\n');
}

function answer(status, error) {
  return Response.json({ error }, { status });
}

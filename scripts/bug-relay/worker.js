const REPO = 'Tivek87/mc-class-mod';
const PRIORITIES = ['low', 'medium', 'high'];
const PLAYER = /^[^\u0000-\u001f\u007f`|\\<>]{1,16}$/;
const VERSION = /^[0-9A-Za-z.+-]{1,32}$/;
const MAX_BODY = 8192;
// A report laid out in blocks (mod 0.9.10 and on) may be longer; GitHub takes an issue body up to 65536.
const MAX_BLOCKS_BODY = 64000;
const MAX_ISSUE = 60000;
const CATEGORY = /^[a-z_]{1,24}$/;

export default {
  async fetch(request, env) {
    if (new URL(request.url).pathname !== '/report') return answer(404, 'not found');
    if (request.method !== 'POST') return answer(405, 'POST only');
    const ip = request.headers.get('CF-Connecting-IP') ?? 'local';
    if (!(await env.LIMITER.limit({ key: ip })).success) return answer(429, 'too many reports');

    const text = await request.text();
    if (text.length > MAX_BLOCKS_BODY) return answer(400, 'too long');
    let report;
    try {
      report = JSON.parse(text);
    } catch {
      return answer(400, 'not json');
    }
    const blocks = report !== null && typeof report === 'object' && report.blocks !== undefined;
    if (!blocks && text.length > MAX_BODY) return answer(400, 'too long');
    const problem = check(report) ?? (blocks ? checkBlocks(report) : null);
    if (problem) return answer(400, problem);

    // Mod versions before ideas send no kind.
    const idea = report.kind === 'idea';
    const labels = [idea ? 'idea' : 'bug-report', `priority: ${report.priority}`];
    const body = blocks ? blocksBody(report, idea) : issueBody(report, idea);
    if (body.length > MAX_ISSUE) return answer(400, 'too long');
    const response = await github(env, `/repos/${REPO}/issues`, {
      title: line(report.title),
      body,
      labels,
    });
    if (response.status !== 201) {
      console.log(`GitHub answered ${response.status}: ${(await response.text()).slice(0, 300)}`);
      return answer(502, 'github refused');
    }
    const issue = await response.json();
    // GitHub silently drops labels on create when the token may not set them there.
    if (!issue.labels?.some((label) => label.name === labels[0])) {
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
  if (report.kind !== undefined && report.kind !== 'bug' && report.kind !== 'idea') return 'kind';
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

function issueBody(report, idea) {
  const priority = report.priority[0].toUpperCase() + report.priority.slice(1);
  return [
    '| Reporter (Minecraft) | Priority | Mod | Minecraft |',
    '|---|---|---|---|',
    `| \`${report.username}\` | ${priority} | ${report.modVersion} | ${report.minecraftVersion} |`,
    '',
    idea ? '### Idea' : '### Description',
    '',
    quiet(report.description.trim()),
    '',
    `<sub>Sent from the in-game ${idea ? 'idea' : 'bug report'} screen.</sub>`,
  ].join('\n');
}

// The blocks of a 0.9.10+ report: each a heading with a table (columns and rows of text) or a text.
function checkBlocks(report) {
  const { blocks } = report;
  if (!Array.isArray(blocks) || blocks.length < 1 || blocks.length > 40) return 'blocks';
  if (report.category !== undefined && !CATEGORY.test(report.category)) return 'category';
  if (report.categoryName !== undefined && (typeof report.categoryName !== 'string' || report.categoryName.length > 40)) {
    return 'category';
  }
  for (const block of blocks) {
    if (typeof block !== 'object' || block === null) return 'block';
    if (typeof block.title !== 'string' || !line(block.title) || block.title.length > 80) return 'block title';
    if (block.columns !== undefined) {
      const { columns, rows } = block;
      if (!Array.isArray(columns) || columns.length < 2 || columns.length > 3) return 'columns';
      if (!columns.every((cell) => typeof cell === 'string' && cell.length <= 40)) return 'columns';
      if (!Array.isArray(rows) || rows.length < 1 || rows.length > 40) return 'rows';
      for (const row of rows) {
        if (!Array.isArray(row) || row.length !== columns.length) return 'row';
        if (!row.every((cell) => typeof cell === 'string' && cell.length <= 400)) return 'cell';
      }
    } else if (typeof block.text !== 'string' || !block.text.trim() || block.text.length > 4000) {
      return 'text';
    }
  }
  return null;
}

// One table cell: on one line, its pipes escaped, never empty.
function cell(text) {
  const flat = line(text).replace(/\\*\|/g, '\\|');
  return flat || '—';
}

function blocksBody(report, idea) {
  const priority = report.priority[0].toUpperCase() + report.priority.slice(1);
  const category = idea && report.categoryName ? cell(report.categoryName) : null;
  const head = category ? '| Category | Reporter (Minecraft) | Priority | Mod | Minecraft |'
    : '| Reporter (Minecraft) | Priority | Mod | Minecraft |';
  const facts = `\`${report.username}\` | ${priority} | ${report.modVersion} | ${report.minecraftVersion} |`;
  const parts = [
    head,
    category ? '|---|---|---|---|---|' : '|---|---|---|---|',
    category ? `| ${category} | ${facts}` : `| ${facts}`,
  ];
  for (const block of report.blocks) {
    parts.push('', `### ${line(block.title).replace(/^#+\s*/, '')}`, '');
    if (block.columns !== undefined) {
      parts.push(`| ${block.columns.map(cell).join(' | ')} |`);
      parts.push(`|${block.columns.map(() => '---').join('|')}|`);
      for (const row of block.rows) parts.push(`| ${row.map(cell).join(' | ')} |`);
    } else {
      // A line break typed in the game stays one on GitHub.
      parts.push(quiet(block.text.replace(/\r\n?/g, '\n').trim()).replace(/([^\n])\n(?=[^\n])/g, '$1  \n'));
    }
  }
  parts.push('', `<sub>Sent from the in-game ${idea ? 'idea' : 'bug report'} screen.</sub>`);
  return parts.join('\n');
}

function answer(status, error) {
  return Response.json({ error }, { status });
}

const fs = require('fs');
const html = fs.readFileSync('target/site/jacoco/index.html', 'utf8');
// Extract all package rows
const pkgRegex = /<a href="([^"]+)\/index\.html"[^>]*>([^<]+)<\/a><\/td>\s*<td class="bar" id="[^"]*">([\d,]+) of ([\d,]+)<\/td>\s*<td class="ctr2" id="[^"]*">(\d+)%/g;
let m;
console.log('Package                              | Missed | Total  | Coverage');
console.log('-'.repeat(70));
while ((m = pkgRegex.exec(html)) !== null) {
  const pkg = m[2].padEnd(37);
  const missed = m[3].padEnd(7);
  const total = m[4].padEnd(7);
  const pct = m[5] + '%';
  console.log(`${pkg}| ${missed}| ${total}| ${pct}`);
}

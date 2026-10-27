// Pré-requisitos opcionais: Node.js, pacote playwright e Google Chrome instalado.
// Use apenas conta e banco de testes. A senha vem do ambiente e não é registrada.
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');

(async () => {
  const base = new URL(process.env.BASE_URL || 'http://localhost:8080').origin;
  const login = process.env.TEST_USER;
  const senha = process.env.TEST_PASSWORD;
  if (!login || !senha) throw new Error('Defina TEST_USER e TEST_PASSWORD para uma conta de teste.');
  const browser = await chromium.launch(process.env.CHROME_PATH
    ? { executablePath: process.env.CHROME_PATH, headless: true }
    : { channel: 'chrome', headless: true });
  try {
    const context = await browser.newContext({ viewport: { width: 390, height: 844 }, locale: 'pt-BR' });
    const page = await context.newPage();
    await page.goto(`${base}/login.html`);
    await page.locator('#username').fill(login);
    await page.locator('#password').fill(senha);
    await page.locator('#entrar').click();
    await page.waitForURL(`${base}/`);
    await page.waitForFunction(() => document.querySelectorAll('#produtos tr').length === 20);
    const counts = await page.evaluate(async () => {
      const produtos = await (await fetch('/api/produtos?tamanho=1')).json();
      const movimentos = await (await fetch('/api/movimentacoes?tamanho=1')).json();
      return { produtos: produtos.totalItens, movimentacoes: movimentos.totalItens };
    });
    if (counts.produtos !== 300 || counts.movimentacoes !== 1000) {
      throw new Error('Prepare um banco isolado com exatamente 300 produtos e 1.000 movimentações.');
    }
    const cdp = await context.newCDPSession(page);
    await cdp.send('Network.enable');
    await cdp.send('Network.setCacheDisabled', { cacheDisabled: true });
    await cdp.send('Network.emulateNetworkConditions', {
      offline: false, latency: 150, downloadThroughput: 200000, uploadThroughput: 93750
    });
    await cdp.send('Emulation.setCPUThrottlingRate', { rate: 4 });
    const tempos = [];
    for (let indice = 0; indice < 6; indice++) {
      const inicio = performance.now();
      await page.goto(`${base}/`, { waitUntil: 'domcontentloaded' });
      await page.waitForFunction(() => document.querySelector('#lista')?.getAttribute('aria-busy') === 'false'
        && document.querySelectorAll('#produtos tr').length === 20);
      await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
      if (indice > 0) tempos.push(Math.round(performance.now() - inicio));
    }
    const ordenados = [...tempos].sort((a, b) => a - b);
    const relatorio = {
      dataUtc: new Date().toISOString(),
      classificacao: 'Exploratório em desktop; não comprova a meta RNF03 no Android exigido.',
      navegador: `Google Chrome ${browser.version()}`, plataforma: process.platform,
      viewport: '390x844', cpuSimulada: 'desaceleração 4x via DevTools',
      rede: { downloadMbps: 1.6, uploadKbps: 750, latenciaMs: 150 },
      cache: 'desabilitado', aquecimentos: 1, ...counts,
      temposMs: tempos, medianaMs: ordenados[2], referenciaMs: 3000,
      abaixoDaReferenciaNesteAmbiente: ordenados[2] <= 3000
    };
    const destino = path.resolve('target/desempenho-exploratorio.json');
    fs.mkdirSync(path.dirname(destino), { recursive: true });
    fs.writeFileSync(destino, `${JSON.stringify(relatorio, null, 2)}\n`);
    console.log(JSON.stringify(relatorio, null, 2));
  } finally { await browser.close(); }
})().catch(erro => { console.error(erro.message); process.exitCode = 1; });

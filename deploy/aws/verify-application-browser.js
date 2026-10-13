async page => {
  const origin = await page.evaluate(() => window.location.origin);
  if (origin !== 'https://sentimentscribe.cloud') throw new Error('Open the verified production website first.');
  const api = 'https://api.sentimentscribe.cloud';
  const checks = {};
  const statuses = [];
  const failures = [];
  let stage = 'registration';
  let freshContext;
  let freshPage;
  const onResponse = response => {
    if (response.url().startsWith(api + '/api/')) {
      statuses.push({path: response.url().slice(api.length).split('?')[0], status: response.status()});
    }
  };
  const onFailure = () => failures.push('network request failed');
  const observe = target => {
    target.on('response', onResponse);
    target.on('requestfailed', onFailure);
  };
  observe(page);
  try {
    await page.setViewportSize({width: 1440, height: 900});
    await page.goto(origin + '/register');
    const credentials = await page.evaluate(() => {
      const random = () => Array.from(crypto.getRandomValues(new Uint8Array(24)), byte => byte.toString(16).padStart(2, '0')).join('');
      return {username: 'rehost-check-' + Date.now(), password: random(), passphrase: random()};
    });
    const title = 'AWS deployment check';
    const body = 'Today I feel happy and hopeful. I enjoyed a peaceful walk with friends, and I am grateful for their kindness. This is a synthetic AWS deployment verification entry, not personal diary data.';
    await page.getByLabel('Username', {exact: true}).fill(credentials.username);
    await page.getByLabel('Password', {exact: true}).fill(credentials.password);
    const registration = page.waitForResponse(response => response.url() === api + '/api/auth/register');
    await page.getByRole('button', {name: 'Create account', exact: true}).click();
    if ((await registration).status() !== 200) throw new Error('Registration failed.');
    await page.getByRole('heading', {name: 'Unlock your diary', exact: true}).waitFor();
    checks.registration = 'passed';
    const unlock = async target => {
      await target.getByLabel('Passphrase', {exact: true}).fill(credentials.passphrase);
      await target.getByRole('button', {name: 'Unlock', exact: true}).click();
      await target.getByRole('heading', {name: 'Home', exact: true}).waitFor();
    };
    stage = 'diary unlock';
    await unlock(page);
    checks.unlock = 'passed';
    stage = 'encrypted server save';
    await page.getByRole('button', {name: 'New Entry', exact: true}).click();
    await page.getByLabel('Title', {exact: true}).fill(title);
    await page.getByLabel('Entry Text', {exact: true}).fill(body);
    const saved = page.waitForResponse(response => response.url() === api + '/api/entries' && ['POST', 'PUT'].includes(response.request().method()));
    await page.getByRole('button', {name: 'Save Entry', exact: true}).click();
    const savedResponse = await saved;
    if (![200, 201].includes(savedResponse.status())) throw new Error('Server save failed.');
    const envelope = await savedResponse.json();
    if (!envelope.bodyCiphertext || !envelope.titleCiphertext || envelope.bodyCiphertext === body || envelope.titleCiphertext === title) throw new Error('Encrypted server response check failed.');
    checks.encryptedSave = 'passed';
    await page.getByRole('button', {name: 'Home', exact: true}).click();
    await page.getByRole('button', {name: 'Open ' + title, exact: true}).waitFor();
    stage = 'refresh and re-unlock';
    await page.reload();
    await page.getByRole('heading', {name: 'Unlock your diary', exact: true}).waitFor();
    await unlock(page);
    await page.getByRole('button', {name: 'Open ' + title, exact: true}).waitFor();
    checks.refresh = 'passed';
    stage = 'fresh-browser login and persistence';
    freshContext = await page.context().browser().newContext();
    freshPage = await freshContext.newPage();
    observe(freshPage);
    await freshPage.goto(origin + '/');
    await freshPage.getByLabel('Username', {exact: true}).fill(credentials.username);
    await freshPage.getByLabel('Password', {exact: true}).fill(credentials.password);
    const login = freshPage.waitForResponse(response => response.url() === api + '/api/auth/login');
    await freshPage.getByRole('button', {name: 'Sign in', exact: true}).click();
    if ((await login).status() !== 200) throw new Error('Login failed.');
    await freshPage.getByRole('heading', {name: 'Unlock your diary', exact: true}).waitFor();
    await unlock(freshPage);
    checks.login = 'passed';
    await freshPage.getByRole('button', {name: 'Open ' + title, exact: true}).waitFor();
    const loaded = freshPage.waitForResponse(response => response.url().startsWith(api + '/api/entries/by-path?'));
    await freshPage.getByRole('button', {name: 'Open ' + title, exact: true}).click();
    if ((await loaded).status() !== 200) throw new Error('Fresh server load failed.');
    await freshPage.waitForFunction(expected => document.querySelector('textarea')?.value === expected, body);
    checks.freshBrowserPersistenceAndDecryption = 'passed';
    stage = 'analysis';
    const analysis = freshPage.waitForResponse(response => response.url() === api + '/api/analysis');
    await freshPage.getByRole('button', {name: 'Show Keywords', exact: true}).click();
    const analysisResponse = await analysis;
    if (analysisResponse.status() !== 200 || !(await analysisResponse.json()).keywords?.length) throw new Error('Analysis failed.');
    checks.analysis = 'passed';
    stage = 'recommendations';
    const recommendations = freshPage.waitForResponse(response => response.url() === api + '/api/recommendations');
    await freshPage.getByRole('button', {name: 'Get Media Recommendations', exact: true}).click();
    const recommendationResponse = await recommendations;
    if (recommendationResponse.status() === 200) {
      const data = await recommendationResponse.json();
      checks.recommendations = {status: 'passed', songs: data.songs?.length ?? 0, movies: data.movies?.length ?? 0};
    } else {
      checks.recommendations = {status: 'failed', httpStatus: recommendationResponse.status()};
    }
    checks.apiOriginAndNetwork = failures.length === 0 ? 'passed' : 'failed';
    return {complete: checks.recommendations.status === 'passed' && failures.length === 0, checks, apiStatuses: statuses, testAccount: credentials.username, testData: 'one synthetic encrypted entry; no existing user data changed'};
  } catch {
    return {complete: false, failedStage: stage, checks, apiStatuses: statuses, error: 'Verification failed; credentials, tokens, and response bodies suppressed.'};
  } finally {
    page.off('response', onResponse);
    page.off('requestfailed', onFailure);
    if (freshContext) await freshContext.close();
    await page.goto(origin + '/');
  }
}

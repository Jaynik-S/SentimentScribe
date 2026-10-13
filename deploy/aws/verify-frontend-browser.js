async page => {
  const origin = await page.evaluate(() => window.location.origin);
  if (!['https://dgzayw2ozg6wz.cloudfront.net', 'https://sentimentscribe.cloud'].includes(origin)) {
    throw new Error('Open the verified new frontend before running this check.');
  }
  const failures = [];
  const assets = new Set();
  const onError = () => failures.push('browser JavaScript error');
  const onResponse = response => {
    const url = response.url();
    if (url.startsWith(origin + '/assets/') && /\.(js|css)$/.test(url)) {
      if (response.status() !== 200) failures.push('frontend asset request failed');
      assets.add(url.endsWith('.js') ? 'js' : 'css');
    }
  };
  page.on('pageerror', onError);
  page.on('response', onResponse);
  try {
    for (const viewport of [{width: 1440, height: 900}, {width: 390, height: 844}]) {
      await page.setViewportSize(viewport);
      for (const [route, heading] of [['/', 'Sign in'], ['/register', 'Create your account']]) {
        const response = await page.goto(origin + route);
        if (!response || response.status() !== 200) throw new Error('SPA route did not return HTTP 200.');
        await page.getByRole('heading', {name: heading, exact: true}).waitFor();
        await page.reload();
        await page.getByRole('heading', {name: heading, exact: true}).waitFor();
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth);
        if (overflow) throw new Error('Frontend has horizontal overflow.');
      }
      await page.screenshot({path: `frontend-${viewport.width}.png`});
    }
    if (failures.length || !assets.has('js') || !assets.has('css')) {
      throw new Error('Browser or frontend asset verification failed.');
    }
    return {origin, routes: ['/', '/register'], viewports: ['1440x900', '390x844'], refresh: 'passed', assets: 'passed', javascriptErrors: 0};
  } finally {
    page.off('pageerror', onError);
    page.off('response', onResponse);
  }
}

import { test, expect } from '@playwright/test';

test('register, login and access the catalog flow', async ({ page }) => {
  console.log('========== E2E VERSION 123 ==========');

  page.on('console', msg => {
    console.log(`[BROWSER ${msg.type()}] ${msg.text()}`);
  });

  const uniqueId = `${Date.now()}`;
  const username = `e2e${uniqueId}`;
  const email = `e2e${uniqueId}@example.com`;
  const password = 'Pass1234';


  await page.goto('/register');

  console.log('URL after goto:', page.url());

  console.log(
    'TOKEN BEFORE:',
    await page.evaluate(() => localStorage.getItem('token'))
  );

  // await page.goto('/register');
  await expect(page.getByRole('heading', { name: 'Create Account' })).toBeVisible();

  await page.locator('input[formcontrolname="username"]').fill(username);
  await page.locator('input[formcontrolname="email"]').fill(email);
  await page.locator('input[formcontrolname="password"]').fill(password);
  await page.locator('select[formcontrolname="role"]').selectOption('CLIENT');

  //check the JWT id it's causing the test to fail
  console.log('token before registration:', await page.evaluate(() =>
    localStorage.getItem('token')
  ));
  await page.getByRole('button', {
    name: 'Complete Registration'
  }).click();

  console.log('URL AFTER CLICK:', page.url());

  console.log(
    'TOKEN AFTER:',
    await page.evaluate(() => localStorage.getItem('token'))
  );
  // await page.getByRole('button', { name: 'Complete Registration' }).click();
  // await page.getByRole('button', { name: 'Complete Registration' }).click();

  console.log('URL after registration:', page.url());

  console.log('token after registration:', await page.evaluate(() =>
    localStorage.getItem('token')
  ));


  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { name: 'Account Login' })).toBeVisible();

  await page.locator('input[formcontrolname="email"]').fill(email);
  await page.locator('input[formcontrolname="password"]').fill(password);
  await page.getByRole('button', { name: 'Authenticate' }).click();

  await expect(page).toHaveURL(/\/(products|$)/);
  await expect(page.getByRole('heading', { name: 'Market Catalog' })).toBeVisible();
  await expect(page.getByText('Browse items, filter inventory, and add products to cart')).toBeVisible();
});

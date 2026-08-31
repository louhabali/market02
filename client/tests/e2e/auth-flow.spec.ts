import { test, expect } from '@playwright/test';

test('register, login and access the catalog flow', async ({ page }) => {
  const uniqueId = `${Date.now()}`;
  const username = `e2e${uniqueId}`;
  const email = `e2e${uniqueId}@example.com`;
  const password = 'pass1234';

  await page.goto('/register');
  await expect(page.getByRole('heading', { name: 'Create Account' })).toBeVisible();

  await page.locator('input[formcontrolname="username"]').fill(username);
  await page.locator('input[formcontrolname="email"]').fill(email);
  await page.locator('input[formcontrolname="password"]').fill(password);
  await page.locator('select[formcontrolname="role"]').selectOption('CLIENT');

  await page.getByRole('button', { name: 'Complete Registration' }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { name: 'Account Login' })).toBeVisible();

  await page.locator('input[formcontrolname="email"]').fill(email);
  await page.locator('input[formcontrolname="password"]').fill(password);
  await page.getByRole('button', { name: 'Authenticate' }).click();

  await expect(page).toHaveURL(/\/(products|$)/);
  await expect(page.getByRole('heading', { name: 'Market Catalog' })).toBeVisible();
  await expect(page.getByText('Browse items, filter inventory, and add products to cart')).toBeVisible();
});

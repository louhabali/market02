import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../app/services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isLoggedIn = authService.isLoggedIn();
  const rawRole = authService.getRole() || '';
  
  // Normalize role string (handles 'ROLE_SELLER' vs 'SELLER')
  const userRole = rawRole.replace('ROLE_', '').toUpperCase();

  // Matches 'expectedRole' specified in app.routes.ts
  const expectedRole = (route.data?.['expectedRole'] || route.data?.['role'] || '').toUpperCase();

  if (isLoggedIn && userRole === expectedRole) {
    return true;
  }

  if (isLoggedIn) {
    router.navigate(['/forbidden']);
    return false;
  }

  router.navigate(['/login']);
  return false;
};
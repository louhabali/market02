import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const expectedRole = route.data['expectedRole'] as string;

  const isLoggedIn = authService.isLoggedIn();
  const userRole = authService.getRole();

  if (isLoggedIn && userRole === expectedRole) {
    return true;
  }

  if (!isLoggedIn) {
    return router.createUrlTree(['/login']);
  }

  // User is logged in but lacks required role privileges -> redirect to Profile
  return router.createUrlTree(['/profile']);
};
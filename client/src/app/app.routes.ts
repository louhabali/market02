import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { ErrorComponent } from './pages/error/error.component';
import { authGuard } from '../guards/auth.guard';
import { guestGuard } from '../guards/guest.guard';
import { ProductsComponent } from './pages/products/products.component';
import { AddProductComponent } from './pages/add-product/add-product.component';
import { ProductPageComponent } from './pages/productpage/product-page.component';
import { CartComponent } from './pages/cart/cart.component';
import { CheckoutComponent } from './pages/checkout/checkout.component';
import { SellerDashboardComponent } from './pages/seller-dashboard/seller-dashboard.component';
import { roleGuard } from '../guards/role.guard';
export const routes: Routes = [
  {
    path: '',
    component: ProductsComponent,
    canActivate : [authGuard]
  },
  {
    path: 'products',
    component: ProductsComponent,
    canActivate : [authGuard]
  },
  {
    path: 'login',
    component: LoginComponent,
    canActivate : [guestGuard]
  },
  {
    path: 'register',
    component: RegisterComponent,
    canActivate : [guestGuard]
  },

  {
    path: 'profile',
    component: ProfileComponent ,
    canActivate : [authGuard]
  },
  { 
    path: 'seller/dashboard', 
    component: SellerDashboardComponent,
    canActivate: [roleGuard],
    data: { expectedRole: 'SELLER' }
  },

  {
    path: 'cart',
    component : CartComponent ,
    canActivate : [authGuard]
  },
  {
    path: 'checkout',
    component : CheckoutComponent ,
    canActivate : [authGuard]
  },

  { path: 'products/add', component: AddProductComponent, canActivate: [authGuard] },
  { path: 'products/:id', component: ProductPageComponent },
  { path: 'unauthorized', component: ErrorComponent, data: { code: '401' } },
  { path: 'forbidden', component: ErrorComponent, data: { code: '403' } },
  { path: 'server-error', component: ErrorComponent, data: { code: '500' } },

  // Wildcard 404 handler block (MUST be placed absolutely last)
  { path: '**', component: ErrorComponent, data: { code: '404' } }

];
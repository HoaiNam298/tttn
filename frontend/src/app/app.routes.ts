import { Routes } from '@angular/router';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  {
    path: 'products',
    loadComponent: () =>
      import('./components/product-list/product-list.component').then(
        (m) => m.ProductListComponent,
      ),
  },
  {
    path: 'products/:id',
    loadComponent: () =>
      import('./components/product-detail/product-detail.component').then(
        (m) => m.ProductDetailComponent,
      ),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./components/login/login.component').then(
        (m) => m.LoginComponent,
      ),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./components/admin/product/admin-catalog.component').then(
        (m) => m.AdminCatalogComponent,
      ),
  },
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  { path: '**', redirectTo: 'products' },
];

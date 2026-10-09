import { Routes } from '@angular/router';
import { adminGuard } from './guards/admin.guard';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./components/home/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./components/register/register.component').then(
        (m) => m.RegisterComponent,
      ),
  },
  {
    path: 'account',
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    loadComponent: () =>
      import('./components/account/account.component').then(
        (m) => m.AccountComponent,
      ),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'profile' },
      {
        path: 'favorites',
        loadComponent: () =>
          import('./components/account/favorites/favorites.component').then(
            (m) => m.FavoritesComponent,
          ),
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import(
            './components/account/notifications/notifications.component'
          ).then((m) => m.NotificationsComponent),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./components/account/profile/profile.component').then(
            (m) => m.ProfileComponent,
          ),
      },
      {
        path: 'addresses',
        loadComponent: () =>
          import('./components/account/addresses/addresses.component').then(
            (m) => m.AddressesComponent,
          ),
      },
    ],
  },
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
    path: 'cart',
    loadComponent: () =>
      import('./components/cart/cart.component').then((m) => m.CartComponent),
  },
  {
    path: 'checkout',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/checkout/checkout.component').then(
        (m) => m.CheckoutComponent,
      ),
  },
  {
    path: 'orders/:id/success',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/order-success/order-success.component').then(
        (m) => m.OrderSuccessComponent,
      ),
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/order-history/order-history.component').then(
        (m) => m.OrderHistoryComponent,
      ),
  },
  {
    path: 'orders/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/order-detail/order-detail.component').then(
        (m) => m.OrderDetailComponent,
      ),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    canActivateChild: [adminGuard],
    loadComponent: () =>
      import('./components/admin/admin.component').then(
        (m) => m.AdminComponent,
      ),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'reviews',
        loadComponent: () =>
          import('./components/admin/reviews/admin-reviews.component').then(
            (m) => m.AdminReviewsComponent,
          ),
      },
      {
        path: 'vouchers',
        loadComponent: () =>
          import('./components/admin/voucher/admin-vouchers.component').then(
            (m) => m.AdminVouchersComponent,
          ),
      },
      {
        path: 'orders/:id',
        data: { adminMode: true },
        loadComponent: () =>
          import('./components/order-detail/order-detail.component').then(
            (m) => m.OrderDetailComponent,
          ),
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./components/admin/dashboard/admin-dashboard.component').then(
            (m) => m.AdminDashboardComponent,
          ),
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./components/admin/product/admin-catalog.component').then(
            (m) => m.AdminCatalogComponent,
          ),
      },
      {
        path: 'categories',
        loadComponent: () =>
          import(
            './components/admin/category/category-management.component'
          ).then((m) => m.CategoryManagementComponent),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./components/admin/order/admin-orders.component').then(
            (m) => m.AdminOrdersComponent,
          ),
      },
    ],
  },
  { path: '**', redirectTo: 'products' },
];

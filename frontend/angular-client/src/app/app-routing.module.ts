import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { AdminGuard } from './core/guards/admin.guard';

const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule)
  },
  {
    path: 'dashboard',
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule)
  },
  {
    path: 'accounts',
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/accounts/accounts.module').then(m => m.AccountsModule)
  },
  {
    path: 'transactions',
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/transactions/transactions.module').then(m => m.TransactionsModule)
  },
  {
    path: 'beneficiaries',
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/beneficiaries/beneficiaries.module').then(m => m.BeneficiariesModule)
  },
  { path: '**', redirectTo: 'dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { scrollPositionRestoration: 'enabled' })],
  exports: [RouterModule]
})
export class AppRoutingModule {}

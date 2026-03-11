import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { AccountService } from '../../core/services/account.service';
import { TransactionService } from '../../core/services/transaction.service';
import { Account, Transaction, Page } from '../../core/models/models';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  accounts: Account[] = [];
  recentTransactions: Transaction[] = [];
  loading = true;
  error = '';

  get totalBalance(): number {
    return this.accounts.reduce((sum, a) => sum + a.balance, 0);
  }

  get activeAccounts(): number {
    return this.accounts.filter(a => a.status === 'ACTIVE').length;
  }

  constructor(
    private accountService: AccountService,
    private transactionService: TransactionService
  ) {}

  ngOnInit(): void {
    forkJoin({
      accounts: this.accountService.getMyAccounts(),
      transactions: this.transactionService.getMyTransactions(0, 5)
    }).subscribe({
      next: ({ accounts, transactions }) => {
        this.accounts = accounts;
        this.recentTransactions = transactions.content;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load dashboard data.';
        this.loading = false;
      }
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'badge-success';
      case 'FAILED':
      case 'ROLLED_BACK': return 'badge-danger';
      case 'PENDING': return 'badge-warning';
      default: return 'badge-neutral';
    }
  }

  isCredit(tx: Transaction): boolean {
    return ['DEPOSIT'].includes(tx.transactionType);
  }
}

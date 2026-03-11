import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AccountService } from '../../../core/services/account.service';
import { Account } from '../../../core/models/models';

@Component({
  selector: 'app-account-list',
  templateUrl: './account-list.component.html',
  styleUrls: ['./account-list.component.scss']
})
export class AccountListComponent implements OnInit {
  accounts: Account[] = [];
  loading = true;
  error = '';
  showCreateModal = false;
  createForm: FormGroup;
  creating = false;
  createError = '';
  createSuccess = '';

  readonly accountTypes = ['SAVINGS', 'CHECKING', 'INVESTMENT'];

  constructor(private accountService: AccountService, private fb: FormBuilder) {
    this.createForm = this.fb.group({
      accountType: ['SAVINGS', Validators.required],
      initialDeposit: [0, [Validators.min(0)]]
    });
  }

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loading = true;
    this.accountService.getMyAccounts().subscribe({
      next: (accounts) => { this.accounts = accounts; this.loading = false; },
      error: () => { this.error = 'Failed to load accounts.'; this.loading = false; }
    });
  }

  openCreateModal(): void {
    this.createForm.reset({ accountType: 'SAVINGS', initialDeposit: 0 });
    this.createError = '';
    this.createSuccess = '';
    this.showCreateModal = true;
  }

  closeCreateModal(): void { this.showCreateModal = false; }

  onCreateAccount(): void {
    if (this.createForm.invalid) { this.createForm.markAllAsTouched(); return; }
    this.creating = true;
    this.createError = '';

    this.accountService.createAccount(this.createForm.value).subscribe({
      next: (account) => {
        this.accounts.push(account);
        this.createSuccess = `Account ${account.accountNumber} created successfully!`;
        this.creating = false;
        setTimeout(() => this.closeCreateModal(), 1500);
      },
      error: (err) => {
        this.createError = err.error?.message || 'Failed to create account.';
        this.creating = false;
      }
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'badge-success';
      case 'FROZEN': return 'badge-warning';
      case 'CLOSED': return 'badge-neutral';
      default: return 'badge-neutral';
    }
  }

  getAccountTypeIcon(type: string): string {
    switch (type) {
      case 'SAVINGS': return '💰';
      case 'CHECKING': return '🏦';
      case 'INVESTMENT': return '📈';
      default: return '💳';
    }
  }
}

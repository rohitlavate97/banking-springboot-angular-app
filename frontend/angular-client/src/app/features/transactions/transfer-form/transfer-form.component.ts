import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AccountService } from '../../../core/services/account.service';
import { TransactionService } from '../../../core/services/transaction.service';
import { Account } from '../../../core/models/models';

type TxAction = 'deposit' | 'withdraw' | 'transfer';

@Component({
  selector: 'app-transfer-form',
  templateUrl: './transfer-form.component.html',
  styleUrls: ['./transfer-form.component.scss']
})
export class TransferFormComponent implements OnInit {
  action: TxAction = 'transfer';
  form: FormGroup;
  accounts: Account[] = [];
  loading = false;
  loadingAccounts = true;
  error = '';
  success = '';

  readonly actions: { key: TxAction; label: string }[] = [
    { key: 'transfer', label: 'Transfer' },
    { key: 'deposit',  label: 'Deposit' },
    { key: 'withdraw', label: 'Withdraw' }
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private accountService: AccountService,
    private transactionService: TransactionService
  ) {
    this.form = this.buildForm('transfer');
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const a = params['action'] as TxAction;
      if (a && ['deposit', 'withdraw', 'transfer'].includes(a)) {
        this.setAction(a);
      }
    });

    this.accountService.getMyAccounts().subscribe({
      next: (accs) => { this.accounts = accs.filter(a => a.status === 'ACTIVE'); this.loadingAccounts = false; },
      error: () => this.loadingAccounts = false
    });
  }

  private buildForm(action: TxAction): FormGroup {
    const base = {
      amount: [null, [Validators.required, Validators.min(0.01)]],
      description: ['']
    };

    if (action === 'deposit') {
      return this.fb.group({ ...base, destinationAccountId: ['', Validators.required] });
    }
    if (action === 'withdraw') {
      return this.fb.group({ ...base, sourceAccountId: ['', Validators.required] });
    }
    return this.fb.group({
      ...base,
      sourceAccountId: ['', Validators.required],
      destinationAccountId: ['', Validators.required]
    });
  }

  setAction(action: TxAction): void {
    this.action = action;
    this.form = this.buildForm(action);
    this.error = '';
    this.success = '';
  }

  get amount() { return this.form.get('amount')!; }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    this.error = '';
    this.success = '';

    const payload = { ...this.form.value, transactionType: this.action.toUpperCase() };
    let obs$;

    if (this.action === 'deposit') {
      obs$ = this.transactionService.deposit(payload);
    } else if (this.action === 'withdraw') {
      obs$ = this.transactionService.withdraw(payload);
    } else {
      obs$ = this.transactionService.transfer(payload);
    }

    obs$.subscribe({
      next: (tx) => {
        this.success = `Transaction ${tx.id} submitted successfully.`;
        this.loading = false;
        setTimeout(() => this.router.navigate(['/transactions']), 1800);
      },
      error: (err) => {
        this.error = err.error?.message || 'Transaction failed. Please try again.';
        this.loading = false;
      }
    });
  }

  formatAccount(acc: Account): string {
    return `${acc.accountType} — ${acc.accountNumber}`;
  }
}

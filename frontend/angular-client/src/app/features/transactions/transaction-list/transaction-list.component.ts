import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TransactionService } from '../../../core/services/transaction.service';
import { Transaction } from '../../../core/models/models';

@Component({
  selector: 'app-transaction-list',
  templateUrl: './transaction-list.component.html',
  styleUrls: ['./transaction-list.component.scss']
})
export class TransactionListComponent implements OnInit {
  transactions: Transaction[] = [];
  loading = true;
  error = '';
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  pageSize = 10;

  constructor(
    private transactionService: TransactionService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['action']) {
        this.router.navigate(['/transactions/new'], { queryParams: { action: params['action'] } });
        return;
      }
      this.loadTransactions(0);
    });
  }

  loadTransactions(page: number): void {
    this.loading = true;
    this.transactionService.getMyTransactions(page, this.pageSize).subscribe({
      next: (data) => {
        this.transactions = data.content;
        this.currentPage = data.number;
        this.totalPages = data.totalPages;
        this.totalElements = data.totalElements;
        this.loading = false;
      },
      error: () => { this.error = 'Failed to load transactions.'; this.loading = false; }
    });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.loadTransactions(page);
  }

  get pages(): number[] {
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages - 1, this.currentPage + 2);
    const arr: number[] = [];
    for (let i = start; i <= end; i++) arr.push(i);
    return arr;
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
    return tx.type === 'DEPOSIT';
  }
}

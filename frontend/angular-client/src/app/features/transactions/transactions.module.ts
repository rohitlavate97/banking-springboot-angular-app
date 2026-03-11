import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { TransactionListComponent } from './transaction-list/transaction-list.component';
import { TransferFormComponent } from './transfer-form/transfer-form.component';

const routes: Routes = [
  { path: '', component: TransactionListComponent },
  { path: 'new', component: TransferFormComponent }
];

@NgModule({
  declarations: [TransactionListComponent, TransferFormComponent],
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule.forChild(routes)]
})
export class TransactionsModule {}

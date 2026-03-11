import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { BeneficiaryListComponent } from './beneficiary-list/beneficiary-list.component';

const routes: Routes = [{ path: '', component: BeneficiaryListComponent }];

@NgModule({
  declarations: [BeneficiaryListComponent],
  imports: [CommonModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class BeneficiariesModule {}

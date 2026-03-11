import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { BeneficiaryService } from '../../../core/services/beneficiary.service';
import { Beneficiary } from '../../../core/models/models';

@Component({
  selector: 'app-beneficiary-list',
  templateUrl: './beneficiary-list.component.html',
  styleUrls: ['./beneficiary-list.component.scss']
})
export class BeneficiaryListComponent implements OnInit {
  beneficiaries: Beneficiary[] = [];
  loading = true;
  error = '';
  showAddModal = false;
  addForm: FormGroup;
  adding = false;
  addError = '';
  addSuccess = '';
  deletingId: string | null = null;

  constructor(private beneficiaryService: BeneficiaryService, private fb: FormBuilder) {
    this.addForm = this.fb.group({
      nickname: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      accountNumber: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(30)]],
      accountHolderName: ['', [Validators.required, Validators.minLength(2)]],
      bankName: ['', [Validators.required]],
      bankCode: ['']
    });
  }

  ngOnInit(): void { this.loadBeneficiaries(); }

  loadBeneficiaries(): void {
    this.loading = true;
    this.beneficiaryService.getAll().subscribe({
      next: (list) => { this.beneficiaries = list; this.loading = false; },
      error: () => { this.error = 'Failed to load beneficiaries.'; this.loading = false; }
    });
  }

  openAddModal(): void {
    this.addForm.reset();
    this.addError = '';
    this.addSuccess = '';
    this.showAddModal = true;
  }

  closeAddModal(): void { this.showAddModal = false; }

  onAdd(): void {
    if (this.addForm.invalid) { this.addForm.markAllAsTouched(); return; }
    this.adding = true;
    this.addError = '';

    this.beneficiaryService.add(this.addForm.value).subscribe({
      next: (b) => {
        this.beneficiaries.push(b);
        this.addSuccess = `Beneficiary "${b.nickname}" added.`;
        this.adding = false;
        setTimeout(() => this.closeAddModal(), 1500);
      },
      error: (err) => {
        this.addError = err.error?.message || 'Failed to add beneficiary.';
        this.adding = false;
      }
    });
  }

  onDelete(id: string, nickname: string): void {
    if (!confirm(`Remove beneficiary "${nickname}"?`)) return;
    this.deletingId = id;

    this.beneficiaryService.delete(id).subscribe({
      next: () => {
        this.beneficiaries = this.beneficiaries.filter(b => b.id !== id);
        this.deletingId = null;
      },
      error: () => {
        this.error = 'Failed to delete beneficiary.';
        this.deletingId = null;
      }
    });
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Beneficiary } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class BeneficiaryService {
  private readonly baseUrl = `${environment.apiUrl}/beneficiaries`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Beneficiary[]> {
    return this.http.get<Beneficiary[]>(this.baseUrl);
  }

  add(payload: {
    nickname: string;
    accountNumber: string;
    accountHolderName: string;
    bankName: string;
    bankCode?: string;
    currency?: string;
  }): Observable<Beneficiary> {
    return this.http.post<Beneficiary>(this.baseUrl, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

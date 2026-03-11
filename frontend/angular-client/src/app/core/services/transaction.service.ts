import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Transaction, TransactionRequest, Page } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly baseUrl = `${environment.apiUrl}/transactions`;

  constructor(private http: HttpClient) {}

  getMyTransactions(page = 0, size = 20): Observable<Page<Transaction>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<Transaction>>(this.baseUrl, { params });
  }

  getTransactionById(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/${id}`);
  }

  deposit(request: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/deposit`, request);
  }

  withdraw(request: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/withdraw`, request);
  }

  transfer(request: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/transfer`, request);
  }
}

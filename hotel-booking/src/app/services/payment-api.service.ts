import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface InitiatePaymentResponse {
  transactionId: number;
}

export interface PaymentStatusResponse {
  id: number;
  status: string;
  qrCode?: {
    code: string;
    expiresAt?: string;
  };
}

export interface ProcessPaymentRequest {
  qrCode: string;
  amount: number;
  currency: string;
  merchantId: string;
  description?: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentApiService {
  private baseUrl = environment.paymentServiceUrl;

  constructor(private http: HttpClient) {}

  initiatePayment(userId: number): Observable<InitiatePaymentResponse> {
    return this.http.post<InitiatePaymentResponse>(`${this.baseUrl}/payments/initiate`, { userId });
  }

  getPaymentStatus(paymentId: number): Observable<PaymentStatusResponse> {
    return this.http.get<PaymentStatusResponse>(`${this.baseUrl}/payments/${paymentId}/status`);
  }

  processPayment(paymentId: number, data: ProcessPaymentRequest): Observable<PaymentStatusResponse> {
    return this.http.post<PaymentStatusResponse>(`${this.baseUrl}/payments/${paymentId}/process`, data);
  }

  cancelPayment(paymentId: number): Observable<PaymentStatusResponse> {
    return this.http.post<PaymentStatusResponse>(`${this.baseUrl}/payments/${paymentId}/cancel`, {});
  }
}

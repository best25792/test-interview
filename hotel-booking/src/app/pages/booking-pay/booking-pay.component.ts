import { ChangeDetectionStrategy, Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, interval, takeUntil, switchMap } from 'rxjs';
import { PaymentApiService } from '../../services/payment-api.service';
import { BookingApiService, Booking } from '../../services/booking-api.service';
import { AuthService } from '../../services/auth.service';

const MERCHANT_ID = 'HOTEL_MERCHANT';

@Component({
  selector: 'app-booking-pay',
  standalone: true,
  imports: [NgIf, RouterLink, FormsModule],
  template: `
    <div class="container">
      <h1>Complete Payment</h1>
      <div *ngIf="error" class="error-message">{{ error }}</div>
      <div *ngIf="success" class="success-message">Booking confirmed! Thank you.</div>

      <div *ngIf="booking && !success && !error" class="card">
        <p><strong>Total:</strong> \${{ booking.totalAmount }}</p>
        <p *ngIf="statusMessage">{{ statusMessage }}</p>

        <div *ngIf="qrCode" class="qr-section">
          <img [src]="qrImageUrl" alt="QR Code" width="200" height="200" />
          <p>Scan this QR code to complete payment</p>
          <p *ngIf="timeRemaining !== null" class="timer">Time remaining: {{ formatTime(timeRemaining) }}</p>
        </div>

        <div *ngIf="showProcessForm">
          <p>Enter the QR code value (or paste from scan):</p>
          <input type="text" [(ngModel)]="qrCodeInput" placeholder="PAYMENT_123_..." style="width: 100%; padding: 0.5rem;" />
          <button class="btn btn-primary" (click)="processPayment()" [disabled]="processing">Process Payment</button>
        </div>

        <button *ngIf="!qrCode && !processing" class="btn btn-primary" (click)="initiatePayment()" [disabled]="initiating">
          {{ initiating ? 'Initiating...' : 'Start Payment' }}
        </button>
      </div>

      <a *ngIf="success" routerLink="/bookings" class="btn btn-primary">View My Bookings</a>
    </div>
  `,
  styles: [`
    .qr-section { text-align: center; margin: 1rem 0; }
    .timer { font-weight: 600; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingPayComponent implements OnInit, OnDestroy {
  booking: Booking | null = null;
  bookingId = 0;
  paymentId: number | null = null;
  qrCode = '';
  qrImageUrl = '';
  qrCodeInput = '';
  statusMessage = '';
  error = '';
  success = false;
  initiating = false;
  processing = false;
  timeRemaining: number | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentApi: PaymentApiService,
    private bookingApi: BookingApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const state = history.state as { booking?: Booking } | undefined;
    this.booking = state?.booking || null;
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.bookingId = parseInt(id, 10);
    if (!this.booking) {
      try {
        const stored = sessionStorage.getItem('booking_pay_' + this.bookingId);
        if (stored) this.booking = JSON.parse(stored);
      } catch {
        // ignore
      }
    }
    if (!this.booking) {
      this.bookingApi.getBooking(this.bookingId).subscribe({ next: (b) => (this.booking = b) });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  initiatePayment(): void {
    const userId = this.authService.getUserId();
    if (!userId || !this.booking) return;
    this.initiating = true;
    this.error = '';
    this.paymentApi.initiatePayment(userId).subscribe({
      next: (res) => {
        this.paymentId = res.transactionId;
        this.initiating = false;
        this.statusMessage = 'Waiting for QR code...';
        this.startPolling();
      },
      error: (err) => {
        this.error = err.error?.message || err.message || 'Failed to initiate payment';
        this.initiating = false;
      },
    });
  }

  private startPolling(): void {
    if (!this.paymentId) return;
    interval(3000)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.paymentApi.getPaymentStatus(this.paymentId!))
      )
      .subscribe({
        next: (res) => {
          if (res.qrCode?.code) {
            this.qrCode = res.qrCode.code;
            this.qrImageUrl = 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=' + encodeURIComponent(res.qrCode.code);
            this.statusMessage = 'QR code ready. Scan or enter below to process.';
            this.destroy$.next();
          }
        },
        error: () => {},
      });
  }

  processPayment(): void {
    if (!this.booking || !this.paymentId) return;
    const code = this.qrCodeInput.trim() || this.qrCode;
    if (!code) {
      this.error = 'Please scan or enter the QR code';
      return;
    }
    this.processing = true;
    this.error = '';
    this.paymentApi
      .processPayment(this.paymentId, {
        qrCode: code,
        amount: this.booking.totalAmount,
        currency: 'USD',
        merchantId: MERCHANT_ID,
        description: `Hotel booking #${this.bookingId}`,
      })
      .subscribe({
        next: () => {
          this.bookingApi.completeBooking(this.bookingId).subscribe({
            next: () => {
              this.success = true;
              this.processing = false;
            },
            error: () => {
              this.success = true;
              this.processing = false;
            },
          });
        },
        error: (err) => {
          this.error = err.error?.message || err.message || 'Payment failed';
          this.bookingApi.cancelBooking(this.bookingId).subscribe();
          this.processing = false;
        },
      });
  }

  formatTime(sec: number): string {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  get showProcessForm(): boolean {
    return !!this.qrCode && !this.success;
  }
}

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, NgIf],
  template: `
    <div class="container" style="max-width: 400px;">
      <h1>Log in</h1>
      <p>Enter your phone number to receive a one-time code.</p>

      <div *ngIf="error" class="error-message">{{ error }}</div>
      <div *ngIf="message" class="success-message">{{ message }}</div>

      <form *ngIf="step === 'phone'" (ngSubmit)="requestOtp()" class="card">
        <div class="form-group">
          <label>Phone number</label>
          <input type="tel" [(ngModel)]="phoneNumber" name="phone" placeholder="+1234567890" required />
        </div>
        <div class="form-group">
          <label>Channel</label>
          <select [(ngModel)]="channel" name="channel">
            <option value="SMS">SMS</option>
            <option value="EMAIL">Email</option>
          </select>
        </div>
        <button type="submit" class="btn btn-primary" [disabled]="loading">{{ loading ? 'Sending...' : 'Send OTP' }}</button>
      </form>

      <form *ngIf="step === 'otp'" (ngSubmit)="verifyOtp()" class="card">
        <p class="text-sm">Code sent to {{ phoneNumber }}</p>
        <div class="form-group">
          <label>OTP code</label>
          <input type="text" inputmode="numeric" [(ngModel)]="code" name="code" placeholder="123456" maxlength="8" required />
        </div>
        <button type="submit" class="btn btn-primary" [disabled]="loading">{{ loading ? 'Verifying...' : 'Verify and log in' }}</button>
        <div style="display: flex; justify-content: space-between; margin-top: 1rem;">
          <button type="button" class="btn" (click)="step = 'phone'">Change number</button>
          <button type="button" class="btn" (click)="resend()" [disabled]="loading || resendCooldown > 0">
            {{ resendCooldown > 0 ? 'Resend in ' + resendCooldown + 's' : 'Resend OTP' }}
          </button>
        </div>
      </form>

      <p style="margin-top: 1rem;">
        <a routerLink="/">Back to home</a>
      </p>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  step: 'phone' | 'otp' = 'phone';
  phoneNumber = '';
  code = '';
  channel: 'SMS' | 'EMAIL' = 'SMS';
  loading = false;
  error = '';
  message = '';
  resendCooldown = 0;
  private redirect = '/';

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.route.queryParams.subscribe((qp) => {
      this.redirect = qp['redirect'] || '/';
    });
  }

  requestOtp(): void {
    this.error = '';
    this.message = '';
    if (!this.phoneNumber.trim()) return;
    this.loading = true;
    this.auth.requestOtp(this.phoneNumber.trim(), this.channel).subscribe({
      next: () => {
        this.message = 'OTP sent. Check your ' + (this.channel === 'SMS' ? 'phone' : 'email') + '.';
        this.step = 'otp';
        this.code = '';
        this.resendCooldown = 60;
        const interval = setInterval(() => {
          this.resendCooldown--;
          if (this.resendCooldown <= 0) clearInterval(interval);
        }, 1000);
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || err.message || 'Failed to send OTP';
        this.loading = false;
      },
    });
  }

  verifyOtp(): void {
    this.error = '';
    if (!this.code.trim()) return;
    this.loading = true;
    this.auth.verifyOtp(this.phoneNumber.trim(), this.code.trim()).subscribe({
      next: () => {
        this.router.navigateByUrl(this.redirect);
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || err.message || 'Invalid or expired OTP';
        this.loading = false;
      },
    });
  }

  resend(): void {
    if (this.resendCooldown > 0) return;
    this.error = '';
    this.message = '';
    this.loading = true;
    this.auth.requestOtp(this.phoneNumber.trim(), this.channel).subscribe({
      next: () => {
        this.message = 'OTP sent again.';
        this.resendCooldown = 60;
        const interval = setInterval(() => {
          this.resendCooldown--;
          if (this.resendCooldown <= 0) clearInterval(interval);
        }, 1000);
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to resend';
        this.loading = false;
      },
    });
  }
}

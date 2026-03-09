import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BookingApiService, Booking } from '../../services/booking-api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-booking-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="container">
      <h1>My Bookings</h1>
      @if (loading) {
        <p>Loading...</p>
      } @else if (!loading && bookings.length === 0) {
        <p>No bookings yet.</p>
      } @else {
        <div class="booking-list">
          @for (b of bookings; track b.id) {
            <div class="card booking-card">
              <div class="booking-header">
                <h3>{{ b.hotelName || 'Hotel' }} - {{ b.roomName || 'Room' }}</h3>
                <span class="status status-{{ b.status ? b.status.toLowerCase() : '' }}">{{ b.status }}</span>
              </div>
              <p>Check-in: {{ b.checkIn }} · Check-out: {{ b.checkOut }}</p>
              <p><strong>Total: \${{ b.totalAmount }}</strong></p>
              <div class="actions">
                @if (b.status === 'PENDING') {
                  <a [routerLink]="['/bookings', b.id, 'pay']" class="btn btn-primary">Pay Now</a>
                  <button class="btn btn-danger" (click)="cancel(b)" [disabled]="cancelling === b.id">
                    {{ cancelling === b.id ? 'Cancelling...' : 'Cancel' }}
                  </button>
                }
              </div>
            </div>
          }
        </div>
      }
      <a routerLink="/hotels" class="btn btn-primary" style="margin-top: 1rem;">Browse Hotels</a>
    </div>
  `,
  styles: [`
    .booking-list { display: flex; flex-direction: column; gap: 1rem; }
    .booking-header { display: flex; justify-content: space-between; align-items: center; }
    .status { padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.75rem; }
    .status-pending { background: #fef3c7; color: #92400e; }
    .status-complete { background: #d1fae5; color: #065f46; }
    .status-cancelled { background: #fee2e2; color: #991b1b; }
    .actions { display: flex; gap: 0.5rem; margin-top: 0.5rem; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingListComponent implements OnInit {
  bookings: Booking[] = [];
  loading = true;
  cancelling: number | null = null;

  constructor(
    private bookingApi: BookingApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    this.bookingApi.getBookings(userId ?? undefined).subscribe({
      next: (list) => {
        this.bookings = list;
        this.loading = false;
      },
      error: () => {
        this.bookings = [];
        this.loading = false;
      },
    });
  }

  cancel(b: Booking): void {
    this.cancelling = b.id;
    this.bookingApi.cancelBooking(b.id).subscribe({
      next: () => {
        b.status = 'CANCELLED';
        this.cancelling = null;
      },
      error: () => {
        this.cancelling = null;
      },
    });
  }
}

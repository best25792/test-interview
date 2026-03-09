import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterLink } from '@angular/router';
import { BookingApiService, CreateBookingRequest } from '../../services/booking-api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-booking-new',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="container">
      <h1>Create Booking</h1>
      @if (loading) {
        <p>Creating booking...</p>
      }
      @if (error) {
        <div class="error-message">{{ error }}</div>
      }
      @if (!loading && !booking) {
        <div>
          <p>Missing booking parameters. Please select a room from a hotel.</p>
          <a routerLink="/hotels">Browse Hotels</a>
        </div>
      }
      @if (booking && !loading) {
        <div>
          <div class="card">
            <p><strong>Hotel:</strong> {{ booking!.hotelName || 'Hotel' }}</p>
            <p><strong>Room:</strong> {{ booking!.roomName || 'Room' }}</p>
            <p><strong>Check-in:</strong> {{ booking!.checkIn }}</p>
            <p><strong>Check-out:</strong> {{ booking!.checkOut }}</p>
            <p><strong>Total:</strong> \${{ booking!.totalAmount }}</p>
          </div>
          <p>Booking created. Redirecting to payment...</p>
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingNewComponent implements OnInit {
  loading = true;
  error = '';
  booking: { id: number; totalAmount: number; hotelName?: string; roomName?: string; checkIn: string; checkOut: string } | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookingApi: BookingApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const qp = this.route.snapshot.queryParams;
    const hotelId = parseInt(qp['hotelId'], 10);
    const roomId = parseInt(qp['roomId'], 10);
    const checkIn = qp['checkIn'];
    const checkOut = qp['checkOut'];
    const userId = this.authService.getUserId();

    if (!hotelId || !roomId || !checkIn || !checkOut || !userId) {
      this.loading = false;
      return;
    }

    const req: CreateBookingRequest = { hotelId, roomId, userId, checkIn, checkOut };
    this.bookingApi.createBooking(req).subscribe({
      next: (b) => {
        this.booking = b;
        this.loading = false;
        try {
          sessionStorage.setItem('booking_pay_' + b.id, JSON.stringify(b));
        } catch {
          // ignore
        }
        this.router.navigate(['/bookings', b.id, 'pay'], { state: { booking: b } });
      },
      error: (err) => {
        this.error = err.message || 'Failed to create booking';
        this.loading = false;
      },
    });
  }
}

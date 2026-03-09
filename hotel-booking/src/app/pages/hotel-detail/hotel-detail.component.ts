import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HotelApiService, Hotel, Room } from '../../services/hotel-api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-hotel-detail',
  standalone: true,
  imports: [RouterLink, FormsModule],
  template: `
    <div class="container">
      <a routerLink="/hotels" [queryParams]="queryParams">← Back to search</a>
      @if (loading) {
        <div>Loading...</div>
      } @else if (!loading && hotel) {
        <div>
          <h1>{{ hotel!.name }}</h1>
          <p class="location">{{ hotel!.location }}</p>
          @if (hotel!.description) {
            <p>{{ hotel!.description }}</p>
          }
          @if (hotel!.amenities?.length) {
            <div class="amenities">
              @for (a of hotel!.amenities; track a) {
                <span class="badge">{{ a }}</span>
              }
            </div>
          }

          <h2>Rooms</h2>
          <div class="form-group">
            <label>Check-in</label>
            <input type="date" [(ngModel)]="checkIn" (ngModelChange)="onDatesChange()" />
          </div>
          <div class="form-group">
            <label>Check-out</label>
            <input type="date" [(ngModel)]="checkOut" (ngModelChange)="onDatesChange()" />
          </div>

          <div class="room-list">
            @for (r of rooms; track r.id) {
              <div class="card room-card">
                <h3>{{ r.name }}</h3>
                <p>{{ r.type }} · Up to {{ r.maxGuests }} guests</p>
                <p class="price">\${{ r.pricePerNight }}/night</p>
                <button class="btn btn-primary" (click)="bookRoom(r)" [disabled]="!canBook()">Book Now</button>
              </div>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .location { color: #6b7280; }
    .amenities { margin: 1rem 0; }
    .badge { display: inline-block; background: #e5e7eb; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.75rem; margin-right: 0.25rem; }
    .room-list { display: grid; gap: 1rem; margin-top: 1rem; }
    .room-card .price { font-weight: 600; }
    .room-card .btn { margin-top: 0.5rem; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HotelDetailComponent implements OnInit {
  hotel: Hotel | null = null;
  rooms: Room[] = [];
  loading = true;
  checkIn = '';
  checkOut = '';
  queryParams: Record<string, string> = {};
  hotelId = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private hotelApi: HotelApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((qp) => (this.queryParams = { ...qp }));
    this.route.paramMap.subscribe((pm) => {
      const id = pm.get('id');
      if (!id) return;
      this.hotelId = parseInt(id, 10);
      this.hotelApi.getHotel(this.hotelId).subscribe((h) => {
        this.hotel = h;
        this.rooms = h.rooms || [];
        this.loading = false;
      });
      const qp = this.route.snapshot.queryParams;
      this.checkIn = qp['checkIn'] || '';
      this.checkOut = qp['checkOut'] || '';
      if (this.checkIn && this.checkOut) this.loadRooms();
    });
  }

  onDatesChange(): void {
    this.loadRooms();
  }

  loadRooms(): void {
    if (!this.hotelId || !this.checkIn || !this.checkOut) return;
    this.hotelApi.getRooms(this.hotelId, this.checkIn, this.checkOut).subscribe((r) => (this.rooms = r));
  }

  canBook(): boolean {
    return !!this.checkIn && !!this.checkOut && new Date(this.checkOut) > new Date(this.checkIn);
  }

  bookRoom(room: Room): void {
    if (!this.canBook()) return;
    const userId = this.authService.getUserId();
    if (!userId) {
      this.router.navigate(['/login'], { queryParams: { redirect: this.router.url } });
      return;
    }
    this.router.navigate(['/bookings/new'], {
      queryParams: {
        hotelId: this.hotelId,
        roomId: room.id,
        checkIn: this.checkIn,
        checkOut: this.checkOut,
      },
    });
  }
}

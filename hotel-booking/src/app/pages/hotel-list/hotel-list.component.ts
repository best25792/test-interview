import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HotelApiService, Hotel } from '../../services/hotel-api.service';

@Component({
  selector: 'app-hotel-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="container">
      <h1>Search Results</h1>
      @if (loading) {
        <p>Loading...</p>
      } @else if (!loading && hotels.length === 0) {
        <p>No hotels found. Try adjusting your search.</p>
      } @else {
        <div class="hotel-grid">
          @for (h of hotels; track h.id) {
            <div class="card hotel-card">
              <h3>{{ h.name }}</h3>
              <p class="location">{{ h.location }}</p>
              @if (h.description) {
                <p class="description">{{ h.description }}</p>
              }
              <p class="price">From {{ getMinPrice(h) }}/night</p>
              @if (h.amenities?.length) {
                <div class="amenities">
                  @for (a of h.amenities; track a) {
                    <span class="badge">{{ a }}</span>
                  }
                </div>
              }
              <a [routerLink]="['/hotels', h.id]" [queryParams]="queryParams" class="btn btn-primary">View Details</a>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .hotel-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.5rem; }
    .hotel-card h3 { margin: 0 0 0.25rem 0; }
    .location { color: #6b7280; font-size: 0.875rem; margin: 0 0 0.5rem 0; }
    .description { font-size: 0.875rem; margin-bottom: 0.5rem; }
    .price { font-weight: 600; margin: 0.5rem 0; }
    .amenities { margin: 0.5rem 0; }
    .badge { display: inline-block; background: #e5e7eb; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.75rem; margin-right: 0.25rem; }
    .hotel-card .btn { margin-top: 1rem; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HotelListComponent implements OnInit {
  hotels: Hotel[] = [];
  loading = true;
  queryParams: Record<string, string> = {};

  getMinPrice(h: Hotel): string {
    const price = h.minPrice != null ? h.minPrice : 0;
    return `$${price}`;
  }

  constructor(
    private hotelApi: HotelApiService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((qp) => {
      this.queryParams = { ...qp };
      const params = {
        name: qp['name'] || undefined,
        location: qp['location'] || undefined,
        minPrice: qp['minPrice'] ? Number(qp['minPrice']) : undefined,
        maxPrice: qp['maxPrice'] ? Number(qp['maxPrice']) : undefined,
        checkIn: qp['checkIn'] || undefined,
        checkOut: qp['checkOut'] || undefined,
      };
      this.hotelApi.search(params).subscribe((list) => {
        this.hotels = list;
        this.loading = false;
      });
    });
  }
}

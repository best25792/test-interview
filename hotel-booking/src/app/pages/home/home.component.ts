import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="container">
      <h1>Find Your Perfect Hotel</h1>
      <p class="subtitle">Search by name, location, price, and available dates</p>

      <form class="search-form card" (ngSubmit)="search()">
        <div class="form-row">
          <div class="form-group">
            <label>Hotel Name</label>
            <input type="text" [(ngModel)]="name" name="name" placeholder="e.g. Grand Hotel" />
          </div>
          <div class="form-group">
            <label>Location</label>
            <input type="text" [(ngModel)]="location" name="location" placeholder="e.g. Bangkok" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Min Price (per night)</label>
            <input type="number" [(ngModel)]="minPrice" name="minPrice" placeholder="0" min="0" step="1" />
          </div>
          <div class="form-group">
            <label>Max Price (per night)</label>
            <input type="number" [(ngModel)]="maxPrice" name="maxPrice" placeholder="1000" min="0" step="1" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Check-in</label>
            <input type="date" [(ngModel)]="checkIn" name="checkIn" />
          </div>
          <div class="form-group">
            <label>Check-out</label>
            <input type="date" [(ngModel)]="checkOut" name="checkOut" />
          </div>
        </div>
        <button type="submit" class="btn btn-primary">Search Hotels</button>
      </form>
    </div>
  `,
  styles: [`
    h1 { font-size: 2rem; margin-bottom: 0.5rem; }
    .subtitle { color: #6b7280; margin-bottom: 2rem; }
    .search-form { max-width: 700px; }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    @media (max-width: 600px) { .form-row { grid-template-columns: 1fr; } }
  `],
})
export class HomeComponent {
  name = '';
  location = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  checkIn = '';
  checkOut = '';

  constructor(private router: Router) {}

  search(): void {
    const params: Record<string, string> = {};
    if (this.name) params['name'] = this.name;
    if (this.location) params['location'] = this.location;
    if (this.minPrice != null) params['minPrice'] = String(this.minPrice);
    if (this.maxPrice != null) params['maxPrice'] = String(this.maxPrice);
    if (this.checkIn) params['checkIn'] = this.checkIn;
    if (this.checkOut) params['checkOut'] = this.checkOut;
    this.router.navigate(['/hotels'], { queryParams: params });
  }
}

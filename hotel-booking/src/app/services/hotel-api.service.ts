import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface HotelSearchParams {
  name?: string;
  location?: string;
  minPrice?: number;
  maxPrice?: number;
  checkIn?: string;
  checkOut?: string;
}

export interface Hotel {
  id: number;
  name: string;
  location: string;
  description?: string;
  amenities?: string[];
  minPrice?: number;
  rooms?: Room[];
}

export interface Room {
  id: number;
  hotelId: number;
  name: string;
  type: string;
  pricePerNight: number;
  maxGuests: number;
  available?: boolean;
}

@Injectable({ providedIn: 'root' })
export class HotelApiService {
  private baseUrl = environment.hotelServiceUrl;

  constructor(private http: HttpClient) {}

  search(params: HotelSearchParams): Observable<Hotel[]> {
    let httpParams = new HttpParams();
    if (params.name) httpParams = httpParams.set('name', params.name);
    if (params.location) httpParams = httpParams.set('location', params.location);
    if (params.minPrice != null) httpParams = httpParams.set('minPrice', params.minPrice);
    if (params.maxPrice != null) httpParams = httpParams.set('maxPrice', params.maxPrice);
    if (params.checkIn) httpParams = httpParams.set('checkIn', params.checkIn);
    if (params.checkOut) httpParams = httpParams.set('checkOut', params.checkOut);

    return this.http.get<Hotel[]>(`${this.baseUrl}/hotels/search`, { params: httpParams }).pipe(
      catchError(() => of(this.getMockHotels()))
    );
  }

  getHotel(id: number): Observable<Hotel> {
    return this.http.get<Hotel>(`${this.baseUrl}/hotels/${id}`).pipe(
      catchError(() => of(this.getMockHotel(id)))
    );
  }

  getRooms(hotelId: number, checkIn?: string, checkOut?: string): Observable<Room[]> {
    let params = new HttpParams();
    if (checkIn) params = params.set('checkIn', checkIn);
    if (checkOut) params = params.set('checkOut', checkOut);
    return this.http.get<Room[]>(`${this.baseUrl}/hotels/${hotelId}/rooms`, { params }).pipe(
      catchError(() => {
        const hotel = this.getMockHotel(hotelId);
        return of(hotel.rooms || []);
      })
    );
  }

  private getMockHotels(): Hotel[] {
    return [
      { id: 1, name: 'Grand Hotel', location: 'Bangkok', description: 'Luxury hotel in city center', amenities: ['WiFi', 'Pool', 'Spa'], minPrice: 150 },
      { id: 2, name: 'Beach Resort', location: 'Phuket', description: 'Beachfront paradise', amenities: ['WiFi', 'Pool', 'Beach'], minPrice: 200 },
      { id: 3, name: 'City Inn', location: 'Bangkok', description: 'Affordable city stay', amenities: ['WiFi'], minPrice: 80 },
    ];
  }

  private getMockHotel(id: number): Hotel {
    const hotels = this.getMockHotels();
    const h = hotels.find((x) => x.id === id) || hotels[0];
    return {
      ...h,
      id,
      rooms: [
        { id: 1, hotelId: id, name: 'Standard Room', type: 'STANDARD', pricePerNight: h.minPrice || 100, maxGuests: 2 },
        { id: 2, hotelId: id, name: 'Deluxe Room', type: 'DELUXE', pricePerNight: (h.minPrice || 100) * 1.5, maxGuests: 4 },
        { id: 3, hotelId: id, name: 'Suite', type: 'SUITE', pricePerNight: (h.minPrice || 100) * 2.5, maxGuests: 6 },
      ],
    };
  }
}

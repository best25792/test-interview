import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface CreateBookingRequest {
  hotelId: number;
  roomId: number;
  userId: number;
  checkIn: string;
  checkOut: string;
}

export interface Booking {
  id: number;
  hotelId: number;
  roomId: number;
  userId: number;
  checkIn: string;
  checkOut: string;
  status: string;
  totalAmount: number;
  paymentId: number | null;
  hotelName?: string;
  roomName?: string;
}

@Injectable({ providedIn: 'root' })
export class BookingApiService {
  private baseUrl = environment.hotelServiceUrl;

  constructor(private http: HttpClient) {}

  createBooking(req: CreateBookingRequest): Observable<Booking> {
    return this.http.post<Booking>(`${this.baseUrl}/bookings`, req).pipe(
      catchError((err) => {
        if (err.status === 0 || err.status === 404) {
          return of(this.mockCreateBooking(req));
        }
        return throwError(() => err);
      })
    );
  }

  getBookings(userId?: number, status?: string): Observable<Booking[]> {
    let params = new HttpParams();
    if (userId != null) params = params.set('userId', userId);
    if (status) params = params.set('status', status);
    return this.http.get<Booking[]>(`${this.baseUrl}/bookings`, { params }).pipe(
      catchError(() => of([]))
    );
  }

  getBooking(id: number): Observable<Booking> {
    return this.http.get<Booking>(`${this.baseUrl}/bookings/${id}`).pipe(
      catchError(() => of({ id, hotelId: 0, roomId: 0, userId: 0, checkIn: '', checkOut: '', status: 'PENDING', totalAmount: 0, paymentId: null } as Booking))
    );
  }

  completeBooking(id: number): Observable<Booking> {
    return this.http.patch<Booking>(`${this.baseUrl}/bookings/${id}/complete`, {}).pipe(
      catchError((err) => {
        if (err.status === 0 || err.status === 404) {
          return of({} as Booking);
        }
        return throwError(() => err);
      })
    );
  }

  cancelBooking(id: number): Observable<Booking> {
    return this.http.patch<Booking>(`${this.baseUrl}/bookings/${id}/cancel`, {}).pipe(
      catchError((err) => {
        if (err.status === 0 || err.status === 404) {
          return of({} as Booking);
        }
        return throwError(() => err);
      })
    );
  }

  private mockCreateBooking(req: CreateBookingRequest): Booking {
    const nights = Math.ceil((new Date(req.checkOut).getTime() - new Date(req.checkIn).getTime()) / (1000 * 60 * 60 * 24));
    const pricePerNight = 150;
    const totalAmount = Math.round(nights * pricePerNight * 100) / 100;
    return {
      id: Math.floor(Math.random() * 10000),
      hotelId: req.hotelId,
      roomId: req.roomId,
      userId: req.userId,
      checkIn: req.checkIn,
      checkOut: req.checkOut,
      status: 'PENDING',
      totalAmount,
      paymentId: null,
      hotelName: 'Grand Hotel',
      roomName: 'Deluxe Room',
    };
  }
}

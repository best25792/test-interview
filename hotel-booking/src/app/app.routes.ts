import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent) },
  { path: 'hotels', loadComponent: () => import('./pages/hotel-list/hotel-list.component').then(m => m.HotelListComponent) },
  { path: 'hotels/:id', loadComponent: () => import('./pages/hotel-detail/hotel-detail.component').then(m => m.HotelDetailComponent) },
  { path: 'bookings/new', loadComponent: () => import('./pages/booking-new/booking-new.component').then(m => m.BookingNewComponent), canActivate: [authGuard] },
  { path: 'bookings/:id/pay', loadComponent: () => import('./pages/booking-pay/booking-pay.component').then(m => m.BookingPayComponent), canActivate: [authGuard] },
  { path: 'bookings', loadComponent: () => import('./pages/booking-list/booking-list.component').then(m => m.BookingListComponent), canActivate: [authGuard] },
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: '**', redirectTo: '' },
];

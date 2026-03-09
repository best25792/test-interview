import { ChangeDetectionStrategy, Component, effect } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <header class="header">
      <div class="container">
        <a routerLink="/" class="logo">Hotel Booking</a>
        <nav>
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">Home</a>
          <a routerLink="/hotels" routerLinkActive="active">Hotels</a>
          @if (isLoggedIn) {
            <a routerLink="/bookings" routerLinkActive="active">My Bookings</a>
            <button (click)="logout()" class="btn btn-danger" style="margin-left: 0.5rem;">Logout</button>
          } @else {
            <a routerLink="/login">Login</a>
          }
        </nav>
      </div>
    </header>
    <main class="main">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .header {
      background: #1e3a5f;
      color: white;
      padding: 1rem 0;
    }
    .header .container {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .logo {
      font-size: 1.25rem;
      font-weight: 700;
      color: white;
    }
    .logo:hover { text-decoration: none; }
    nav a {
      color: rgba(255,255,255,0.9);
      margin-left: 1.5rem;
    }
    nav a:hover, nav a.active { color: white; }
    .main { padding: 2rem 0; min-height: calc(100vh - 120px); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  isLoggedIn = false;

  constructor(private authService: AuthService) {
    effect(() => {
      this.isLoggedIn = this.authService.isLoggedIn$();
    });
  }

  logout() {
    this.authService.logout();
  }
}

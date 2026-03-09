# Hotel Booking - Angular App

Angular frontend for hotel booking, integrated with the existing user-service (auth) and payment-service (QR payments).

## Features

- **Hotel search**: Filter by name, location, price range, available dates
- **Create booking**: Select room and dates, create booking
- **Payment**: QR flow - initiate payment, display QR, process with booking total
- **Cancel booking**: Cancel pending bookings (no refund)

## Run locally

```bash
npm install
npm start
```

Runs at http://localhost:4200.

## Build for production

```bash
npm run build
```

Output in `dist/hotel-booking/browser`.

## Docker

```bash
docker build -t hotel-booking .
docker run -p 4200:80 hotel-booking
```

Or with docker-compose: `docker compose up hotel-booking`

## Environment

API URLs are configured in `src/environments/`:

- `userServiceUrl`: User service (auth) - default http://localhost:8081/api/v1
- `paymentServiceUrl`: Payment service - default http://localhost:8083/api/v1
- `hotelServiceUrl`: Hotel service (to be implemented) - default http://localhost:8086/api/v1

## Backend

The hotel-service backend is specified in [`docs/hotel-booking-backend-spec.md`](../docs/hotel-booking-backend-spec.md). Until implemented, hotel search and booking creation use mock data; payment requires the existing payment-service.

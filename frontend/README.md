# Payment System Frontend

A Next.js frontend application for the E-Wallet QR Code Payment System.

## Features

- **User Management**: Create users, view user details
- **Wallet Management**: Check wallet balance, top up wallet, view available balance
- **Payment Processing**: Initiate payments, process QR codes, confirm payments, cancel and refund payments
- **Transaction History**: View transaction records and payment history
- **QR Code Display**: Visual QR code generation for payments

## Getting Started

### Prerequisites

- Node.js 18+ and npm (or yarn/pnpm)
- Backend server running on `http://localhost:8080`

### Installation

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. (Optional) Create a `.env.local` file to configure the API base URL:
   ```
   NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
   ```

4. Run the development server:
   ```bash
   npm run dev
   ```

5. Open [http://localhost:3000](http://localhost:3000) in your browser.

### Build for Production

```bash
npm run build
npm start
```

## Project Structure

```
frontend/
├── app/
│   ├── layout.tsx          # Root layout with navigation
│   ├── page.tsx            # Home page
│   ├── users/
│   │   └── page.tsx        # User management page
│   ├── payments/
│   │   └── page.tsx        # Payment management page
│   ├── transactions/
│   │   └── page.tsx        # Transaction history page
│   └── globals.css         # Global styles
├── lib/
│   └── api.ts              # API service layer
├── package.json
├── tsconfig.json
└── tailwind.config.ts
```

## API Integration

The frontend uses the API service layer in `lib/api.ts` which includes:

- **User API**: Create user, get user, wallet operations
- **Payment API**: Initiate, process, confirm, cancel, refund payments
- **QR Code API**: Get QR code details, validate QR codes
- **Transaction API**: Get transaction details, list transactions

## Usage

### User Management

1. **Create User**: Navigate to Users → Create User tab
   - Fill in username, email, phone number
   - Set active/verified status
   - Click "Create User"

2. **View User**: Navigate to Users → View User tab
   - Enter user ID
   - Click "Get User" to view details

3. **Wallet Management**: Navigate to Users → Wallet Management tab
   - View wallet balance and available balance
   - Top up wallet with any amount

### Payment Flow

1. **Initiate Payment**: Navigate to Payments → Initiate Payment
   - Enter user ID
   - Click "Initiate Payment"
   - QR code will be displayed for the merchant

2. **Process Payment**: Navigate to Payments → Process Payment (Merchant)
   - Enter payment ID and QR code details
   - Enter amount, currency, merchant ID
   - Click "Process Payment"

3. **Confirm Payment**: Navigate to Payments → View/Manage Payment
   - Enter payment ID
   - Click "Confirm Payment" to complete the payment

### Transaction History

1. Navigate to Transactions
2. Enter transaction ID to view details, or
3. Use filters (Payment ID, Type) to list transactions

## Technologies

- **Next.js 14**: React framework with App Router
- **TypeScript**: Type-safe JavaScript
- **Tailwind CSS**: Utility-first CSS framework
- **Axios**: HTTP client for API calls
- **qrcode.react**: QR code generation library

## API Endpoints

The frontend integrates with the following backend endpoints:

- `POST /api/v1/users` - Create user
- `GET /api/v1/users/{id}` - Get user
- `GET /api/v1/users/{id}/wallet/balance` - Get wallet balance
- `GET /api/v1/users/{id}/wallet/available` - Get available balance
- `POST /api/v1/users/{id}/wallet/topup` - Top up wallet
- `POST /api/v1/payments/initiate` - Initiate payment
- `GET /api/v1/payments/{id}` - Get payment
- `POST /api/v1/payments/{id}/process` - Process payment
- `POST /api/v1/payments/{id}/confirm` - Confirm payment
- `POST /api/v1/payments/{id}/cancel` - Cancel payment
- `POST /api/v1/payments/{id}/refund` - Refund payment
- `GET /api/v1/transactions` - List transactions
- `GET /api/v1/transactions/{id}` - Get transaction

## Development

- The app uses TypeScript for type safety
- Tailwind CSS for styling
- All API calls are centralized in `lib/api.ts`
- Components are organized by feature in the `app` directory

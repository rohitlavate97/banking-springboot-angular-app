export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  role: string;
  email: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface Account {
  id: string;
  accountNumber: string;
  accountType: 'CHECKING' | 'SAVINGS' | 'FIXED_DEPOSIT';
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'CLOSED';
  balance: number;
  availableBalance: number;
  currency: string;
  alias?: string;
  createdAt: string;
}

export interface Transaction {
  id: string;
  referenceNumber: string;
  userId: string;
  sourceAccountNumber?: string;
  destinationAccountNumber?: string;
  type: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER';
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'ROLLED_BACK';
  amount: number;
  currency: string;
  description?: string;
  correlationId: string;
  failureReason?: string;
  createdAt: string;
}

export interface TransactionRequest {
  sourceAccountNumber?: string;
  destinationAccountNumber?: string;
  amount: number;
  currency?: string;
  description?: string;
}

export interface Beneficiary {
  id: string;
  nickname: string;
  accountNumber: string;
  accountHolderName: string;
  bankName: string;
  bankCode?: string;
  currency: string;
  active: boolean;
  createdAt: string;
}

export interface UserProfile {
  id: string;
  authUserId: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  kycStatus: 'PENDING' | 'SUBMITTED' | 'VERIFIED' | 'REJECTED';
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

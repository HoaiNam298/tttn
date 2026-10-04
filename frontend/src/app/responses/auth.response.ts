import { AuthUser } from '../models/auth-user.model';

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

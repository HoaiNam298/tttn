import { AuthUser } from '../models/auth-user.model';

export interface AuthResponse {
  accessToken: string;
  user: AuthUser;
}

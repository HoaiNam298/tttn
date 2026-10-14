export interface StoredAuthSession {
  id: string;
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
}

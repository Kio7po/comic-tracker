export type UserRole = 'USER' | 'ADMIN';

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  displayName: string;
  role: UserRole;
}

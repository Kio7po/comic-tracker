export type UserRole = 'USER' | 'ADMIN';

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  displayName: string;
  biography: string | null;
  pictureUrl: string | null;
  locale: string | null;
  role: UserRole;
}

export type UserRole = "TUTOR" | "STUDENT";

export interface AuthSession {
  token: string;
  userId: string;
  name: string;
  email: string;
  role: UserRole;
}

export type UserRole = "TUTOR" | "STUDENT" | "ADMIN";

export interface AuthSession {
  token: string;
  userId: string;
  name: string;
  email: string;
  role: UserRole;
}

export interface Student {
  id: string;
  name: string;
  email: string;
  createdAt: string;
}

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  createdAt: string;
}

export type UserRole = "TUTOR" | "STUDENT" | "ADMIN";

export interface AuthSession {
  token: string;
  userId: string;
  name: string;
  email: string;
  role: UserRole;
}

// Single combined Google sign-in-or-sign-up response: an existing account logs straight in
// (session populated); a brand-new account with no role chosen yet comes back with needsRole
// true and session null, so the UI can ask for a role and re-send the same idToken.
export interface GoogleAuthResult {
  needsRole: boolean;
  session: AuthSession | null;
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

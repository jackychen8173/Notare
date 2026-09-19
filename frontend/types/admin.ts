export interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: "TUTOR" | "STUDENT";
  active: boolean;
  createdAt: string;
}

export interface AdminDashboardStats {
  tutorCount: number;
  studentCount: number;
  courseCount: number;
  submissionsPending: number;
  submissionsReleased: number;
}

export interface Assignment {
  id: string;
  courseId: string;
  courseName: string;
  title: string;
  description: string | null;
  dueDate: string;
  topicId: string | null;
  topicName: string | null;
  gradeCategoryId: string | null;
  gradeCategoryName: string | null;
}

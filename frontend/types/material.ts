export interface Material {
  id: string;
  courseId: string;
  topicId: string | null;
  topicName: string | null;
  title: string;
  description: string | null;
  url: string | null;
  createdAt: string;
}

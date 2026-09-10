export interface RubricCriterion {
  id: string;
  name: string;
  description: string | null;
  pointsPossible: number;
  position: number;
}

export interface Rubric {
  id: string;
  assignmentId: string;
  title: string;
  criteria: RubricCriterion[];
}

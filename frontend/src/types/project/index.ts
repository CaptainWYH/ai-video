export interface Project {
  id: number;
  name: string;
  description?: string | null;
  script?: string | null;
  storyboard?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectPayload {
  name: string;
  description?: string;
  script?: string;
  storyboard?: string;
}

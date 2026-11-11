/** An uploaded file's details. Download it through openFile (hooks/useFiles). */
export interface StoredFileInfo {
  id: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
}

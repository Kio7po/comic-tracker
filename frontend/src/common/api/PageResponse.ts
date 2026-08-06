export interface PageResponse<T> {
  items: T[];
  existMoreItems: boolean;
  totalItems: number | null;
}

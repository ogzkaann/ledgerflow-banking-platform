import { ChevronLeft, ChevronRight } from "lucide-react";

interface PaginationProps {
  readonly page: number;
  readonly totalPages: number;
  readonly totalElements: number;
  readonly onPageChange: (page: number) => void;
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onPageChange,
}: PaginationProps) {
  if (totalPages <= 1) {
    return <p className="pagination__summary">{totalElements} result{totalElements === 1 ? "" : "s"}</p>;
  }
  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        className="button button--secondary"
        type="button"
        disabled={page <= 0}
        onClick={() => onPageChange(page - 1)}
      >
        <ChevronLeft aria-hidden="true" />
        Previous
      </button>
      <span>
        Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong> · {totalElements} results
      </span>
      <button
        className="button button--secondary"
        type="button"
        disabled={page + 1 >= totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        Next
        <ChevronRight aria-hidden="true" />
      </button>
    </nav>
  );
}

import { ArrowLeft } from "lucide-react";
import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <section className="not-found">
      <span className="eyebrow">404 · Route not found</span>
      <h1>This operations view does not exist.</h1>
      <p>The requested route is not part of the LedgerFlow console.</p>
      <Link className="button button--primary" to="/">
        <ArrowLeft aria-hidden="true" />
        Return to overview
      </Link>
    </section>
  );
}

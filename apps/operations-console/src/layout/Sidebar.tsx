import {
  Bell,
  Building2,
  FlaskConical,
  Gauge,
  Landmark,
  Network,
  X,
} from "lucide-react";
import { NavLink } from "react-router-dom";

const navigation: ReadonlyArray<{
  readonly to: string;
  readonly label: string;
  readonly icon: typeof Gauge;
  readonly end?: boolean;
}> = [
  { to: "/", label: "Overview", icon: Gauge, end: true },
  { to: "/accounts", label: "Accounts", icon: Landmark },
  { to: "/transfers", label: "Transfers", icon: Building2 },
  { to: "/notifications", label: "Notifications", icon: Bell },
  { to: "/demo", label: "Demo Lab", icon: FlaskConical },
  { to: "/system", label: "System", icon: Network },
];

interface SidebarProps {
  readonly open: boolean;
  readonly onClose: () => void;
}

export function Sidebar({ open, onClose }: SidebarProps) {
  return (
    <>
      <aside className={`sidebar ${open ? "sidebar--open" : ""}`} aria-label="Primary navigation">
        <div className="sidebar__brand">
          <span className="brand-mark" aria-hidden="true">
            LF
          </span>
          <div>
            <strong>LedgerFlow</strong>
            <span>Operations console</span>
          </div>
          <button className="icon-button sidebar__close" type="button" onClick={onClose}>
            <X aria-hidden="true" />
            <span className="sr-only">Close navigation</span>
          </button>
        </div>
        <nav className="sidebar__nav">
          {navigation.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              onClick={onClose}
              className={({ isActive }) => (isActive ? "nav-link nav-link--active" : "nav-link")}
            >
              <Icon aria-hidden="true" />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar__proof">
          <span className="sidebar__proof-dot" aria-hidden="true" />
          <div>
            <strong>Real workflow</strong>
            <span>Gateway · Kafka · PostgreSQL</span>
          </div>
        </div>
      </aside>
      {open && (
        <button
          className="sidebar-backdrop"
          type="button"
          aria-label="Close navigation"
          onClick={onClose}
        />
      )}
    </>
  );
}

import { LogOut, Menu, ShieldCheck } from "lucide-react";
import { useAuth } from "react-oidc-context";
import { appConfig } from "@/config";
import { roleLabel, type Permissions } from "@/auth/permissions";

interface TopBarProps {
  readonly permissions: Permissions;
  readonly onOpenNavigation: () => void;
}

export function TopBar({ permissions, onOpenNavigation }: TopBarProps) {
  const auth = useAuth();
  const preferredUsername = auth.user?.profile.preferred_username;
  const userLabel =
    typeof preferredUsername === "string" ? preferredUsername : (auth.user?.profile.sub ?? "Signed in");

  return (
    <header className="topbar">
      <button className="icon-button topbar__menu" type="button" onClick={onOpenNavigation}>
        <Menu aria-hidden="true" />
        <span className="sr-only">Open navigation</span>
      </button>
      <div className="topbar__environment">
        <span className="environment-badge">{appConfig.environment}</span>
        <span className="session-indicator">
          <span aria-hidden="true" />
          Session active
        </span>
      </div>
      <div className="topbar__user">
        <ShieldCheck aria-hidden="true" />
        <div>
          <strong>{userLabel}</strong>
          <span>{roleLabel(permissions.role)}</span>
        </div>
        <button
          className="button button--quiet"
          type="button"
          onClick={() => void auth.signoutRedirect()}
        >
          <LogOut aria-hidden="true" />
          Logout
        </button>
      </div>
    </header>
  );
}

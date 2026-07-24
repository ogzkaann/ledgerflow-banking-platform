import { useState } from "react";
import { Outlet } from "react-router-dom";
import { usePermissions } from "@/auth/usePermissions";
import { Sidebar } from "./Sidebar";
import { TopBar } from "./TopBar";

export function AppShell() {
  const [navigationOpen, setNavigationOpen] = useState(false);
  const permissions = usePermissions();

  return (
    <div className="app-shell">
      <Sidebar open={navigationOpen} onClose={() => setNavigationOpen(false)} />
      <div className="app-shell__main">
        <TopBar permissions={permissions} onOpenNavigation={() => setNavigationOpen(true)} />
        <main id="main-content" className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

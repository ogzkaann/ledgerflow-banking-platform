import type { PropsWithChildren } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { usePermissions } from "./usePermissions";

export function WritePermissionGate({ children }: PropsWithChildren) {
  const permissions = usePermissions();
  const location = useLocation();
  return permissions.canCreate ? children : <Navigate to="/" replace state={{ denied: location.pathname }} />;
}

export function AdminPermissionGate({ children }: PropsWithChildren) {
  const permissions = usePermissions();
  const location = useLocation();
  return permissions.canFundDemo ? children : <Navigate to="/" replace state={{ denied: location.pathname }} />;
}

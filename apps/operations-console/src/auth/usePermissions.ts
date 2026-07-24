import { useAuth } from "react-oidc-context";
import { permissionsFor } from "./permissions";

export function usePermissions() {
  const auth = useAuth();
  return permissionsFor(auth.user?.profile);
}

import { useAuth } from "react-oidc-context";
import { permissionsFor, tokenProfile } from "./permissions";

export function usePermissions() {
  const auth = useAuth();
  const profilePermissions = permissionsFor(auth.user?.profile);
  return profilePermissions.canRead
    ? profilePermissions
    : permissionsFor(tokenProfile(auth.user?.access_token));
}

import { useEffect, useState, type PropsWithChildren, type ReactNode } from "react";
import { useAuth } from "react-oidc-context";
import { LogIn, ShieldAlert } from "lucide-react";
import { setAccessTokenProvider } from "@/api/clients";

function FullPageStatus({
  title,
  detail,
  action,
}: {
  readonly title: string;
  readonly detail: string;
  readonly action?: ReactNode;
}) {
  return (
    <main className="auth-status">
      <div className="auth-status__brand">
        <span className="brand-mark" aria-hidden="true">
          LF
        </span>
        <span>LedgerFlow Operations</span>
      </div>
      <section className="auth-status__panel" aria-live="polite">
        <ShieldAlert aria-hidden="true" />
        <h1>{title}</h1>
        <p>{detail}</p>
        {action}
      </section>
    </main>
  );
}

export function AuthGate({ children }: PropsWithChildren) {
  const auth = useAuth();
  const [sessionExpired, setSessionExpired] = useState(false);

  useEffect(() => setAccessTokenProvider(() => auth.user?.access_token), [auth.user?.access_token]);

  useEffect(() => {
    const expire = () => setSessionExpired(true);
    window.addEventListener("ledgerflow:session-expired", expire);
    return () => window.removeEventListener("ledgerflow:session-expired", expire);
  }, []);

  useEffect(() => {
    if (
      !sessionExpired &&
      !auth.isLoading &&
      !auth.isAuthenticated &&
      !auth.activeNavigator &&
      !auth.error
    ) {
      void auth.signinRedirect({
        state: { returnTo: `${window.location.pathname}${window.location.search}` },
      });
    }
  }, [auth, sessionExpired]);

  if (sessionExpired) {
    return (
      <FullPageStatus
        title="Your session has expired"
        detail="LedgerFlow removed the in-memory session from active use. Sign in again to continue."
        action={
          <button
            className="button button--primary"
            type="button"
            onClick={() => {
              setSessionExpired(false);
              void auth.removeUser().then(() =>
                auth.signinRedirect({
                  state: { returnTo: `${window.location.pathname}${window.location.search}` },
                }),
              );
            }}
          >
            <LogIn aria-hidden="true" />
            Sign in again
          </button>
        }
      />
    );
  }

  if (auth.error) {
    return (
      <FullPageStatus
        title="Authentication could not be completed"
        detail={auth.error.message}
        action={
          <button
            className="button button--primary"
            type="button"
            onClick={() =>
              void auth.signinRedirect({
                state: { returnTo: `${window.location.pathname}${window.location.search}` },
              })
            }
          >
            <LogIn aria-hidden="true" />
            Try sign-in again
          </button>
        }
      />
    );
  }

  if (auth.isLoading || auth.activeNavigator || !auth.isAuthenticated) {
    return (
      <FullPageStatus
        title="Connecting to LedgerFlow identity"
        detail="Redirecting securely to Keycloak using Authorization Code flow with PKCE."
      />
    );
  }

  return children;
}

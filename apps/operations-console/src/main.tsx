import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "react-oidc-context";
import { App } from "./App";
import { AuthGate } from "./auth/AuthGate";
import { authConfig } from "./auth/auth-config";
import { FeedbackProvider } from "./feedback/FeedbackContext";
import "./styles.css";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        const status =
          error && typeof error === "object" && "problem" in error
            ? (error as { problem?: { status?: number } }).problem?.status
            : undefined;
        if (status === 401 || status === 403 || status === 404 || status === 429) return false;
        return failureCount < 2;
      },
      staleTime: 5_000,
      refetchOnWindowFocus: true,
    },
    mutations: {
      retry: false,
    },
  },
});

const rootElement = document.getElementById("root");
if (!rootElement) throw new Error("LedgerFlow root element is missing");

createRoot(rootElement).render(
  <StrictMode>
    <AuthProvider {...authConfig}>
      <AuthGate>
        <QueryClientProvider client={queryClient}>
          <FeedbackProvider>
            <BrowserRouter>
              <App />
            </BrowserRouter>
          </FeedbackProvider>
        </QueryClientProvider>
      </AuthGate>
    </AuthProvider>
  </StrictMode>,
);

import { vi } from "vitest";

type DemoRole = "ledgerflow-admin" | "ledgerflow-operator" | "ledgerflow-auditor";

export const mockAuth = {
  isAuthenticated: true,
  isLoading: false,
  activeNavigator: undefined,
  error: undefined,
  user: {
    access_token: "test-token-never-logged",
    profile: {
      sub: "test-user",
      preferred_username: "test-user",
      realm_access: { roles: ["ledgerflow-admin"] as DemoRole[] },
    },
  },
  signinRedirect: vi.fn(),
  signoutRedirect: vi.fn(),
  removeUser: vi.fn().mockResolvedValue(undefined),
};

export function setMockRole(role: DemoRole) {
  mockAuth.user.profile.realm_access.roles = [role];
}

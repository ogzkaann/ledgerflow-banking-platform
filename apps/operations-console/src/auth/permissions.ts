export const ledgerflowRoles = [
  "ledgerflow-operator",
  "ledgerflow-auditor",
  "ledgerflow-admin",
] as const;

export type LedgerFlowRole = (typeof ledgerflowRoles)[number];

export interface Permissions {
  readonly role: LedgerFlowRole | null;
  readonly canRead: boolean;
  readonly canCreate: boolean;
  readonly canFundDemo: boolean;
}

function isRole(value: unknown): value is LedgerFlowRole {
  return typeof value === "string" && ledgerflowRoles.some((role) => role === value);
}

export function realmRoles(profile: unknown): LedgerFlowRole[] {
  if (!profile || typeof profile !== "object") return [];
  const access = (profile as Record<string, unknown>).realm_access;
  if (!access || typeof access !== "object") return [];
  const roles = (access as Record<string, unknown>).roles;
  return Array.isArray(roles) ? roles.filter(isRole) : [];
}

export function permissionsFor(profile: unknown): Permissions {
  const roles = realmRoles(profile);
  const role =
    roles.find((candidate) => candidate === "ledgerflow-admin") ??
    roles.find((candidate) => candidate === "ledgerflow-operator") ??
    roles.find((candidate) => candidate === "ledgerflow-auditor") ??
    null;
  return {
    role,
    canRead: role !== null,
    canCreate: role === "ledgerflow-admin" || role === "ledgerflow-operator",
    canFundDemo: role === "ledgerflow-admin",
  };
}

export function roleLabel(role: LedgerFlowRole | null): string {
  if (!role) return "No LedgerFlow role";
  return role.replace("ledgerflow-", "").replace(/^\w/, (letter) => letter.toUpperCase());
}

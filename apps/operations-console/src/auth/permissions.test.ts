import { permissionsFor, realmRoles, roleLabel, tokenProfile } from "./permissions";

describe("realm role mapping", () => {
  it("gives admin the complete UI permission set", () => {
    expect(permissionsFor({ realm_access: { roles: ["ledgerflow-admin"] } })).toEqual({
      role: "ledgerflow-admin", canRead: true, canCreate: true, canFundDemo: true,
    });
  });

  it("reads signed-token claims for UI hints when the ID-token profile omits roles", () => {
    const payload = btoa(JSON.stringify({ realm_access: { roles: ["ledgerflow-operator"] } }))
      .replaceAll("+", "-")
      .replaceAll("/", "_")
      .replaceAll("=", "");
    expect(permissionsFor(tokenProfile(`header.${payload}.signature`))).toMatchObject({
      role: "ledgerflow-operator",
      canCreate: true,
    });
    expect(tokenProfile("malformed")).toBeUndefined();
  });

  it("keeps auditors read-only and ignores unrelated roles", () => {
    expect(permissionsFor({ realm_access: { roles: ["offline_access", "ledgerflow-auditor"] } })).toMatchObject({ role: "ledgerflow-auditor", canRead: true, canCreate: false, canFundDemo: false });
    expect(realmRoles({ realm_access: { roles: ["offline_access"] } })).toEqual([]);
    expect(roleLabel("ledgerflow-auditor")).toBe("Auditor");
  });
});

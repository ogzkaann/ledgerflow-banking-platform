import { permissionsFor, realmRoles, roleLabel } from "./permissions";

describe("realm role mapping", () => {
  it("gives admin the complete UI permission set", () => {
    expect(permissionsFor({ realm_access: { roles: ["ledgerflow-admin"] } })).toEqual({
      role: "ledgerflow-admin", canRead: true, canCreate: true, canFundDemo: true,
    });
  });

  it("keeps auditors read-only and ignores unrelated roles", () => {
    expect(permissionsFor({ realm_access: { roles: ["offline_access", "ledgerflow-auditor"] } })).toMatchObject({ role: "ledgerflow-auditor", canRead: true, canCreate: false, canFundDemo: false });
    expect(realmRoles({ realm_access: { roles: ["offline_access"] } })).toEqual([]);
    expect(roleLabel("ledgerflow-auditor")).toBe("Auditor");
  });
});
